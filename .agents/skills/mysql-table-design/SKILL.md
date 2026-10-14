---
name: mysql-table-design
description: >-
  Áp dụng khi thiết kế, tạo mới hoặc tối ưu hóa Schema, Bảng (Tables), Chỉ mục (Indexes),
  Khóa ngoại và Ràng buộc cho Cơ sở dữ liệu MySQL (phiên bản 8.0+) trong dự án Phòng Khám Chúc Từ.
---

# MySQL Table Design & Optimization Guidelines

Cẩm nang thiết kế cơ sở dữ liệu MySQL chuẩn mực, bảo đảm hiệu năng cao và tính toàn vẹn dữ liệu y tế.

---

## 1. Các Quy Tắc Cốt Lõi Cho MySQL (InnoDB Engine)
- **Engine lưu trữ**: Luôn sử dụng `ENGINE=InnoDB` để hỗ trợ Giao dịch (ACID Transactions) và Khóa ngoại (Foreign Keys).
- **Bộ mã ký tự (Charset) Tiếng Việt**: 
  - `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci` (hoặc `utf8mb4_0900_ai_ci` trên MySQL 8.0).
  - Giúp lưu tên tiếng Việt có dấu, emoji, ký tự y khoa không bị lỗi font (lỗi `?`).
- **Khóa chính (Primary Key)**: 
  - Bảng nghiệp vụ nội bộ: `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY`.
  - Bảng giao dịch công khai: Có thể dùng `VARCHAR(36)` (UUID v4) hoặc `BINARY(16)` (UUID v7 sắp xếp theo thời gian) để tránh lộ số lượng bệnh nhân.
- **Thời gian (Timestamps)**: 
  - Dùng `DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP` cho `created_at`.
  - Dùng `DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` cho `updated_at`.

---

## 2. Thiết Kế Schema Mẫu Cho Phòng Khám Chúc Từ

### Bảng 1: Bệnh Nhân (`patients`)
```sql
CREATE TABLE `patients` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `patient_code` VARCHAR(20) NOT NULL COMMENT 'Mã BN: BN-2026-0001',
  `full_name` VARCHAR(100) NOT NULL,
  `phone` VARCHAR(15) NOT NULL,
  `date_of_birth` DATE NOT NULL,
  `gender` ENUM('MALE', 'FEMALE', 'OTHER') NOT NULL,
  `address` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_patient_code` (`patient_code`),
  INDEX `idx_patient_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Bảng 2: Bác Sĩ (`doctors`)
```sql
CREATE TABLE `doctors` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `full_name` VARCHAR(100) NOT NULL,
  `specialty` VARCHAR(100) NOT NULL COMMENT 'Chuyên khoa: Y học cổ truyền, Châm cứu, v.v.',
  `phone` VARCHAR(15) NOT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_doctor_specialty` (`specialty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Bảng 3: Lịch Hẹn Khám (`appointments`) - Chống Trùng Ca (Double Booking)
```sql
CREATE TABLE `appointments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `appointment_code` VARCHAR(30) NOT NULL COMMENT 'PKCT-YYYYMMDD-XXXX',
  `patient_id` BIGINT UNSIGNED NOT NULL,
  `doctor_id` BIGINT UNSIGNED NOT NULL,
  `appointment_date` DATE NOT NULL,
  `time_slot` VARCHAR(20) NOT NULL COMMENT 'VD: 08:00-08:30',
  `status` ENUM('PENDING', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
  `symptoms` TEXT DEFAULT NULL,
  `cancel_reason` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_appointment_code` (`appointment_code`),
  -- Ràng buộc chống trùng lịch: 1 Bác sĩ chỉ nhận 1 ca trong 1 khung giờ ngày đó
  UNIQUE KEY `uk_doctor_slot` (`doctor_id`, `appointment_date`, `time_slot`),
  INDEX `idx_app_date_status` (`appointment_date`, `status`),
  CONSTRAINT `fk_app_patient` FOREIGN KEY (`patient_id`) REFERENCES `patients` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_app_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```
