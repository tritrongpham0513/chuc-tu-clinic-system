# HỆ THỐNG QUẢN LÝ VÀ TIẾP NHẬN BỆNH NHÂN - PHÒNG KHÁM CHÚC TỪ

> **Chuc Tu Traditional Medicine Clinic Management System**  
> Nền tảng website tiếp nhận đặt lịch khám bệnh và quản trị vận hành phòng khám Y học Cổ truyền, Châm cứu, Bấm huyệt và Phục hồi chức năng.

---

## 1. TRIẾT LÝ THIẾT KẾ: "ELDERLY-CENTRIC" (ƯU TIÊN NGƯỜI CAO TUỔI)

Khác với các hệ thống y tế công nghệ cao thông thường, Phòng Khám Chúc Từ được định hình với tôn chỉ: **Mộc mạc – Ấm áp – Dễ dùng**:

- **Cỡ chữ to & Dễ đọc**: Toàn bộ nội dung văn bản thông thường (body text) đạt kích thước tối thiểu `18px - 20px`, tiêu đề từ `24px - 32px`.
- **Bảng màu Thảo mộc & Nâu gỗ (Warm Herbal)**: 
  - Nâu gỗ trầm (`#5C3A21` / `#78350F`): Màu bàn bắt mạch, tủ thuốc đông y.
  - Xanh thảo mộc (`#2D5A27` / `#15803D`): Màu lá thuốc nam, biểu trưng cho sự hồi phục tự nhiên.
  - Vàng rơm ấm (`#D97706` / `#F59E0B`): Điểm xuyết cho các nút bấm hành động quan trọng.
  - Nền giấy xuyến chỉ (`#FDFBF7`): Độ tương phản cao (> 7:1 đạt chuẩn WCAG AAA), dịu mắt cho người lớn tuổi.
- **Loại bỏ hiệu ứng gây rối mắt**: Tuyệt đối không dùng chữ trượt bay, pop-up bất ngờ hay hoạt ảnh 3D.
- **Mobile-First & Sticky Bottom Bar**: 2 nút lớn cố định ở đáy màn hình điện thoại:
  - **Nút Hotline**: Bấm gọi ngay cho bác sĩ trực (`tel:...`).
  - **Nút Chỉ đường**: Mở trực tiếp Google Maps đến trụ sở phòng khám (Điện Biên Phủ, TP.HCM).

---

## 2. KIẾN TRÚC CÔNG NGHỆ (TECH STACK)

Dự án áp dụng mô hình **Monorepo** chuẩn mực:

```text
phong-kham-chuc-tu/
├── frontend/           # Ứng dụng Web Client & Admin CMS (React 18 + Vite + Tailwind)
├── backend/            # RESTful API Backend (Java 17/21 + Spring Boot 3.x/4.x)
├── database/           # Kịch bản DDL MySQL 8.0 & Seed Data
├── docs/               # Tài liệu dự án (PROJECT_CONTEXT.md, context.txt)
├── AGENTS.md           # Bộ quy tắc hướng dẫn AI Agent
└── README.md           # Tài liệu giới thiệu dự án
```

| Tầng hệ thống | Công nghệ | Mô tả kỹ thuật |
| :--- | :--- | :--- |
| **Frontend** | React 18 (Vite) | Tốc độ tải trang siêu nhanh, SPA mượt mà, tối ưu mạng 3G/4G |
| **Styling** | Tailwind CSS | Quản lý bảng màu thảo mộc và typography cỡ lớn tập trung |
| **Backend** | Java 17/21 + Spring Boot 3/4 | Clean Layered Architecture, Spring Security 6, Spring Data JPA |
| **Authentication** | Stateless JWT | Xác thực phân quyền RBAC an toàn, thời gian sống linh hoạt |
| **Database** | MySQL 8.0 | Engine InnoDB, Charset `utf8mb4_unicode_ci` hỗ trợ tiếng Việt trọn vẹn |

---

## 3. PHÂN HỆ NGƯỜI DÙNG & VAI TRÒ (RBAC)

Hệ thống hỗ trợ 4 vai trò rõ ràng:

1. **QUẢN TRỊ VIÊN (`ADMIN`)**:
   - Quản lý tài khoản và phân quyền bác sĩ, nhân viên.
   - Quản lý danh mục bài viết, khung giờ khám và cấu hình hệ thống.
2. **BÁC SĨ (`DOCTOR`)**:
   - Theo dõi danh sách ca khám trong ngày.
   - Cập nhật ghi chú bệnh án, duyệt/hoàn thành ca khám.
   - Đăng tải bài viết chia sẻ mẹo dân gian, cẩm nang sức khỏe.
3. **LỄ TÂN / ĐIỀU DƯỠNG (`STAFF`)**:
   - Tiếp nhận lịch hẹn mới từ người bệnh.
   - Gọi điện xác nhận, sắp xếp ca khám và phân bổ bác sĩ.
4. **BỆNH NHÂN & CON CHÁU (`PATIENT`)**:
   - Đặt lịch khám nhanh không bắt buộc đăng nhập (chỉ cần điền SĐT & triệu chứng).
   - Tra cứu mã hẹn khám bệnh.
   - Đăng ký tài khoản để quản lý hồ sơ khám của nhiều người thân trong gia đình.

---

## 4. CƠ SỞ DỮ LIỆU (8 BẢNG HOÀN CHỈNH)

- `users`: Tài khoản đăng nhập hệ thống nội bộ (Băm mật khẩu bằng BCrypt).
- `doctors`: Hồ sơ chuyên môn bác sĩ, chuyên khoa, liên kết 1-1 với `users`.
- `patients`: Hồ sơ bệnh nhân (mã BN, họ tên, năm sinh, SĐT).
- `time_slots`: Khung giờ khám (mỗi ca 30 phút, sáng/chiều, giới hạn số người/ca).
- `doctor_schedules`: Lịch làm việc hàng tuần của từng bác sĩ.
- `appointments`: Lịch hẹn khám (mã booking, chống trùng lịch, quản lý trạng thái).
- `article_categories`: Danh mục bài viết cẩm nang sức khỏe.
- `articles`: Bài viết mẹo chữa bệnh, y học cổ truyền, tin từ thiện.

---

## 5. HƯỚNG DẪN KHỞI CHẠY (QUICK START)

### CÁCH 1: KHỞI CHẠY SIÊU TỐC BẰNG DOCKER (KHUYÊN DÙNG - 1 LỆNH DUY NHẤT)

> **Yêu cầu**: Máy tính đã cài đặt **Docker Desktop**. Bạn không cần cài thêm Java, Maven, Node.js hay MySQL.

1. **Clone mã nguồn và chuyển sang nhánh tính năng:**
   ```bash
   git clone https://github.com/tritrongpham0513/chuc-tu-clinic-system.git
   cd chuc-tu-clinic-system
   git checkout feat/clinic-management-system
   ```

2. **Chạy 1 lệnh duy nhất để khởi động toàn bộ hệ thống:**
   ```bash
   docker compose up --build -d
   ```
   *(Hệ thống sẽ tự động build Backend, Frontend, dựng Database MySQL, nạp 21 bảng và 3 tài khoản mẫu)*

3. **Mở trình duyệt truy cập:**
   - **Giao diện Website & Quản trị**: [http://localhost:3000](http://localhost:3000)
   - **Tài liệu API Swagger**: [http://localhost:8088/swagger-ui.html](http://localhost:8088/swagger-ui.html)

---

### CÁCH 2: KHỞI CHẠY THỦ CÔNG TỪNG PHẦN (LOCAL DEVELOPMENT)

> **Yêu cầu**: Đã cài đặt Java 17+, MySQL 8.0+, Node.js 18+.

#### Bước 1: Khởi tạo Cơ sở Dữ liệu MySQL
Mở MySQL Workbench hoặc Terminal và chạy lần lượt 3 file script:
```bash
mysql -u root -p < database/001_schema_v2.sql
mysql -u root -p < database/002_add_patient_fields.sql
mysql -u root -p < database/seed_staff_and_doctor.sql
```

#### Bước 2: Thiết lập file môi trường (.env)
Sao chép file mẫu thành file `.env` tại thư mục gốc:
```bash
# Windows PowerShell:
Copy-Item .env.example .env

# Linux / macOS:
cp .env.example .env
```
Mở file `.env` và điền mật khẩu MySQL của máy bạn (`DB_PASSWORD=...`).

#### Bước 3: Khởi chạy Backend (Spring Boot)
```bash
cd backend
# Windows:
.\mvnw.cmd spring-boot:run

# Linux / macOS:
./mvnw spring-boot:run
```
*API sẵn sàng tại:* `http://localhost:8080/api/v1`

#### Bước 4: Khởi chạy Frontend (React + Vite)
Mở cửa sổ Terminal mới:
```bash
cd frontend
npm install
npm run dev
```
*Giao diện sẵn sàng tại:* `http://localhost:5173`

---

## 6. DANH SÁCH TÀI KHOẢN MẪU CÓ SẴN (TESTING ACCOUNTS)

Hệ thống đã chuẩn bị sẵn 3 tài khoản để bạn có thể đăng nhập kiểm thử ngay:

| Vai trò | Tên đăng nhập | Mật khẩu mặc định | Đường dẫn đăng nhập |
| :--- | :--- | :--- | :--- |
| **Quản trị viên (ADMIN)** | `admin` | `MatKhauAdmin@2026` | `/admin/login` |
| **Tiếp tân / Điều dưỡng (STAFF)** | `letan` | `MatKhauAdmin@2026` | `/staff/login` |
| **Bác sĩ YHCT (DOCTOR)** | `bacsi` | `MatKhauAdmin@2026` | `/doctor/login` |
