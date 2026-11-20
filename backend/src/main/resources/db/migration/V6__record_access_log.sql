-- V6__record_access_log.sql: Bảng nhật ký truy cập hồ sơ bệnh án ngoài phạm vi (Break-glass)
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `record_access_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT 'ID tài khoản thực hiện truy cập ngoài phạm vi (FK users)',
  `patient_id` BIGINT UNSIGNED NOT NULL COMMENT 'ID hồ sơ bệnh nhân được truy cập (FK patients)',
  `record_id` BIGINT UNSIGNED DEFAULT NULL COMMENT 'ID phiếu kết quả khám (nếu có)',
  `scope` VARCHAR(20) NOT NULL DEFAULT 'BREAK_GLASS' COMMENT 'Phạm vi truy cập: BREAK_GLASS',
  `reason` VARCHAR(500) NOT NULL COMMENT 'Lý do bác sĩ kích hoạt quyền xem khẩn cấp',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_record_access_log_user_created` (`user_id`, `created_at`),
  INDEX `idx_record_access_log_patient` (`patient_id`),
  CONSTRAINT `fk_record_access_log_user` FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_record_access_log_patient` FOREIGN KEY (`patient_id`)
    REFERENCES `patients` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Nhật ký truy cập hồ sơ y tế khẩn cấp ngoài phạm vi';
