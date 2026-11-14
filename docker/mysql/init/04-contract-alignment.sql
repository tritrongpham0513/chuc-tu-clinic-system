-- ============================================================================
-- PHÒNG KHÁM CHÚC TỪ - MIGRATION 003: BỔ SUNG CỘT & ĐỒNG BỘ CONTRACT v1
-- MySQL 8.0+ | InnoDB | utf8mb4_unicode_ci
-- ============================================================================
-- LƯU Ý QUAN TRỌNG:
--   * File này KHÔNG chứa lệnh `USE ...`, mà thao tác trên database đang được
--     chọn ở phía client. Nhờ vậy cùng một file chạy được cho cả
--     `phongkham_chuctu` và `phongkham_chuctu_test`, đồng thời dùng được cho
--     thư mục docker/mysql/init (MySQL container tự chọn MYSQL_DATABASE).
--   * MySQL 8.0 KHÔNG hỗ trợ `ADD COLUMN IF NOT EXISTS` (chỉ MariaDB hỗ trợ).
--     Vì vậy mọi thao tác thêm cột được thực hiện qua thủ tục `ct_add_column`
--     có kiểm tra information_schema => chạy lại nhiều lần vẫn an toàn.
--
-- Cách chạy:
--   mysql -u <user> -p phongkham_chuctu < database/003_contract_alignment.sql
--
-- Hướng dẫn rollback (nếu cần):
--   ALTER TABLE `patients` DROP COLUMN `dob`, DROP COLUMN `national_id`, DROP COLUMN `email`, DROP COLUMN `locked_at`, DROP COLUMN `info_updated_at`;
--   ALTER TABLE `appointments` DROP COLUMN `service_type`, DROP COLUMN `expected_time`, DROP COLUMN `cancelled_at`, DROP COLUMN `rescheduled_from_id`, DROP COLUMN `check_in_at`, DROP COLUMN `entered_at`;
--   ALTER TABLE `medical_records` DROP COLUMN `visit_date`, DROP COLUMN `pulse`, DROP COLUMN `blood_pressure`, DROP COLUMN `treatment_acupuncture`, DROP COLUMN `treatment_acupressure`, DROP COLUMN `treatment_other`, DROP COLUMN `doctor_name`, DROP COLUMN `recorded_by`;
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0. THỦ TỤC HỖ TRỢ: Thêm cột nếu chưa tồn tại (idempotent)
-- ----------------------------------------------------------------------------
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

-- ----------------------------------------------------------------------------
-- 1. BẢNG PATIENTS: Bổ sung trường thông tin cá nhân & cho phép year_of_birth NULL
-- ----------------------------------------------------------------------------
CALL ct_add_column('patients', 'dob',             'DATE NULL COMMENT ''Ngày sinh đầy đủ (nếu có)'' AFTER `full_name`');
CALL ct_add_column('patients', 'national_id',     'VARCHAR(20) NULL COMMENT ''CCCD / CMND'' AFTER `dob`');
CALL ct_add_column('patients', 'email',           'VARCHAR(100) NULL COMMENT ''Email người bệnh'' AFTER `phone`');
CALL ct_add_column('patients', 'locked_at',       'DATETIME NULL COMMENT ''Thời điểm khóa hồ sơ'' AFTER `is_active`');
CALL ct_add_column('patients', 'info_updated_at', 'DATETIME NULL COMMENT ''Thời điểm cập nhật thông tin gần nhất'' AFTER `locked_at`');

-- year_of_birth: cho phép NULL (người lớn tuổi có thể chỉ nhớ năm sinh)
ALTER TABLE `patients`
  MODIFY COLUMN `year_of_birth` INT NULL;

-- ----------------------------------------------------------------------------
-- 2. BẢNG APPOINTMENTS: Bổ sung trường điều phối, đổi giờ & hủy
-- ----------------------------------------------------------------------------
CALL ct_add_column('appointments', 'service_type',        'VARCHAR(30) NULL COMMENT ''CHAM_CUU, XOA_BOP_BAM_HUYET, BOC_THUOC, VAT_LY_TRI_LIEU, CHUA_RO'' AFTER `service_id`');
CALL ct_add_column('appointments', 'expected_time',       'TIME NULL COMMENT ''Giờ dự kiến khám'' AFTER `time_slot_id`');
CALL ct_add_column('appointments', 'cancelled_at',        'DATETIME NULL COMMENT ''Thời điểm hủy lịch'' AFTER `cancel_reason`');
CALL ct_add_column('appointments', 'rescheduled_from_id', 'BIGINT UNSIGNED NULL COMMENT ''ID phiếu khám cũ nếu đây là phiếu dời lịch'' AFTER `cancelled_at`');
CALL ct_add_column('appointments', 'check_in_at',         'DATETIME NULL COMMENT ''Thời điểm tiếp nhận (check-in) thực tế'' AFTER `confirmed_at`');
CALL ct_add_column('appointments', 'entered_at',          'DATETIME NULL COMMENT ''Thời điểm vào ghế khám'' AFTER `check_in_at`');

-- Thêm foreign key tham chiếu đến chính appointments cho rescheduled_from_id nếu chưa có
SET @fk_check = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                 WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'fk_app_rescheduled_from');
SET @sql_fk = IF(@fk_check = 0,
    'ALTER TABLE `appointments` ADD CONSTRAINT `fk_app_rescheduled_from` FOREIGN KEY (`rescheduled_from_id`) REFERENCES `appointments`(`id`) ON DELETE SET NULL ON UPDATE CASCADE',
    'DO 0');
PREPARE stmt_fk FROM @sql_fk;
EXECUTE stmt_fk;
DEALLOCATE PREPARE stmt_fk;

-- ----------------------------------------------------------------------------
-- 3. BẢNG MEDICAL_RECORDS: Cho phép doctor_id NULL & tách riêng phương pháp
-- ----------------------------------------------------------------------------
ALTER TABLE `medical_records`
  MODIFY COLUMN `doctor_id` BIGINT UNSIGNED NULL;

CALL ct_add_column('medical_records', 'visit_date',            'DATE NULL COMMENT ''Ngày đến khám thực tế'' AFTER `doctor_id`');
CALL ct_add_column('medical_records', 'pulse',                 'VARCHAR(50) NULL COMMENT ''Mạch'' AFTER `pulse_bp`');
CALL ct_add_column('medical_records', 'blood_pressure',        'VARCHAR(50) NULL COMMENT ''Huyết áp'' AFTER `pulse`');
CALL ct_add_column('medical_records', 'treatment_acupuncture', 'TEXT NULL COMMENT ''Phương pháp châm cứu'' AFTER `treatment`');
CALL ct_add_column('medical_records', 'treatment_acupressure', 'TEXT NULL COMMENT ''Phương pháp bấm huyệt / xoa bóp'' AFTER `treatment_acupuncture`');
CALL ct_add_column('medical_records', 'treatment_other',       'TEXT NULL COMMENT ''Phương pháp trị liệu khác'' AFTER `treatment_acupressure`');
CALL ct_add_column('medical_records', 'doctor_name',           'VARCHAR(100) NULL COMMENT ''Tên bác sĩ khám'' AFTER `treatment_other`');
CALL ct_add_column('medical_records', 'recorded_by',           'BIGINT UNSIGNED NULL COMMENT ''Người lập bệnh án'' AFTER `doctor_name`');

-- ----------------------------------------------------------------------------
-- 3.1. BẢNG OTP_VERIFICATIONS: Mở rộng otp_code để lưu SHA-256 hash (64 ký tự)
-- ----------------------------------------------------------------------------
ALTER TABLE `otp_verifications`
  MODIFY COLUMN `otp_code` VARCHAR(64) NOT NULL COMMENT 'Mã OTP hoặc SHA-256 hash của mã OTP';

-- ----------------------------------------------------------------------------
-- 4. BẢNG CLINIC_SETTINGS: Cập nhật các thông số vận hành chuẩn cho v1
-- ----------------------------------------------------------------------------
INSERT INTO `clinic_settings` (`setting_key`, `setting_value`, `description`)
VALUES
  ('slot_capacity', '6', 'Số bệnh nhân tối đa trong 1 khung giờ'),
  ('seats', '5', 'Số ghế khám thực tế trong phòng'),
  ('slot_minutes', '60', 'Thời lượng mỗi khung giờ khám (phút)'),
  ('user_max_days', '30', 'Số ngày người bệnh được phép đặt trước tối đa'),
  ('counter_max_days', '7', 'Số ngày tiếp tân đặt hộ tại quầy tối đa'),
  ('priority_early_minutes', '15', 'Số phút đến sớm trước khung giờ để được xếp nhóm Ưu tiên'),
  ('max_profiles', '10', 'Số hồ sơ người thân tối đa được tạo trên 1 tài khoản'),
  ('auto_invite', '0', 'Tự động mời người kế tiếp vào ghế khi có ghế trống (1: bật, 0: tắt)'),
  ('reception_open', '08:00', 'Giờ mở cửa tiếp nhận'),
  ('reception_close', '16:30', 'Giờ đóng cửa tiếp nhận'),
  ('no_show_cancel_at', '17:00', 'Giờ hệ thống tự động hủy các lịch chưa đến trong ngày')
ON DUPLICATE KEY UPDATE
  `setting_value` = VALUES(`setting_value`),
  `description` = VALUES(`description`);

-- ----------------------------------------------------------------------------
-- 5. DỌN THỦ TỤC TẠM
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `ct_add_column`;
