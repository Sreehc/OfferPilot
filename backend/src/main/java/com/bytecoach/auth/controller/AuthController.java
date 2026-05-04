package com.bytecoach.auth.controller;

import com.bytecoach.auth.dto.CaptchaVO;
import com.bytecoach.auth.dto.LoginLogVO;
import com.bytecoach.auth.dto.LoginRequest;
import com.bytecoach.auth.dto.LoginResponse;
import com.bytecoach.auth.dto.RegisterRequest;
import com.bytecoach.auth.service.AuthService;
import com.bytecoach.auth.service.LoginLogService;
import com.bytecoach.common.api.Result;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.security.util.SecurityUtils;
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
