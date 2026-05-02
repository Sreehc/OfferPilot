package com.bytecoach.knowledge.service.impl;

import com.bytecoach.ai.config.VectorProperties;
import com.bytecoach.knowledge.service.VectorStoreService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.SafeEncoder;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisVectorStoreService implements VectorStoreService {

    private final VectorProperties vectorProperties;
    private final RedisProperties redisProperties;

    private JedisPooled jedis;

    @PostConstruct
    public void init() {
        String host = redisProperties.getHost() != null ? redisProperties.getHost() : "127.0.0.1";
        int port = redisProperties.getPort() != 0 ? redisProperties.getPort() : 6379;
        String password = redisProperties.getPassword();
        int database = redisProperties.getDatabase();

        DefaultJedisClientConfig.Builder configBuilder = DefaultJedisClientConfig.builder()
                .database(database);
        if (password != null && !password.isEmpty()) {
            configBuilder.password(password);
        }
        jedis = new JedisPooled(new HostAndPort(host, port), configBuilder.build());
        log.info("Redis VectorStore initialized: host={}, port={}, db={}", host, port, database);
        ensureIndex();
    }

    @PreDestroy
    public void destroy() {
        if (jedis != null) {
            jedis.close();
        }
    }

    @Override
    public void ensureIndex() {
        String indexName = vectorProperties.getIndexName();
        try {
            jedis.ftInfo(indexName);
            log.info("Redis vector index '{}' already exists", indexName);
        } catch (JedisDataException e) {
            if (e.getMessage() != null && e.getMessage().contains("Unknown Index")) {
                createIndex(indexName);
            } else {
                log.error("Failed to check Redis index: {}", e.getMessage());
            }
        }
    }

    private void createIndex(String indexName) {
        try {
            // Use raw Connection to send FT.CREATE — avoids Jedis 5.x search API changes
            String[] args = {
                    indexName,
                    "PREFIX", "1", "bytecoach:chunk:",
                    "SCHEMA",
                    "chunkId", "NUMERIC",
                    "docId", "NUMERIC",
                    "content", "TEXT", "WEIGHT", "1.0",
                    "embedding", "VECTOR", "HNSW", "6",
                    "TYPE", "FLOAT32",
                    "DIM", "1536",
                    "DISTANCE_METRIC", "COSINE",
                    "M", "16",
                    "EF_CONSTRUCTION", "200"
            };
            ProtocolCommand ftCreate = () -> SafeEncoder.encode("FT.CREATE");
            try (Connection conn = jedis.getPool().getResource()) {
                conn.sendCommand(ftCreate, SafeEncoder.encodeMany(args));
                Object result = conn.getOne();
                log.info("Created Redis vector index '{}': {}", indexName, result);
            }
        } catch (Exception e) {
            log.error("Failed to create Redis vector index: {}", e.getMessage());
        }
    }

    @Override
    public String store(Long chunkId, Long docId, String content, float[] embedding) {
        String key = "bytecoach:chunk:" + chunkId;
        byte[] embeddingBytes = floatArrayToBytes(embedding);
        jedis.hset(key, "chunkId", String.valueOf(chunkId));
        jedis.hset(key, "docId", String.valueOf(docId));
        jedis.hset(key, "content", content != null ? content : "");
        jedis.hset(SafeEncoder.encode(key), SafeEncoder.encode("embedding"), embeddingBytes);
        return key;
    }

    @Override
    public void remove(Long chunkId) {
        jedis.del("bytecoach:chunk:" + chunkId);
    }

    @Override
    public void removeByDocId(Long docId) {
        // Scan and delete all chunks belonging to this doc
        ScanParams params = new ScanParams().match("bytecoach:chunk:*").count(100);
        String cursor = "0";
        do {
            ScanResult<String> result = jedis.scan(cursor, params);
            cursor = result.getCursor();
            for (String key : result.getResult()) {
                String storedDocId = jedis.hget(key, "docId");
                if (storedDocId != null && storedDocId.equals(String.valueOf(docId))) {
                    jedis.del(key);
                }
            }
        } while (!"0".equals(cursor));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<VectorSearchResult> search(float[] queryEmbedding, int limit) {
        String indexName = vectorProperties.getIndexName();
        byte[] queryBytes = floatArrayToBytes(queryEmbedding);

        String query = "*=>[KNN " + limit + " @embedding $BLOB AS score]";
        try {
            var result = jedis.ftSearch(indexName, query,
                    redis.clients.jedis.search.SearchParams.searchParams()
                            .addParam("BLOB", queryBytes)
                            .setSortBy("score", true)
                            .setLimit(0, limit)
                            .setNoContent(false));

            List<VectorSearchResult> results = new ArrayList<>();
            var docs = result.getDocuments();
            if (docs == null) return results;

            for (var doc : docs) {
                var properties = doc.getProperties();
                if (properties == null) continue;

                Long chunkId = null;
                Long docId = null;
                float score = 0f;

                for (var prop : properties) {
                    switch (prop.getKey()) {
                        case "chunkId" -> chunkId = Long.parseLong(prop.getValue().toString());
                        case "docId" -> docId = Long.parseLong(prop.getValue().toString());
                        case "score" -> {
                            try {
                                // RediSearch returns distance; similarity = 1 - distance for COSINE
                                float distance = Float.parseFloat(prop.getValue().toString());
                                score = 1f - distance;
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
                if (chunkId != null && docId != null) {
                    results.add(new VectorSearchResult(chunkId, docId, score));
                }
            }
            return results;
        } catch (Exception e) {
            log.warn("Vector search failed, returning empty results: {}", e.getMessage());
            return List.of();
        }
    }

    private byte[] floatArrayToBytes(float[] floats) {
        ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : floats) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }
}
