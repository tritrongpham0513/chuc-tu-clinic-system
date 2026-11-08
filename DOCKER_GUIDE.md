# 🐳 Hướng Dẫn Khởi Chạy Hệ Thống Bằng Docker (1 Lệnh Duy Nhất)

Hệ thống Phòng Khám Chúc Từ đã được đóng gói container hóa hoàn chỉnh theo kiến trúc chuẩn sản xuất với **3 dịch vụ tích hợp**:
1. **Frontend**: React 18 + Vite, phục vụ bởi Web Server Nginx (đã tích hợp proxy đảo ngược `/api/` chống lỗi CORS).
2. **Backend**: Java 17 + Spring Boot 3, tối ưu image JRE Alpine siêu nhẹ và kiểm tra sức khỏe tự động.
3. **Database**: MySQL 8.0 (InnoDB, UTF-8 chuẩn Y tế), tự động nạp toàn bộ cấu trúc bảng và tài khoản mẫu ngay lần chạy đầu tiên.

---

## ⚡ 1. Khởi chạy toàn bộ hệ thống (1 Lệnh duy nhất)

Mở Terminal tại thư mục gốc dự án (`phong-kham-chuc-tu`) và chạy:

```bash
docker compose up --build -d
```
> *(Nếu máy bạn dùng Docker Compose v1 cũ, có thể chạy `docker-compose up --build -d`)*

Container sẽ tự động tải các image cần thiết, biên dịch Backend & Frontend, khởi tạo Database và thiết lập kết nối mạng nội bộ.

---

## 🌐 2. Địa chỉ truy cập các dịch vụ

| Dịch vụ | URL truy cập | Mô tả |
| :--- | :--- | :--- |
| **Giao diện Người dùng & Admin** | [http://localhost:3000](http://localhost:3000) | Trang chủ đặt lịch, Cổng Admin, Bàn Bác sĩ, Bàn Lễ tân |
| **Backend REST API** | [http://localhost:8080](http://localhost:8080) | Cổng API Spring Boot phục vụ trực tiếp |
| **Tài liệu Swagger / OpenAPI** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | Kiểm thử trực tiếp 20+ endpoint API |
| **MySQL Database** | `localhost:3306` | User: `root` \| Password: `root` \| DB: `phongkham_chuctu` |

---

## 🔑 3. Tài khoản mẫu có sẵn trong hệ thống

Database container đã được cài đặt sẵn 3 tài khoản mẫu để bạn có thể đăng nhập thử nghiệm ngay lập tức:

| Vai trò | Tên đăng nhập | Mật khẩu mặc định | Trang đăng nhập |
| :--- | :--- | :--- | :--- |
| **Quản trị viên (ADMIN)** | `admin` | `MatKhauAdmin@2026` | [http://localhost:3000/admin/login](http://localhost:3000/admin/login) |
| **Tiếp tân (STAFF)** | `letan` | `MatKhauAdmin@2026` | [http://localhost:3000/staff/login](http://localhost:3000/staff/login) |
| **Bác sĩ YHCT (DOCTOR)** | `bacsi` | `MatKhauAdmin@2026` | [http://localhost:3000/doctor/login](http://localhost:3000/doctor/login) |

---

## 🛠️ 4. Các lệnh quản lý tiện ích

### 📋 Xem nhật ký hoạt động (Logs)
```bash
# Xem log toàn bộ các service theo thời gian thực
docker compose logs -f

# Chỉ xem log của Backend Spring Boot
docker compose logs -f backend

# Chỉ xem log của Frontend Nginx
docker compose logs -f frontend
```

### 🔍 Kiểm tra trạng thái các container
```bash
docker compose ps
```

### ⏹️ Tạm dừng hệ thống
```bash
docker compose down
```

### 🔄 Khởi tạo lại database sạch từ đầu
```bash
# Lệnh này sẽ xóa volume dữ liệu cũ và nạp lại script gốc khi chạy up
docker compose down -v
docker compose up --build -d
```

