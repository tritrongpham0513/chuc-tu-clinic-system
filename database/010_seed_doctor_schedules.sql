-- ============================================================================
-- KHỞI TẠO LỊCH TRỰC CỐ ĐỊNH CHO BÁC SĨ (DOCTOR SCHEDULES)
-- ============================================================================

-- Phân công Bác sĩ Nguyễn Văn Minh (user 'bacsi') trực:
-- - Thứ 2 đến Thứ 6: Cả ngày (FULL_DAY: Sáng 08:00-12:00, Chiều 13:00-17:00)
-- - Thứ 7: Buổi sáng (MORNING: 08:00-12:00)
-- - Chủ nhật: Nghỉ phòng khám

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
