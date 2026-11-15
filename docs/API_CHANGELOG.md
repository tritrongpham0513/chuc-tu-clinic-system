# 📋 API CHANGELOG - PHÒNG KHÁM CHÚC TỪ (V1)

Tài liệu ghi nhận các thay đổi, chuẩn hoá và loại bỏ endpoint cũ nhằm đảm bảo tính bảo mật, thống nhất kiến trúc và thu gọn bề mặt tấn công của hệ thống Phòng Khám Chúc Từ.

---

## 1. CÁC ENDPOINT ĐÃ XOÁ VÀ THAY THẾ

| Endpoint bị xoá | Phương thức | Lý do xoá | Endpoint chuẩn thay thế |
| :--- | :--- | :--- | :--- |
| `/api/v1/auth/quick-login` | `POST` | **Lỗ hổng bảo mật P0**: Backdoor đăng nhập trực tiếp bằng SĐT không cần OTP hoặc mật khẩu. | `POST /api/v1/auth/login` (yêu cầu gửi OTP) và `POST /api/v1/auth/login/verify` (xác thực OTP). |
| `/api/v1/queue/today` | `GET` | Endpoint thử nghiệm cũ làm lộ tên đầy đủ và thông tin bệnh nhân trong hàng đợi công khai. | `GET /api/v1/staff/queue` (Ẩn danh hóa họ tên dạng "Nguyễn V. An", ẩn SĐT, địa chỉ, tiền sử bệnh khi gọi công khai; đầy đủ khi nhân viên đăng nhập). |
| `/api/v1/admin/uploads` | `POST` | Trùng lặp với endpoint upload chuẩn. Đã chuẩn hoá frontend gọi sang `/api/v1/uploads`. | `POST /api/v1/uploads` (Yêu cầu quyền ADMIN/STAFF/DOCTOR, kiểm tra magic bytes ảnh JPEG/PNG/WEBP/GIF, trả về `{id, url}`). |
| `/api/v1/articles/**` | `GET/POST/...` | Trùng lặp hoàn toàn với phân hệ bài viết chuẩn (`/api/v1/posts` và `/api/v1/admin/posts`). | `GET /api/v1/posts/**` (Public) và `GET/POST/PUT/DELETE /api/v1/admin/posts/**` (Admin). |
| `/api/v1/booking/**` | `GET/POST` | Giao diện cũ thuộc package `compat`, không được frontend sử dụng. | `GET /api/v1/me/availability` và `POST /api/v1/me/appointments` (chuẩn Cổng bệnh nhân). |
| `/api/v1/patient-profiles/**` | `GET/POST` | Endpoint compat cũ. | `GET/POST/PATCH /api/v1/me/patients/**`. |
| `/api/v1/admin/appointments/**` | `GET/POST/...` | Endpoint compat cũ, nghiệp vụ tiếp nhận/khám nằm ở phân hệ Staff. | `GET/POST /api/v1/staff/appointments/**`. |
| `/api/v1/admin/medical-records/**` | `GET/POST/...` | Endpoint compat cũ, nghiệp vụ bệnh án thực hiện qua phân hệ Bác sĩ/Staff. | `GET/PUT /api/v1/staff/records/**`. |
| `/api/v1/admin/dashboard/stats` | `GET` | Endpoint compat cũ. | `GET /api/v1/admin/dashboard`. |
| `/api/v1/admin/users/**` | `GET/POST/...` | Endpoint compat cũ. Quản trị tài khoản đã được chuẩn hoá tại `AdminController`. | `GET/POST/PATCH /api/v1/admin/accounts/**`. |
| `/api/v1/doctor/**` | `GET/POST/...` | Endpoint compat cũ, mọi tác vụ phòng khám tập trung qua Staff API. | `/api/v1/staff/**`. |
| `/api/v1/receptionist/**` | `GET/POST/...` | Endpoint compat cũ, nghiệp vụ lễ tân tập trung qua Staff API. | `/api/v1/staff/**`. |

---

## 2. NÂNG CẤP BẢO MẬT & TÍNH NHẤT QUÁN NGHIỆP VỤ

1. **Khóa tài khoản tự động (Account Lockout)**:
   - Sai mật khẩu liên tiếp 5 lần sẽ khóa tài khoản trong 15 phút. Trả mã HTTP 423 `ACCOUNT_LOCKED`.
2. **Loại bỏ hiển thị mã OTP (devCode)**:
   - OTP tuyệt đối không trả về client ở production. Cờ `app.otp.expose-dev-code=false` mặc định ở production. Khi lỗi gửi SMS, trả HTTP 502/503 mà không lộ mã.
3. **Bắt buộc biến môi trường JWT_SECRET**:
   - Ứng dụng fail-fast khi khởi động nếu thiếu `JWT_SECRET` hoặc chuỗi secret không đạt độ dài 256-bit (32 bytes).
4. **Cơ chế Hibernate `ddl-auto: validate`**:
   - Mặc định ở môi trường chạy thật, phát hiện mọi lệch schema so với entities.
5. **Giới hạn tỷ lệ tra cứu lịch hẹn**:
   - Endpoint `GET /api/v1/appointments/{bookingCode}` (yêu cầu header `X-Phone`) được áp dụng rate limit tối đa 20 lượt/phút trên mỗi cặp IP + BookingCode để chống brute-force số điện thoại.
6. **Xác thực định dạng ảnh bằng Magic Bytes**:
   - `POST /api/v1/uploads` kiểm tra trực tiếp chuỗi byte đầu của tệp (JPEG: `FF D8 FF`, PNG: `89 50 4E 47`, WEBP: `RIFF...WEBP`, GIF: `GIF8`). Chặn đứng hành vi tải lên file thực thi giả mạo đuôi ảnh.
7. **Thống nhất cấu hình phòng khám (Clinic Setting SSOT)**:
   - Toàn bộ tham số vận hành (`slot_capacity`, `seats`, `user_max_days`, `counter_max_days`, `max_profiles`, `booking_advance_days`, `booking_min_hours`, `reception_open`, `reception_close`, `no_show_cancel_at`) được quản lý tập trung qua `ClinicSettingService` với bộ đệm cache in-memory 5 phút.
