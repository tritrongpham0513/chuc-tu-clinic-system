-- ============================================================================
-- PHÒNG KHÁM CHÚC TỪ - MIGRATION 005: CÁC BẢNG HỖ TRỢ NGHIỆP VỤ v1
-- MySQL 8.0+ | InnoDB | utf8mb4_unicode_ci
-- ============================================================================
-- Hướng dẫn rollback:
--   DROP TABLE IF EXISTS `idempotency_keys`, `counters`, `medical_record_edits`,
--                        `appointment_seats`, `checkins`;
-- ============================================================================

-- LƯU Ý: Không dùng `USE ...` — file chạy trên database đang được chọn ở client,
-- nhờ vậy dùng được cho cả DB thật, DB test và docker/mysql/init.

-- 1. BẢNG CHECKINS: Quản lý hàng đợi thực tế & ghế khám bền vững
CREATE TABLE IF NOT EXISTS `checkins` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `appointment_id`  BIGINT UNSIGNED NOT NULL,
  `status`          ENUM('WAITING', 'IN_ROOM', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'WAITING',
  `priority_group`  ENUM('PRIORITY', 'NORMAL') NOT NULL DEFAULT 'NORMAL',
  `seat_no`         INT NULL COMMENT 'Số thứ tự ghế khám (1..5) khi đang trong phòng',
  `queue_no`        INT NOT NULL COMMENT 'Số thứ tự check-in trong ngày',
  `check_in_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `entered_at`      DATETIME NULL COMMENT 'Thời điểm mời vào ghế khám',
  `completed_at`    DATETIME NULL COMMENT 'Thời điểm khám xong',
  `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_checkin_appointment` (`appointment_id`),
  INDEX `idx_checkin_queue` (`status`, `priority_group`, `queue_no`),
  INDEX `idx_checkin_seat` (`seat_no`, `status`),
  CONSTRAINT `fk_checkin_appointment` FOREIGN KEY (`appointment_id`) REFERENCES `appointments`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Bảng hàng đợi khám bệnh thực tế';

-- 2. BẢNG APPOINTMENT_SEATS: Đảm bảo tối đa 6 phiếu / khung giờ bằng ràng buộc UNIQUE của Database
CREATE TABLE IF NOT EXISTS `appointment_seats` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `appointment_date`  DATE NOT NULL,
  `time_slot_id`      BIGINT UNSIGNED NOT NULL,
  `seat_no`           INT NOT NULL COMMENT 'Chỗ số 1 đến 6 trong khung giờ',
  `appointment_id`    BIGINT UNSIGNED NOT NULL,
  `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seat_slot` (`appointment_date`, `time_slot_id`, `seat_no`),
  UNIQUE KEY `uk_seat_appointment` (`appointment_id`),
  INDEX `idx_seat_date_slot` (`appointment_date`, `time_slot_id`),
  CONSTRAINT `fk_seat_timeslot` FOREIGN KEY (`time_slot_id`) REFERENCES `time_slots`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_seat_appointment` FOREIGN KEY (`appointment_id`) REFERENCES `appointments`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_seat_no` CHECK (`seat_no` BETWEEN 1 AND 20)
) ENGINE=InnoDB COMMENT='Giữ chỗ trong khung giờ, chống vượt capacity ở mức database';

-- 3. BẢNG MEDICAL_RECORD_EDITS: Audit trail lịch sử chỉnh sửa phiếu kết quả khám
CREATE TABLE IF NOT EXISTS `medical_record_edits` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `record_id`   BIGINT UNSIGNED NOT NULL,
  `edited_by`   BIGINT UNSIGNED NOT NULL,
  `edited_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `changes`     JSON NOT NULL COMMENT 'Ghi lại from -> to của từng trường thay đổi',
  PRIMARY KEY (`id`),
  INDEX `idx_edit_record` (`record_id`, `edited_at`),
  CONSTRAINT `fk_edit_record` FOREIGN KEY (`record_id`) REFERENCES `medical_records`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_edit_user` FOREIGN KEY (`edited_by`) REFERENCES `users`(`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Lịch sử chỉnh sửa bệnh án trong 24 giờ';

-- 4. BẢNG COUNTERS: Sinh mã tự tăng nguyên tử và an toàn (CT-BN-yy-xxxx, CT-PK-yy-ddMM-xxx)
CREATE TABLE IF NOT EXISTS `counters` (
  `name`        VARCHAR(50) NOT NULL,
  `value`       BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB COMMENT='Bộ đếm sinh mã số phiếu và mã bệnh nhân';

-- 5. BẢNG IDEMPOTENCY_KEYS: Chống gửi yêu cầu đúp khi người lớn tuổi bấm nút đặt lịch nhiều lần
CREATE TABLE IF NOT EXISTS `idempotency_keys` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `key_hash`      CHAR(64) NOT NULL COMMENT 'Mã SHA-256 của Idempotency-Key header',
  `user_id`       BIGINT UNSIGNED NULL,
  `endpoint`      VARCHAR(255) NOT NULL,
  `request_hash`  CHAR(64) NOT NULL,
  `response_json` LONGTEXT NOT NULL,
  `status_code`   INT NOT NULL DEFAULT 200,
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idempotency_key_user` (`key_hash`, `user_id`),
  INDEX `idx_idempotency_created` (`created_at`)
) ENGINE=InnoDB COMMENT='Lưu kết quả các yêu cầu Idempotent';

