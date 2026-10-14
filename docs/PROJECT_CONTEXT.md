# 🏥 HỒ SƠ NGỮ CẢNH DỰ ÁN (ENTERPRISE PROJECT CONTEXT)
## HỆ THỐNG QUẢN LÝ VÀ NHẬN BỆNH - PHÒNG KHÁM CHÚC TỪ
> **Phiên bản tài liệu**: 1.0.0 | **Ngày ban hành**: 2026-10-14  
> **Tiêu chuẩn áp dụng**: Enterprise Architecture Standard (Clean Architecture & Elderly-Centric Design)  
> **Trạng thái**: Active Development

---

## 1. TỔNG QUAN DỰ ÁN & TUYÊN NGÔN THIẾT KẾ (EXECUTIVE SUMMARY)

### 1.1. Sứ mệnh dự án
Xây dựng nền tảng website tiếp nhận bệnh nhân và quản lý vận hành phòng khám **Phòng Khám Chúc Từ**, tập trung vào y học cổ truyền, châm cứu, phục hồi chức năng và chăm sóc sức khỏe cộng đồng.

### 1.2. Chân dung người dùng mục tiêu (User Persona)
- **Nhóm 1 (Trọng tâm)**: Người bệnh lớn tuổi (từ 50 - 80 tuổi), mắc các bệnh về xương khớp, thoái hóa, mất ngủ, tai biến. Thị lực kém, tay run, phản xạ chậm, không am hiểu công nghệ.
- **Nhóm 2**: Con cháu của người bệnh, người bận rộn đặt lịch khám hộ cho cha mẹ/ông bà qua smartphone.
- **Nhóm 3 (Nội bộ)**: Bác sĩ, Lễ tân và Quản trị viên phòng khám cần một hệ thống quản trị (CMS/Dashboard) cực kỳ tinh gọn, thuần Việt, không menu rối rắm.

### 1.3. Triết lý thẩm mỹ & Vibe (Brand Philosophy)
- **TINH THẦN**: **Mộc mạc – Ấm áp – Bình an – Đầy ắp Y đức**.
- **NGUYÊN TẮC BẤT DI BẤT DỊCH**: **Tuyệt đối KHÔNG** làm giao diện theo phong cách bệnh viện công nghệ cao, màu trắng toát sắc lạnh, đèn neon hay kính kim loại bóng bẩy gây cảm giác xa cách, lạnh lẽo và âu lo cho người bệnh.

---

## 2. KIẾN TRÚC CÔNG NGHỆ CHUẨN DOANH NGHIỆP (TECH STACK)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          CLIENT TIER (FRONT-END)                        │
│   React 18+ (Vite) | Tailwind CSS / Pure CSS | Axios | React Router v6  │
│          Thiết kế Mobile-First | Typography cho Người Lớn Tuổi          │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ (RESTful JSON / HTTPS)
┌────────────────────────────────────▼────────────────────────────────────┐
│                        APPLICATION TIER (BACK-END)                      │
│             Java 17/21 + Spring Boot 3.x (Clean Architecture)           │
│   Spring Web | Spring Security 6 (JWT) | Spring Data JPA | Validation   │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ (HikariCP Connection Pool)
┌────────────────────────────────────▼────────────────────────────────────┐
│                           DATABASE TIER (STORAGE)                       │
│           MySQL 8.0 (InnoDB Engine | Charset utf8mb4_unicode_ci)        │
└─────────────────────────────────────────────────────────────────────────┘
```

| Tầng hệ thống | Công nghệ lựa chọn | Lý do và Quy chuẩn kỹ thuật |
| :--- | :--- | :--- |
| **Frontend** | **React.js** (Khởi tạo bằng Vite) | Tốc độ tải trang siêu nhanh, SPA mượt mà, dễ dàng xây dựng component tái sử dụng. |
| **Styling** | **Tailwind CSS + Custom Theme** | Tùy biến bảng màu Thảo mộc & Nâu gỗ, quản lý kích thước chữ tập trung. |
| **Backend** | **Java Spring Boot 3.x** | Tiêu chuẩn doanh nghiệp (Enterprise Grade): Ổn định cao, bảo mật chặt chẽ, đa luồng tốt, dễ bảo trì lâu dài. |
| **Database** | **MySQL 8.0** | RDBMS phổ biến, hỗ trợ quan hệ bảng chặt chẽ (ACID), Indexing và Foreign Keys mạnh mẽ. |
| **API Protocol**| **RESTful API Specification** | Tuân thủ chuẩn định dạng JSON, HTTP Methods (`GET`, `POST`, `PUT`, `DELETE`), mã lỗi HTTP chuẩn quốc tế. |

---

## 3. TIÊU CHUẨN FRONT-END & TRẢI NGHIỆM NGƯỜI LỚN TUỔI (ELDERLY UX/UI)

### 3.1. Bảng màu "Mộc mạc Thảo mộc & Nâu gỗ" (Warm Herbal Palette)
* **Màu Nâu Gỗ Trầm (Wood Brown - Primary)**: `#5C3A21` hoặc `#78350F` — Đại diện cho bàn bắt mạch, tủ thuốc đông y, tạo cảm giác vững chãi, mộc mạc.
* **Màu Xanh Thảo Dược (Herbal Green - Secondary)**: `#2D5A27` hoặc `#15803D` — Đại diện cho lá cây thuốc nam, sự sống, hồi phục tự nhiên.
* **Màu Vàng Rơm Ấm (Warm Ochre - Accent)**: `#D97706` hoặc `#F59E0B` — Điểm xuyết cho các nút bấm quan trọng, lời khuyên sức khỏe.
* **Màu Nền (Background)**: `#FDFBF7` (Trắng ngà / Giấy xuyến chỉ) — Không dùng màu trắng tuyết `#FFFFFF` chói gắt.
* **Màu Chữ (Text)**: `#1F1610` (Nâu đen đậm) — Đạt tỷ lệ tương phản tối đa (> 7:1 đạt chuẩn WCAG AAA).

### 3.2. Quy tắc giao diện cho người cao tuổi
1. **Cỡ chữ (Typography)**:
   - Cỡ chữ nội dung đọc (Body text): **Tối thiểu 18px - 20px**.
   - Khoảng cách dòng (`line-height`): Tối thiểu `1.6 - 1.8` để chữ không dính vào nhau.
   - Font chữ khuyên dùng: Noto Sans, Be Vietnam Pro, hoặc Inter (dày dặn, nét rõ ràng, không dùng font chân mỏng hoặc uốn lượn rối mắt).
2. **Loại bỏ hiệu ứng gây rối mắt (No Distracting Animations)**:
   - Nghiêm cấm: Chữ bay từ trái sang phải, chữ trượt giật cục, pop-up nhảy bất ngờ, xoay lật 3D.
   - Chỉ cho phép: Fade in êm dịu (150ms - 200ms) khi tải trang.
3. **Cụm nút cố định dính đáy trên điện thoại (Mobile Sticky Bottom Bar)**:
   - Thiết kế 2 nút to nằm cố định ở đáy màn hình điện thoại:
     - 📞 **NÚT 1: GỌI HOTLINE BÁC SĨ** (Bấm là gọi ngay `tel:...`, màu xanh lá nổi bật).
     - 📍 **NÚT 2: CHỈ ĐƯỜNG ĐẾN PHÒNG KHÁM** (Bấm là mở thẳng Google Maps dẫn đường).
4. **Tối ưu 3G/4G & Tải trang cực nhanh**:
   - Sử dụng định dạng ảnh nén thế hệ mới WebP.
   - Kỹ thuật **Lazy loading** cho toàn bộ hình ảnh và iframe Google Maps.
   - Tránh nạp các thư viện JavaScript thừa thãi nặng nề.

### 3.3. Tích hợp bản đồ chính xác
- Nhúng Iframe Google Maps tương tác mượt mà, ghim đúng tọa độ trụ sở phòng khám trên trục đường **Điện Biên Phủ** (TP. Hồ Chí Minh).

### 3.4. Giao diện Quản trị nội bộ tinh gọn (Admin CMS)
- **100% Tiếng Việt thuần túy**, thuật ngữ bình dị, dễ hiểu.
- Bỏ toàn bộ các dashboard biểu đồ rối rắm không dùng đến.
- Tập trung vào **3 phân hệ cốt lõi**:
  1. **Lịch hẹn tiếp nhận**: Xem danh sách bệnh nhân vừa đăng ký, số điện thoại, giờ hẹn, bấm nút xác nhận.
  2. **Quản lý bài viết**: Đăng bài thuốc, mẹo chữa bệnh, hình ảnh hoạt động từ thiện (Soạn thảo văn bản đơn giản).
  3. **Quản lý tài khoản**: Thêm/xóa tài khoản bác sĩ, phân quyền nhân viên.

---

## 4. KIẾN TRÚC BACK-END & DỮ LIỆU (JAVA SPRING BOOT & MYSQL)

### 4.1. Cấu trúc dự án Java Spring Boot (Clean Layered Architecture)
```text
com.chuctupk
├── config/              # Cấu hình Spring Security, CORS, Swagger, WebMvc
├── controller/          # REST Controllers: Nhận HTTP request, validate DTO
│   ├── AppointmentController.java
│   ├── ArticleController.java
│   └── AuthController.java
├── service/             # Nghiệp vụ nghiệp vụ (Business Logic & Transactions)
│   ├── AppointmentService.java
│   └── ...
├── repository/          # Spring Data JPA Repositories (Giao tiếp MySQL)
│   ├── AppointmentRepository.java
│   └── ...
├── entity/              # JPA Entities (Ánh xạ các bảng MySQL)
│   ├── Appointment.java
│   ├── Patient.java
│   ├── User.java
│   └── Role.java
├── dto/                 # Data Transfer Objects (Request/Response payload)
│   ├── request/
│   └── response/
└── exception/           # Xử lý ngoại lệ tập trung (@ControllerAdvice)
```

### 4.2. Bảo mật & Phân quyền (Authentication & Authorization)
- **Cơ chế**: Stateless Authentication sử dụng **Spring Security 6 + JSON Web Token (JWT)**.
- **Phân quyền (RBAC)**:
  - `ROLE_ADMIN`: Toàn quyền quản trị tài khoản, cấu hình hệ thống, bài viết, báo cáo.
  - `ROLE_DOCTOR`: Xem danh sách ca khám trong ngày, cập nhật trạng thái bệnh nhân.
  - `ROLE_STAFF`: Tiếp nhận đặt lịch, gọi điện xác nhận ca khám, hỗ trợ bệnh nhân.
  - `ROLE_PUBLIC`: Người dùng vãng lai đặt lịch khám, đọc bài viết mẹo sức khỏe.
- **Mật khẩu**: Băm mật khẩu bắt buộc bằng **BCryptPasswordEncoder** (Strength 12).

### 4.3. Thiết kế Cơ sở Dữ liệu MySQL 8.0 (Relational Database Schema)

```sql
-- 1. BẢNG TÀI KHOẢN HỆ THỐNG
CREATE TABLE `users` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password_hash` VARCHAR(255) NOT NULL,
  `full_name` VARCHAR(100) NOT NULL,
  `email` VARCHAR(100) NULL,
  `phone` VARCHAR(15) NOT NULL,
  `role` ENUM('ADMIN', 'DOCTOR', 'STAFF') NOT NULL DEFAULT 'STAFF',
  `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. BẢNG BỆNH NHÂN
CREATE TABLE `patients` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `full_name` VARCHAR(100) NOT NULL,
  `phone` VARCHAR(15) NOT NULL,
  `year_of_birth` INT NOT NULL,
  `gender` ENUM('MALE', 'FEMALE', 'OTHER') NOT NULL,
  `address` VARCHAR(255) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_patient_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. BẢNG ĐẶT LỊCH HẸN KHÁM (CHỐNG TRÙNG LỊCH)
CREATE TABLE `appointments` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `booking_code` VARCHAR(30) NOT NULL UNIQUE COMMENT 'PKCT-YYYYMMDD-XXXX',
  `patient_id` BIGINT NOT NULL,
  `appointment_date` DATE NOT NULL,
  `time_slot` VARCHAR(20) NOT NULL COMMENT 'VD: 08:30 - 09:00',
  `symptoms` TEXT NULL COMMENT 'Mô tả triệu chứng bệnh',
  `status` ENUM('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
  `note` VARCHAR(255) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_app_date_slot` (`appointment_date`, `time_slot`),
  CONSTRAINT `fk_app_patient` FOREIGN KEY (`patient_id`) REFERENCES `patients` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. BẢNG BÀI VIẾT & MẸO SỨC KHỎE
CREATE TABLE `articles` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(255) NOT NULL,
  `slug` VARCHAR(255) NOT NULL UNIQUE,
  `summary` TEXT NOT NULL,
  `content` LONGTEXT NOT NULL,
  `thumbnail_url` VARCHAR(500) NULL,
  `author_id` BIGINT NOT NULL,
  `is_published` BOOLEAN NOT NULL DEFAULT TRUE,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_article_author` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 5. QUY CHUẨN RESTful API VÀ MÃ ĐÁP ỨNG (API SPECIFICATION)

### 5.1. Cấu trúc Response chuẩn hóa (Standard Response Envelope)
Mọi API trả về từ Spring Boot đều bọc trong cấu trúc thống nhất:

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Tiếp nhận lịch khám thành công!",
  "data": {
    "bookingCode": "PKCT-20261014-0128",
    "patientName": "Nguyễn Văn A",
    "appointmentDate": "2026-10-15",
    "timeSlot": "08:30 - 09:00",
    "status": "PENDING"
  },
  "timestamp": "2026-10-14T22:15:00"
}
```

### 5.2. Danh sách Endpoint cốt lõi
* **Xác thực**:
  - `POST /api/v1/auth/login` (Đăng nhập nhân viên/bác sĩ, trả về JWT Token)
  - `GET /api/v1/auth/me` (Lấy thông tin tài khoản đang đăng nhập)
* **Đặt lịch nhận bệnh**:
  - `POST /api/v1/appointments/book` (Bệnh nhân/người nhà gửi form đặt lịch)
  - `GET /api/v1/appointments/available-slots?date=YYYY-MM-DD` (Lấy các ca còn trống)
  - `GET /api/v1/appointments` (Admin xem danh sách lịch hẹn)
  - `PUT /api/v1/appointments/{id}/status` (Cập nhật trạng thái duyệt/hủy ca khám)
* **Bài viết / Cẩm nang sức khỏe**:
  - `GET /api/v1/articles` (Công khai danh sách bài viết)
  - `GET /api/v1/articles/{slug}` (Xem chi tiết bài viết)
  - `POST /api/v1/articles` (Admin đăng bài mới)
  - `PUT /api/v1/articles/{id}` (Admin sửa bài viết)

---

## 6. KẾ HOẠCH BÀN GIAO & PHÂN CHIA GIAI ĐOẠN (ROADMAP)

- **Giai đoạn 1 (Khởi tạo Nền tảng)**: 
  - Khởi tạo Backend Spring Boot 3 + MySQL, tạo Entity và Migration database.
  - Khởi tạo Frontend React + Vite + Tailwind với Palette Thảo mộc/Gỗ ấm.
- **Giai đoạn 2 (Tính năng Tiếp nhận bệnh & Đặt lịch)**:
  - Form đặt lịch cho người lớn tuổi (chữ to, đơn giản, chống nhập sai số điện thoại).
  - API lưu lịch hẹn và cơ chế thông báo tiếp nhận.
  - Tích hợp Sticky Hotline & Iframe Google Maps Điện Biên Phủ.
- **Giai đoạn 3 (Admin CMS Tinh gọn)**:
  - Đăng nhập bảo mật JWT.
  - Bảng quản lý duyệt lịch khám 100% tiếng Việt.
  - Quản lý bài viết cẩm nang y tế.
- **Giai đoạn 4 (Kiểm thử, Tối ưu 3G/4G & Hoàn thiện)**:
  - Test trên nhiều kích thước màn hình điện thoại thực tế.
  - Nén ảnh, lazy loading tối ưu tốc độ mạng chậm.
