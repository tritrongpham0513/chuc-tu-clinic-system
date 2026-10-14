# 📋 QUY TẮC PHÁT TRIỂN HỆ THỐNG - PHÒNG KHÁM CHÚC TỪ (AGENTS.MD)

> **Mục đích**: File quy tắc cốt lõi (Always-on Rules) hướng dẫn hành vi, phong cách lập trình và tiêu chuẩn thiết kế cho toàn bộ AI Agent và Lập trình viên khi làm việc trong dự án Phòng Khám Chúc Từ.

---

## 1. NGUYÊN TẮC CỐT LÕI (CORE PRINCIPLES)

1. **Ưu tiên số 1: Trải nghiệm người cao tuổi (Elderly-First UX)**
   - Người bệnh là người lớn tuổi (50–80 tuổi) mắt kém, tay run, không thạo công nghệ.
   - Mọi giao diện người dùng (Bệnh nhân) PHẢI:
     - **Font size**: Tối thiểu `18px - 20px` cho văn bản thông thường (body text), tiêu đề từ `24px - 32px`.
     - **Tương phản**: Chữ tối (`#1F1610`) trên nền sáng ấm (`#FDFBF7`), độ tương phản cao đạt chuẩn WCAG AAA (> 7:1).
     - **Hiệu ứng**: **Tuyệt đối KHÔNG** dùng chữ trượt/bay, pop-up nhảy bất ngờ, hoạt ảnh 3D gây rối mắt hoặc chóng mặt. Chỉ dùng hiệu ứng mờ dần nhẹ (fade-in 150-200ms).
     - **Khoảng cách bấm (Touch targets)**: Nút bấm tối thiểu `48px x 48px` hoặc padding rộng rãi, dễ bấm trên màn hình cảm ứng.

2. **Vibe & Màu sắc thương hiệu (Warm Herbal & Wood)**
   - **Tông màu chủ đạo**: Nâu gỗ trầm (`#5C3A21` / `#78350F`) tượng trưng cho bàn bắt mạch, tủ thuốc đông y; Xanh lá thảo mộc (`#2D5A27` / `#15803D`) tượng trưng cho cây thuốc nam và sự hồi phục.
   - **Màu nhấn**: Vàng rơm ấm (`#D97706` / `#F59E0B`) cho nút quan trọng, cảnh báo.
   - **Nền**: Trắng ngà/giấy xuyến chỉ (`#FDFBF7`), không dùng nền trắng tinh tuyết chói mắt.
   - **KHÔNG ĐƯỢC PHÉP**: Thiết kế theo kiểu công nghệ cao, màu trắng toát bệnh viện sắc lạnh, kính bóng bẩy neon gây xa cách.

3. **Mobile-First & Nút khẩn cấp cố định (Sticky Bottom Bar)**
   - Mọi trang phía người bệnh bắt buộc có 2 nút to nằm cố định ở đáy màn hình điện thoại:
     - 📞 **Nút Hotline**: Bấm gọi ngay số bác sĩ (`tel:...`).
     - 📍 **Nút Chỉ đường**: Mở trực tiếp Google Maps đến trụ sở phòng khám (Điện Biên Phủ, TP.HCM).

4. **Quản trị nội bộ (Admin CMS) thuần Việt & Tinh gọn**
   - 100% ngôn ngữ tiếng Việt giản dị, dễ hiểu cho điều dưỡng và bác sĩ.
   - Bỏ qua các biểu đồ dashboard phức tạp không cần thiết; tập trung vào 3 tác vụ chính: Tiếp nhận lịch khám, Đăng bài cẩm nang sức khỏe, Quản lý tài khoản.

---

## 2. QUY CHUẨN CÔNG NGHỆ & KIẾN TRÚC (TECH STACK)

### 2.1. Cấu trúc thư mục Monorepo
```text
phong-kham-chuc-tu/
├── frontend/           # React 18+ (Vite) + Tailwind CSS + Axios
├── backend/            # Java 17/21 + Spring Boot 3.x (Clean Architecture)
├── database/           # MySQL 8.0 scripts, migrations, seed data
├── PROJECT_CONTEXT.md  # Tài liệu kiến trúc & nghiệp vụ chi tiết
└── AGENTS.md           # File quy tắc chỉ dẫn AI (File này)
```

### 2.2. Frontend (React + Vite)
- Khởi tạo và cấu hình bằng Vite, quản lý state gọn nhẹ (React Context / Zustand nếu cần).
- Routing rõ ràng:
  - Trang công khai: Trang chủ, Giới thiệu phòng khám, Đặt lịch khám, Cẩm nang sức khỏe, Liên hệ/Bản đồ.
  - Phân hệ Admin: Đăng nhập (`/admin/login`), Quản lý lịch tiếp nhận (`/admin/appointments`), Quản lý bài viết (`/admin/articles`).
- Tối ưu hiệu năng: Ảnh định dạng WebP, lazy loading cho component & Google Maps iframe.

### 2.3. Backend (Java Spring Boot 3)
- Kiến trúc phân lớp chuẩn (Clean Layered Architecture):
  - `config/`: Security, CORS, Swagger...
  - `controller/`: REST Controllers, tiếp nhận request và trả về `ApiResponse<T>`.
  - `service/` & `service/impl/`: Business logic, `@Transactional`.
  - `repository/`: Spring Data JPA Interfaces.
  - `entity/`: JPA Entities ánh xạ bảng MySQL.
  - `dto/`: Request/Response payload, validate bằng Hibernate Validator (`@NotBlank`, `@Pattern`...).
  - `exception/`: Global Exception Handler (`@RestControllerAdvice`).
- Mật khẩu băm bằng `BCryptPasswordEncoder`. Xác thực bằng Stateless JWT Token.

### 2.4. Database (MySQL 8.0)
- Engine: `InnoDB`, Charset: `utf8mb4`, Collation: `utf8mb4_unicode_ci`.
- Bắt buộc có Index cho các trường tra cứu thường xuyên (`phone`, `appointment_date`, `booking_code`).
- Đặt tên bảng dạng số nhiều chữ thường: `users`, `patients`, `appointments`, `articles`.

### 2.5. Định dạng phản hồi RESTful API
Mọi endpoint trả về định dạng chuẩn:
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Nội dung thông báo tiếng Việt",
  "data": { ... },
  "timestamp": "2026-10-14T22:30:00"
}
```

---

## 3. QUY TẮC BẢO MẬT & DỮ LIỆU Y TẾ

1. Không bao giờ log thông tin nhạy cảm của bệnh nhân (CCCD, địa chỉ chi tiết, chẩn đoán mật) ra file log công khai.
2. Form đặt lịch phía bệnh nhân phải có kiểm tra định dạng số điện thoại Việt Nam (10 chữ số, bắt đầu bằng `03`, `05`, `07`, `08`, `09`).
3. Chống trùng lịch (Double Booking): Mỗi slot khám phải được kiểm tra tính khả dụng trước khi ghi nhận vào DB.

---

## 4. QUY ƯỚC LÀM VIỆC CỦA AGENT

- Khi sinh mã hoặc chỉnh sửa mã, luôn đối chiếu với [PROJECT_CONTEXT.md](file:///d:/phong-kham-chuc-tu/PROJECT_CONTEXT.md) để đảm bảo đồng bộ biến màu, cỡ chữ, và schema.
- Luôn giải thích ngắn gọn, rõ ràng bằng tiếng Việt.
- Ưu tiên tính ổn định, dễ đọc, dễ kiểm thử trước khi tối ưu nâng cao.
