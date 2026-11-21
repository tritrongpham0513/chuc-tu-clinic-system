-- Sprint 5: Thêm cấu hình số bệnh nhân tối đa mỗi bác sĩ trong 1 khung giờ
INSERT INTO clinic_settings (setting_key, setting_value, description)
VALUES ('patients_per_doctor', '3', 'Số bệnh nhân tối đa mỗi bác sĩ trong 1 khung giờ')
ON DUPLICATE KEY UPDATE description = VALUES(description);
