# 🏥 HỆ THỐNG QUẢN LÝ VÀ TIẾP NHẬN BỆNH NHÂN - PHÒNG KHÁM CHÚC TỪ

> **Chuc Tu Traditional Medicine Clinic Management System**  
> Nền tảng website tiếp nhận đặt lịch khám bệnh và quản trị vận hành phòng khám Y học Cổ truyền, Châm cứu, Bấm huyệt và Phục hồi chức năng.

---

## 🌿 1. TRIẾT LÝ THIẾT KẾ: "ELDERLY-CENTRIC" (ƯU TIÊN NGƯỜI CAO TUỔI)

Khác với các hệ thống y tế công nghệ cao sắc lạnh thông thường, Phòng Khám Chúc Từ được định hình với tôn chỉ: **Mộc mạc – Ấm áp – Dễ dùng**:

- 👓 **Cỡ chữ to & Dễ đọc**: Toàn bộ nội dung body text đạt kích thước tối thiểu `18px - 20px`, tiêu đề từ `24px - 32px`.
- 🎨 **Bảng màu Thảo mộc & Nâu gỗ (Warm Herbal)**: 
  - Nâu gỗ trầm (`#5C3A21` / `#78350F`) — Màu bàn bắt mạch, tủ thuốc đông y.
  - Xanh thảo mộc (`#2D5A27` / `#15803D`) — Màu lá thuốc nam, biểu trưng cho sự hồi phục tự nhiên.
  - Vàng rơm ấm (`#D97706` / `#F59E0B`) — Điểm xuyết cho các nút bấm hành động quan trọng.
  - Nền giấy xuyến chỉ (`#FDFBF7`) — Độ tương phản cao (> 7:1 đạt chuẩn WCAG AAA), dịu mắt cho người già.
- 🚫 **Loại bỏ hiệu ứng gây rối mắt**: Tuyệt đối không dùng chữ trượt bay, pop-up bất ngờ hay hoạt ảnh 3D.
- 📱 **Mobile-First & Sticky Bottom Bar**: 2 nút lớn cố định ở đáy màn hình điện thoại:
  - 📞 **Nút Hotline**: Bấm gọi ngay cho bác sĩ trực.
  - 📍 **Nút Chỉ đường**: Mở trực tiếp Google Maps đến trụ sở phòng khám (Điện Biên Phủ, TP.HCM).

---

## 🛠️ 2. KIẾN TRÚC CÔNG NGHỆ (TECH STACK)

Dự án áp dụng mô hình **Monorepo** chuẩn mực:

```text
phong-kham-chuc-tu/
├── frontend/           # Ứng dụng Web Client & Admin CMS (React 18 + Vite + Tailwind)
├── backend/            # RESTful API Backend (Java 17/21 + Spring Boot 3.x)
├── database/           # Kịch bản DDL MySQL 8.0 & Seed Data
├── PROJECT_CONTEXT.md  # Hồ sơ nghiệp vụ & Kiến trúc chi tiết
├── AGENTS.md           # Bộ quy tắc hướng dẫn AI Agent
└── README.md           # Tài liệu giới thiệu dự án
```

| Tầng hệ thống | Công nghệ | Mô tả kỹ thuật |
| :--- | :--- | :--- |
| **Frontend** | React 18 (Vite) | Tốc độ tải trang siêu nhanh, SPA mượt mà, tối ưu mạng 3G/4G |
| **Styling** | Tailwind CSS | Quản lý bảng màu thảo mộc và typography cỡ lớn tập trung |
| **Backend** | Java 17/21 + Spring Boot 3 | Clean Layered Architecture, Spring Security 6, Spring Data JPA |
| **Authentication** | Stateless JWT | Xác thực phân quyền RBAC an toàn, thời gian sống linh hoạt |
| **Database** | MySQL 8.0 | Engine InnoDB, Charset `utf8mb4_unicode_ci` hỗ trợ tiếng Việt trọn vẹn |

---

## 👥 3. PHÂN HỆ NGƯỜI DÙNG & VAI TRÒ (RBAC)

Hệ thống hỗ trợ 4 vai trò rõ ràng:

1. **👑 QUẢN TRỊ VIÊN (`ADMIN`)**:
   - Quản lý tài khoản và phân quyền bác sĩ, nhân viên.
   - Quản lý danh mục bài viết, khung giờ khám và cấu hình hệ thống.
2. **🩺 BÁC SĨ (`DOCTOR`)**:
   - Theo dõi danh sách ca khám trong ngày.
   - Cập nhật ghi chú bệnh án, duyệt/hoàn thành ca khám.
   - Đăng tải bài viết chia sẻ mẹo dân gian, cẩm nang sức khỏe.
3. **📋 LỄ TÂN / ĐIỀU DƯỠNG (`STAFF`)**:
   - Tiếp nhận lịch hẹn mới từ người bệnh.
   - Gọi điện xác nhận, sắp xếp ca khám và phân bổ bác sĩ.
4. **👴 BỆNH NHÂN & CON CHÁU (`PATIENT`)**:
   - Đặt lịch khám nhanh không bắt buộc đăng nhập (chỉ cần điền SĐT & triệu chứng).
   - Tra cứu mã hẹn khám bệnh.
   - Đăng ký tài khoản để quản lý hồ sơ khám của nhiều người thân trong gia đình.

---

## 🗄️ 4. CƠ SỞ DỮ LIỆU (8 BẢNG HOÀN CHỈNH)

- `users`: Tài khoản đăng nhập hệ thống nội bộ (Băm mật khẩu bằng BCrypt).
- `doctors`: Hồ sơ chuyên môn bác sĩ, chuyên khoa, liên kết 1-1 với `users`.
- `patients`: Hồ sơ bệnh nhân (mã BN, họ tên, năm sinh, SĐT).
- `time_slots`: Khung giờ khám (mỗi ca 30 phút, sáng/chiều, giới hạn số người/ca).
- `doctor_schedules`: Lịch làm việc hàng tuần của từng bác sĩ.
- `appointments`: Lịch hẹn khám (mã booking, chống trùng lịch, quản lý trạng thái).
- `article_categories`: Danh mục bài viết cẩm nang sức khỏe.
- `articles`: Bài viết mẹo chữa bệnh, y học cổ truyền, tin từ thiện.

---

## 🚀 5. HƯỚNG DẪN KHỞI CHẠY (QUICK START)

### Bước 1: Khởi tạo Cơ sở Dữ liệu MySQL
1. Mở MySQL Workbench hoặc MySQL CLI:
```bash
mysql -u root -p < database/001_schema.sql
mysql -u root -p < database/002_seed_data.sql
```
*(Tài khoản mặc định: `admin` / `admin123`)*.

### Bước 2: Khởi chạy Backend (Spring Boot)
```bash
cd backend
mvn clean spring-boot:run
```
*API sẵn sàng tại:* `http://localhost:8080/api/v1`

### Bước 3: Khởi chạy Frontend (React + Vite)
```bash
cd frontend
npm install
npm run dev
```
*Giao diện sẵn sàng tại:* `http://localhost:5173`

---

## 📝 BẢN QUYỀN & PHÁT TRIỂN

* **Dự án**: Hệ thống quản lý Phòng Khám Chúc Từ
* **Tác giả**: [tritrongpham0513](https://github.com/tritrongpham0513)
* **Giấy phép**: MIT License
