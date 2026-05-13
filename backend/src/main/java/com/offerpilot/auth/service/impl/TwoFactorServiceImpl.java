package com.offerpilot.auth.service.impl;

import com.offerpilot.auth.dto.LoginResponse;
import com.offerpilot.auth.dto.TwoFactorEnableVO;
import com.offerpilot.auth.dto.TwoFactorSetupVO;
import com.offerpilot.auth.dto.TwoFactorStatusVO;
import com.offerpilot.auth.service.DeviceService;
import com.offerpilot.auth.service.TwoFactorService;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.security.util.JwtTokenUtil;
import com.offerpilot.user.entity.User;
import com.offerpilot.user.service.UserService;
import com.offerpilot.user.vo.UserInfoVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorServiceImpl implements TwoFactorService {

    private static final String SETUP_SECRET_PREFIX = "2fa:setup:";
    private static final String TEMP_TOKEN_PREFIX = "2fa:temp:";
    private static final String ISSUER = "OfferPilot";
    private static final int TEMP_TOKEN_TTL_MINUTES = 5;
    private static final int SETUP_SECRET_TTL_MINUTES = 10;
    private static final int RECOVERY_CODE_COUNT = 8;
    private static final int RECOVERY_CODE_LENGTH = 8;

    private final UserService userService;
    private final DeviceService deviceService;
    private final JwtTokenUtil jwtTokenUtil;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${offerpilot.security.jwt-secret:}")
    private String jwtSecret;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);

    @Override
    public TwoFactorSetupVO setup(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        if (Boolean.TRUE.equals(user.getTotpEnabled())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "两步验证已启用");
        }

        String secret = secretGenerator.generate();
        String otpauthUri = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                ISSUER, user.getUsername(), secret, ISSUER);

        // Store secret temporarily in Redis
        redisTemplate.opsForValue().set(
                SETUP_SECRET_PREFIX + userId, secret, SETUP_SECRET_TTL_MINUTES, TimeUnit.MINUTES);

        return TwoFactorSetupVO.builder()
                .otpauthUri(otpauthUri)
                .secret(secret)
                .build();
    }

    @Override
    @Transactional
    public TwoFactorEnableVO enable(Long userId, String code) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        if (Boolean.TRUE.equals(user.getTotpEnabled())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "两步验证已启用");
        }

        // Retrieve the secret from Redis
        String secret = redisTemplate.opsForValue().get(SETUP_SECRET_PREFIX + userId);
        if (secret == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "设置已过期，请重新开始");
        }

        // Verify the code
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        if (!verifier.isValidCode(secret, code)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "验证码错误");
        }

        // Generate recovery codes
        List<String> recoveryCodes = generateRecoveryCodes();

        // Encrypt and store
        String encryptedSecret = encrypt(secret);
        String encryptedCodes = encryptCodes(recoveryCodes);

        user.setTotpSecret(encryptedSecret);
        user.setTotpEnabled(true);
        user.setRecoveryCodes(encryptedCodes);
        userService.updateById(user);

        // Clean up Redis
        redisTemplate.delete(SETUP_SECRET_PREFIX + userId);

        log.info("2FA enabled for user {}", userId);

        return TwoFactorEnableVO.builder()
                .recoveryCodes(recoveryCodes)
                .build();
    }

    @Override
    @Transactional
    public void disable(Long userId, String code) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        if (!Boolean.TRUE.equals(user.getTotpEnabled())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "两步验证未启用");
        }

        // Verify the code
        String secret = decrypt(user.getTotpSecret());
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        if (!verifier.isValidCode(secret, code)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "验证码错误");
        }

        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        user.setRecoveryCodes(null);
        userService.updateById(user);

        log.info("2FA disabled for user {}", userId);
    }

    @Override
    public TwoFactorStatusVO getStatus(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }

        Integer remaining = null;
        if (Boolean.TRUE.equals(user.getTotpEnabled()) && user.getRecoveryCodes() != null) {
            List<Map<String, Object>> codes = decryptCodes(user.getRecoveryCodes());
            remaining = (int) codes.stream().filter(c -> !Boolean.TRUE.equals(c.get("used"))).count();
        }

        return TwoFactorStatusVO.builder()
                .enabled(user.getTotpEnabled() != null && user.getTotpEnabled())
                .recoveryCodesRemaining(remaining)
                .build();
    }

    @Override
    @Transactional
    public LoginResponse verify(String tempToken, String code) {
        String redisKey = TEMP_TOKEN_PREFIX + tempToken;
        String data = redisTemplate.opsForValue().get(redisKey);
        if (data == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "验证已过期，请重新登录");
        }

        // Parse temp token data: userId:username:deviceFingerprint:deviceName
        String[] parts = data.split(":", 4);
        Long userId = Long.parseLong(parts[0]);
        String username = parts.length > 1 ? parts[1] : "";
        String deviceFingerprint = parts.length > 2 && !parts[2].equals("null") ? parts[2] : null;
        String deviceName = parts.length > 3 && !parts[3].equals("null") ? parts[3] : null;

        // Delete temp token (one-time use)
        redisTemplate.delete(redisKey);

        User user = userService.getById(userId);
        if (user == null || !Boolean.TRUE.equals(user.getTotpEnabled())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "两步验证未启用");
        }

        String secret = decrypt(user.getTotpSecret());
        boolean verified = false;

        // Try TOTP code first
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        if (verifier.isValidCode(secret, code)) {
            verified = true;
        } else {
            // Try recovery code
            verified = tryRecoveryCode(user, code);
        }

        if (!verified) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "验证码错误");
        }

        // Generate final JWT
        Long deviceId = null;
        if (deviceFingerprint != null && !deviceFingerprint.isBlank()) {
            try {
                deviceId = deviceService.recordDevice(userId, deviceFingerprint, deviceName, null, null);
            } catch (Exception e) {
                log.warn("Failed to record device during 2FA verify: {}", e.getMessage());
            }
        }

        UserInfoVO userInfo = UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .emailVerified(Boolean.TRUE.equals(user.getEmailVerified()))
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .githubLinked(user.getGithubId() != null && !user.getGithubId().isBlank())
                .githubUsername(user.getGithubUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .lastLoginTime(user.getLastLoginTime())
                .build();

        return LoginResponse.builder()
                .token(jwtTokenUtil.generateToken(userId, username, deviceId, deviceFingerprint))
                .userInfo(userInfo)
                .deviceId(deviceId)
                .build();
    }

    @Override
    public String createTempToken(Long userId, String username, String deviceFingerprint, String deviceName) {
        String tempToken = UUID.randomUUID().toString().replace("-", "");
        String data = String.format("%s:%s:%s:%s", userId, username, deviceFingerprint, deviceName);
        redisTemplate.opsForValue().set(
                TEMP_TOKEN_PREFIX + tempToken, data, TEMP_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        return tempToken;
    }

    private boolean tryRecoveryCode(User user, String code) {
        if (user.getRecoveryCodes() == null) {
            return false;
        }
        List<Map<String, Object>> codes = decryptCodes(user.getRecoveryCodes());
        boolean found = false;
        for (Map<String, Object> entry : codes) {
            if (code.equals(entry.get("code")) && !Boolean.TRUE.equals(entry.get("used"))) {
                entry.put("used", true);
                found = true;
                break;
            }
        }
        if (found) {
            user.setRecoveryCodes(encryptCodesRaw(codes));
            userService.updateById(user);
        }
        return found;
    }

    private List<String> generateRecoveryCodes() {
        SecureRandom random = new SecureRandom();
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            StringBuilder sb = new StringBuilder(RECOVERY_CODE_LENGTH);
            for (int j = 0; j < RECOVERY_CODE_LENGTH; j++) {
                sb.append("ABCDEFGHJKLMNPQRSTUVWXYZ23456789".charAt(
                        random.nextInt("ABCDEFGHJKLMNPQRSTUVWXYZ23456789".length())));
            }
            // Insert a dash in the middle for readability
            sb.insert(4, '-');
            codes.add(sb.toString());
        }
        return codes;
    }

    private String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, getAesKey());
            return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "加密失败");
        }
    }

    private String decrypt(String encrypted) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, getAesKey());
            return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "解密失败");
        }
    }

    private SecretKeySpec getAesKey() {
        // Derive a 16-byte key from the JWT secret
        byte[] keyBytes;
        if (jwtSecret.length() >= 16) {
            keyBytes = jwtSecret.substring(0, 16).getBytes(StandardCharsets.UTF_8);
        } else {
            keyBytes = String.format("%-16s", jwtSecret).getBytes(StandardCharsets.UTF_8);
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    private String encryptCodes(List<String> codes) {
        try {
            List<Map<String, Object>> codeList = new ArrayList<>();
            for (String code : codes) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("code", code);
                entry.put("used", false);
                codeList.add(entry);
            }
            return encrypt(objectMapper.writeValueAsString(codeList));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "序列化失败");
        }
    }

    private String encryptCodesRaw(List<Map<String, Object>> codes) {
        try {
            return encrypt(objectMapper.writeValueAsString(codes));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "序列化失败");
        }
    }

    private List<Map<String, Object>> decryptCodes(String encrypted) {
        try {
            String json = decrypt(encrypted);
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
