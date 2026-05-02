package com.bytecoach.ai.service;

import java.util.List;

public interface EmbeddingGateway {
    /**
     * Generate embedding vector for a single text.
     * @return float array of dimensions specified in config, or empty array if disabled
     */
    float[] embed(String text);

    /**
     * Generate embedding vectors for multiple texts in one batch call.
     * @return list of float arrays, same order as input
     */
    List<float[]> embedBatch(List<String> texts);
}
