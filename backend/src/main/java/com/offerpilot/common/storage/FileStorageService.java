package com.offerpilot.common.storage;

import org.springframework.core.io.Resource;

public interface FileStorageService {

    StoredFile store(StorageDirectory directory, String originalFilename, byte[] content, String contentType);

    Resource loadPrivate(String storageKey);

    Resource loadPublic(String relativePath);
}
