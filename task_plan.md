# Kế hoạch Sửa Lỗi & Tinh Chỉnh Hệ Thống (Implementation Plan)

## Mục tiêu
Sửa toàn bộ các lỗi đã phát hiện trong quá trình kiểm tra, hoàn thiện logic đặt lịch, bảo mật cookie, đồng bộ Docker và tinh chỉnh kiến trúc hệ thống Phòng Khám Chúc Từ.

---

## Các Hạng Mục Thực Hiện

### Bước 1: Sửa Logic Đặt Lịch Khám & Chuẩn Hóa Múi Giờ
- [x] Cập nhật `PatientPortalServiceImpl.java`:
  - Inject Bean `Clock clock` (múi giờ `Asia/Ho_Chi_Minh`), thay thế toàn bộ `LocalDate.now()` / `LocalTime.now()`.
  - Bổ sung kiểm tra chặt chẽ: Chặn đặt lịch vào khung giờ đã trôi qua trong ngày hôm nay (`slotDate.equals(today) && chosenSlot.getEndTime().isBefore(nowTime)`).
  - Rà soát đồng bộ với `StaffServiceImpl.java` (kiểm tra khung giờ đã qua khi tạo phiếu tại quầy không check-in ngay).

### Bước 2: Bảo Mật Cookie & Loại Bỏ Cookie Không HttpOnly
- [x] Cập nhật `CookieHelper.java`:
  - Thiết lập `httpOnly(true)` cho mọi cookie (`ct_session` và `ct_token`) để ngăn ngừa triệt để nguy cơ lộ token qua tấn công XSS.

### Bước 3: Tinh Chỉnh Tạo Tài Khoản Người Bệnh Phía Admin
- [x] Cập nhật `AdminCreateAccountRequest.java`: Bổ sung `birthYear` (Integer) và `gender` (String), duy trì constructor cũ để tương thích 100% với các bài test.
- [x] Cập nhật `AdminServiceImpl.java`: Tiếp nhận năm sinh và giới tính người dùng truyền vào, loại bỏ hardcode 1970.

### Bước 4: Đồng Bộ Cấu Hình Docker, Môi Trường & Seed Data
- [x] Cập nhật `docker-compose.yml`: Đồng bộ biến `VITE_API_URL` khớp với `frontend/Dockerfile` và `api.js`.
- [x] Thêm file `docker/mysql/init/08-seed-articles.sql` (từ `database/seed_articles.sql`) để cơ sở dữ liệu Docker khi khởi tạo có sẵn bài viết cẩm nang y học cổ truyền.
- [x] Đặt mặc định an toàn `SMS_ENABLED=false` trong file gốc `.env`.

### Bước 5: Dọn Dẹp & Chuẩn Hóa Các Controller Trùng Lặp
- [x] Di chuyển 10 controller trùng lặp/bypass repository sang `com.chuctupk.controller.compat`:
  - `AdminAppointmentController`, `ReceptionistController`, `AdminDashboardController`, `AdminMedicalRecordController`, `AdminUserController`, `AdminPatientController`, `DoctorWorkspaceController`, `BookingController`, `PatientProfileController`, `QueueController`.
- [x] Đánh dấu `@Deprecated` trên từng class controller cũ.
- [x] Nâng cấp `ArchitectureConventionTest.java` sử dụng `classLoader.getResources` quét toàn diện classpath, kiểm tra 100% controller chính thức tuân thủ Layered Architecture.

### Bước 6: Kiểm Tra & Xác Minh Toàn Diện
- [x] Chạy `mvnw test-compile` trên Backend (Thành công 100%, 222 source files).
- [x] Chạy 46 bài test Slice API (`V1*SliceTest`) trên Backend (46/46 passed).
- [x] Chạy `ArchitectureConventionTest` (2/2 passed).
- [x] Chạy `npm run build` trên Frontend (Thành công 100% trong 4.08s).
- [x] Cập nhật `findings.md`, `progress.md` và `task_plan.md`.
