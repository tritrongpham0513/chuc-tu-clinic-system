USE `phongkham_chuctu`;

-- ============================================================================
-- CẬP NHẬT GIỜ TIẾP NHẬN PHÒNG KHÁM: reception_close = '17:00'
-- Nhằm cho phép khung giờ khám 16:00 - 17:00 được tiếp nhận hợp lệ
-- ============================================================================

UPDATE `clinic_settings`
SET `setting_value` = '17:00'
WHERE `setting_key` = 'reception_close';

INSERT INTO `clinic_settings` (`setting_key`, `setting_value`, `description`)
VALUES ('reception_close', '17:00', 'Giờ đóng cửa tiếp nhận (cho phép slot 16:00 - 17:00)')
ON DUPLICATE KEY UPDATE `setting_value` = '17:00';
