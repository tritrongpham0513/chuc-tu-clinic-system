-- Cập nhật thời gian đặt lịch tối thiểu trước giờ khám thành 1 giờ (nếu vẫn đang giữ mặc định 2 giờ ban đầu)
UPDATE clinic_settings
SET setting_value = '1'
WHERE setting_key = 'booking_min_hours' AND setting_value = '2';

-- Vô hiệu hóa khung giờ 16:00 - 17:00 cũ vượt quá giờ đóng cửa tiếp nhận (16:30)
UPDATE time_slots
SET is_active = 0
WHERE start_time = '16:00:00' AND end_time > '16:30:00';
