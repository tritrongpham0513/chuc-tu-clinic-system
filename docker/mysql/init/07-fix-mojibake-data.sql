-- ============================================================================
-- PHÒNG KHÁM CHÚC TỪ - MIGRATION 006: SỬA DỮ LIỆU BỊ LỖI MÃ HOÁ (MOJIBAKE)
-- MySQL 8.0+ | InnoDB | utf8mb4_unicode_ci
-- ============================================================================
-- BỐI CẢNH
--   Một lần nhập dữ liệu trước đây đã đọc các byte UTF-8 của tiếng Việt bằng
--   bảng mã DOS (CP850) rồi lưu lại chuỗi kết quả dưới dạng UTF-8. Hệ quả là
--   trên website, tên danh mục / dịch vụ / bác sĩ hiển thị thành các ký tự
--   vẽ khung kiểu "Dinh dã░ß╗íng thß║úo mß╗Öc" thay vì "Dinh dưỡng thảo mộc".
--
-- CÁCH SỬA
--   Đảo ngược đúng phép biến đổi: chuyển chuỗi về byte CP850 rồi giải mã lại
--   bằng UTF-8.  Công thức: CONVERT(CAST(CONVERT(col USING cp850) AS BINARY) USING utf8mb4)
--
-- AN TOÀN
--   * Script IDEMPOTENT: chỉ cập nhật những ô thật sự chứa ký tự lỗi, nhận diện
--     bằng so sánh NHỊ PHÂN trên từng byte (không dùng LIKE/REGEXP vì collation
--     utf8mb4_0900_ai_ci coi 'ß' = 's', sẽ làm hỏng dữ liệu đúng).
--   * Chạy lại lần hai không thay đổi gì (0 dòng bị ảnh hưởng).
--   * Không chứa lệnh `USE ...`: chạy trên database đang được chọn ở client.
--
-- Cách chạy:
--   mysql -u <user> -p --default-character-set=utf8mb4 <db> < database/006_fix_mojibake_data.sql
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0. HÀM NHẬN DIỆN CHUỖI BỊ LỖI
-- ----------------------------------------------------------------------------
DROP FUNCTION IF EXISTS `ct_is_mojibake`;

DELIMITER $$
CREATE FUNCTION `ct_is_mojibake`(p_text LONGTEXT) RETURNS TINYINT
DETERMINISTIC
BEGIN
    IF p_text IS NULL OR p_text = '' THEN
        RETURN 0;
    END IF;

    -- Các ký tự chỉ xuất hiện khi byte UTF-8 bị đọc nhầm bằng CP850
    IF LOCATE(0xE2949C, CAST(p_text AS BINARY)) > 0   -- ├
    OR LOCATE(0xE29480, CAST(p_text AS BINARY)) > 0   -- ─
    OR LOCATE(0xE29482, CAST(p_text AS BINARY)) > 0   -- │
    OR LOCATE(0xE29597, CAST(p_text AS BINARY)) > 0   -- ╗
    OR LOCATE(0xE2959E, CAST(p_text AS BINARY)) > 0   -- ╞
    OR LOCATE(0xE29591, CAST(p_text AS BINARY)) > 0   -- ║
    OR LOCATE(0xE29691, CAST(p_text AS BINARY)) > 0   -- ░
    OR LOCATE(0xC39F,   CAST(p_text AS BINARY)) > 0   -- ß
    OR LOCATE(0xCE93,   CAST(p_text AS BINARY)) > 0   -- Γ
    THEN
        RETURN 1;
    END IF;

    RETURN 0;
END$$
DELIMITER ;

-- ----------------------------------------------------------------------------
-- 1. DANH MỤC BÀI VIẾT
-- ----------------------------------------------------------------------------
UPDATE `article_categories`
SET `name` = CONVERT(CAST(CONVERT(`name` USING cp850) AS BINARY) USING utf8mb4)
WHERE `ct_is_mojibake`(`name`) = 1;

UPDATE `article_categories`
SET `description` = CONVERT(CAST(CONVERT(`description` USING cp850) AS BINARY) USING utf8mb4)
WHERE `ct_is_mojibake`(`description`) = 1;

-- ----------------------------------------------------------------------------
-- 2. DỊCH VỤ KHÁM
-- ----------------------------------------------------------------------------
UPDATE `services`
SET `name` = CONVERT(CAST(CONVERT(`name` USING cp850) AS BINARY) USING utf8mb4)
WHERE `ct_is_mojibake`(`name`) = 1;

UPDATE `services`
SET `description` = CONVERT(CAST(CONVERT(`description` USING cp850) AS BINARY) USING utf8mb4)
WHERE `ct_is_mojibake`(`description`) = 1;

-- ----------------------------------------------------------------------------
-- 3. BÁC SĨ
-- ----------------------------------------------------------------------------
UPDATE `doctors`
SET `full_name` = CONVERT(CAST(CONVERT(`full_name` USING cp850) AS BINARY) USING utf8mb4)
WHERE `ct_is_mojibake`(`full_name`) = 1;

UPDATE `doctors`
SET `title` = CONVERT(CAST(CONVERT(`title` USING cp850) AS BINARY) USING utf8mb4)
WHERE `ct_is_mojibake`(`title`) = 1;

UPDATE `doctors`
SET `specialty` = CONVERT(CAST(CONVERT(`specialty` USING cp850) AS BINARY) USING utf8mb4)
WHERE `ct_is_mojibake`(`specialty`) = 1;

UPDATE `doctors`
SET `bio` = CONVERT(CAST(CONVERT(`bio` USING cp850) AS BINARY) USING utf8mb4)
WHERE `ct_is_mojibake`(`bio`) = 1;

-- ----------------------------------------------------------------------------
-- 4. TÀI KHOẢN NGƯỜI DÙNG
-- ----------------------------------------------------------------------------
UPDATE `users`
SET `full_name` = CONVERT(CAST(CONVERT(`full_name` USING cp850) AS BINARY) USING utf8mb4)
WHERE `ct_is_mojibake`(`full_name`) = 1;

-- ----------------------------------------------------------------------------
-- 5. CẤU HÌNH PHÒNG KHÁM
-- ----------------------------------------------------------------------------
UPDATE `clinic_settings`
SET `setting_value` = CONVERT(CAST(CONVERT(`setting_value` USING cp850) AS BINARY) USING utf8mb4)
WHERE `ct_is_mojibake`(`setting_value`) = 1;

UPDATE `clinic_settings`
SET `description` = CONVERT(CAST(CONVERT(`description` USING cp850) AS BINARY) USING utf8mb4)
WHERE `ct_is_mojibake`(`description`) = 1;

-- ----------------------------------------------------------------------------
-- 6. SỬA CÁC GIÁ TRỊ ĐÃ MẤT KÝ TỰ TỪ TRƯỚC (không thể đảo ngược bằng CP850)
--    Chỉ áp dụng cho tài khoản/bác sĩ demo, có điều kiện để chạy lại an toàn.
-- ----------------------------------------------------------------------------
UPDATE `doctors`
SET `full_name` = 'Lương y Demo'
WHERE `id` = 1 AND `full_name` = 'L??ng y Demo';

UPDATE `doctors`
SET `title` = 'Lương y'
WHERE `id` = 1 AND `title` = 'L??ng y';

UPDATE `doctors`
SET `specialty` = 'Châm cứu'
WHERE `id` = 1 AND `specialty` = 'Châm c?u';

UPDATE `users`
SET `full_name` = 'Lương y Demo'
WHERE `username` = 'bs_demo' AND `full_name` = 'L??ng y Demo';

-- ----------------------------------------------------------------------------
-- 7. DỌN HÀM TẠM
-- ----------------------------------------------------------------------------
DROP FUNCTION IF EXISTS `ct_is_mojibake`;
