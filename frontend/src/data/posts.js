// Dữ liệu bài viết mẫu (theo thiết kế). Sau này thay bằng dữ liệu từ API.
// tone: 'green' | 'gold' — màu nhãn chuyên mục.
export const CATEGORIES = [
  { id: 'all', label: 'Tất cả' },
  { id: 'gioithieu', label: 'Giới thiệu' },
  { id: 'dichvu', label: 'Dịch vụ' },
  { id: 'tintuc', label: 'Tin tức' },
  { id: 'huongdan', label: 'Hướng dẫn' },
  { id: 'thiennguyen', label: 'Thiện nguyện' },
]

export const POSTS = [
  {
    cat: 'huongdan', category: 'Hướng dẫn', tone: 'gold', date: '24/09/2026',
    title: 'Cần chuẩn bị gì trước buổi châm cứu đầu tiên',
    summary: 'Cô chú nên ăn nhẹ, mặc đồ rộng rãi và mang theo toa thuốc đang uống khi tới khám.',
  },
  {
    cat: 'thiennguyen', category: 'Thiện nguyện', tone: 'green', date: '18/09/2026',
    title: 'Buổi khám thiện nguyện cùng bà con trong phường',
    summary: 'Hơn 120 cô cô chú trong phường được khám và phát thuốc miễn phí sáng Chủ Nhật vừa rồi.',
  },
  {
    cat: 'dichvu', category: 'Dịch vụ', tone: 'gold', date: '11/09/2026',
    title: 'Châm cứu chữa được những chứng đau nào',
    summary: 'Đau vai gáy, đau lưng, tê bì chân tay và di chứng sau tai biến là nhóm thường gặp nhất.',
  },
  {
    cat: 'tintuc', category: 'Tin tức', tone: 'green', date: '05/09/2026',
    title: 'Thay đổi giờ khám buổi chiều từ tuần sau',
    summary: 'Buổi chiều nhận bệnh tới 16:30 thay vì 16:00, để bà con ở xa kịp tới nơi.',
  },
  {
    cat: 'gioithieu', category: 'Giới thiệu', tone: 'green', date: '28/08/2026',
    title: 'Đôi dòng về Sư cô Chúc Từ và những ngày đầu',
    summary: 'Từ một gian phòng nhỏ mượn tạm, phòng khám nay đón hàng trăm lượt bà con mỗi tuần.',
  },
  {
    cat: 'huongdan', category: 'Hướng dẫn', tone: 'gold', date: '20/08/2026',
    title: 'Cách đăng ký tài khoản và đặt lịch khám',
    summary: 'Chỉ cần số điện thoại và vài phút. Con cháu cũng có thể đăng ký giúp ông bà.',
  },
]
