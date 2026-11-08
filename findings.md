# Findings & Technical Decisions - Docker Packaging

## Quyết định Kiến trúc Container
1. **Dịch vụ MySQL (`chuctupk-mysql`)**:
   - Sử dụng image `mysql:8.0` chính thức.
   - Character set: `utf8mb4`, Collation: `utf8mb4_unicode_ci`.
   - Mount init scripts từ `docker/mysql/init/` vào `/docker-entrypoint-initdb.d/`.
   - Healthcheck sử dụng `mysqladmin ping` để Backend biết chính xác khi nào DB đã sẵn sàng trước khi kết nối (tránh race condition).

2. **Dịch vụ Backend (`chuctupk-backend`)**:
   - Image runtime: `eclipse-temurin:17-jre-alpine` giúp dung lượng image dưới 250MB.
   - Phụ thuộc vào `mysql` service với điều kiện `condition: service_healthy`.
   - Biến môi trường kết nối: `DB_URL=jdbc:mysql://mysql:3306/phongkham_chuctu?...`.
   - Cổng expose: `8080:8080`.

3. **Dịch vụ Frontend (`chuctupk-frontend`)**:
   - Image runtime: `nginx:alpine` phục vụ file tĩnh sau khi build qua Vite.
   - Nginx cấu hình:
     - `try_files $uri $uri/ /index.html;` hỗ trợ React Router SPA.
     - Proxy chuyển tiếp `/api/` trực tiếp sang backend `http://backend:8080/api/` hoặc client gọi trực tiếp qua port 8080.
   - Cổng expose: `80:80` (hoặc `3000:80` nếu người dùng muốn, mặc định chuẩn bị `80:80` và hỗ trợ biến `FRONTEND_PORT`).

4. **Bảo mật và Thông tin nhạy cảm**:
   - Mọi mật khẩu và API key trong Docker Compose đều có fallback mặc định cho môi trường Dev.
   - File cấu hình thực tế sử dụng `.env` hoặc file `.env.docker` (đã nằm trong `.gitignore`).
