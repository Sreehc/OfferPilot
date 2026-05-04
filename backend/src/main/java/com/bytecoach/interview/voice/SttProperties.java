package com.bytecoach.interview.voice;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bytecoach.stt")
public class SttProperties {
    /** STT API base URL (e.g. https://api.openai.com/v1). */
    private String baseUrl;

    /** STT API key. */
    private String apiKey;

    /** Model name (e.g. whisper-1). */
    private String model = "whisper-1";

    /** Language hint for STT (e.g. zh, en). */
    private String language = "zh";

    /** Max audio file size in bytes (default 25MB). */
    private long maxAudioSizeBytes = 25 * 1024 * 1024;
}
