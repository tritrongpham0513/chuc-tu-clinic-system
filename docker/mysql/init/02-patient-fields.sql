-- Migration 002: Thêm cột is_primary và is_active vào bảng patients
-- Chạy trên DB phongkham_chuctu và phongkham_chuctu_test

USE `phongkham_chuctu`;

ALTER TABLE `patients`
  ADD COLUMN IF NOT EXISTS `is_primary` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = hồ sơ chính chủ (không xóa được)' AFTER `medical_history`,
  ADD COLUMN IF NOT EXISTS `is_active`  TINYINT(1) NOT NULL DEFAULT 1 COMMENT '0 = đã xóa mềm/unlink' AFTER `is_primary`;

-- Index để query nhanh
ALTER TABLE `patients`
  ADD INDEX IF NOT EXISTS `idx_patients_user_active` (`user_id`, `is_active`);

-- Đánh dấu hồ sơ có relationship = 'Bản thân' là primary (nếu có data cũ)
UPDATE `patients` SET `is_primary` = 1 WHERE `relationship` = 'Bản thân' OR `relationship` IS NULL;
