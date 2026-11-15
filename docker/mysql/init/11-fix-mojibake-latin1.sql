USE `phongkham_chuctu`;

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================================================
-- SỬA LỖI FONT / MOJIBAKE CHO TẤT CẢ BẢNG (IDEMPOTENT)
-- ============================================================================

-- 1. Bảng users
UPDATE `users` SET `full_name` = 'Quản trị viên Phòng khám' WHERE `username` = 'admin';
UPDATE `users` SET `full_name` = 'Lễ tân Chúc Từ' WHERE `username` = 'letan';
UPDATE `users` SET `full_name` = 'Bác sĩ Nguyễn Văn Minh' WHERE `username` = 'bacsi';

-- 2. Bảng doctors
UPDATE `doctors` SET 
  `full_name` = 'Bác sĩ Nguyễn Văn Minh',
  `title` = 'Bác sĩ Y học cổ truyền',
  `specialty` = 'Cơ xương khớp & Châm cứu',
  `bio` = 'Bác sĩ Y học cổ truyền giàu kinh nghiệm bắt mạch và bốc thuốc nam'
WHERE `id` = 1 OR `user_id` IN (SELECT `id` FROM `users` WHERE `username` = 'bacsi');

-- 3. Bảng article_categories
UPDATE `article_categories` SET `name` = 'Y học cổ truyền', `description` = 'Kiến thức châm cứu, bấm huyệt, thảo mộc chữa bệnh' WHERE `slug` = 'y-hoc-co-truyen';
UPDATE `article_categories` SET `name` = 'Cơ xương khớp', `description` = 'Phòng và trị các bệnh thoái hóa khớp, đau lưng, vai gáy' WHERE `slug` = 'co-xuong-khop';
UPDATE `article_categories` SET `name` = 'Dưỡng sinh người cao tuổi', `description` = 'Chế độ sinh hoạt, vận động và giấc ngủ cho người lớn tuổi' WHERE `slug` = 'duong-sinh-nguoi-cao-tuoi';
UPDATE `article_categories` SET `name` = 'Mẹo dân gian', `description` = 'Bài thuốc và mẹo chữa bệnh dân gian' WHERE `slug` = 'meo-dan-gian';
UPDATE `article_categories` SET `name` = 'Châm cứu - Bấm huyệt', `description` = 'Kiến thức châm cứu, bấm huyệt tại nhà' WHERE `slug` = 'cham-cuu-bam-huyet';
UPDATE `article_categories` SET `name` = 'Phục hồi chức năng', `description` = 'Bài tập và chăm sóc sau tai biến, chấn thương' WHERE `slug` = 'phuc-hoi-chuc-nang';
UPDATE `article_categories` SET `name` = 'Dinh dưỡng thảo mộc', `description` = 'Ăn uống theo y học cổ truyền' WHERE `slug` = 'dinh-duong-thao-moc';
UPDATE `article_categories` SET `name` = 'Hoạt động từ thiện', `description` = 'Tin tức khám bệnh từ thiện của phòng khám' WHERE `slug` = 'hoat-dong-tu-thien';

-- 4. Bảng services
UPDATE `services` SET `name` = 'Khám Y học cổ truyền', `description` = 'Bắt mạch, chẩn đoán và kê đơn thuốc đông y' WHERE `slug` = 'kham-y-hoc-co-truyen';
UPDATE `services` SET `name` = 'Châm cứu', `description` = 'Châm cứu giảm đau, trị liệu xương khớp' WHERE `slug` = 'cham-cuu';
UPDATE `services` SET `name` = 'Bấm huyệt - Xoa bóp', `description` = 'Bấm huyệt, xoa bóp trị liệu' WHERE `slug` = 'bam-huyet-xoa-bop';
UPDATE `services` SET `name` = 'Phục hồi chức năng', `description` = 'Phục hồi sau tai biến, chấn thương' WHERE `slug` = 'phuc-hoi-chuc-nang';

-- 5. Bảng clinic_settings
UPDATE `clinic_settings` SET `setting_value` = 'Phòng Khám Chúc Từ' WHERE `setting_key` = 'clinic_name';
UPDATE `clinic_settings` SET `setting_value` = 'Đường Điện Biên Phủ, TP. Hồ Chí Minh' WHERE `setting_key` = 'address';
UPDATE `clinic_settings` SET `setting_value` = 'Sáng 08:00 - 11:30, Chiều 13:30 - 17:00' WHERE `setting_key` = 'working_hours';

-- 6. Bảng articles
UPDATE `articles` SET 
  `title` = 'Châm cứu điều trị đau nhức xương khớp ở người cao tuổi',
  `summary` = 'Châm cứu là phương pháp trị liệu không dùng thuốc an toàn, giúp đả thông kinh lạc, giảm đau nhức thoái hóa cột sống, khớp gối hiệu quả cho các cô bác lớn tuổi.',
  `content` = '<p>Theo Y học cổ truyền, “thông bất thống, thống bất thông” — khí huyết lưu thông thì cơ thể không đau, khí huyết ứ trệ kinh lạc bế tắc sẽ sinh ra đau nhức. Người cao tuổi chính khí suy giảm, phong hàn thấp tà dễ xâm nhập vào kinh lạc cơ khớp.</p><h3>1. Châm cứu có tác dụng gì?</h3><p>Châm cứu kích thích các huyệt đạo như Độc Tỵ, Huyết Hải, Dương Lăng Tuyền, Thận Du giúp lưu thông khí huyết, kích hoạt cơ thể tự sản sinh chất giảm đau tự nhiên và tăng cường tuần hoàn nuôi dưỡng sụn khớp.</p><h3>2. Lời khuyên cho cô chú</h3><p>Cô chú nên giữ ấm chân tay, tập dưỡng sinh nhẹ nhàng vào buổi sáng và đến phòng khám đúng liệu trình để đạt kết quả bền vững.</p>'
WHERE `id` = 1 OR `slug` = 'cham-cuu-dieu-tri-dau-nhuc-xuong-khop-o-nguoi-cao-tuoi';

UPDATE `articles` SET 
  `title` = 'Bấm huyệt dưỡng sinh giúp an giấc, giảm đau đầu chóng mặt',
  `summary` = 'Hướng dẫn các huyệt đạo cơ bản như Nội Quan, Tam Âm Giao, An Miên mà người cao tuổi có thể tự day bấm nhẹ nhàng trước khi đi ngủ để có giấc ngủ sâu và êm ái.',
  `content` = '<p>Mất ngủ ở người lớn tuổi thường do tâm tỳ lưỡng hư hoặc can thận âm hư. Việc lạm dụng thuốc an thần dễ gây mệt mỏi vào ban ngày.</p><h3>Các huyệt dưỡng tâm an thần</h3><p><strong>Huyệt An Miên:</strong> Nằm ở phía sau dái tai, day nhẹ 3-5 phút trước khi ngủ giúp thư giãn hệ thần kinh.</p><p><strong>Huyệt Dũng Tuyền:</strong> Ngay lòng bàn chân, xoa nóng bàn chân kết hợp ngâm chân nước gừng ấm 15 phút mỗi tối sẽ giúp ấm thận, hạ huyết áp và ngủ rất ngon.</p>'
WHERE `id` = 2 OR `slug` = 'bam-huyet-duong-sinh-giup-an-giac-giam-dau-dau-chong-mat';

UPDATE `articles` SET 
  `title` = 'Bài thuốc Nam thanh nhiệt, giải độc và kiện tỳ mùa nắng nóng',
  `summary` = 'Những cây thuốc nam quen thuộc quanh vườn nhà như rễ tranh, mía lau, râu ngô, hoa cúc giúp mát gan, lợi tiểu và nâng cao sức đề kháng cho cả gia đình.',
  `content` = '<p>Cây cỏ thuốc Nam nước Việt ta vô cùng phong phú và lành tính. Mùa hè oi bức, người lớn tuổi dễ bị háo nước, tiểu gắt, bứt rứt trong người.</p><p>Cô chú có thể dùng nước sắc râu ngô kết hợp bông mã đề và một chút mía lau uống thay trà giải nhiệt hàng ngày. Bắt mạch và kê đơn thuốc Nam tại Chúc Từ hoàn toàn miễn phí, cô chú có thể ghé phòng khám để được lương y chỉ định phù hợp thể trạng.</p>'
WHERE `id` = 3 OR `slug` = 'bai-thuoc-nam-thanh-nhiet-giai-doc-kien-ty';
