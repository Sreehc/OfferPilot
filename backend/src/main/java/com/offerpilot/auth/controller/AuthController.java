package com.offerpilot.auth.controller;

import com.offerpilot.auth.dto.AuthDeliveryVO;
import com.offerpilot.auth.dto.CaptchaVO;
import com.offerpilot.auth.dto.ForgotPasswordRequest;
import com.offerpilot.auth.dto.LoginLogVO;
import com.offerpilot.auth.dto.LoginRequest;
import com.offerpilot.auth.dto.LoginResponse;
import com.offerpilot.auth.dto.OAuthProviderVO;
import com.offerpilot.auth.dto.RegisterRequest;
import com.offerpilot.auth.dto.ResetPasswordRequest;
import com.offerpilot.auth.dto.VerifyEmailRequest;
import com.offerpilot.auth.service.AuthService;
import com.offerpilot.auth.service.LoginLogService;
import com.offerpilot.common.api.Result;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.security.util.SecurityUtils;
import com.offerpilot.user.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Tag(name = "认证管理", description = "用户注册、登录、登出")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final AuthService authService;
    private final LoginLogService loginLogService;
    private final StringRedisTemplate redisTemplate;

    @Operation(summary = "用户注册", description = "注册新用户并返回 Token")
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "用户登录", description = "用户名密码登录，返回 JWT Token")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "发送邮箱验证码", description = "向当前账号邮箱发送验证验证码")
    @PostMapping("/email/send-verification-code")
    public Result<AuthDeliveryVO> sendEmailVerificationCode() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(authService.sendEmailVerificationCode(userId));
    }

    @Operation(summary = "验证邮箱验证码", description = "提交验证码并更新当前账号邮箱验证状态")
    @PostMapping("/email/verify")
    public Result<UserInfoVO> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(authService.verifyEmail(userId, request.getCode()));
    }

    @Operation(summary = "发送找回密码验证码", description = "向注册邮箱发送重置密码验证码")
    @PostMapping("/password/forgot")
    public Result<AuthDeliveryVO> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return Result.success(authService.sendPasswordResetCode(request.getEmail()));
    }

    @Operation(summary = "重置密码", description = "通过邮箱验证码重置密码")
    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return Result.success();
    }

    @Operation(summary = "第三方登录提供商列表", description = "返回当前预留的第三方登录入口状态")
    @GetMapping("/oauth/providers")
    public Result<List<OAuthProviderVO>> oauthProviders() {
        return Result.success(authService.listOAuthProviders());
    }

    @Operation(summary = "GitHub 登录回调预留", description = "预留 GitHub OAuth 回调路径，未启用时直接返回提示")
    @GetMapping("/oauth/github/callback")
    public Result<Void> githubCallback() {
        throw new com.offerpilot.common.exception.BusinessException(
                com.offerpilot.common.api.ResultCode.BAD_REQUEST.getCode(),
                "GitHub 登录尚未启用");
    }

    @Operation(summary = "用户登出", description = "将当前 Token 加入黑名单")
    @PostMapping("/logout")
    public Result<Void> logout(
            @Parameter(description = "Bearer Token") @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return Result.success();
    }

    @Operation(summary = "健康检查")
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("ok");
    }

    @Operation(summary = "查询当前用户登录日志", description = "分页查询当前用户的登录历史记录")
    @GetMapping("/login-logs")
    public Result<PageResult<LoginLogVO>> loginLogs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(loginLogService.listByUser(userId, pageNum, pageSize));
    }

    @Operation(summary = "获取图形验证码", description = "生成图形验证码，返回 Base64 图片和 key")
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        String code = generateCode();
        String key = UUID.randomUUID().toString().replace("-", "");

        // Store in Redis with 5 min TTL
        redisTemplate.opsForValue().set(CAPTCHA_PREFIX + key, code, 5, TimeUnit.MINUTES);

        String image = generateCaptchaImage(code);

        return Result.success(CaptchaVO.builder()
                .key(key)
                .image("data:image/png;base64," + image)
                .build());
    }

    private String generateCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(CAPTCHA_CHARS.charAt(random.nextInt(CAPTCHA_CHARS.length())));
        }
        return sb.toString();
    }

    private String generateCaptchaImage(String code) {
        int width = 120;
        int height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Background
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, width, height);

        // Noise lines
        Random random = new Random();
        g.setColor(new Color(200, 200, 200));
        for (int i = 0; i < 5; i++) {
            int x1 = random.nextInt(width), y1 = random.nextInt(height);
            int x2 = random.nextInt(width), y2 = random.nextInt(height);
            g.drawLine(x1, y1, x2, y2);
        }

        // Characters
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(random.nextInt(100), random.nextInt(100), random.nextInt(100)));
            int y = 28 + random.nextInt(6) - 3;
            g.drawString(String.valueOf(code.charAt(i)), 10 + i * 27, y);
        }

        g.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }
}
