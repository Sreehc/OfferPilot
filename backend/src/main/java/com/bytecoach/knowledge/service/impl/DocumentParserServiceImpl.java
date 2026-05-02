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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class DocumentParserServiceImpl implements DocumentParserService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("md", "markdown", "txt", "text");
    private static final int MAX_CHUNK_SIZE = 800;
    private static final int MIN_CHUNK_SIZE = 50;

    @Override
    public List<String> parse(InputStream inputStream, String fileName) {
        String extension = getExtension(fileName);
        String content = readAll(inputStream);
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        return switch (extension) {
            case "md", "markdown" -> splitMarkdown(content);
            case "txt", "text" -> splitPlainText(content);
            default -> splitPlainText(content);
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
     * Split Markdown by headings, preserving heading context in each chunk.
     */
    private List<String> splitMarkdown(String content) {
        List<String> chunks = new ArrayList<>();
        String[] lines = content.split("\n");
        StringBuilder current = new StringBuilder();
        String currentHeading = "";

        for (String line : lines) {
            if (line.matches("^#{1,4}\\s+.*")) {
                // New heading: flush current chunk if it has content
                if (current.length() >= MIN_CHUNK_SIZE) {
                    chunks.add(current.toString().trim());
                } else if (current.length() > 0) {
                    // Merge small chunk with next
                    currentHeading = current.toString().trim();
                }
                current = new StringBuilder();
                current.append(line).append("\n");
            } else {
                current.append(line).append("\n");
                // Split large chunks at paragraph boundaries
                if (current.length() >= MAX_CHUNK_SIZE && line.isBlank()) {
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
            // Append small trailing chunk to last chunk
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
            if (current.length() + para.length() > MAX_CHUNK_SIZE && current.length() >= MIN_CHUNK_SIZE) {
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
