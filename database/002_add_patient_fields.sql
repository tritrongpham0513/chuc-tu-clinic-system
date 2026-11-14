-- ============================================================================
-- PHÒNG KHÁM CHÚC TỪ - MIGRATION 002: BỔ SUNG CỘT HỒ SƠ BỆNH NHÂN
-- MySQL 8.0+ | InnoDB | utf8mb4_unicode_ci
-- ============================================================================
-- Ghi chú: Hai cột này đã được gộp trực tiếp vào 001_schema_v2.sql.
-- File này được giữ lại để tương thích với các môi trường cũ, và được viết
-- theo hướng IDEMPOTENT (chạy lại nhiều lần vẫn an toàn).
-- MySQL 8.0 không hỗ trợ `ADD COLUMN IF NOT EXISTS` nên phải kiểm tra
-- information_schema trước khi thêm.
-- ============================================================================

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

CALL ct_add_column('patients', 'is_primary', 'TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = hồ sơ chính chủ (không xóa được)'' AFTER `medical_history`');
CALL ct_add_column('patients', 'is_active',  'TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''0 = đã xóa mềm/unlink'' AFTER `is_primary`');

DROP PROCEDURE IF EXISTS `ct_add_column`;
