package com.bytecoach.knowledge.service.impl;

import com.bytecoach.knowledge.service.DocumentParserService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.bytecoach.common.config.ByteCoachProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentParserServiceImpl implements DocumentParserService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("md", "markdown", "txt", "text", "pdf");
    private static final int MIN_CHUNK_SIZE = 50;

    private final ByteCoachProperties props;

    @Override
    public List<String> parse(InputStream inputStream, String fileName) {
        String extension = getExtension(fileName);
        return switch (extension) {
            case "md", "markdown" -> {
                String content = readAll(inputStream);
                yield StringUtils.hasText(content) ? splitMarkdown(content) : List.of();
            }
            case "txt", "text" -> {
                String content = readAll(inputStream);
                yield StringUtils.hasText(content) ? splitPlainText(content) : List.of();
            }
            case "pdf" -> {
                String content = extractPdfText(inputStream);
                yield StringUtils.hasText(content) ? splitPlainText(content) : List.of();
            }
            default -> {
                String content = readAll(inputStream);
                yield StringUtils.hasText(content) ? splitPlainText(content) : List.of();
            }
        };
    }

    @Override
    public boolean isSupported(String fileName) {
        return SUPPORTED_EXTENSIONS.contains(getExtension(fileName).toLowerCase());
    }

    @Override
    public List<String> supportedExtensions() {
        return new ArrayList<>(SUPPORTED_EXTENSIONS);
    }

    /**
     * Extract text from PDF using Apache Tika.
     */
    private String extractPdfText(InputStream inputStream) {
        try {
            BodyContentHandler handler = new BodyContentHandler(props.getDocument().getTikaMaxChars());
            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(inputStream, handler, metadata, new ParseContext());
            String text = handler.toString();
            log.info("PDF parsed: {} chars extracted, metadata={}", text.length(), metadata.toString());
            return text;
        } catch (Exception e) {
            log.error("PDF parsing failed: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Split Markdown by headings, preserving heading context in each chunk.
     */
    private List<String> splitMarkdown(String content) {
        List<String> chunks = new ArrayList<>();
        String[] lines = content.split("\n");
        StringBuilder current = new StringBuilder();
        String currentHeading = "";

        for (String line : lines) {
            if (line.matches("^#{1,4}\\s+.*")) {
                if (current.length() >= MIN_CHUNK_SIZE) {
                    chunks.add(current.toString().trim());
                } else if (current.length() > 0) {
                    currentHeading = current.toString().trim();
                }
                current = new StringBuilder();
                current.append(line).append("\n");
            } else {
                current.append(line).append("\n");
                if (current.length() >= props.getDocument().getMaxChunkSize() && line.isBlank()) {
                    chunks.add(current.toString().trim());
                    current = new StringBuilder();
                    if (StringUtils.hasText(currentHeading)) {
                        current.append(currentHeading).append("\n\n");
                    }
                }
            }
        }
        if (current.length() >= MIN_CHUNK_SIZE) {
            chunks.add(current.toString().trim());
        } else if (current.length() > 0 && !chunks.isEmpty()) {
            String last = chunks.remove(chunks.size() - 1);
            chunks.add(last + "\n\n" + current.toString().trim());
        }
        return chunks;
    }

    /**
     * Split plain text by double newlines (paragraphs), merging small paragraphs.
     */
    private List<String> splitPlainText(String content) {
        List<String> paragraphs = Arrays.stream(content.split("\n\\s*\n"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String para : paragraphs) {
            if (current.length() + para.length() > props.getDocument().getMaxChunkSize() && current.length() >= MIN_CHUNK_SIZE) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
            }
            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(para);
        }
        if (current.length() >= MIN_CHUNK_SIZE) {
            chunks.add(current.toString().trim());
        } else if (current.length() > 0 && !chunks.isEmpty()) {
            String last = chunks.remove(chunks.size() - 1);
            chunks.add(last + "\n\n" + current.toString().trim());
        }
        return chunks;
    }

    private String readAll(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            log.error("Failed to read document stream: {}", e.getMessage());
            return "";
        }
    }

    private String getExtension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
