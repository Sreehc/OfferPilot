package com.offerpilot.knowledge.service;

import java.util.List;

public interface VectorStoreService {

    /**
     * Ensure the vector index exists in Redis. Called at application startup.
     */
    void ensureIndex();

    /**
     * Store a vector for a knowledge chunk.
     * @param chunkId the knowledge_chunk.id
     * @param docId the knowledge_doc.id
     * @param content the chunk text content
     * @param embedding the embedding float array
     * @return the Redis key used for storage
     */
    String store(Long chunkId, Long docId, String content, float[] embedding);

    /**
     * Remove a vector by chunk ID.
     */
    void remove(Long chunkId);

    /**
     * Remove all vectors for a document.
     */
    void removeByDocId(Long docId);

    /**
     * Search for similar chunks using vector similarity.
     * @param queryEmbedding the query embedding
     * @param limit max results
     * @return list of search results sorted by similarity descending
     */
    List<VectorSearchResult> search(float[] queryEmbedding, int limit);

    record VectorSearchResult(Long chunkId, Long docId, float score) {
    }
}
