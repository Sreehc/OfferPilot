-- V6: Notification system
CREATE TABLE IF NOT EXISTS notification (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    type            VARCHAR(64)     NOT NULL COMMENT 'review_remind / plan_remind / interview_feedback / community_accept / community_vote / rank_upgrade',
    title           VARCHAR(200)    NOT NULL,
    content         TEXT,
    link            VARCHAR(500),
    is_read         TINYINT(1)      NOT NULL DEFAULT 0,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_notification_user_read (user_id, is_read),
    INDEX idx_notification_user_time (user_id, create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
