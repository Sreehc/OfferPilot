USE bytecoach;

ALTER TABLE user
    ADD COLUMN IF NOT EXISTS totp_secret VARCHAR(64) DEFAULT NULL COMMENT 'TOTP 密钥（AES 加密存储）' AFTER last_login_time,
    ADD COLUMN IF NOT EXISTS totp_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用两步验证' AFTER totp_secret,
    ADD COLUMN IF NOT EXISTS recovery_codes TEXT DEFAULT NULL COMMENT '恢复码 JSON 数组（AES 加密存储）' AFTER totp_enabled;
