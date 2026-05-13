package com.offerpilot.interview.voice;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Whisper-compatible STT gateway.
 * Calls OpenAI-style audio transcription API.
 *
 * Configuration via application.yml:
 *   offerpilot.stt.base-url: https://api.openai.com/v1
 *   offerpilot.stt.api-key: sk-...
 *   offerpilot.stt.model: whisper-1
 *   offerpilot.stt.language: zh
 *
 * When base-url or api-key is not set, isAvailable() returns false
 * and the voice interview feature will be gracefully disabled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WhisperSttGateway implements SttGateway {

    private final SttProperties sttProperties;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public boolean isAvailable() {
        return sttProperties.getBaseUrl() != null && !sttProperties.getBaseUrl().isBlank()
                && sttProperties.getApiKey() != null && !sttProperties.getApiKey().isBlank();
    }

    @Override
    public SttResult transcribe(byte[] audioData, String mimeType) {
        if (!isAvailable()) {
            throw new IllegalStateException("STT service is not configured. Set offerpilot.stt.base-url and offerpilot.stt.api-key.");
        }

        if (audioData.length > sttProperties.getMaxAudioSizeBytes()) {
            throw new IllegalArgumentException("Audio file exceeds max size of " + (sttProperties.getMaxAudioSizeBytes() / 1024 / 1024) + "MB");
        }

        long startTime = System.currentTimeMillis();

        try {
            // Build multipart form data for Whisper API
            String boundary = "----OfferPilotSttBoundary" + System.currentTimeMillis();
            String fileName = mimeType.contains("webm") ? "audio.webm" : "audio.wav";

            byte[] body = buildMultipartBody(boundary, audioData, fileName, mimeType);

            String url = sttProperties.getBaseUrl().replaceAll("/+$", "")
                    + "/audio/transcriptions";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + sttProperties.getApiKey())
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int elapsed = (int) (System.currentTimeMillis() - startTime);

            if (response.statusCode() != 200) {
                log.error("STT API returned {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("STT transcription failed with status " + response.statusCode());
            }

            // Parse simple JSON response: {"text": "...", ...}
            String responseBody = response.body();
            String text = extractJsonField(responseBody, "text");

            // Whisper doesn't return confidence in the standard API,
            // estimate based on text length (longer = more confident)
            BigDecimal confidence = text.length() > 10 ? new BigDecimal("0.85") : new BigDecimal("0.60");

            return new SttResult(text, confidence, elapsed);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("STT request interrupted", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("STT transcription failed", e);
            throw new RuntimeException("STT transcription failed: " + e.getMessage(), e);
        }
    }

    private byte[] buildMultipartBody(String boundary, byte[] audioData, String fileName, String mimeType) {
        var baos = new java.io.ByteArrayOutputStream();
        String crlf = "\r\n";

        try {
            // File part
            baos.write(("--" + boundary + crlf).getBytes());
            baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + crlf).getBytes());
            baos.write(("Content-Type: " + mimeType + crlf).getBytes());
            baos.write(crlf.getBytes());
            baos.write(audioData);
            baos.write(crlf.getBytes());

            // Model part
            baos.write(("--" + boundary + crlf).getBytes());
            baos.write(("Content-Disposition: form-data; name=\"model\"" + crlf).getBytes());
            baos.write(crlf.getBytes());
            baos.write(sttProperties.getModel().getBytes());
            baos.write(crlf.getBytes());

            // Language part
            if (sttProperties.getLanguage() != null && !sttProperties.getLanguage().isBlank()) {
                baos.write(("--" + boundary + crlf).getBytes());
                baos.write(("Content-Disposition: form-data; name=\"language\"" + crlf).getBytes());
                baos.write(crlf.getBytes());
                baos.write(sttProperties.getLanguage().getBytes());
                baos.write(crlf.getBytes());
            }

            // Response format
            baos.write(("--" + boundary + crlf).getBytes());
            baos.write(("Content-Disposition: form-data; name=\"response_format\"" + crlf).getBytes());
            baos.write(crlf.getBytes());
            baos.write("json".getBytes());
            baos.write(crlf.getBytes());

            // Closing boundary
            baos.write(("--" + boundary + "--" + crlf).getBytes());

        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to build multipart body", e);
        }

        return baos.toByteArray();
    }

    private String extractJsonField(String json, String field) {
        // Simple JSON parser for {"text": "..."} — avoids external dependency
        String search = "\"" + field + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return "";
        int colonIdx = json.indexOf(':', idx + search.length());
        if (colonIdx < 0) return "";
        int startQuote = json.indexOf('"', colonIdx + 1);
        if (startQuote < 0) return "";
        int endQuote = json.indexOf('"', startQuote + 1);
        if (endQuote < 0) return "";
        return json.substring(startQuote + 1, endQuote);
    }
}
