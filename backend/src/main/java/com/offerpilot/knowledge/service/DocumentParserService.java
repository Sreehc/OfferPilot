package com.offerpilot.knowledge.service;

import java.io.InputStream;
import java.util.List;

public interface DocumentParserService {

    /**
     * Parse a document into text chunks.
     * @param inputStream the file content
     * @param fileName the original file name (used to determine format)
     * @return list of text chunks
     */
    List<String> parse(InputStream inputStream, String fileName);

    /**
     * Check if a file format is supported.
     */
    boolean isSupported(String fileName);

    /**
     * Supported file extensions.
     */
    List<String> supportedExtensions();
}
