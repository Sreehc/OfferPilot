package com.offerpilot.common.storage;

import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.config.OfferPilotProperties;
import com.offerpilot.common.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private final OfferPilotProperties offerPilotProperties;

    @Override
    public StoredFile store(StorageDirectory directory, String originalFilename, byte[] content, String contentType) {
        try {
            String extension = resolveExtension(originalFilename);
            LocalDate today = LocalDate.now();
            String relativePath = Paths.get(
                            directory.getCode(),
                            String.valueOf(today.getYear()),
                            String.format("%02d", today.getMonthValue()),
                            String.format("%02d", today.getDayOfMonth()),
                            UUID.randomUUID() + extension)
                    .toString()
                    .replace('\\', '/');
            Path root = resolveRoot();
            Path target = root.resolve(relativePath).normalize();
            Files.createDirectories(target.getParent());
            Files.write(target, content);

            return StoredFile.builder()
                    .storageKey("local://" + relativePath)
                    .relativePath(relativePath)
                    .accessUrl(directory.isPublicReadable()
                            ? offerPilotProperties.getStorage().getPublicBaseUrl() + "/" + relativePath
                            : null)
                    .contentType(contentType)
                    .size(content.length)
                    .build();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "文件存储失败: " + e.getMessage());
        }
    }

    @Override
    public Resource loadPublic(String relativePath) {
        Path root = resolveRoot();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root) || !Files.exists(target) || !Files.isRegularFile(target)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "文件不存在");
        }
        if (!relativePath.startsWith(StorageDirectory.AVATAR.getCode() + "/")) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "该文件不允许公开访问");
        }
        return new FileSystemResource(target);
    }

    private Path resolveRoot() {
        return Paths.get(offerPilotProperties.getStorage().getLocalRoot()).toAbsolutePath().normalize();
    }

    private String resolveExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return "";
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        return extension.length() > 16 ? "" : extension;
    }
}
