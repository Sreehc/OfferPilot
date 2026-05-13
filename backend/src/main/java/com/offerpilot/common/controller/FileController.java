package com.offerpilot.common.controller;

import com.offerpilot.common.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "文件访问", description = "公开文件访问接口")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "访问公开文件", description = "当前仅用于公开头像资源")
    @GetMapping("/public/**")
    public ResponseEntity<Resource> getPublicFile(HttpServletRequest request) throws IOException {
        String prefix = "/api/files/public/";
        String requestUri = request.getRequestURI();
        String relativePath = requestUri.substring(requestUri.indexOf(prefix) + prefix.length());
        Resource resource = fileStorageService.loadPublic(relativePath);
        String contentType = Files.probeContentType(resource.getFile().toPath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
