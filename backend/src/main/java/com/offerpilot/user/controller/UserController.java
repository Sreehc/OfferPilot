package com.offerpilot.user.controller;

import com.offerpilot.common.api.Result;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.common.storage.FileStorageService;
import com.offerpilot.common.storage.StorageDirectory;
import com.offerpilot.common.storage.StoredFile;
import com.offerpilot.common.storage.UploadPolicyService;
import com.offerpilot.security.util.SecurityUtils;
import com.offerpilot.user.entity.User;
import com.offerpilot.user.mapper.UserMapper;
import com.offerpilot.user.service.UserService;
import com.offerpilot.user.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
    private final FileStorageService fileStorageService;
    private final UploadPolicyService uploadPolicyService;

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

        try {
            uploadPolicyService.validate(StorageDirectory.AVATAR, file.getOriginalFilename(), file.getContentType(), file.getSize());
            StoredFile storedFile = fileStorageService.store(
                    StorageDirectory.AVATAR,
                    file.getOriginalFilename(),
                    file.getBytes(),
                    file.getContentType());
            String avatarUrl = storedFile.getAccessUrl();
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setAvatar(avatarUrl);
                userMapper.updateById(user);
            }

            return Result.success(avatarUrl);
        } catch (java.io.IOException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "头像上传失败: " + e.getMessage());
        }
    }
}
