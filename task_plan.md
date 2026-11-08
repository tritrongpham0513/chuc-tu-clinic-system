# Kế hoạch Đóng gói Docker Hệ thống Phòng Khám Chúc Từ

## Mục tiêu
Đóng gói toàn bộ hệ thống gồm MySQL 8.0, Backend (Spring Boot 3 / Java 17), và Frontend (React Vite + Nginx) thành môi trường container hóa chuẩn chỉ cần 1 lệnh duy nhất (`docker compose up --build -d`) là có thể khởi chạy hoàn chỉnh.

---

## Các Giai Đoạn Thực Hiện

### Giai đoạn 1: Chuẩn bị Cấu hình Môi trường & Cơ sở Dữ liệu Docker
- [x] Tạo `docker-compose.yml` với cấu hình 3 services: `mysql`, `backend`, `frontend` + volume & network.
- [x] Thiết lập thư mục script khởi tạo database tự động (`docker/mysql/init/`) chạy khi container MySQL tạo mới lần đầu.
- [x] Tạo file biến môi trường Docker mẫu `.env.docker.example` và cấu hình fallback mặc định trong `docker-compose.yml`.

### Giai đoạn 2: Đóng gói Backend (Java Spring Boot)
- [x] Tạo `backend/Dockerfile` multi-stage:
  - Stage 1: Maven build (`maven:3.9-eclipse-temurin-17-alpine`) để compile mã nguồn thành file JAR (cache dependency hiệu quả).
  - Stage 2: Runtime JRE (`eclipse-temurin:17-jre-alpine`) siêu nhẹ, an toàn, non-root user.
- [x] Tạo `backend/.dockerignore` loại bỏ `.git`, `target/`, `.env`, logs...

### Giai đoạn 3: Đóng gói Frontend (React Vite + Nginx)
- [x] Tạo `frontend/Dockerfile` multi-stage:
  - Stage 1: Node.js build (`node:20-alpine`) chạy `npm run build`.
  - Stage 2: Web server Nginx (`nginx:alpine`) phục vụ static files và cấu hình proxy `/api/` trỏ về backend service.
- [x] Tạo `frontend/nginx.conf` với cấu hình SPA routing (`try_files $uri /index.html;`), gzip compression, và proxy API.
- [x] Tạo `frontend/.dockerignore` loại bỏ `node_modules/`, `dist/`, `.env`, `.git`.

### Giai đoạn 4: Kiểm tra Cú pháp, Đồng bộ & Hướng dẫn Sử dụng
- [x] Kiểm tra cú pháp `docker-compose.yml` (`docker-compose config`).
- [x] Tạo file hướng dẫn chạy nhanh `DOCKER_GUIDE.md` (tiếng Việt, 1 click / 1 command).
- [x] Cập nhật file trí nhớ `findings.md`, `progress.md`.
- [x] Đẩy commit lên nhánh `feat/clinic-management-system`.
