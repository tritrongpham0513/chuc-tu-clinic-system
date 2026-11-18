-- ============================================================================
-- PHÒNG KHÁM CHÚC TỪ - FLYWAY MIGRATION V1: INITIAL SCHEMA CHUẨN
-- MySQL 8.0+ | InnoDB | utf8mb4_unicode_ci
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. BẢNG TÀI KHOẢN HỆ THỐNG
CREATE TABLE IF NOT EXISTS `users` (
  `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username`              VARCHAR(50)  NOT NULL COMMENT 'Tên đăng nhập',
  `password_hash`         VARCHAR(255) NOT NULL COMMENT 'BCrypt strength 12',
  `full_name`             VARCHAR(100) NOT NULL,
  `email`                 VARCHAR(100) DEFAULT NULL,
  `phone`                 VARCHAR(15)  NOT NULL,
  `role`                  ENUM('ADMIN','DOCTOR','STAFF','PATIENT') NOT NULL DEFAULT 'PATIENT',
  `is_active`             TINYINT(1)   NOT NULL DEFAULT 1,
  `must_change_password`  TINYINT(1)   NOT NULL DEFAULT 1,
  `failed_login_count`    INT          NOT NULL DEFAULT 0,
  `locked_until`          DATETIME     DEFAULT NULL,
  `last_login_at`         DATETIME     DEFAULT NULL,
  `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`),
  UNIQUE KEY `uk_users_email` (`email`),
  INDEX `idx_users_phone` (`phone`),
  INDEX `idx_users_role_active` (`role`, `is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tài khoản đăng nhập hệ thống';

-- 2. REFRESH TOKENS
CREATE TABLE IF NOT EXISTS `refresh_tokens` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED NOT NULL,
  `token_hash`  CHAR(64)     NOT NULL COMMENT 'SHA-256 hash',
  `expires_at`  DATETIME     NOT NULL,
  `revoked`     TINYINT(1)   NOT NULL DEFAULT 0,
  `user_agent`  VARCHAR(255) DEFAULT NULL,
  `ip_address`  VARCHAR(45)  DEFAULT NULL,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_token_hash` (`token_hash`),
  INDEX `idx_refresh_user` (`user_id`),
  INDEX `idx_refresh_expires` (`expires_at`),
  CONSTRAINT `fk_refresh_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. MÃ OTP
CREATE TABLE IF NOT EXISTS `otp_verifications` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `phone`         VARCHAR(15)  NOT NULL,
  `otp_code`      VARCHAR(64)  NOT NULL COMMENT 'Mã OTP hoặc SHA-256 hash của mã OTP',
  `otp_type`      ENUM('REGISTER','LOGIN','FORGOT_PASSWORD') NOT NULL DEFAULT 'REGISTER',
  `expires_at`    DATETIME     NOT NULL,
  `is_verified`   TINYINT(1)   NOT NULL DEFAULT 0,
  `attempt_count` INT          NOT NULL DEFAULT 0,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_otp_phone_type` (`phone`, `otp_type`, `is_verified`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. CẤU HÌNH PHÒNG KHÁM
CREATE TABLE IF NOT EXISTS `clinic_settings` (
  `setting_key`   VARCHAR(100) NOT NULL,
  `setting_value` TEXT         DEFAULT NULL,
  `description`   VARCHAR(255) DEFAULT NULL,
  `updated_by`    BIGINT UNSIGNED DEFAULT NULL,
  `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`setting_key`),
  CONSTRAINT `fk_setting_user` FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. DỊCH VỤ KHÁM CHỮA BỆNH
CREATE TABLE IF NOT EXISTS `services` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`          VARCHAR(150) NOT NULL,
  `slug`          VARCHAR(150) NOT NULL,
  `description`   TEXT         DEFAULT NULL,
  `duration_min`  INT          NOT NULL DEFAULT 30,
  `price`         DECIMAL(12,0) DEFAULT NULL,
  `icon_url`      VARCHAR(500) DEFAULT NULL,
  `sort_order`    INT          NOT NULL DEFAULT 0,
  `is_active`     TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_service_slug` (`slug`),
  INDEX `idx_service_active_sort` (`is_active`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. NGÀY NGHỈ PHÒNG KHÁM
CREATE TABLE IF NOT EXISTS `clinic_holidays` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `holiday_date` DATE         NOT NULL,
  `name`         VARCHAR(150) NOT NULL,
  `is_closed`    TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_holiday_date` (`holiday_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. BÁC SĨ
CREATE TABLE IF NOT EXISTS `doctors` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`           BIGINT UNSIGNED NOT NULL,
  `full_name`         VARCHAR(100) NOT NULL,
  `title`             VARCHAR(50)  DEFAULT NULL,
  `specialty`         VARCHAR(100) NOT NULL,
  `experience_years`  INT          DEFAULT NULL,
  `bio`               TEXT         DEFAULT NULL,
  `phone`             VARCHAR(15)  NOT NULL,
  `avatar_url`        VARCHAR(500) DEFAULT NULL,
  `sort_order`        INT          NOT NULL DEFAULT 0,
  `is_active`         TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_user` (`user_id`),
  INDEX `idx_doctor_specialty` (`specialty`),
  INDEX `idx_doctor_active_sort` (`is_active`, `sort_order`),
  CONSTRAINT `fk_doctor_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. KHUNG GIỜ KHÁM
CREATE TABLE IF NOT EXISTS `time_slots` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `slot_label`   VARCHAR(20) NOT NULL,
  `start_time`   TIME        NOT NULL,
  `end_time`     TIME        NOT NULL,
  `period`       ENUM('MORNING','AFTERNOON') NOT NULL,
  `max_patients` INT         NOT NULL DEFAULT 5,
  `sort_order`   INT         NOT NULL DEFAULT 0,
  `is_active`    TINYINT(1)  NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_slot_time` (`start_time`, `end_time`),
  CONSTRAINT `chk_slot_time` CHECK (`end_time` > `start_time`),
  CONSTRAINT `chk_slot_max` CHECK (`max_patients` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. LỊCH TRỰC BÁC SĨ
CREATE TABLE IF NOT EXISTS `doctor_schedules` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. NGÀY NGHỈ BÁC SĨ
CREATE TABLE IF NOT EXISTS `doctor_day_offs` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. HỒ SƠ BỆNH NHÂN
CREATE TABLE IF NOT EXISTS `patients` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT UNSIGNED DEFAULT NULL,
  `patient_code`    VARCHAR(20)  NOT NULL,
  `full_name`       VARCHAR(100) NOT NULL,
  `dob`             DATE         DEFAULT NULL,
  `national_id`     VARCHAR(20)  DEFAULT NULL,
  `phone`           VARCHAR(15)  NOT NULL,
  `email`           VARCHAR(100) DEFAULT NULL,
  `year_of_birth`   INT          DEFAULT NULL,
  `gender`          ENUM('MALE','FEMALE','OTHER') NOT NULL,
  `address`         VARCHAR(255) DEFAULT NULL,
  `relationship`    VARCHAR(50)  DEFAULT NULL,
  `medical_history` TEXT         DEFAULT NULL,
  `is_primary`      TINYINT(1)   NOT NULL DEFAULT 0,
  `is_active`       TINYINT(1)   NOT NULL DEFAULT 1,
  `locked_at`       DATETIME     DEFAULT NULL,
  `info_updated_at` DATETIME     DEFAULT NULL,
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_patient_code` (`patient_code`),
  INDEX `idx_patient_phone` (`phone`),
  INDEX `idx_patient_name` (`full_name`),
  INDEX `idx_patient_user` (`user_id`),
  INDEX `idx_patients_user_active` (`user_id`, `is_active`),
  CONSTRAINT `fk_patient_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. PHIẾU HẸN KHÁM BỆNH
CREATE TABLE IF NOT EXISTS `appointments` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `booking_code`        VARCHAR(30) NOT NULL,
  `patient_id`          BIGINT UNSIGNED NOT NULL,
  `doctor_id`           BIGINT UNSIGNED DEFAULT NULL,
  `service_id`          BIGINT UNSIGNED DEFAULT NULL,
  `service_type`        VARCHAR(30)  DEFAULT NULL,
  `appointment_date`    DATE        NOT NULL,
  `time_slot_id`        BIGINT UNSIGNED NOT NULL,
  `expected_time`       TIME        DEFAULT NULL,
  `symptoms`            TEXT        DEFAULT NULL,
  `booker_name`         VARCHAR(100) DEFAULT NULL,
  `booker_phone`        VARCHAR(15)  DEFAULT NULL,
  `source`              ENUM('WEB','PHONE','WALK_IN','ADMIN') NOT NULL DEFAULT 'WEB',
  `status`              ENUM('PENDING','CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED','NO_SHOW')
                        NOT NULL DEFAULT 'PENDING',
  `confirmed_by`        BIGINT UNSIGNED DEFAULT NULL,
  `confirmed_at`        DATETIME    DEFAULT NULL,
  `check_in_at`         DATETIME    DEFAULT NULL,
  `entered_at`          DATETIME    DEFAULT NULL,
  `cancel_reason`       VARCHAR(255) DEFAULT NULL,
  `cancelled_at`        DATETIME    DEFAULT NULL,
  `rescheduled_from_id` BIGINT UNSIGNED DEFAULT NULL,
  `reschedule_count`    INT         NOT NULL DEFAULT 0,
  `note`                VARCHAR(500) DEFAULT NULL,
  `created_at`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_app_rescheduled_from` FOREIGN KEY (`rescheduled_from_id`) REFERENCES `appointments`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. GIỮ CHỖ KHUNG GIỜ (CHỐNG VƯỢT CAPACITY)
CREATE TABLE IF NOT EXISTS `appointment_seats` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `appointment_date`  DATE NOT NULL,
  `time_slot_id`      BIGINT UNSIGNED NOT NULL,
  `seat_no`           INT NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. HÀNG ĐỢI CHECK-IN THỰC TẾ
CREATE TABLE IF NOT EXISTS `checkins` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `appointment_id`  BIGINT UNSIGNED NOT NULL,
  `status`          ENUM('WAITING', 'IN_ROOM', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'WAITING',
  `priority_group`  ENUM('PRIORITY', 'NORMAL') NOT NULL DEFAULT 'NORMAL',
  `seat_no`         INT NULL,
  `queue_no`        INT NOT NULL,
  `check_in_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `entered_at`      DATETIME NULL,
  `completed_at`    DATETIME NULL,
  `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_checkin_appointment` (`appointment_id`),
  INDEX `idx_checkin_queue` (`status`, `priority_group`, `queue_no`),
  INDEX `idx_checkin_seat` (`seat_no`, `status`),
  CONSTRAINT `fk_checkin_appointment` FOREIGN KEY (`appointment_id`) REFERENCES `appointments`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. LỊCH SỬ TRẠNG THÁI HẸN KHÁM
CREATE TABLE IF NOT EXISTS `appointment_status_history` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `appointment_id` BIGINT UNSIGNED NOT NULL,
  `old_status`     VARCHAR(20) DEFAULT NULL,
  `new_status`     VARCHAR(20) NOT NULL,
  `changed_by`     BIGINT UNSIGNED DEFAULT NULL,
  `reason`         VARCHAR(255) DEFAULT NULL,
  `changed_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_hist_appointment` (`appointment_id`, `changed_at`),
  CONSTRAINT `fk_hist_appointment` FOREIGN KEY (`appointment_id`) REFERENCES `appointments`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_hist_user` FOREIGN KEY (`changed_by`) REFERENCES `users`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. BỆNH ÁN Y HỌC CỔ TRUYỀN
CREATE TABLE IF NOT EXISTS `medical_records` (
  `id`                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `appointment_id`         BIGINT UNSIGNED NOT NULL,
  `patient_id`             BIGINT UNSIGNED NOT NULL,
  `doctor_id`              BIGINT UNSIGNED DEFAULT NULL,
  `visit_date`             DATE         DEFAULT NULL,
  `pulse_bp`               VARCHAR(50)  DEFAULT NULL,
  `pulse`                  VARCHAR(50)  DEFAULT NULL,
  `blood_pressure`         VARCHAR(50)  DEFAULT NULL,
  `diagnosis`              TEXT         DEFAULT NULL,
  `treatment`              TEXT         DEFAULT NULL,
  `treatment_acupuncture`  TEXT         DEFAULT NULL,
  `treatment_acupressure`  TEXT         DEFAULT NULL,
  `treatment_other`        TEXT         DEFAULT NULL,
  `prescription`           TEXT         DEFAULT NULL,
  `advice`                 TEXT         DEFAULT NULL,
  `doctor_name`            VARCHAR(100) DEFAULT NULL,
  `recorded_by`            BIGINT UNSIGNED DEFAULT NULL,
  `follow_up_date`         DATE         DEFAULT NULL,
  `created_at`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_appointment` (`appointment_id`),
  INDEX `idx_record_patient` (`patient_id`, `created_at`),
  INDEX `idx_record_followup` (`follow_up_date`),
  CONSTRAINT `fk_record_appointment` FOREIGN KEY (`appointment_id`) REFERENCES `appointments`(`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_record_patient` FOREIGN KEY (`patient_id`) REFERENCES `patients`(`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_record_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. LỊCH SỬ CHỈNH SỬA BỆNH ÁN
CREATE TABLE IF NOT EXISTS `medical_record_edits` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `record_id`   BIGINT UNSIGNED NOT NULL,
  `edited_by`   BIGINT UNSIGNED NOT NULL,
  `edited_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `changes`     JSON         NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_edit_record` (`record_id`, `edited_at`),
  CONSTRAINT `fk_edit_record` FOREIGN KEY (`record_id`) REFERENCES `medical_records`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_edit_user` FOREIGN KEY (`edited_by`) REFERENCES `users`(`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18. LIỆU TRÌNH ĐIỀU TRỊ
CREATE TABLE IF NOT EXISTS `treatment_sessions` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `medical_record_id` BIGINT UNSIGNED NOT NULL,
  `session_number`    INT          NOT NULL DEFAULT 1,
  `therapy_type`      VARCHAR(50)  NOT NULL,
  `points_used`       VARCHAR(255) DEFAULT NULL,
  `duration_minutes`  INT          NOT NULL DEFAULT 30,
  `patient_reaction`  TEXT         DEFAULT NULL,
  `therapist_id`      BIGINT UNSIGNED DEFAULT NULL,
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_session_record` (`medical_record_id`),
  CONSTRAINT `fk_session_record` FOREIGN KEY (`medical_record_id`) REFERENCES `medical_records`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 19. ĐƠN THUỐC
CREATE TABLE IF NOT EXISTS `prescriptions` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `medical_record_id` BIGINT UNSIGNED NOT NULL,
  `remedy_name`       VARCHAR(150) DEFAULT NULL,
  `dosage_instructions` TEXT       DEFAULT NULL,
  `total_doses`       INT          NOT NULL DEFAULT 1,
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_prescription_record` (`medical_record_id`),
  CONSTRAINT `fk_prescription_record` FOREIGN KEY (`medical_record_id`) REFERENCES `medical_records`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 20. CHI TIẾT VỊ THUỐC TRONG ĐƠN
CREATE TABLE IF NOT EXISTS `prescription_items` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `prescription_id` BIGINT UNSIGNED NOT NULL,
  `herb_name`       VARCHAR(100) NOT NULL,
  `weight_gram`     DECIMAL(6,1) NOT NULL,
  `note`            VARCHAR(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_item_prescription` (`prescription_id`),
  CONSTRAINT `fk_item_prescription` FOREIGN KEY (`prescription_id`) REFERENCES `prescriptions`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 21. DANH MỤC BÀI VIẾT
CREATE TABLE IF NOT EXISTS `article_categories` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(100) NOT NULL,
  `slug`        VARCHAR(100) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `sort_order`  INT          NOT NULL DEFAULT 0,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 22. BÀI VIẾT
CREATE TABLE IF NOT EXISTS `articles` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `title`         VARCHAR(255) NOT NULL,
  `slug`          VARCHAR(255) NOT NULL,
  `summary`       TEXT         NOT NULL,
  `content`       LONGTEXT     NOT NULL,
  `thumbnail_url` VARCHAR(500) DEFAULT NULL,
  `category_id`   BIGINT UNSIGNED DEFAULT NULL,
  `author_id`     BIGINT UNSIGNED NOT NULL,
  `is_published`  TINYINT(1)   NOT NULL DEFAULT 0,
  `is_featured`   TINYINT(1)   NOT NULL DEFAULT 0,
  `is_pinned`     TINYINT(1)   NOT NULL DEFAULT 0,
  `published_at`  DATETIME     DEFAULT NULL,
  `view_count`    INT          NOT NULL DEFAULT 0,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_slug` (`slug`),
  INDEX `idx_article_category` (`category_id`),
  INDEX `idx_article_published` (`is_published`, `published_at`),
  INDEX `idx_article_featured` (`is_featured`),
  INDEX `idx_article_pinned` (`is_pinned`),
  CONSTRAINT `fk_article_category` FOREIGN KEY (`category_id`) REFERENCES `article_categories`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_article_author` FOREIGN KEY (`author_id`) REFERENCES `users`(`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 23. THẺ (TAGS)
CREATE TABLE IF NOT EXISTS `tags` (
  `id`   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `slug` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 24. LIÊN KẾT BÀI VIẾT - THẺ
CREATE TABLE IF NOT EXISTS `article_tags` (
  `article_id` BIGINT UNSIGNED NOT NULL,
  `tag_id`     BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`article_id`, `tag_id`),
  CONSTRAINT `fk_at_article` FOREIGN KEY (`article_id`) REFERENCES `articles`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_at_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 25. BỘ ĐẾM SINH MÃ NGUYÊN TỬ
CREATE TABLE IF NOT EXISTS `counters` (
  `name`        VARCHAR(50) NOT NULL,
  `value`       BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 26. IDEMPOTENCY KEYS
CREATE TABLE IF NOT EXISTS `idempotency_keys` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `key_hash`      CHAR(64)     NOT NULL,
  `user_id`       BIGINT UNSIGNED NULL,
  `endpoint`      VARCHAR(255) NOT NULL,
  `request_hash`  CHAR(64)     NOT NULL,
  `response_json` LONGTEXT     NOT NULL,
  `status_code`   INT          NOT NULL DEFAULT 200,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idempotency_key_user` (`key_hash`, `user_id`),
  INDEX `idx_idempotency_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 27. MEDIA FILES
CREATE TABLE IF NOT EXISTS `media_files` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `file_name`   VARCHAR(255) NOT NULL,
  `file_path`   VARCHAR(500) NOT NULL,
  `file_size`   BIGINT       NOT NULL,
  `mime_type`   VARCHAR(100) NOT NULL,
  `uploaded_by` BIGINT UNSIGNED DEFAULT NULL,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_media_user` FOREIGN KEY (`uploaded_by`) REFERENCES `users`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 28. LIÊN HỆ, THÔNG BÁO, AUDIT LOG
CREATE TABLE IF NOT EXISTS `contact_messages` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `notification_logs` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `audit_logs` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED DEFAULT NULL,
  `action`      VARCHAR(50)  NOT NULL,
  `entity_type` VARCHAR(50)  NOT NULL,
  `entity_id`   BIGINT UNSIGNED DEFAULT NULL,
  `detail`      VARCHAR(500) DEFAULT NULL,
  `ip_address`  VARCHAR(45)  DEFAULT NULL,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_audit_user` (`user_id`,`created_at`),
  INDEX `idx_audit_entity` (`entity_type`,`entity_id`),
  CONSTRAINT `fk_audit_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
