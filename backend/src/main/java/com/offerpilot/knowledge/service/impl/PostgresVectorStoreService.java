package com.offerpilot.knowledge.service.impl;

import com.offerpilot.ai.config.VectorProperties;
import com.offerpilot.knowledge.service.VectorStoreService;
import jakarta.annotation.PostConstruct;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class PostgresVectorStoreService implements VectorStoreService {

    private final VectorProperties vectorProperties;
    private final DataSource dataSource;

    @PostConstruct
    public void init() {
        if (vectorProperties.isInitializeSchema()) {
            ensureIndex();
        }
    }

    @Override
    public void ensureIndex() {
        VectorProperties.Postgres properties = vectorProperties.getPostgres();
        String qualifiedTable = qualifiedTable();
        String createTableSql = """
                CREATE TABLE IF NOT EXISTS %s (
                    chunk_id BIGINT PRIMARY KEY,
                    doc_id BIGINT NOT NULL,
                    embedding vector(%d) NOT NULL,
                    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )
                """.formatted(qualifiedTable, vectorProperties.getDimensions());
        String createDocIndexSql = "CREATE INDEX IF NOT EXISTS idx_knowledge_embedding_doc_id ON %s (doc_id)"
                .formatted(qualifiedTable);
        String createVectorIndexSql = "CREATE INDEX IF NOT EXISTS idx_knowledge_embedding_hnsw ON %s USING hnsw (embedding vector_cosine_ops)"
                .formatted(qualifiedTable);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + properties.getSchema());
            statement.execute(createTableSql);
            statement.execute(createDocIndexSql);
            statement.execute(createVectorIndexSql);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize PostgreSQL vector store", e);
        }
    }

    @Override
    public String store(Long chunkId, Long docId, String content, float[] embedding) {
        validateDimensions(embedding);
        String sql = """
                INSERT INTO %s (chunk_id, doc_id, embedding, update_time)
                VALUES (?, ?, CAST(? AS vector), NOW())
                ON CONFLICT (chunk_id)
                DO UPDATE SET doc_id = EXCLUDED.doc_id,
                              embedding = EXCLUDED.embedding,
                              update_time = NOW()
                """.formatted(qualifiedTable());
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, chunkId);
            statement.setLong(2, docId);
            statement.setString(3, toVectorLiteral(embedding));
            statement.executeUpdate();
            return chunkId.toString();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to store vector in PostgreSQL", e);
        }
    }

    @Override
    public void remove(Long chunkId) {
        executeUpdate("DELETE FROM %s WHERE chunk_id = ?".formatted(qualifiedTable()), chunkId);
    }

    @Override
    public void removeByDocId(Long docId) {
        executeUpdate("DELETE FROM %s WHERE doc_id = ?".formatted(qualifiedTable()), docId);
    }

    @Override
    public List<VectorSearchResult> search(float[] queryEmbedding, int limit) {
        validateDimensions(queryEmbedding);
        String sql = """
                SELECT chunk_id, doc_id, 1 - (embedding <=> CAST(? AS vector)) AS score
                FROM %s
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """.formatted(qualifiedTable());
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String vectorLiteral = toVectorLiteral(queryEmbedding);
            statement.setString(1, vectorLiteral);
            statement.setString(2, vectorLiteral);
            statement.setInt(3, limit);

            List<VectorSearchResult> results = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(new VectorSearchResult(
                            resultSet.getLong("chunk_id"),
                            resultSet.getLong("doc_id"),
                            resultSet.getFloat("score")));
                }
            }
            return results;
        } catch (SQLException e) {
            log.warn("PostgreSQL vector search failed, returning empty results: {}", e.getMessage());
            return List.of();
        }
    }

    private void executeUpdate(String sql, Long id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update PostgreSQL vector store", e);
        }
    }

    private void validateDimensions(float[] embedding) {
        Integer expected = vectorProperties.getDimensions();
        if (expected == null || expected <= 0) {
            throw new IllegalStateException("Vector dimensions must be configured");
        }
        if (embedding.length != expected) {
            throw new IllegalArgumentException("Embedding dimension mismatch: expected %d but got %d"
                    .formatted(expected, embedding.length));
        }
    }

    private String toVectorLiteral(float[] embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(embedding[i]);
        }
        return builder.append(']').toString();
    }

    private String qualifiedTable() {
        VectorProperties.Postgres properties = vectorProperties.getPostgres();
        return properties.getSchema() + "." + properties.getTable();
    }
}
