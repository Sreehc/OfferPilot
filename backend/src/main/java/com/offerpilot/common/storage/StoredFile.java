package com.offerpilot.common.storage;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StoredFile {
    private String storageKey;
    private String relativePath;
    private String accessUrl;
    private String contentType;
    private long size;
}
