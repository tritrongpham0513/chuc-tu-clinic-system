-- V13__user_notifications.sql: Hộp thư thông báo nội bộ (per-user) cho ADMIN/STAFF/DOCTOR
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user_notifications` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`        BIGINT UNSIGNED NOT NULL,
  `appointment_id` BIGINT UNSIGNED DEFAULT NULL,
  `type`           VARCHAR(40)  NOT NULL COMMENT 'APPOINTMENT_CREATED / APPOINTMENT_CANCELLED',
  `title`          VARCHAR(150) NOT NULL,
  `body`           VARCHAR(500) NOT NULL,
  `link`           VARCHAR(200) DEFAULT NULL COMMENT 'Đường dẫn nội bộ để mở đúng trang',
  `read_at`        DATETIME     DEFAULT NULL COMMENT 'NULL = chưa đọc',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_notifications_once` (`user_id`, `appointment_id`, `type`),
  INDEX `idx_user_notifications_unread` (`user_id`, `read_at`, `created_at`),
  CONSTRAINT `fk_user_notifications_user` FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_user_notifications_appointment` FOREIGN KEY (`appointment_id`)
    REFERENCES `appointments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Hộp thư thông báo nội bộ, mỗi dòng cho 1 người nhận';
