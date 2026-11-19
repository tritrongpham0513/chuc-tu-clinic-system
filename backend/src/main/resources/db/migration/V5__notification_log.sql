-- V5__notification_log.sql: Bảng nhật ký gửi thông báo (SMS, LOG, MANUAL, ZNS)
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `notification_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `appointment_id` BIGINT UNSIGNED DEFAULT NULL,
  `channel` VARCHAR(20) NOT NULL COMMENT 'LOG / SMS / MANUAL / ZNS',
  `template` VARCHAR(50) NOT NULL COMMENT 'BOOKING_SUCCESS / CANCELLED / REMINDER_2H',
  `recipient_masked` VARCHAR(20) NOT NULL COMMENT 'SĐT đã che, không lưu SĐT thô',
  `status` VARCHAR(20) NOT NULL COMMENT 'SENT / SKIPPED / FAILED',
  `error_code` VARCHAR(100) DEFAULT NULL COMMENT 'Mã lỗi nhà cung cấp',
  `provider` VARCHAR(30) DEFAULT NULL COMMENT 'speedsms / esms / log',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_notification_log_appointment` (`appointment_id`),
  INDEX `idx_notification_log_created_at` (`created_at`),
  CONSTRAINT `fk_notification_log_appointment` FOREIGN KEY (`appointment_id`)
    REFERENCES `appointments` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Nhật ký thông báo gửi đi';
