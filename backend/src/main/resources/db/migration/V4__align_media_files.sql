-- Đồng bộ bảng media_files với entity MediaFile (file_url/size_bytes/width/height/alt_text).
SET NAMES utf8mb4;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'media_files' AND COLUMN_NAME = 'file_url') = 0
    AND (SELECT COUNT(*) FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'media_files' AND COLUMN_NAME = 'file_path') > 0,
    'ALTER TABLE `media_files` CHANGE COLUMN `file_path` `file_url` VARCHAR(500) NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'media_files' AND COLUMN_NAME = 'size_bytes') = 0
    AND (SELECT COUNT(*) FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'media_files' AND COLUMN_NAME = 'file_size') > 0,
    'ALTER TABLE `media_files` CHANGE COLUMN `file_size` `size_bytes` BIGINT UNSIGNED NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'media_files' AND COLUMN_NAME = 'width') = 0,
    'ALTER TABLE `media_files` ADD COLUMN `width` INT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'media_files' AND COLUMN_NAME = 'height') = 0,
    'ALTER TABLE `media_files` ADD COLUMN `height` INT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'media_files' AND COLUMN_NAME = 'alt_text') = 0,
    'ALTER TABLE `media_files` ADD COLUMN `alt_text` VARCHAR(255) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'media_files' AND INDEX_NAME = 'idx_media_uploader') = 0,
    'ALTER TABLE `media_files` ADD INDEX `idx_media_uploader` (`uploaded_by`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
