USE bytecoach;
SET NAMES utf8mb4;

SET @category_update_time_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'category'
      AND COLUMN_NAME = 'update_time'
);
SET @category_update_time_sql = IF(
    @category_update_time_exists = 0,
    'ALTER TABLE category ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER create_time',
    'SELECT 1'
);
PREPARE category_update_time_stmt FROM @category_update_time_sql;
EXECUTE category_update_time_stmt;
DEALLOCATE PREPARE category_update_time_stmt;

SET @question_update_time_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'question'
      AND COLUMN_NAME = 'update_time'
);
SET @question_update_time_sql = IF(
    @question_update_time_exists = 0,
    'ALTER TABLE question ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER create_time',
    'SELECT 1'
);
PREPARE question_update_time_stmt FROM @question_update_time_sql;
EXECUTE question_update_time_stmt;
DEALLOCATE PREPARE question_update_time_stmt;

SET @login_log_update_time_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'login_log'
      AND COLUMN_NAME = 'update_time'
);
SET @login_log_update_time_sql = IF(
    @login_log_update_time_exists = 0,
    'ALTER TABLE login_log ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER create_time',
    'SELECT 1'
);
PREPARE login_log_update_time_stmt FROM @login_log_update_time_sql;
EXECUTE login_log_update_time_stmt;
DEALLOCATE PREPARE login_log_update_time_stmt;
