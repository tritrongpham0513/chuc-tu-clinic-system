-- Seed Danh mục bài viết và Bài viết mẫu cho Cẩm nang Y học cổ truyền Chúc Từ

INSERT INTO article_categories (id, name, slug, description, sort_order)
VALUES 
(1, 'Y học cổ truyền', 'y-hoc-co-truyen', 'Kiến thức châm cứu, bấm huyệt, thảo mộc chữa bệnh', 1),
(2, 'Cơ xương khớp', 'co-xuong-khop', 'Phòng và trị các bệnh thoái hóa khớp, đau lưng, vai gáy', 2),
(3, 'Dưỡng sinh người cao tuổi', 'duong-sinh-nguoi-cao-tuoi', 'Chế độ sinh hoạt, vận động và giấc ngủ cho người lớn tuổi', 3)
ON DUPLICATE KEY UPDATE name=VALUES(name);

SET @admin_id = COALESCE((SELECT id FROM users WHERE username = 'admin' LIMIT 1), 1);

INSERT INTO articles (id, title, slug, summary, content, thumbnail_url, category_id, author_id, is_published, is_featured, published_at, view_count)
VALUES
(1, 
 'Châm cứu điều trị đau nhức xương khớp ở người cao tuổi',
 'cham-cuu-dieu-tri-dau-nhuc-xuong-khop-o-nguoi-cao-tuoi',
 'Châm cứu là phương pháp trị liệu không dùng thuốc an toàn, giúp đả thông kinh lạc, giảm đau nhức thoái hóa cột sống, khớp gối hiệu quả cho các cô bác lớn tuổi.',
 '<p>Theo Y học cổ truyền, “thông bất thống, thống bất thông” — khí huyết lưu thông thì cơ thể không đau, khí huyết ứ trệ kinh lạc bế tắc sẽ sinh ra đau nhức. Người cao tuổi chính khí suy giảm, phong hàn thấp tà dễ xâm nhập vào kinh lạc cơ khớp.</p><h3>1. Châm cứu có tác dụng gì?</h3><p>Châm cứu kích thích các huyệt đạo như Độc Tỵ, Huyết Hải, Dương Lăng Tuyền, Thận Du giúp lưu thông khí huyết, kích hoạt cơ thể tự sản sinh chất giảm đau tự nhiên và tăng cường tuần hoàn nuôi dưỡng sụn khớp.</p><h3>2. Lời khuyên cho cô chú</h3><p>Cô chú nên giữ ấm chân tay, tập dưỡng sinh nhẹ nhàng vào buổi sáng và đến phòng khám đúng liệu trình để đạt kết quả bền vững.</p>',
 'https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?auto=format&fit=crop&w=600&q=80',
 1, @admin_id, 1, 1, NOW(), 125),

(2,
 'Bấm huyệt dưỡng sinh giúp an giấc, giảm đau đầu chóng mặt',
 'bam-huyet-duong-sinh-giup-an-giac-giam-dau-dau-chong-mat',
 'Hướng dẫn các huyệt đạo cơ bản như Nội Quan, Tam Âm Giao, An Miên mà người cao tuổi có thể tự day bấm nhẹ nhàng trước khi đi ngủ để có giấc ngủ sâu và êm ái.',
 '<p>Mất ngủ ở người lớn tuổi thường do tâm tỳ lưỡng hư hoặc can thận âm hư. Việc lạm dụng thuốc an thần dễ gây mệt mỏi vào ban ngày.</p><h3>Các huyệt dưỡng tâm an thần</h3><p><strong>Huyệt An Miên:</strong> Nằm ở phía sau dái tai, day nhẹ 3-5 phút trước khi ngủ giúp thư giãn hệ thần kinh.</p><p><strong>Huyệt Dũng Tuyền:</strong> Ngay lòng bàn chân, xoa nóng bàn chân kết hợp ngâm chân nước gừng ấm 15 phút mỗi tối sẽ giúp ấm thận, hạ huyết áp và ngủ rất ngon.</p>',
 'https://images.unsplash.com/photo-1506126613408-eca07ce68773?auto=format&fit=crop&w=600&q=80',
 3, @admin_id, 1, 1, NOW(), 98),

(3,
 'Bài thuốc Nam thanh nhiệt, giải độc và kiện tỳ mùa nắng nóng',
 'bai-thuoc-nam-thanh-nhiet-giai-doc-kien-ty',
 'Những cây thuốc nam quen thuộc quanh vườn nhà như rễ tranh, mía lau, râu ngô, hoa cúc giúp mát gan, lợi tiểu và nâng cao sức đề kháng cho cả gia đình.',
 '<p>Cây cỏ thuốc Nam nước Việt ta vô cùng phong phú và lành tính. Mùa hè oi bức, người lớn tuổi dễ bị háo nước, tiểu gắt, bứt rứt trong người.</p><p>Cô chú có thể dùng nước sắc râu ngô kết hợp bông mã đề và một chút mía lau uống thay trà giải nhiệt hàng ngày. Bắt mạch và kê đơn thuốc Nam tại Chúc Từ hoàn toàn miễn phí, cô chú có thể ghé phòng khám để được lương y chỉ định phù hợp thể trạng.</p>',
 'https://images.unsplash.com/photo-1512290900672-1f55b9e0f6c2?auto=format&fit=crop&w=600&q=80',
 1, @admin_id, 1, 0, NOW(), 64)

ON DUPLICATE KEY UPDATE 
    title=VALUES(title),
    summary=VALUES(summary),
    content=VALUES(content),
    is_published=VALUES(is_published),
    is_featured=VALUES(is_featured);
