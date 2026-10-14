-- ============================================================================
-- 🏥 PHÒNG KHÁM CHÚC TỪ - DỮ LIỆU KHỞI TẠO (SEED DATA)
-- ============================================================================
-- Chạy file này SAU KHI đã chạy 001_schema.sql
-- ============================================================================
-- CHỈ KHỞI TẠO tài khoản Admin đầu tiên (bắt buộc để đăng nhập lần đầu).
-- Các dữ liệu khác (khung giờ, danh mục, bác sĩ...) Admin tự tạo qua CMS.
-- ============================================================================

USE `phongkham_chuctu`;

-- ============================================================================
-- TÀI KHOẢN ADMIN MẶC ĐỊNH
-- Mật khẩu gốc: admin123 (BCrypt hash, strength 12)
-- ⚠️ BẮT BUỘC ĐỔI MẬT KHẨU SAU KHI TRIỂN KHAI!
-- ============================================================================
INSERT INTO `users` (`username`, `password_hash`, `full_name`, `email`, `phone`, `role`, `is_active`)
VALUES
  ('admin', '$2a$12$LJ3m4ys3PjGKgiOjfWxIxOJGq4eFH1PvGbG5GaXpEoE1yxMJfH5jy', 'Quản trị viên', 'admin@chuctupk.vn', '0901234567', 'ADMIN', 1);

-- ============================================================================
-- ✅ HOÀN TẤT SEED DATA
-- Sau khi đăng nhập Admin, hãy vào CMS để tạo:
--   1. Khung giờ khám (time_slots)
--   2. Hồ sơ bác sĩ (doctors) + lịch làm việc (doctor_schedules)
--   3. Danh mục bài viết (article_categories)
-- ============================================================================
