USE bytecoach;
SET NAMES utf8mb4;

SET @chat_session_update_time_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'chat_session'
      AND COLUMN_NAME = 'update_time'
);

SET @chat_session_update_time_sql = IF(
    @chat_session_update_time_exists = 0,
    'ALTER TABLE chat_session ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER create_time',
    'SELECT 1'
);

PREPARE chat_session_update_time_stmt FROM @chat_session_update_time_sql;
EXECUTE chat_session_update_time_stmt;
DEALLOCATE PREPARE chat_session_update_time_stmt;
