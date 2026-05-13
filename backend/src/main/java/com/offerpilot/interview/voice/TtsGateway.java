package com.offerpilot.interview.voice;

/**
 * Text-to-Speech gateway interface.
 * Converts AI comments and follow-up questions to audio for voice interviews.
 */
public interface TtsGateway {

    /**
     * Synthesize text to audio bytes.
     *
     * @param text the text to speak
     * @return audio data (MP3 format), or null if TTS is not available
     */
    byte[] synthesize(String text);

    /**
     * Check if the TTS service is available.
     */
    boolean isAvailable();
}
