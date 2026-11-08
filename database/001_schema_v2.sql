-- ============================================================================
-- PHÒNG KHÁM CHÚC TỪ - DATABASE SCHEMA v2.0 (ĐẦY ĐỦ)
-- MySQL 8.0+ | InnoDB | utf8mb4_unicode_ci
-- ============================================================================
-- NHÓM 1 - Tài khoản & bảo mật : users, refresh_tokens
-- NHÓM 2 - Phòng khám          : clinic_settings, services, clinic_holidays
-- NHÓM 3 - Bác sĩ & lịch       : doctors, doctor_schedules, doctor_day_offs, time_slots
-- NHÓM 4 - Bệnh nhân & khám    : patients, appointments, appointment_status_history,
--                                 medical_records
-- NHÓM 5 - Nội dung            : article_categories, articles, tags, article_tags,
--                                 media_files
-- NHÓM 6 - Liên hệ & hệ thống  : contact_messages, notification_logs, audit_logs
-- ============================================================================
-- LƯU Ý: Script này XÓA và TẠO LẠI toàn bộ bảng (dùng cho môi trường dev).
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `phongkham_chuctu`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `phongkham_chuctu`;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `audit_logs`, `notification_logs`, `contact_messages`,
  `media_files`, `article_tags`, `tags`, `articles`, `article_categories`,
  `medical_records`, `appointment_status_history`, `appointments`, `patients`,
  `time_slots`, `doctor_day_offs`, `doctor_schedules`, `doctors`,
  `clinic_holidays`, `services`, `clinic_settings`, `refresh_tokens`, `users`;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- NHÓM 1: TÀI KHOẢN & BẢO MẬT
-- ============================================================================

CREATE TABLE `users` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username`      VARCHAR(50)  NOT NULL COMMENT 'Tên đăng nhập',
  `password_hash` VARCHAR(255) NOT NULL COMMENT 'BCrypt strength 12',
  `full_name`     VARCHAR(100) NOT NULL,
  `email`         VARCHAR(100) DEFAULT NULL,
  `phone`         VARCHAR(15)  NOT NULL,
  `role`          ENUM('ADMIN','DOCTOR','STAFF','PATIENT') NOT NULL DEFAULT 'PATIENT' COMMENT 'ADMIN: Quản trị, DOCTOR: Bác sĩ, STAFF: Lễ tân/nhân viên, PATIENT: Bệnh nhân',
  `is_active`     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '0 = khóa tài khoản',
  `must_change_password` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 = bắt buộc đổi mật khẩu ở lần đăng nhập tới',
  `failed_login_count` INT     NOT NULL DEFAULT 0 COMMENT 'Số lần đăng nhập sai liên tiếp',
  `locked_until`  DATETIME     DEFAULT NULL COMMENT 'Khóa tạm đến thời điểm này',
  `last_login_at` DATETIME     DEFAULT NULL,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`),
  UNIQUE KEY `uk_users_email` (`email`),
  INDEX `idx_users_phone` (`phone`),
  INDEX `idx_users_role_active` (`role`, `is_active`)
) ENGINE=InnoDB COMMENT='Tài khoản đăng nhập hệ thống';

CREATE TABLE `refresh_tokens` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED NOT NULL,
  `token_hash`  CHAR(64)    NOT NULL COMMENT 'SHA-256 của refresh token (không lưu token gốc)',
  `expires_at`  DATETIME    NOT NULL,
  `revoked`     TINYINT(1)  NOT NULL DEFAULT 0,
  `user_agent`  VARCHAR(255) DEFAULT NULL,
  `ip_address`  VARCHAR(45)  DEFAULT NULL,
  `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_token_hash` (`token_hash`),
  INDEX `idx_refresh_user` (`user_id`),
  INDEX `idx_refresh_expires` (`expires_at`),
  CONSTRAINT `fk_refresh_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Refresh token JWT (đăng xuất / thu hồi phiên)';

CREATE TABLE `otp_verifications` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `phone`         VARCHAR(15)  NOT NULL COMMENT 'Số điện thoại nhận OTP',
  `otp_code`      VARCHAR(10)  NOT NULL COMMENT 'Mã OTP (thường 6 số)',
  `otp_type`      ENUM('REGISTER','LOGIN','FORGOT_PASSWORD') NOT NULL DEFAULT 'REGISTER',
  `expires_at`    DATETIME     NOT NULL COMMENT 'Thời hạn hết hạn (thường 5 phút)',
  `is_verified`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1 = đã xác thực thành công',
  `attempt_count` INT          NOT NULL DEFAULT 0 COMMENT 'Số lần nhập sai',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_otp_phone_type` (`phone`, `otp_type`, `is_verified`)
) ENGINE=InnoDB COMMENT='Bảng lưu mã OTP gửi qua SMS';

-- ============================================================================
-- NHÓM 2: THÔNG TIN PHÒNG KHÁM
-- ============================================================================

CREATE TABLE `clinic_settings` (
  `setting_key`   VARCHAR(100) NOT NULL COMMENT 'VD: hotline, address, map_lat',
  `setting_value` TEXT         DEFAULT NULL,
  `description`   VARCHAR(255) DEFAULT NULL COMMENT 'Mô tả tiếng Việt cho Admin',
  `updated_by`    BIGINT UNSIGNED DEFAULT NULL,
  `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`setting_key`),
  CONSTRAINT `fk_setting_user` FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Cấu hình phòng khám (hotline, địa chỉ, tọa độ Maps, giờ mở cửa...)';

CREATE TABLE `services` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`          VARCHAR(150) NOT NULL COMMENT 'VD: Châm cứu, Bấm huyệt, Xoa bóp',
  `slug`          VARCHAR(150) NOT NULL,
  `description`   TEXT         DEFAULT NULL,
  `duration_min`  INT          NOT NULL DEFAULT 30 COMMENT 'Thời lượng dự kiến (phút)',
  `price`         DECIMAL(12,0) DEFAULT NULL COMMENT 'Giá (VND), NULL = liên hệ',
  `icon_url`      VARCHAR(500) DEFAULT NULL,
  `sort_order`    INT          NOT NULL DEFAULT 0,
  `is_active`     TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_service_slug` (`slug`),
  INDEX `idx_service_active_sort` (`is_active`, `sort_order`)
) ENGINE=InnoDB COMMENT='Dịch vụ khám chữa bệnh';

CREATE TABLE `clinic_holidays` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `holiday_date` DATE         NOT NULL,
  `name`         VARCHAR(150) NOT NULL COMMENT 'VD: Tết Nguyên Đán',
  `is_closed`    TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1 = nghỉ cả ngày',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_holiday_date` (`holiday_date`)
) ENGINE=InnoDB COMMENT='Ngày nghỉ chung của phòng khám (không nhận lịch)';

-- ============================================================================
-- NHÓM 3: BÁC SĨ & LỊCH LÀM VIỆC
-- ============================================================================

CREATE TABLE `doctors` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '1 bác sĩ = 1 tài khoản',
  `full_name`   VARCHAR(100) NOT NULL,
  `title`       VARCHAR(50)  DEFAULT NULL COMMENT 'Học hàm: BS, BSCKI, Lương y, ThS...',
  `specialty`   VARCHAR(100) NOT NULL,
  `experience_years` INT     DEFAULT NULL,
  `bio`         TEXT         DEFAULT NULL,
  `phone`       VARCHAR(15)  NOT NULL,
  `avatar_url`  VARCHAR(500) DEFAULT NULL COMMENT 'Ảnh WebP',
  `sort_order`  INT          NOT NULL DEFAULT 0,
  `is_active`   TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_user` (`user_id`),
  INDEX `idx_doctor_specialty` (`specialty`),
  INDEX `idx_doctor_active_sort` (`is_active`, `sort_order`),
  CONSTRAINT `fk_doctor_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Hồ sơ bác sĩ';

CREATE TABLE `time_slots` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `slot_label`   VARCHAR(20) NOT NULL COMMENT '08:00 - 08:30',
  `start_time`   TIME        NOT NULL,
  `end_time`     TIME        NOT NULL,
  `period`       ENUM('MORNING','AFTERNOON') NOT NULL,
  `max_patients` INT         NOT NULL DEFAULT 3 COMMENT 'Số bệnh nhân tối đa / slot / bác sĩ',
  `sort_order`   INT         NOT NULL DEFAULT 0,
  `is_active`    TINYINT(1)  NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_slot_time` (`start_time`, `end_time`),
  CONSTRAINT `chk_slot_time` CHECK (`end_time` > `start_time`),
  CONSTRAINT `chk_slot_max` CHECK (`max_patients` > 0)
) ENGINE=InnoDB COMMENT='Khung giờ khám (30 phút/slot)';

CREATE TABLE `doctor_schedules` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `doctor_id`   BIGINT UNSIGNED NOT NULL,
  `day_of_week` ENUM('MON','TUE','WED','THU','FRI','SAT','SUN') NOT NULL,
  `period`      ENUM('MORNING','AFTERNOON','FULL_DAY') NOT NULL,
  `is_active`   TINYINT(1)  NOT NULL DEFAULT 1,
  `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_day_period` (`doctor_id`,`day_of_week`,`period`),
  INDEX `idx_schedule_day` (`day_of_week`, `is_active`),
  CONSTRAINT `fk_schedule_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Lịch làm việc cố định hàng tuần của bác sĩ';

CREATE TABLE `doctor_day_offs` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `doctor_id`  BIGINT UNSIGNED NOT NULL,
  `off_date`   DATE        NOT NULL,
  `period`     ENUM('MORNING','AFTERNOON','FULL_DAY') NOT NULL DEFAULT 'FULL_DAY',
  `reason`     VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_off` (`doctor_id`,`off_date`,`period`),
  INDEX `idx_off_date` (`off_date`),
  CONSTRAINT `fk_off_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Ngày nghỉ đột xuất / nghỉ phép của bác sĩ';

-- ============================================================================
-- NHÓM 4: BỆNH NHÂN & LỊCH KHÁM
-- ============================================================================

CREATE TABLE `patients` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT 'NULL nếu đặt lịch không cần tài khoản',
  `patient_code`  VARCHAR(20)  NOT NULL COMMENT 'BN-2026-0001',
  `full_name`     VARCHAR(100) NOT NULL,
  `phone`         VARCHAR(15)  NOT NULL COMMENT '10 số, đầu 03/05/07/08/09',
  `year_of_birth` INT          NOT NULL,
  `gender`        ENUM('MALE','FEMALE','OTHER') NOT NULL,
  `address`       VARCHAR(255) DEFAULT NULL,
  `relationship`  VARCHAR(50)  DEFAULT NULL COMMENT 'Quan hệ với người đặt hộ: cha, mẹ, ông, bà...',
  `medical_history` TEXT       DEFAULT NULL COMMENT 'Tiền sử bệnh, dị ứng thuốc',
  `is_primary`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1 = hồ sơ chính chủ (không xóa được)',
  `is_active`     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '0 = đã xóa mềm/unlink',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_patient_code` (`patient_code`),
  INDEX `idx_patient_phone` (`phone`),
  INDEX `idx_patient_name` (`full_name`),
  INDEX `idx_patient_user` (`user_id`),
  INDEX `idx_patients_user_active` (`user_id`, `is_active`),
  CONSTRAINT `fk_patient_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `chk_patient_yob` CHECK (`year_of_birth` BETWEEN 1900 AND 2100)
) ENGINE=InnoDB COMMENT='Hồ sơ bệnh nhân (nhiều bệnh nhân có thể dùng chung 1 SĐT người đặt hộ)';

CREATE TABLE `appointments` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `booking_code`     VARCHAR(30) NOT NULL COMMENT 'PKCT-YYYYMMDD-XXXX',
  `patient_id`       BIGINT UNSIGNED NOT NULL,
  `doctor_id`        BIGINT UNSIGNED DEFAULT NULL COMMENT 'NULL = lễ tân gán sau',
  `service_id`       BIGINT UNSIGNED DEFAULT NULL,
  `appointment_date` DATE        NOT NULL,
  `time_slot_id`     BIGINT UNSIGNED NOT NULL,
  `symptoms`         TEXT        DEFAULT NULL,
  `booker_name`      VARCHAR(100) DEFAULT NULL COMMENT 'Người đặt hộ (con cháu), nếu khác bệnh nhân',
  `booker_phone`     VARCHAR(15)  DEFAULT NULL COMMENT 'SĐT người đặt hộ để gọi xác nhận',
  `source`           ENUM('WEB','PHONE','WALK_IN','ADMIN') NOT NULL DEFAULT 'WEB',
  `status`           ENUM('PENDING','CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED','NO_SHOW')
                     NOT NULL DEFAULT 'PENDING',
  `confirmed_by`     BIGINT UNSIGNED DEFAULT NULL COMMENT 'Lễ tân xác nhận',
  `confirmed_at`     DATETIME    DEFAULT NULL,
  `cancel_reason`    VARCHAR(255) DEFAULT NULL,
  `note`             VARCHAR(500) DEFAULT NULL COMMENT 'Ghi chú nội bộ',
  `created_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_booking_code` (`booking_code`),
  INDEX `idx_app_date_doctor_slot` (`appointment_date`,`doctor_id`,`time_slot_id`),
  INDEX `idx_app_date_slot` (`appointment_date`,`time_slot_id`),
  INDEX `idx_app_status_date` (`status`,`appointment_date`),
  INDEX `idx_app_patient` (`patient_id`),
  INDEX `idx_app_booker_phone` (`booker_phone`),
  CONSTRAINT `fk_app_patient` FOREIGN KEY (`patient_id`) REFERENCES `patients`(`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_app_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_app_service` FOREIGN KEY (`service_id`) REFERENCES `services`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_app_timeslot` FOREIGN KEY (`time_slot_id`) REFERENCES `time_slots`(`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_app_confirmed_by` FOREIGN KEY (`confirmed_by`) REFERENCES `users`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Lịch hẹn khám. Chống trùng: Service đếm theo (date, doctor, slot) <= max_patients, dùng @Lock PESSIMISTIC_WRITE trên time_slots';

CREATE TABLE `appointment_status_history` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `appointment_id` BIGINT UNSIGNED NOT NULL,
  `old_status`     VARCHAR(20) DEFAULT NULL,
  `new_status`     VARCHAR(20) NOT NULL,
  `changed_by`     BIGINT UNSIGNED DEFAULT NULL COMMENT 'NULL = hệ thống / bệnh nhân',
  `reason`         VARCHAR(255) DEFAULT NULL,
  `changed_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_hist_appointment` (`appointment_id`, `changed_at`),
  CONSTRAINT `fk_hist_appointment` FOREIGN KEY (`appointment_id`) REFERENCES `appointments`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_hist_user` FOREIGN KEY (`changed_by`) REFERENCES `users`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Lịch sử đổi trạng thái lịch hẹn (ai xác nhận / hủy, khi nào)';

CREATE TABLE `medical_records` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `appointment_id`  BIGINT UNSIGNED NOT NULL,
  `patient_id`      BIGINT UNSIGNED NOT NULL,
  `doctor_id`       BIGINT UNSIGNED NOT NULL,
  `pulse_bp`        VARCHAR(50)  DEFAULT NULL COMMENT 'Mạch / huyết áp',
  `diagnosis`       TEXT         DEFAULT NULL COMMENT 'Chẩn đoán (YHCT / YHHĐ)',
  `treatment`       TEXT         DEFAULT NULL COMMENT 'Phương pháp: châm cứu, bấm huyệt...',
  `prescription`    TEXT         DEFAULT NULL COMMENT 'Bài thuốc / đơn thuốc',
  `advice`          TEXT         DEFAULT NULL COMMENT 'Lời dặn',
  `follow_up_date`  DATE         DEFAULT NULL COMMENT 'Ngày tái khám',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_appointment` (`appointment_id`),
  INDEX `idx_record_patient` (`patient_id`, `created_at`),
  INDEX `idx_record_followup` (`follow_up_date`),
  CONSTRAINT `fk_record_appointment` FOREIGN KEY (`appointment_id`) REFERENCES `appointments`(`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_record_patient` FOREIGN KEY (`patient_id`) REFERENCES `patients`(`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_record_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors`(`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Bệnh án sau mỗi lần khám (DỮ LIỆU NHẠY CẢM - không log ra file)';

-- ============================================================================
-- NHÓM 5: NỘI DUNG (CẨM NANG, TIN TỨC)
-- ============================================================================

CREATE TABLE `article_categories` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(100) NOT NULL,
  `slug`        VARCHAR(100) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `sort_order`  INT          NOT NULL DEFAULT 0,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_slug` (`slug`)
) ENGINE=InnoDB COMMENT='Danh mục bài viết';

CREATE TABLE `articles` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `title`         VARCHAR(255) NOT NULL,
  `slug`          VARCHAR(255) NOT NULL,
  `summary`       TEXT         NOT NULL,
  `content`       LONGTEXT     NOT NULL,
  `thumbnail_url` VARCHAR(500) DEFAULT NULL,
  `category_id`   BIGINT UNSIGNED DEFAULT NULL,
  `author_id`     BIGINT UNSIGNED NOT NULL,
  `is_published`  TINYINT(1)   NOT NULL DEFAULT 0,
  `is_featured`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'Ghim lên trang chủ',
  `published_at`  DATETIME     DEFAULT NULL,
  `view_count`    INT          NOT NULL DEFAULT 0,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_slug` (`slug`),
  INDEX `idx_article_published` (`is_published`,`published_at`),
  INDEX `idx_article_category` (`category_id`,`is_published`,`published_at`),
  INDEX `idx_article_featured` (`is_featured`,`is_published`),
  FULLTEXT KEY `ft_article_search` (`title`,`summary`),
  CONSTRAINT `fk_article_category` FOREIGN KEY (`category_id`) REFERENCES `article_categories`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_article_author` FOREIGN KEY (`author_id`) REFERENCES `users`(`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Bài viết cẩm nang, mẹo sức khỏe, tin từ thiện';

CREATE TABLE `tags` (
  `id`   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `slug` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_slug` (`slug`)
) ENGINE=InnoDB COMMENT='Thẻ bài viết (đau lưng, mất ngủ, tiểu đường...)';

CREATE TABLE `article_tags` (
  `article_id` BIGINT UNSIGNED NOT NULL,
  `tag_id`     BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`article_id`,`tag_id`),
  INDEX `idx_at_tag` (`tag_id`),
  CONSTRAINT `fk_at_article` FOREIGN KEY (`article_id`) REFERENCES `articles`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_at_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Quan hệ N-N bài viết - thẻ';

CREATE TABLE `media_files` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `file_name`     VARCHAR(255) NOT NULL,
  `file_url`      VARCHAR(500) NOT NULL,
  `mime_type`     VARCHAR(100) NOT NULL COMMENT 'image/webp...',
  `size_bytes`    BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `width`         INT DEFAULT NULL,
  `height`        INT DEFAULT NULL,
  `alt_text`      VARCHAR(255) DEFAULT NULL COMMENT 'Mô tả ảnh (hỗ trợ người khiếm thị)',
  `uploaded_by`   BIGINT UNSIGNED DEFAULT NULL,
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_media_uploader` (`uploaded_by`),
  CONSTRAINT `fk_media_user` FOREIGN KEY (`uploaded_by`) REFERENCES `users`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Thư viện ảnh (bài viết, hoạt động từ thiện, bác sĩ)';

-- ============================================================================
-- NHÓM 6: LIÊN HỆ & HỆ THỐNG
-- ============================================================================

CREATE TABLE `contact_messages` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `full_name`   VARCHAR(100) NOT NULL,
  `phone`       VARCHAR(15)  NOT NULL,
  `message`     TEXT         NOT NULL,
  `is_handled`  TINYINT(1)   NOT NULL DEFAULT 0,
  `handled_by`  BIGINT UNSIGNED DEFAULT NULL,
  `handled_at`  DATETIME     DEFAULT NULL,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_contact_handled` (`is_handled`,`created_at`),
  CONSTRAINT `fk_contact_user` FOREIGN KEY (`handled_by`) REFERENCES `users`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Lời nhắn từ form Liên hệ';

CREATE TABLE `notification_logs` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `appointment_id` BIGINT UNSIGNED DEFAULT NULL,
  `channel`        ENUM('SMS','ZALO','EMAIL') NOT NULL,
  `recipient`      VARCHAR(100) NOT NULL,
  `type`           ENUM('BOOKING_RECEIVED','CONFIRMED','REMINDER','CANCELLED','FOLLOW_UP') NOT NULL,
  `content`        VARCHAR(500) DEFAULT NULL,
  `status`         ENUM('PENDING','SENT','FAILED') NOT NULL DEFAULT 'PENDING',
  `error_message`  VARCHAR(255) DEFAULT NULL,
  `sent_at`        DATETIME DEFAULT NULL,
  `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_notif_appointment` (`appointment_id`),
  INDEX `idx_notif_status` (`status`,`created_at`),
  CONSTRAINT `fk_notif_appointment` FOREIGN KEY (`appointment_id`) REFERENCES `appointments`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Nhật ký gửi SMS/Zalo nhắc lịch khám';

CREATE TABLE `audit_logs` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED DEFAULT NULL,
  `action`      VARCHAR(50)  NOT NULL COMMENT 'CREATE / UPDATE / DELETE / LOGIN / VIEW_RECORD',
  `entity_type` VARCHAR(50)  NOT NULL COMMENT 'appointments, users, articles...',
  `entity_id`   BIGINT UNSIGNED DEFAULT NULL,
  `detail`      VARCHAR(500) DEFAULT NULL COMMENT 'KHÔNG ghi dữ liệu y tế nhạy cảm',
  `ip_address`  VARCHAR(45)  DEFAULT NULL,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_audit_user` (`user_id`,`created_at`),
  INDEX `idx_audit_entity` (`entity_type`,`entity_id`),
  CONSTRAINT `fk_audit_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Nhật ký thao tác (ai làm gì, lúc nào)';

-- ============================================================================
-- DỮ LIỆU KHỞI TẠO (SEED)
-- ============================================================================

-- Tài khoản ADMIN KHÔNG tạo ở đây. Backend tự tạo lúc khởi động (AdminInitializer)
-- từ biến môi trường ADMIN_USERNAME / ADMIN_PASSWORD, nên không lộ mật khẩu trong SQL.

-- Khung giờ: sáng 08:00-11:30, chiều 13:30-17:00 (mỗi slot 30 phút)
INSERT INTO `time_slots` (`slot_label`,`start_time`,`end_time`,`period`,`max_patients`,`sort_order`) VALUES
('08:00 - 08:30','08:00:00','08:30:00','MORNING',3,1),
('08:30 - 09:00','08:30:00','09:00:00','MORNING',3,2),
('09:00 - 09:30','09:00:00','09:30:00','MORNING',3,3),
('09:30 - 10:00','09:30:00','10:00:00','MORNING',3,4),
('10:00 - 10:30','10:00:00','10:30:00','MORNING',3,5),
('10:30 - 11:00','10:30:00','11:00:00','MORNING',3,6),
('11:00 - 11:30','11:00:00','11:30:00','MORNING',3,7),
('13:30 - 14:00','13:30:00','14:00:00','AFTERNOON',3,8),
('14:00 - 14:30','14:00:00','14:30:00','AFTERNOON',3,9),
('14:30 - 15:00','14:30:00','15:00:00','AFTERNOON',3,10),
('15:00 - 15:30','15:00:00','15:30:00','AFTERNOON',3,11),
('15:30 - 16:00','15:30:00','16:00:00','AFTERNOON',3,12),
('16:00 - 16:30','16:00:00','16:30:00','AFTERNOON',3,13),
('16:30 - 17:00','16:30:00','17:00:00','AFTERNOON',3,14);

INSERT INTO `article_categories` (`name`,`slug`,`description`,`sort_order`) VALUES
('Mẹo dân gian','meo-dan-gian','Bài thuốc và mẹo chữa bệnh dân gian',1),
('Châm cứu - Bấm huyệt','cham-cuu-bam-huyet','Kiến thức châm cứu, bấm huyệt tại nhà',2),
('Phục hồi chức năng','phuc-hoi-chuc-nang','Bài tập và chăm sóc sau tai biến, chấn thương',3),
('Dinh dưỡng thảo mộc','dinh-duong-thao-moc','Ăn uống theo y học cổ truyền',4),
('Hoạt động từ thiện','hoat-dong-tu-thien','Tin tức khám bệnh từ thiện của phòng khám',5);

INSERT INTO `services` (`name`,`slug`,`description`,`duration_min`,`sort_order`) VALUES
('Khám Y học cổ truyền','kham-y-hoc-co-truyen','Bắt mạch, chẩn đoán và kê đơn thuốc đông y',30,1),
('Châm cứu','cham-cuu','Châm cứu giảm đau, trị liệu xương khớp',45,2),
('Bấm huyệt - Xoa bóp','bam-huyet-xoa-bop','Bấm huyệt, xoa bóp trị liệu',45,3),
('Phục hồi chức năng','phuc-hoi-chuc-nang','Phục hồi sau tai biến, chấn thương',60,4);

-- Cấu hình phòng khám (Admin cập nhật qua CMS; cập nhật SĐT/tọa độ thật)
INSERT INTO `clinic_settings` (`setting_key`,`setting_value`,`description`) VALUES
('clinic_name','Phòng Khám Chúc Từ','Tên phòng khám'),
('hotline','0900000000','Số hotline bác sĩ trực (nút Gọi ngay)'),
('address','Đường Điện Biên Phủ, TP. Hồ Chí Minh','Địa chỉ phòng khám'),
('map_lat','10.7769','Vĩ độ Google Maps (cập nhật tọa độ chính xác)'),
('map_lng','106.7009','Kinh độ Google Maps (cập nhật tọa độ chính xác)'),
('working_hours','Sáng 08:00 - 11:30, Chiều 13:30 - 17:00','Giờ làm việc hiển thị trên web'),
('booking_advance_days','14','Cho phép đặt lịch trước tối đa bao nhiêu ngày'),
('booking_min_hours','2','Phải đặt trước giờ khám tối thiểu bao nhiêu giờ');

-- ============================================================================
-- HOÀN TẤT: 21 BẢNG
-- ============================================================================
