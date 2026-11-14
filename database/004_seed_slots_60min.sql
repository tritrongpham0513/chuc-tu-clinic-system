-- ============================================================================
-- PHÒNG KHÁM CHÚC TỪ - MIGRATION 004: CHUYỂN ĐỔI KHUNG GIỜ KHÁM 60 PHÚT
-- MySQL 8.0+ | InnoDB | utf8mb4_unicode_ci
-- ============================================================================
-- Hướng dẫn rollback:
--   UPDATE `time_slots` SET `is_active` = 1 WHERE `slot_label` LIKE '%30%';
--   DELETE FROM `time_slots` WHERE `slot_label` IN (
--     '08:00 - 09:00','09:00 - 10:00','10:00 - 11:00','11:00 - 12:00',
--     '13:00 - 14:00','14:00 - 15:00','15:00 - 16:00','16:00 - 17:00'
--   );
-- ============================================================================

-- LƯU Ý: Không dùng `USE ...` — file chạy trên database đang được chọn ở client,
-- nhờ vậy dùng được cho cả DB thật, DB test và docker/mysql/init.

-- 1. Vô hiệu hóa các khung giờ 30 phút cũ (để bảo toàn lịch sử dữ liệu cũ)
UPDATE `time_slots` SET `is_active` = 0;

-- 2. Thêm 8 khung giờ chuẩn 60 phút (tối đa 6 bệnh nhân / khung)
INSERT INTO `time_slots` (`slot_label`, `start_time`, `end_time`, `period`, `max_patients`, `sort_order`, `is_active`)
VALUES
  ('08:00 - 09:00', '08:00:00', '09:00:00', 'MORNING',   6, 1, 1),
  ('09:00 - 10:00', '09:00:00', '10:00:00', 'MORNING',   6, 2, 1),
  ('10:00 - 11:00', '10:00:00', '11:00:00', 'MORNING',   6, 3, 1),
  ('11:00 - 12:00', '11:00:00', '12:00:00', 'MORNING',   6, 4, 1),
  ('13:00 - 14:00', '13:00:00', '14:00:00', 'AFTERNOON', 6, 5, 1),
  ('14:00 - 15:00', '14:00:00', '15:00:00', 'AFTERNOON', 6, 6, 1),
  ('15:00 - 16:00', '15:00:00', '16:00:00', 'AFTERNOON', 6, 7, 1),
  ('16:00 - 17:00', '16:00:00', '17:00:00', 'AFTERNOON', 6, 8, 1)
ON DUPLICATE KEY UPDATE
  `slot_label`   = VALUES(`slot_label`),
  `period`       = VALUES(`period`),
  `max_patients` = VALUES(`max_patients`),
  `sort_order`   = VALUES(`sort_order`),
  `is_active`    = 1;

