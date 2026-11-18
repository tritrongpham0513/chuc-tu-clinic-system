-- ============================================================================
-- PHÒNG KHÁM CHÚC TỪ - FLYWAY MIGRATION V2: SEED MASTER DATA CHUẨN
-- MySQL 8.0+ | InnoDB | utf8mb4_unicode_ci
-- ============================================================================

SET NAMES utf8mb4;

-- 1. CẤU HÌNH PHÒNG KHÁM
INSERT INTO `clinic_settings` (`setting_key`, `setting_value`, `description`) VALUES
('clinic_name', 'Phòng Khám Chúc Từ', 'Tên phòng khám'),
('hotline', '0900000000', 'Số hotline bác sĩ trực'),
('address', '57/8-10 Điện Biên Phủ, Phường 25, Quận Bình Thạnh, TP. Hồ Chí Minh', 'Địa chỉ phòng khám'),
('map_lat', '10.7769', 'Vĩ độ Google Maps'),
('map_lng', '106.7009', 'Kinh độ Google Maps'),
('working_hours', '8:00 - 16:30', 'Giờ làm việc hiển thị trên web'),
('booking_advance_days', '14', 'Cho phép đặt lịch trước tối đa bao nhiêu ngày'),
('booking_min_hours', '2', 'Phải đặt trước giờ khám tối thiểu bao nhiêu giờ'),
('slot_capacity', '5', 'Số bệnh nhân tối đa trong 1 khung giờ'),
('seats', '5', 'Số ghế khám thực tế trong phòng'),
('slot_minutes', '60', 'Thời lượng mỗi khung giờ khám (phút)'),
('user_max_days', '30', 'Số ngày người bệnh được phép đặt trước tối đa'),
('counter_max_days', '7', 'Số ngày tiếp tân đặt hẹn tại quầy tối đa'),
('priority_early_minutes', '15', 'Số phút đến sớm trước khung giờ để được xếp nhóm Ưu tiên'),
('max_profiles', '10', 'Số hồ sơ người thân tối đa được tạo trên 1 tài khoản'),
('auto_invite', '0', 'Tự động mời người kế tiếp vào ghế khi có ghế trống'),
('reception_open', '08:00', 'Giờ mở cửa tiếp nhận'),
('reception_close', '16:30', 'Giờ đóng cửa tiếp nhận'),
('no_show_cancel_at', '17:00', 'Giờ hệ thống tự động hủy các lịch chưa đến trong ngày')
ON DUPLICATE KEY UPDATE `setting_value` = VALUES(`setting_value`), `description` = VALUES(`description`);

-- 2. KHUNG GIỜ KHÁM (8 KHUNG CHUẨN: 08:00 - 16:30)
INSERT INTO `time_slots` (`slot_label`, `start_time`, `end_time`, `period`, `max_patients`, `sort_order`, `is_active`) VALUES
('08:00 - 09:00', '08:00:00', '09:00:00', 'MORNING',   5, 1, 1),
('09:00 - 10:00', '09:00:00', '10:00:00', 'MORNING',   5, 2, 1),
('10:00 - 11:00', '10:00:00', '11:00:00', 'MORNING',   5, 3, 1),
('11:00 - 12:00', '11:00:00', '12:00:00', 'MORNING',   5, 4, 1),
('13:00 - 14:00', '13:00:00', '14:00:00', 'AFTERNOON', 5, 5, 1),
('14:00 - 15:00', '14:00:00', '15:00:00', 'AFTERNOON', 5, 6, 1),
('15:00 - 16:00', '15:00:00', '16:00:00', 'AFTERNOON', 5, 7, 1),
('16:00 - 16:30', '16:00:00', '16:30:00', 'AFTERNOON', 5, 8, 1)
ON DUPLICATE KEY UPDATE
  `slot_label`   = VALUES(`slot_label`),
  `period`       = VALUES(`period`),
  `max_patients` = VALUES(`max_patients`),
  `sort_order`   = VALUES(`sort_order`),
  `is_active`    = 1;

-- 3. DỊCH VỤ KHÁM CHỮA BỆNH
INSERT INTO `services` (`name`, `slug`, `description`, `duration_min`, `sort_order`, `is_active`) VALUES
('Khám Y học cổ truyền', 'kham-y-hoc-co-truyen', 'Bắt mạch, chẩn đoán và kê đơn thuốc đông y', 30, 1, 1),
('Châm cứu', 'cham-cuu', 'Châm cứu giảm đau, trị liệu xương khớp', 45, 2, 1),
('Bấm huyệt - Xoa bóp', 'bam-huyet-xoa-bop', 'Bấm huyệt, xoa bóp trị liệu', 45, 3, 1),
('Phục hồi chức năng', 'phuc-hoi-chuc-nang', 'Phục hồi sau tai biến, chấn thương', 60, 4, 1)
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`), `is_active` = 1;

-- 4. DANH MỤC BÀI VIẾT
INSERT INTO `article_categories` (`id`, `name`, `slug`, `description`, `sort_order`) VALUES
(1, 'Mẹo dân gian', 'meo-dan-gian', 'Bài thuốc và mẹo chữa bệnh dân gian', 1),
(2, 'Châm cứu - Bấm huyệt', 'cham-cuu-bam-huyet', 'Kiến thức châm cứu, bấm huyệt tại nhà', 2),
(3, 'Phục hồi chức năng', 'phuc-hoi-chuc-nang', 'Bài tập và chăm sóc sau tai biến, chấn thương', 3),
(4, 'Dinh dưỡng thảo mộc', 'dinh-duong-thao-moc', 'Ăn uống theo y học cổ truyền', 4),
(5, 'Hoạt động từ thiện', 'hoat-dong-tu-thien', 'Tin tức khám bệnh từ thiện của phòng khám', 5)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `description` = VALUES(`description`);

-- 5. TÀI KHOẢN DEMO (Mật khẩu mặc định chỉ dùng cho môi trường phát triển và bắt buộc phải đổi ở lần đăng nhập đầu)
INSERT INTO `users` (`username`, `password_hash`, `full_name`, `email`, `phone`, `role`, `is_active`, `must_change_password`) VALUES
('admin', '$2a$12$infhtIEJhboiMlUAcOOvu.ZZJzpbaVRFv97BUFVpswVx4CyMwX1jO', 'Quản trị viên Phòng khám', 'admin@chuctu.vn', '0912345678', 'ADMIN', 1, 1),
('letan', '$2a$12$infhtIEJhboiMlUAcOOvu.ZZJzpbaVRFv97BUFVpswVx4CyMwX1jO', 'Lễ tân Chúc Từ', 'letan@chuctu.vn', '0901234567', 'STAFF', 1, 1),
('bacsi', '$2a$12$infhtIEJhboiMlUAcOOvu.ZZJzpbaVRFv97BUFVpswVx4CyMwX1jO', 'Bác sĩ Nguyễn Văn Minh', 'bacsi@chuctu.vn', '0911223344', 'DOCTOR', 1, 1)
ON DUPLICATE KEY UPDATE `role` = VALUES(`role`), `is_active` = 1;

-- 6. HỒ SƠ BÁC SĨ (Bác sĩ Nguyễn Văn Minh)
INSERT INTO `doctors` (`user_id`, `full_name`, `title`, `specialty`, `experience_years`, `bio`, `phone`, `is_active`, `sort_order`)
SELECT `id`, `full_name`, 'Bác sĩ Y học cổ truyền', 'Cơ xương khớp & Châm cứu', 15, 'Bác sĩ Y học cổ truyền giàu kinh nghiệm bắt mạch và bốc thuốc nam', `phone`, 1, 1
FROM `users` WHERE `username` = 'bacsi'
ON DUPLICATE KEY UPDATE `full_name` = VALUES(`full_name`), `is_active` = 1;

-- 7. LỊCH TRỰC CỐ ĐỊNH CHO BÁC SĨ (Thứ 2 - Thứ 6 Cả ngày, Thứ 7 Sáng)
INSERT INTO `doctor_schedules` (`doctor_id`, `day_of_week`, `period`, `is_active`)
SELECT d.id, 'MON', 'FULL_DAY', 1 FROM `doctors` d WHERE d.full_name LIKE '%Minh%' OR d.id = 1 LIMIT 1
ON DUPLICATE KEY UPDATE `is_active` = 1;

INSERT INTO `doctor_schedules` (`doctor_id`, `day_of_week`, `period`, `is_active`)
SELECT d.id, 'TUE', 'FULL_DAY', 1 FROM `doctors` d WHERE d.full_name LIKE '%Minh%' OR d.id = 1 LIMIT 1
ON DUPLICATE KEY UPDATE `is_active` = 1;

INSERT INTO `doctor_schedules` (`doctor_id`, `day_of_week`, `period`, `is_active`)
SELECT d.id, 'WED', 'FULL_DAY', 1 FROM `doctors` d WHERE d.full_name LIKE '%Minh%' OR d.id = 1 LIMIT 1
ON DUPLICATE KEY UPDATE `is_active` = 1;

INSERT INTO `doctor_schedules` (`doctor_id`, `day_of_week`, `period`, `is_active`)
SELECT d.id, 'THU', 'FULL_DAY', 1 FROM `doctors` d WHERE d.full_name LIKE '%Minh%' OR d.id = 1 LIMIT 1
ON DUPLICATE KEY UPDATE `is_active` = 1;

INSERT INTO `doctor_schedules` (`doctor_id`, `day_of_week`, `period`, `is_active`)
SELECT d.id, 'FRI', 'FULL_DAY', 1 FROM `doctors` d WHERE d.full_name LIKE '%Minh%' OR d.id = 1 LIMIT 1
ON DUPLICATE KEY UPDATE `is_active` = 1;

INSERT INTO `doctor_schedules` (`doctor_id`, `day_of_week`, `period`, `is_active`)
SELECT d.id, 'SAT', 'MORNING', 1 FROM `doctors` d WHERE d.full_name LIKE '%Minh%' OR d.id = 1 LIMIT 1
ON DUPLICATE KEY UPDATE `is_active` = 1;
