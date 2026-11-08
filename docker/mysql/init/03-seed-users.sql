USE `phongkham_chuctu`;

-- ============================================================================
-- KHỞI TẠO TÀI KHOẢN MẪU HỆ THỐNG PHÒNG KHÁM CHÚC TỪ (DOCKER INIT)
-- Mật khẩu mặc định cho tất cả tài khoản mẫu: MatKhauAdmin@2026
-- ============================================================================

-- 1. Tài khoản Quản trị viên (admin / MatKhauAdmin@2026)
INSERT INTO `users` (`username`, `password_hash`, `full_name`, `email`, `phone`, `role`, `is_active`, `must_change_password`)
VALUES ('admin', '$2a$12$infhtIEJhboiMlUAcOOvu.ZZJzpbaVRFv97BUFVpswVx4CyMwX1jO', 'Quản trị viên Phòng khám', 'admin@chuctu.vn', '0912345678', 'ADMIN', 1, 0)
ON DUPLICATE KEY UPDATE 
  `password_hash` = '$2a$12$infhtIEJhboiMlUAcOOvu.ZZJzpbaVRFv97BUFVpswVx4CyMwX1jO', 
  `must_change_password` = 0,
  `is_active` = 1;

-- 2. Tài khoản Tiếp tân (letan / MatKhauAdmin@2026)
INSERT INTO `users` (`username`, `password_hash`, `full_name`, `phone`, `role`, `is_active`, `must_change_password`)
VALUES ('letan', '$2a$12$infhtIEJhboiMlUAcOOvu.ZZJzpbaVRFv97BUFVpswVx4CyMwX1jO', 'Lễ tân Chúc Từ', '0901234567', 'STAFF', 1, 0)
ON DUPLICATE KEY UPDATE 
  `password_hash` = VALUES(`password_hash`),
  `role` = 'STAFF',
  `is_active` = 1,
  `must_change_password` = 0;

-- 3. Tài khoản Bác sĩ (bacsi / MatKhauAdmin@2026)
INSERT INTO `users` (`username`, `password_hash`, `full_name`, `phone`, `role`, `is_active`, `must_change_password`)
VALUES ('bacsi', '$2a$12$infhtIEJhboiMlUAcOOvu.ZZJzpbaVRFv97BUFVpswVx4CyMwX1jO', 'Bác sĩ Nguyễn Văn Minh', '0911223344', 'DOCTOR', 1, 0)
ON DUPLICATE KEY UPDATE 
  `password_hash` = VALUES(`password_hash`),
  `role` = 'DOCTOR',
  `is_active` = 1,
  `must_change_password` = 0;

-- 4. Bác sĩ trong bảng doctors
INSERT INTO `doctors` (`user_id`, `full_name`, `title`, `specialty`, `experience_years`, `bio`, `phone`, `is_active`, `sort_order`)
SELECT `id`, `full_name`, 'Bác sĩ Y học cổ truyền', 'Cơ xương khớp & Châm cứu', 15, 'Bác sĩ Y học cổ truyền giàu kinh nghiệm bắt mạch và bốc thuốc nam', `phone`, 1, 1
FROM `users` WHERE `username` = 'bacsi'
ON DUPLICATE KEY UPDATE 
  `full_name` = VALUES(`full_name`),
  `is_active` = 1;
