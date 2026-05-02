package com.bytecoach.user.controller;

import com.bytecoach.common.api.Result;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.security.util.SecurityUtils;
import com.bytecoach.user.entity.User;
import com.bytecoach.user.mapper.UserMapper;
import com.bytecoach.user.service.UserService;
import com.bytecoach.user.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "用户信息", description = "当前用户信息查询")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @Value("${file.upload-dir:./uploads/avatars}")
    private String uploadDir;

    @Value("${file.base-url:http://localhost:8080/uploads/avatars}")
    private String baseUrl;

    @Operation(summary = "当前用户", description = "获取当前登录用户的详细信息")
    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.success(userService.getCurrentUserInfo());
    }

    @Operation(summary = "上传头像", description = "上传用户头像图片")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅支持图片文件");
        }

        // Validate file size (max 2MB)
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "图片大小不能超过 2MB");
        }

        try {
            Path dir = Paths.get(uploadDir);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            String ext = contentType.replace("image/", ".");
            String filename = "avatar_" + userId + "_" + UUID.randomUUID() + ext;
            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());

            String avatarUrl = baseUrl + "/" + filename;
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setAvatar(avatarUrl);
                userMapper.updateById(user);
            }

            return Result.success(avatarUrl);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "头像上传失败: " + e.getMessage());
        }
    }
}

