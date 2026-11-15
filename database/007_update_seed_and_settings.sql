-- ============================================================================
-- PHÒNG KHÁM CHÚC TỪ - MIGRATION 007: CẬP NHẬT CẤU HÌNH PHÒNG KHÁM VÀ TÀI KHOẢN MẪU
-- MySQL 8.0+ | InnoDB | utf8mb4_unicode_ci
-- ============================================================================
-- Script Idempotent: có thể chạy nhiều lần an toàn mà không sinh lỗi hay nhân đôi dữ liệu.
-- Không sử dụng USE ...;

SET NAMES utf8mb4;

-- 1. Cập nhật số hotline phòng khám (thay thế số giả 0900000000)
UPDATE clinic_settings
SET setting_value = '0912345678'
WHERE setting_key = 'hotline' AND (setting_value = '0900000000' OR setting_value IS NULL OR setting_value = '');

-- 2. Cập nhật email cho tài khoản mẫu letan và bacsi nếu chưa có
UPDATE users
SET email = 'letan@chuctu.vn'
WHERE username = 'letan' AND (email IS NULL OR email = '');

UPDATE users
SET email = 'bsnam@chuctu.vn'
WHERE username = 'bacsi' AND (email IS NULL OR email = '');

-- 3. Đảm bảo cấu hình các thông số vận hành đúng chuẩn
INSERT INTO clinic_settings (setting_key, setting_value, description)
VALUES 
    ('slot_capacity', '6', 'Số bệnh nhân tối đa trong 1 khung giờ'),
    ('seats', '5', 'Số ghế khám thực tế trong phòng'),
    ('user_max_days', '30', 'Số ngày người bệnh được phép đặt trước tối đa'),
    ('counter_max_days', '7', 'Số ngày tiếp tân đặt hẹn tại quầy tối đa'),
    ('max_profiles', '10', 'Số hồ sơ người thân tối đa được tạo trên 1 tài khoản'),
    ('booking_advance_days', '14', 'Cho phép đặt lịch trước tối đa bao nhiêu ngày'),
    ('booking_min_hours', '2', 'Phải đặt trước giờ khám tối thiểu bao nhiêu giờ'),
    ('priority_early_minutes', '15', 'Số phút đến sớm trước khung giờ để được xếp nhóm ưu tiên'),
    ('no_show_cancel_at', '17:00', 'Giờ hệ thống tự động hủy các lịch chưa đến trong ngày'),
    ('reception_open', '08:00', 'Giờ mở cửa tiếp nhận'),
    ('reception_close', '16:30', 'Giờ đóng cửa tiếp nhận'),
    ('auto_invite', '0', 'Tự động mời người kế tiếp vào ghế khi có ghế trống')
ON DUPLICATE KEY UPDATE description = VALUES(description);
