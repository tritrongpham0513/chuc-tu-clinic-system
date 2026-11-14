# 🚀 HƯỚNG DẪN DEPLOY HỆ THỐNG PHÒNG KHÁM CHÚC TỪ LÊN RENDER

> **Tài liệu hướng dẫn triển khai thực tế (Production Deployment)**  
> Áp dụng cho: Backend (Spring Boot 4 / Java 17), Frontend (React Vite + Nginx), Database (MySQL 8.0).

---

## 🏗️ 1. TỔNG QUAN KIẾN TRÚC TRÊN RENDER

Render (render.com) là nền tảng Cloud hiện đại hỗ trợ Docker container rất mạnh mẽ. Tuy nhiên, Render **chỉ có sẵn dịch vụ PostgreSQL**, không có MySQL managed service.

Do đó, kiến trúc tối ưu và tiết kiệm chi phí nhất (hoàn toàn **MIỄN PHÍ**) bao gồm:
1. **Cơ sở dữ liệu (MySQL 8.0)**: Tạo miễn phí trên **TiDB Cloud** hoặc **Aiven for MySQL** (tương thích 100% MySQL 8.0, có 5GB miễn phí trọn đời, kết nối siêu nhanh tới Render Singapore).
2. **Backend Web Service**: Chạy qua `backend/Dockerfile` trên Render.
3. **Frontend Web Service**: Chạy qua `frontend/Dockerfile` (Nginx Reverse Proxy) trên Render, tự động chuyển `/api/**` sang Backend mà không bị chặn Cookie phiên `ct_session`.

---

## 🗄️ BƯỚC 1: TẠO DATABASE MYSQL CLOUD MIỄN PHÍ (2 PHÚT)

Bạn có thể chọn **TiDB Cloud** (khuyên dùng nhất vì miễn phí trọn đời và hỗ trợ MySQL 8.0 cực chuẩn):

1. Truy cập [https://tidbcloud.com/](https://tidbcloud.com/) hoặc [https://aiven.io/](https://aiven.io/) và đăng ký bằng tài khoản Google/GitHub.
2. Tạo cụm **Serverless Tier (Free)**:
   - Chọn Region: **Singapore (ap-southeast-1)** (gần Việt Nam nhất).
   - Đặt tên Database: `phongkham_chuctu`.
   - Đặt mật khẩu cho người dùng `root` (hoặc admin).
3. Sau khi tạo xong, lấy thông tin kết nối:
   - **Host**: (Ví dụ: `gateway01.ap-southeast-1.prod.aws.tidbcloud.com`)
   - **Port**: `4000` (hoặc `3306` tùy nhà cung cấp)
   - **User**: (Ví dụ: `xxxx.root`)
   - **Password**: Mật khẩu bạn vừa đặt
4. **Nạp dữ liệu khởi tạo ban đầu**:
   - Mở DBeaver, Navicat hoặc MySQL Workbench trên máy bạn.
   - Kết nối tới Database Cloud bằng thông tin trên.
   - Mở và chạy lần lượt các file trong thư mục `docker/mysql/init/` từ máy bạn:
     - `01-schema.sql`
     - `02-patient-fields.sql`
     - `03-seed-users.sql`
     - `04-contract-alignment.sql`
     - `05-seed-slots-60min.sql`
     - `06-v1-support.sql`
     - `07-fix-mojibake-data.sql`
     - `08-seed-articles.sql`
   *(Database sẽ có đầy đủ 27 bảng và các tài khoản mẫu `admin`, `bacsi`, `letan`)*.

---

## ⚙️ BƯỚC 2: DEPLOY BACKEND LÊN RENDER

1. Đăng nhập vào [Render Dashboard](https://dashboard.render.com/).
2. Nhấn nút **New +** ở góc trên cùng bên phải ➡️ Chọn **Web Service**.
3. Chọn **Build and deploy from a Git repository** ➡️ Kết nối tài khoản GitHub và chọn kho mã nguồn: `tritrongpham0513/chuc-tu-clinic-system`.
4. Điền cấu hình cơ bản:
   - **Name**: `chuctu-backend` (hoặc tên bạn thích).
   - **Region**: **Singapore** (chọn cùng vùng với Database để tốc độ nhanh nhất).
   - **Branch**: `feat/clinic-management-system`.
   - **Root Directory**: (Để trống).
   - **Runtime**: **Docker**.
   - **Dockerfile Path**: `./backend/Dockerfile`.
   - **Docker Build Context**: `./backend`.
   - **Instance Type**: **Free**.
5. Mở mục **Advanced** ➡️ Nhập **Health Check Path**: `/api/v1/health`.
6. Tại mục **Environment Variables**, thêm các biến sau:
   | Tên biến (Key) | Giá trị mẫu (Value) | Ghi chú |
   | :--- | :--- | :--- |
   | `DB_URL` | `jdbc:mysql://<host>:<port>/phongkham_chuctu?useSSL=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8` | Chuỗi kết nối Database Cloud ở Bước 1 |
   | `DB_USERNAME` | `<user_database>` | Tên tài khoản Database Cloud |
   | `DB_PASSWORD` | `<password_database>` | Mật khẩu Database Cloud |
   | `ADMIN_USERNAME`| `admin` | Tên đăng nhập Admin |
   | `ADMIN_PASSWORD`| `<Mat_khau_admin_tu_chon>` | Mật khẩu Admin khởi tạo |
   | `ADMIN_PHONE`   | `0912345678` | Số điện thoại Admin |
   | `JWT_SECRET`    | `ChuctuClinicSecretKeyForJwtToken2026MustBeAtLeast256BitsLong` | Khóa bí mật ký token |
   | `SMS_ENABLED`   | `false` | Tắt SMS thật trong môi trường demo |
7. Nhấn **Create Web Service**.
8. Đợi khoảng 2–3 phút để Render build Dockerfile. Khi Render báo `Live`, sao chép địa chỉ URL của backend (ví dụ: `https://chuctu-backend.onrender.com`).
   - Mở kiểm tra tại: `https://chuctu-backend.onrender.com/api/v1/health` ➡️ Thấy trả về `{"success": true, ... "status": "UP"}` là Backend đã chạy hoàn hảo!

---

## 🎨 BƯỚC 3: DEPLOY FRONTEND LÊN RENDER

1. Trong Render Dashboard, bấm **New +** ➡️ Chọn **Web Service**.
2. Chọn repo `tritrongpham0513/chuc-tu-clinic-system`.
3. Điền cấu hình:
   - **Name**: `chuctu-frontend` (hoặc tên phòng khám bạn chọn).
   - **Region**: **Singapore**.
   - **Branch**: `feat/clinic-management-system`.
   - **Runtime**: **Docker**.
   - **Dockerfile Path**: `./frontend/Dockerfile`.
   - **Docker Build Context**: `./frontend`.
   - **Instance Type**: **Free**.
4. Tại mục **Environment Variables**, thêm 1 biến duy nhất:
   | Key | Value |
   | :--- | :--- |
   | `API_UPSTREAM` | `https://chuctu-backend.onrender.com` |
   *(Điền chính xác đường dẫn URL Backend bạn vừa tạo ở Bước 2, có `https://`, không có gạch chéo `/` ở cuối)*.
5. Nhấn **Create Web Service**.
6. Đợi Render build xong trong khoảng 1–2 phút. Khi báo `Live`, bạn sẽ nhận được đường dẫn frontend (ví dụ: `https://chuctu-frontend.onrender.com`).

---

## 🎉 BƯỚC 4: TRUY CẬP VÀ KIỂM THỬ HỆ THỐNG TRÊN CLOUD

- 🌐 **Trang chủ Người bệnh**: `https://chuctu-frontend.onrender.com`
- 🏥 **Bàn làm việc Bác sĩ / Tiếp tân**: `https://chuctu-frontend.onrender.com/noi-bo/`
  - Đăng nhập thử bằng tài khoản: `letan` / `MatKhauAdmin@2026` hoặc `bacsi` / `MatKhauAdmin@2026`.
- 📋 **Tài liệu Swagger API**: `https://chuctu-backend.onrender.com/swagger-ui.html`

---

## 💡 LƯU Ý KHI DÙNG GÓI RENDER FREE TIER

1. **Cơ chế Sleep khi không có truy cập (Spin-down)**:
   - Trên gói Free của Render, nếu không có ai truy cập trong vòng 15 phút, Web Service sẽ tạm "ngủ".
   - Khi có lượt truy cập mới, Render mất khoảng 30–50 giây để "đánh thức" lại service. Đây là hành vi hoàn toàn bình thường của gói miễn phí.
2. **Nginx Reverse Proxy**:
   - Nhờ có Nginx đóng vai trò Reverse Proxy trong `frontend/Dockerfile`, toàn bộ cookie xác thực (`ct_session`) được coi là cookie cùng nguồn (First-party cookie), giúp tránh 100% các lỗi bị trình duyệt Safari/Chrome chặn cookie của bên thứ ba khi chạy trên 2 tên miền khác nhau!
