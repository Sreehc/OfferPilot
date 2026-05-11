package com.bytecoach.knowledge.service.impl;

import com.bytecoach.ai.config.EmbeddingProperties;
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

    private final EmbeddingProperties embeddingProperties;
    private final VectorProperties vectorProperties;
    private final RedisProperties redisProperties;

    private JedisPooled jedis;

    @PostConstruct
    public void init() {
        if (!embeddingProperties.isEnabled()) {
            log.info("Embedding disabled, skipping Redis vector store initialization");
            return;
        }

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
            ProtocolCommand ftInfo = () -> SafeEncoder.encode("FT.INFO");
            try (Connection conn = jedis.getPool().getResource()) {
                conn.sendCommand(ftInfo, SafeEncoder.encode(indexName));
                conn.getOne();
            }
            log.info("Redis vector index '{}' already exists", indexName);
        } catch (JedisDataException e) {
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.contains("Unknown Index") || message.contains("Unknown index name")) {
                createIndex(indexName);
            } else {
                log.error("Failed to check Redis index: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to check Redis index: {}", e.getMessage());
            throw e;
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
        if (jedis == null) {
            return "bytecoach:chunk:" + chunkId;
        }
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
        if (jedis == null) {
            return;
        }
        jedis.del("bytecoach:chunk:" + chunkId);
    }

    @Override
    public void removeByDocId(Long docId) {
        if (jedis == null) {
            return;
        }
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
        if (jedis == null) {
            return List.of();
        }
        String indexName = vectorProperties.getIndexName();
        byte[] queryBytes = floatArrayToBytes(queryEmbedding);
        String query = "*=>[KNN " + limit + " @embedding $BLOB AS score]";

        try {
            // Build raw FT.SEARCH command with binary PARAMS support
            byte[][] args = new byte[][]{
                    SafeEncoder.encode("FT.SEARCH"),
                    SafeEncoder.encode(indexName),
                    SafeEncoder.encode(query),
                    SafeEncoder.encode("PARAMS"), SafeEncoder.encode("2"),
                    SafeEncoder.encode("BLOB"), queryBytes,
                    SafeEncoder.encode("SORTBY"), SafeEncoder.encode("score"),
                    SafeEncoder.encode("LIMIT"), SafeEncoder.encode("0"),
                    SafeEncoder.encode(String.valueOf(limit))
            };

            ProtocolCommand ftSearch = () -> SafeEncoder.encode("FT.SEARCH");
            List<Object> result;
            try (Connection conn = jedis.getPool().getResource()) {
                conn.sendCommand(ftSearch, args);
                result = (List<Object>) conn.getOne();
            }

            // Response: [count, id1, [field, val, ...], id2, [field, val, ...], ...]
            List<VectorSearchResult> results = new ArrayList<>();
            if (result == null || result.size() < 2) return results;

            for (int i = 1; i + 1 < result.size(); i += 2) {
                List<Object> fields = (List<Object>) result.get(i + 1);
                if (fields == null) continue;

                Long chunkId = null;
                Long docId = null;
                float score = 0f;

                for (int j = 0; j + 1 < fields.size(); j += 2) {
                    String key = SafeEncoder.encode((byte[]) fields.get(j));
                    byte[] valBytes = (byte[]) fields.get(j + 1);
                    String val = SafeEncoder.encode(valBytes);
                    switch (key) {
                        case "chunkId" -> chunkId = Long.parseLong(val);
                        case "docId" -> docId = Long.parseLong(val);
                        case "score" -> {
                            try {
                                float distance = Float.parseFloat(val);
                                score = 1f - distance; // COSINE: similarity = 1 - distance
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
