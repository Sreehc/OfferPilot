package com.bytecoach.interview.voice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * TTS gateway stub.
 * Voice playback is handled on the frontend using the Web Speech API (speechSynthesis).
 * This gateway exists for future server-side TTS integration (e.g. Azure TTS, Google TTS).
 *
 * When a server-side TTS provider is configured, implement this interface
 * and replace the @Primary annotation to serve audio from the backend.
 */
@Slf4j
@Component
public class WebSpeechTtsGateway implements TtsGateway {

    @Override
    public boolean isAvailable() {
        // Frontend handles TTS via Web Speech API
        return false;
    }

    @Override
    public byte[] synthesize(String text) {
        // Not implemented — frontend uses browser's speechSynthesis API
        log.debug("TTS synthesis requested but handled by frontend Web Speech API");
        return null;
    }
}
