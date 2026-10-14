-- ============================================================================
-- 🏥 PHÒNG KHÁM CHÚC TỪ - DATABASE SCHEMA
-- ============================================================================
-- Phiên bản  : 1.0.0
-- Ngày tạo   : 2026-10-14
-- Engine     : InnoDB (ACID Transactions, Foreign Keys)
-- Charset    : utf8mb4_unicode_ci (Hỗ trợ tiếng Việt có dấu)
-- MySQL      : 8.0+
-- ============================================================================
-- THỨ TỰ TẠO BẢNG (theo thứ tự phụ thuộc khóa ngoại):
--   1. users               (Tài khoản đăng nhập)
--   2. doctors              (Hồ sơ bác sĩ → FK users)
--   3. patients             (Hồ sơ bệnh nhân)
--   4. time_slots           (Cấu hình khung giờ khám)
--   5. doctor_schedules     (Lịch làm việc bác sĩ → FK doctors)
--   6. appointments         (Lịch hẹn khám → FK patients, doctors)
--   7. article_categories   (Danh mục bài viết)
--   8. articles             (Bài viết sức khỏe → FK users, article_categories)
-- ============================================================================

-- Tạo Database (nếu chưa có)
CREATE DATABASE IF NOT EXISTS `phongkham_chuctu`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `phongkham_chuctu`;

-- ============================================================================
-- BẢNG 1: TÀI KHOẢN HỆ THỐNG (users)
-- Mục đích: Quản lý đăng nhập JWT cho Admin, Bác sĩ, Lễ tân
-- ============================================================================
CREATE TABLE `users` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username`      VARCHAR(50)     NOT NULL                   COMMENT 'Tên đăng nhập, duy nhất',
  `password_hash` VARCHAR(255)    NOT NULL                   COMMENT 'Mật khẩu đã mã hóa BCrypt',
  `full_name`     VARCHAR(100)    NOT NULL                   COMMENT 'Họ và tên đầy đủ',
  `email`         VARCHAR(100)    DEFAULT NULL               COMMENT 'Email liên hệ (không bắt buộc)',
  `phone`         VARCHAR(15)     NOT NULL                   COMMENT 'Số điện thoại liên hệ',
  `role`          ENUM('ADMIN', 'DOCTOR', 'STAFF', 'PATIENT') NOT NULL DEFAULT 'PATIENT'
                                                             COMMENT 'Vai trò: Quản trị / Bác sĩ / Lễ tân / Bệnh nhân',
  `is_active`     TINYINT(1)      NOT NULL DEFAULT 1         COMMENT '1 = Hoạt động, 0 = Khóa tài khoản',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  INDEX `idx_user_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Bảng tài khoản đăng nhập hệ thống nội bộ';

-- ============================================================================
-- BẢNG 2: HỒ SƠ BÁC SĨ (doctors)
-- Mục đích: Lưu thông tin chuyên môn, liên kết 1-1 với users
-- ============================================================================
CREATE TABLE `doctors` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT UNSIGNED NOT NULL                   COMMENT 'FK → users.id (1 bác sĩ = 1 tài khoản)',
  `full_name`     VARCHAR(100)    NOT NULL                   COMMENT 'Họ tên bác sĩ (hiển thị trên web)',
  `specialty`     VARCHAR(100)    NOT NULL                   COMMENT 'Chuyên khoa: Y học cổ truyền, Châm cứu, PHCN...',
  `bio`           TEXT            DEFAULT NULL               COMMENT 'Giới thiệu ngắn về bác sĩ',
  `phone`         VARCHAR(15)     NOT NULL                   COMMENT 'SĐT liên hệ trực tiếp',
  `avatar_url`    VARCHAR(500)    DEFAULT NULL               COMMENT 'Đường dẫn ảnh đại diện (WebP)',
  `is_active`     TINYINT(1)      NOT NULL DEFAULT 1         COMMENT '1 = Đang khám, 0 = Tạm nghỉ',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_user` (`user_id`),
  INDEX `idx_doctor_specialty` (`specialty`),
  CONSTRAINT `fk_doctor_user` FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Hồ sơ chuyên môn bác sĩ phòng khám';

-- ============================================================================
-- BẢNG 3: HỒ SƠ BỆNH NHÂN (patients)
-- Mục đích: Lưu thông tin bệnh nhân đăng ký qua form web
-- ============================================================================
CREATE TABLE `patients` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT UNSIGNED DEFAULT NULL               COMMENT 'FK → users.id (NULL nếu đặt lịch không cần tài khoản)',
  `patient_code`  VARCHAR(20)     NOT NULL                   COMMENT 'Mã bệnh nhân: BN-2026-0001',
  `full_name`     VARCHAR(100)    NOT NULL                   COMMENT 'Họ và tên bệnh nhân',
  `phone`         VARCHAR(15)     NOT NULL                   COMMENT 'SĐT liên hệ (10 số, đầu 03/05/07/08/09)',
  `year_of_birth` INT             NOT NULL                   COMMENT 'Năm sinh (VD: 1955)',
  `gender`        ENUM('MALE', 'FEMALE', 'OTHER') NOT NULL   COMMENT 'Giới tính: Nam / Nữ / Khác',
  `address`       VARCHAR(255)    DEFAULT NULL               COMMENT 'Địa chỉ (không bắt buộc)',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_patient_code` (`patient_code`),
  INDEX `idx_patient_user` (`user_id`),
  INDEX `idx_patient_phone` (`phone`),
  CONSTRAINT `fk_patient_user` FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Hồ sơ bệnh nhân đăng ký khám';

-- ============================================================================
-- BẢNG 4: CẤU HÌNH KHUNG GIỜ KHÁM (time_slots)
-- Mục đích: Định nghĩa các slot 30 phút (sáng + chiều), giới hạn số bệnh nhân
-- ============================================================================
CREATE TABLE `time_slots` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `slot_label`    VARCHAR(20)     NOT NULL                   COMMENT 'Nhãn hiển thị: 08:00 - 08:30',
  `start_time`    TIME            NOT NULL                   COMMENT 'Giờ bắt đầu slot',
  `end_time`      TIME            NOT NULL                   COMMENT 'Giờ kết thúc slot',
  `period`        ENUM('MORNING', 'AFTERNOON') NOT NULL      COMMENT 'Ca: Sáng (08:00-11:30) / Chiều (13:30-17:00)',
  `max_patients`  INT             NOT NULL DEFAULT 3         COMMENT 'Số bệnh nhân tối đa mỗi slot mỗi bác sĩ',
  `sort_order`    INT             NOT NULL DEFAULT 0         COMMENT 'Thứ tự sắp xếp hiển thị',
  `is_active`     TINYINT(1)      NOT NULL DEFAULT 1         COMMENT '1 = Đang dùng, 0 = Tạm ẩn',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_slot_time` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Cấu hình khung giờ khám bệnh (mỗi slot 30 phút)';

-- ============================================================================
-- BẢNG 5: LỊCH LÀM VIỆC BÁC SĨ (doctor_schedules)
-- Mục đích: Xác định bác sĩ nào làm việc vào thứ mấy, ca nào
-- ============================================================================
CREATE TABLE `doctor_schedules` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `doctor_id`     BIGINT UNSIGNED NOT NULL                   COMMENT 'FK → doctors.id',
  `day_of_week`   ENUM('MON','TUE','WED','THU','FRI','SAT','SUN') NOT NULL
                                                             COMMENT 'Ngày trong tuần',
  `period`        ENUM('MORNING', 'AFTERNOON', 'FULL_DAY') NOT NULL
                                                             COMMENT 'Ca: Sáng / Chiều / Cả ngày',
  `is_active`     TINYINT(1)      NOT NULL DEFAULT 1         COMMENT '1 = Đang áp dụng, 0 = Tạm nghỉ',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  -- Mỗi bác sĩ chỉ có 1 lịch cho mỗi ngày + ca
  UNIQUE KEY `uk_doctor_day_period` (`doctor_id`, `day_of_week`, `period`),
  CONSTRAINT `fk_schedule_doctor` FOREIGN KEY (`doctor_id`)
    REFERENCES `doctors` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Lịch làm việc hàng tuần của bác sĩ';

-- ============================================================================
-- BẢNG 6: LỊCH HẸN KHÁM BỆNH (appointments) ⭐ BẢNG TRỌNG YẾU
-- Mục đích: Ghi nhận đặt lịch, chống trùng ca, quản lý trạng thái
-- ============================================================================
CREATE TABLE `appointments` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `booking_code`     VARCHAR(30)     NOT NULL                COMMENT 'Mã lịch hẹn: PKCT-YYYYMMDD-XXXX',
  `patient_id`       BIGINT UNSIGNED NOT NULL                COMMENT 'FK → patients.id',
  `doctor_id`        BIGINT UNSIGNED DEFAULT NULL            COMMENT 'FK → doctors.id (NULL nếu bệnh nhân chưa chọn BS, lễ tân gán sau)',
  `appointment_date` DATE            NOT NULL                COMMENT 'Ngày khám',
  `time_slot_id`     BIGINT UNSIGNED NOT NULL                COMMENT 'FK → time_slots.id (Khung giờ)',
  `symptoms`         TEXT            DEFAULT NULL            COMMENT 'Mô tả triệu chứng / lý do khám',
  `status`           ENUM('PENDING', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
                                     NOT NULL DEFAULT 'PENDING'
                                                             COMMENT 'Trạng thái xử lý lịch hẹn',
  `cancel_reason`    VARCHAR(255)    DEFAULT NULL            COMMENT 'Lý do hủy (nếu CANCELLED)',
  `note`             VARCHAR(500)    DEFAULT NULL            COMMENT 'Ghi chú nội bộ của lễ tân/bác sĩ',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_booking_code` (`booking_code`),

  -- ⚡ HỖ TRỢ KIỂM TRA CHỐNG TRÙNG LỊCH (Double Booking Prevention):
  -- Tầng Service sẽ đếm số ca theo (date, doctor, time_slot) so với max_patients
  INDEX `idx_app_date_doctor` (`appointment_date`, `doctor_id`, `time_slot_id`),
  INDEX `idx_app_status` (`status`),
  INDEX `idx_app_patient` (`patient_id`),

  CONSTRAINT `fk_app_patient` FOREIGN KEY (`patient_id`)
    REFERENCES `patients` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_app_doctor` FOREIGN KEY (`doctor_id`)
    REFERENCES `doctors` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_app_timeslot` FOREIGN KEY (`time_slot_id`)
    REFERENCES `time_slots` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Lịch hẹn khám bệnh - bảng trọng yếu nhất hệ thống';

-- ============================================================================
-- BẢNG 7: DANH MỤC BÀI VIẾT (article_categories)
-- Mục đích: Phân loại bài viết cẩm nang sức khỏe
-- ============================================================================
CREATE TABLE `article_categories` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`          VARCHAR(100)    NOT NULL                   COMMENT 'Tên danh mục: Mẹo dân gian, Châm cứu...',
  `slug`          VARCHAR(100)    NOT NULL                   COMMENT 'URL-friendly: meo-dan-gian',
  `description`   VARCHAR(255)    DEFAULT NULL               COMMENT 'Mô tả ngắn danh mục',
  `sort_order`    INT             NOT NULL DEFAULT 0         COMMENT 'Thứ tự hiển thị',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Danh mục phân loại bài viết cẩm nang sức khỏe';

-- ============================================================================
-- BẢNG 8: BÀI VIẾT CẨM NANG SỨC KHỎE (articles)
-- Mục đích: Lưu bài viết mẹo chữa bệnh, tin phòng khám, hoạt động từ thiện
-- ============================================================================
CREATE TABLE `articles` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `title`         VARCHAR(255)    NOT NULL                   COMMENT 'Tiêu đề bài viết',
  `slug`          VARCHAR(255)    NOT NULL                   COMMENT 'URL-friendly: cach-chua-dau-lung',
  `summary`       TEXT            NOT NULL                   COMMENT 'Tóm tắt ngắn hiển thị ở danh sách',
  `content`       LONGTEXT        NOT NULL                   COMMENT 'Nội dung đầy đủ (HTML hoặc Markdown)',
  `thumbnail_url` VARCHAR(500)    DEFAULT NULL               COMMENT 'Ảnh đại diện bài viết (WebP)',
  `category_id`   BIGINT UNSIGNED DEFAULT NULL               COMMENT 'FK → article_categories.id',
  `author_id`     BIGINT UNSIGNED NOT NULL                   COMMENT 'FK → users.id (Người viết bài)',
  `is_published`  TINYINT(1)      NOT NULL DEFAULT 0         COMMENT '1 = Xuất bản, 0 = Bản nháp',
  `published_at`  DATETIME        DEFAULT NULL               COMMENT 'Thời điểm xuất bản',
  `view_count`    INT UNSIGNED    NOT NULL DEFAULT 0         COMMENT 'Số lượt xem bài viết',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_slug` (`slug`),
  INDEX `idx_article_published` (`is_published`, `published_at`),
  INDEX `idx_article_category` (`category_id`),
  CONSTRAINT `fk_article_category` FOREIGN KEY (`category_id`)
    REFERENCES `article_categories` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_article_author` FOREIGN KEY (`author_id`)
    REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Bài viết cẩm nang sức khỏe, mẹo chữa bệnh';

-- ============================================================================
-- ✅ HOÀN TẤT: 8 BẢNG ĐÃ ĐƯỢC TẠO THÀNH CÔNG
-- ============================================================================
