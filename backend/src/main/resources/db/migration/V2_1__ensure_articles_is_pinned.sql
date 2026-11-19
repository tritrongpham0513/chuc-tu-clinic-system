-- Bổ sung articles.is_pinned cho CSDL cũ, để V3 (INSERT vào cột này) chạy được.
SET NAMES utf8mb4;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'articles' AND COLUMN_NAME = 'is_pinned') = 0,
    'ALTER TABLE `articles` ADD COLUMN `is_pinned` TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'articles' AND INDEX_NAME = 'idx_article_pinned') = 0,
    'ALTER TABLE `articles` ADD INDEX `idx_article_pinned` (`is_pinned`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
