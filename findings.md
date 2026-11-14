# Sổ Tay Phát Hiện & Đánh Giá Toàn Bộ Hệ Thống (Findings & System Audit)

## 1. Tổng Quan Trạng Thái Hệ Thống
- **Backend (Spring Boot 3 / Java 17)**: Biên dịch thành công 100% (`mvnw test-compile` OK). Cấu hình phân tầng Clean Layered Architecture với JWT stateless, RBAC 4 vai trò (ADMIN, STAFF, DOCTOR, PATIENT).
- **Frontend (React 18 + Vite)**: Build thành công 100% (`npm run build` OK, 15 chunks tạo ra trong ~10s). Giao diện tối ưu rất tốt chuẩn Người cao tuổi (Elderly-First UX), màu sắc Warm Herbal & Wood, font size 18px+, touch target 48-56px, không animation giật cục.
- **Database (MySQL 8.0)**: InnoDB utf8mb4_unicode_ci, các bảng có ràng buộc UNIQUE chống trùng lịch và giữ chỗ ghế khám (`appointment_seats`).
- **Container (Docker)**: Multi-stage Dockerfile cho cả Backend và Frontend, Nginx reverse proxy, docker-compose với healthcheck.

---

## 2. Các Lỗi Nghiêm Trọng / Rủi Ro Lớn (Critical Bugs & High Risks)

1. **Rủi ro build Docker Frontend do mất file `package-lock.json`**:
   - `frontend/Dockerfile` yêu cầu: `COPY package.json package-lock.json ./` và `RUN npm ci`.
   - File này từng bị xóa trên workspace; đã được khôi phục thành công từ Git HEAD. Cần duy trì file này để tránh gãy build Docker.

2. **Xung đột phân quyền giữa Servlet Filter và Controller Method Security**:
   - Các controller: `AdminAppointmentController`, `AdminMedicalRecordController`, `AdminDashboardController` đều gắn `@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR')")`.
   - Nhưng trong `SecurityConfig.java`:
     ```java
     .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
     ```
   - Tầng Servlet Filter chặn trước khi tới Method Security, dẫn đến việc tài khoản Bác sĩ (`DOCTOR`) và Tiếp tân (`STAFF`) khi gọi vào các endpoint `/api/v1/admin/**` này sẽ bị trả về ngay lập tức mã lỗi **403 Forbidden**.
   - *Ghi chú*: Rất may mắn là Frontend hiện tại đã được tách sang gọi `/api/v1/staff/**` thay vì gọi vào các controller này.

3. **Thiếu kiểm tra ca khám trong quá khứ đối với ngày hôm nay (Past Slot on Today)**:
   - Trong `PatientPortalServiceImpl.bookAppointment`, hệ thống chỉ kiểm tra:
     ```java
     if (slotDate.isBefore(LocalDate.now())) { ... }
     ```
   - Nếu bệnh nhân gửi request đặt lịch cho chính ngày hôm nay nhưng vào khung giờ đã trôi qua (ví dụ lúc 15:00 đặt khung 08:00 sáng), API đặt lịch vẫn chấp nhận và cấp ghế.
   - Cần bổ sung kiểm tra: Nếu `slotDate.equals(today)` thì `chosenSlot.getEndTime()` không được trước `LocalTime.now(clock)`.

4. **Sử dụng `LocalDate.now()` trực tiếp thay vì Bean `Clock` (Lệch múi giờ)**:
   - `DateTimeConfig.java` đã cung cấp Bean `Clock` múi giờ `Asia/Ho_Chi_Minh`.
   - `StaffServiceImpl` và `AdminServiceImpl` dùng `clock` rất tốt, nhưng `PatientPortalServiceImpl` lại gọi trực tiếp `LocalDate.now()` và `LocalTime.now()`.
   - Nếu máy chủ triển khai trên Cloud (AWS/GCP/Render) chạy hệ điều hành múi giờ UTC, các phép kiểm tra ngày giờ đặt lịch sẽ bị lệch 7 tiếng.

5. **Lộ token qua Cookie không có HttpOnly**:
   - `CookieHelper.java` (dòng 61) thiết lập cookie `ct_token_legacy` với `httpOnly(false)`. Frontend hiện tại chỉ dùng HttpOnly Cookie và Authorization Bearer Header, việc giữ cookie không HttpOnly tạo rủi ro rò rỉ token nếu dính XSS.

---

## 3. Các Điểm Bất Hợp Lý & Code Smells (Architecture Smells & Inconsistencies)

1. **Phân mảnh Controller & Controller rác (Duplicate / Dead Controllers)**:
   - Hệ thống đang tồn tại **2 bộ Controller song song**:
     - *Bộ chính thức v1*: `StaffController`, `StaffPatientController`, `StaffRecordController`, `MeController`, `AdminController`, `PublicPostController`, `MetaController`, `AuthController`.
     - *Bộ dư thừa / bypass service*: `AdminAppointmentController`, `ReceptionistController`, `AdminDashboardController`, `AdminMedicalRecordController`, `AdminUserController`, `DoctorWorkspaceController`, `BookingController`, `PatientProfileController`, `QueueController`.
   - Bộ dư thừa vừa bypass service (inject trực tiếp 5-7 JPA Repositories vào Controller), vừa chứa logic lỗi (như tự ý gán bác sĩ đầu tiên trong DB), vừa không được Frontend gọi tới. Nên dọn dẹp hoặc đánh dấu `@Deprecated` / xóa bỏ để tránh nhầm lẫn.

2. **Lưu cache tạm của OTP đăng ký và Reset Token trong RAM máy chủ (`ConcurrentHashMap`)**:
   - Trong `AuthServiceImpl.java`, `pendingRegistrations`, `resetTokens`, `pendingEmailChanges` được lưu trong RAM của tiến trình Java.
   - Khi server restart hoặc chạy mô hình scale đa container (load balancing), người dùng vừa nhận OTP xong bấm xác nhận sẽ bị báo lỗi "Mã không hợp lệ hoặc đã hết hạn".

3. **Hardcode năm sinh 1970 khi Admin tạo tài khoản Bệnh nhân**:
   - Tại `AdminServiceImpl.createAccount`, khi tạo tài khoản bệnh nhân, hồ sơ bệnh nhân mặc định bị gán cứng `yearOfBirth(1970)` vì DTO `AdminCreateAccountRequest` chưa có trường năm sinh và giới tính.

4. **Mâu thuẫn biến môi trường Frontend giữa `docker-compose.yml` và `Dockerfile`**:
   - `docker-compose.yml` truyền `args: VITE_API_BASE_URL: /api/v1`.
   - `frontend/Dockerfile` lại khai báo `ARG VITE_API_URL=`.
   - Cần đồng bộ tên biến môi trường thành `VITE_API_URL`.

5. **Thiếu script Seed bài viết cẩm nang trong `docker/mysql/init/`**:
   - `database/seed_articles.sql` có dữ liệu mẫu nhưng chưa được copy sang `docker/mysql/init/`. Khi chạy container database mới tinh, bảng `articles` sẽ bị trống.

6. **Mâu thuẫn file cấu hình `.env`**:
   - Gốc `.env`: `SMS_ENABLED=true`
   - `backend/.env`: `SMS_ENABLED=false`
   - Tùy vào working directory khi chạy app, `DotenvLoader` sẽ load file khác nhau dẫn đến hành vi gửi SMS khác nhau.
