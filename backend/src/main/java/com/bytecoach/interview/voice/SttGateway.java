package com.bytecoach.interview.voice;

import java.math.BigDecimal;

/**
 * Speech-to-Text gateway interface.
 * Supports audio transcription for voice-based interviews.
 */
public interface SttGateway {

    /**
     * Transcribe audio bytes to text.
     *
     * @param audioData raw audio bytes (WAV/MP3/WEBM)
     * @param mimeType  audio MIME type (e.g. "audio/webm", "audio/wav")
     * @return transcription result
     */
    SttResult transcribe(byte[] audioData, String mimeType);

    /**
     * Check if the STT service is available.
     */
    boolean isAvailable();

    record SttResult(
            String text,
            BigDecimal confidence,
            int processingTimeMs
    ) {}
}
