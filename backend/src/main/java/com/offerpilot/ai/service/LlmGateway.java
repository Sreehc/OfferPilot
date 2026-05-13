package com.offerpilot.ai.service;

import com.offerpilot.ai.dto.AiChatRequest;
import com.offerpilot.ai.dto.AiChatResponse;
import java.util.function.Consumer;

public interface LlmGateway {
    AiChatResponse chatCompletion(AiChatRequest request);

    /**
     * Streaming chat completion. Calls onToken for each token received,
     * then returns the full accumulated response.
     */
    AiChatResponse streamCompletion(AiChatRequest request, Consumer<String> onToken);
}
