-- ============================================================================
-- PHÒNG KHÁM CHÚC TỪ - MIGRATION 008: ĐỒNG NHẤT SỨC CHỨA KHUNG GIỜ VÀ ĐỔI LỊCH HẸN
-- MySQL 8.0+ | InnoDB | utf8mb4_unicode_ci
-- ============================================================================
-- Script Idempotent: có thể chạy nhiều lần an toàn mà không sinh lỗi hay nhân đôi dữ liệu.
-- Không sử dụng USE ...;

SET NAMES utf8mb4;

-- 1. Chuẩn hóa thủ tục hỗ trợ thêm cột an toàn (nếu chưa có)
DROP PROCEDURE IF EXISTS `ct_add_column`;

DELIMITER $$
CREATE PROCEDURE `ct_add_column`(
    IN p_table  VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_table
          AND COLUMN_NAME  = p_column
    ) THEN
        SET @ct_sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE ct_stmt FROM @ct_sql;
        EXECUTE ct_stmt;
        DEALLOCATE PREPARE ct_stmt;
    END IF;
END$$
DELIMITER ;

-- 2. Thêm cột số lần đổi lịch (reschedule_count) vào bảng appointments
CALL ct_add_column('appointments', 'reschedule_count', 'INT NOT NULL DEFAULT 0 COMMENT ''Số lần bệnh nhân đổi lịch hẹn (tối đa 1 lần)'' AFTER `rescheduled_from_id`');

DROP PROCEDURE IF EXISTS `ct_add_column`;

-- 3. Cập nhật slot_capacity = 5 để đồng nhất với số ghế khám thực tế (seats = 5)
UPDATE clinic_settings
SET setting_value = '5'
WHERE setting_key = 'slot_capacity';

INSERT INTO clinic_settings (setting_key, setting_value, description)
VALUES ('slot_capacity', '5', 'Số bệnh nhân tối đa trong 1 khung giờ (đồng bộ với 5 ghế khám)')
ON DUPLICATE KEY UPDATE setting_value = '5', description = VALUES(description);

-- 4. Đồng bộ max_patients của các khung giờ về tối đa 5
UPDATE `time_slots`
SET `max_patients` = 5
WHERE `max_patients` > 5;

