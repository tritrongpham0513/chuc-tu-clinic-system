# NHẬT KÝ THAY ĐỔI & CHUẨN HOÁ KIẾN TRÚC BACKEND (API V1)

> **Dự án**: Hệ thống Quản lý và Tiếp nhận Bệnh nhân - Phòng Khám Chúc Từ  
> **Phiên bản hoàn tất**: API v1 (Clean Architecture & Elderly-First UX)  
> **Cổng mặc định Backend**: `4000`

---

## 1. TỔNG QUAN TÁI CẤU TRÚC (OVERVIEW)

Hệ thống Backend đã được đồng bộ và nâng cấp toàn diện nhằm kết nối mượt mà với ứng dụng Frontend (React 18 + Vite), loại bỏ hoàn toàn các lỗi màn hình trắng, xung đột đường dẫn hoặc mã lỗi không khớp.

Toàn bộ quy trình thực hiện theo 9 giai đoạn (Phase G0 → G8) nghiêm ngặt:

1. **Phase G0 (Nền tảng & Cấu hình)**:
   - Thống nhất cổng mặc định Backend về `4000` (`application.yml`, Docker, Nginx).
   - Chuẩn hoá tiền tố API về `/api/v1` với lớp bọc dữ liệu `ApiResponse<T>`:
     `{ success, statusCode, code, message, data, fields, retryAfter, timestamp }`.
   - Xây dựng lớp chống tham nhũng (Anti-Corruption Layer) `ContractMapper.java`.
   - Triển khai endpoints hệ thống: `GET /api/v1/health` và `GET /api/v1/meta` (14 trường thông tin phòng khám).
   - Thêm các migration MySQL: `003_contract_alignment.sql`, `004_seed_slots_60min.sql`, `005_v1_support.sql`.

2. **Phase G1 (Xác thực & OTP - Auth API)**:
   - Triển khai 13 endpoints xác thực dưới `/api/v1/auth/**`:
     - Gửi/xác thực OTP đăng nhập bệnh nhân (hỗ trợ mã dev `123456`).
     - Đăng nhập nhân viên/bác sĩ/quản trị viên (email/username + password).
     - Quên mật khẩu, đổi mật khẩu và cấp lại mật khẩu tạm thời.
     - Kiểm tra trạng thái đăng nhập `GET /api/v1/auth/me` (trả về 200 OK với `user: null` khi chưa đăng nhập, không ném 401).
     - Quản lý phiên làm việc JWT và Refresh Token qua HttpOnly Cookie.

3. **Phase G2 (Cổng Bệnh nhân - Patient & Me Portal)**:
   - Quản lý tối đa 10 hồ sơ người thân (`/api/v1/me/profiles/**`), bảo vệ hồ sơ chính chủ `BAN_THAN` không thể bị xóa (`CANNOT_DELETE_PRIMARY`).
   - Lấy khung giờ khám trống 60 phút (`GET /api/v1/me/availability`).
   - Đặt lịch khám có khóa chống gửi trùng lặp (`IdempotencyKey`) và chống trùng khung giờ (`SLOT_FULL`).
   - Lịch sử phiếu khám và kết quả khám của cá nhân.

4. **Phase G3 (Nội bộ: Lịch khám, Đặt hộ, Check-in & Hàng đợi)**:
   - Lịch khám trong ngày có tóm tắt số liệu 6 trạng thái.
   - Tiếp nhận bệnh nhân (Check-in) cấp số thứ tự hàng đợi (`queueNo: 001..999`) và chỉ định ghế khám (1..5).
   - Màn hình hàng đợi TV chờ (`/api/v1/staff/queue`) với thông tin hiển thị rõ ràng cho người cao tuổi.
   - Chức năng đặt hẹn hộ tại quầy tiếp tân, dời lịch khám, hủy lịch.

5. **Phase G4 (Hồ sơ bệnh nhân & Kết quả khám bệnh - Records)**:
   - Quản lý hồ sơ bệnh nhân nội bộ, cảnh báo trùng số điện thoại (`DUPLICATE_PHONE`).
   - Bổ sung trường `lastVisitDate` hiển thị ngày khám gần nhất.
   - Lập và chỉnh sửa phiếu kết quả khám bệnh (`StaffRecordController`) với chính sách giới hạn sửa trong vòng 24 giờ (`RECORD_EXPIRED`).
   - Tự động hoàn tất lịch hẹn (`COMPLETED`) khi lưu kết quả khám.

6. **Phase G5 (Quản trị toàn quyền - Admin CMS)**:
   - Dashboard Admin với đầy đủ 7 khối số liệu thời gian thực.
   - Quản lý tài khoản: tạo mới, sửa, phân quyền, cấp mật khẩu tạm, khoá/mở khoá (chặn tự khoá chính mình `CANNOT_LOCK_SELF`).
   - Khoá và mở khoá hồ sơ bệnh nhân (`LockPatientDialog`).
   - Quản lý bài viết CMS: tự động làm sạch mã độc HTML (Anti-XSS), quy tắc chỉ 1 bài duy nhất được ghim (`pinned`), tách riêng bài nổi bật (`featured`).
   - Media Upload: kiểm soát dung lượng ≤ 3MB, giới hạn danh sách MIME ảnh hợp lệ (`image/jpeg`, `image/png`, `image/webp`, `image/gif`).

7. **Phase G6 (Tác vụ nền & Tính nhất quán)**:
   - Tác vụ tự động huỷ lịch không đến (No-show) vào lúc 17:00 hàng ngày (`Asia/Ho_Chi_Minh`) với lý do `"Không đến khám (hệ thống tự huỷ lúc 17:00)"`.
   - Tác vụ dọn dẹp mã OTP và Refresh Token hết hạn vào 02:00 sáng.
   - Tiêm `Clock` hỗ trợ kiểm thử giả lập thời gian chính xác.

8. **Phase G7 (Kiểm thử & Bản đặc tả)**:
   - 51/51 Unit, Slice & Architecture tests pass 100%.
   - Tạo bộ kịch bản kiểm thử HTTP nhanh: `docs/smoke-test.http`.
   - Tạo bản đặc tả OpenAPI 3.0: `docs/openapi-v1.yaml`.

9. **Phase G8 (Dọn dẹp mã cũ)**:
   - Loại bỏ hoàn toàn package `com.chuctupk.controller.compat` không còn sử dụng.
   - Thống nhất các endpoints qua tiền tố chính thức `/api/v1/**` và hỗ trợ ánh xạ kép `/api/**` trên các V1 Controllers.

---

## 2. BẢNG MÃ LỖI CHUẨN HOÁ (ERROR CODES)

Mọi phản hồi lỗi đều tuân thủ mã định danh SCREAMING_SNAKE cùng thông điệp tiếng Việt thân thiện với người cao tuổi:

| Mã lỗi (`code`) | HTTP Status | Thông điệp tiếng Việt |
| :--- | :--- | :--- |
| `SESSION_CHANGED` | 409 Conflict | Phiên đăng nhập đã thay đổi ở thẻ khác. Trang sẽ tự động tải lại |
| `STALE` | 409 Conflict | Dữ liệu đã được người khác cập nhật trước đó. Vui lòng tải lại trang |
| `SLOT_FULL` | 409 Conflict | Khung giờ này đã đủ bệnh nhân. Cô chú chọn khung khác giúp phòng khám nhé |
| `CANNOT_LOCK_SELF`| 400 Bad Request | Quản trị viên không thể tự khóa tài khoản của chính mình |
| `DUPLICATE_PHONE` | 409 Conflict | Số điện thoại này đã có hồ sơ trong hệ thống |
| `CANNOT_DELETE_PRIMARY` | 400 Bad Request | Không thể xóa hồ sơ chính chủ (Bản thân) |
| `RECORD_EXPIRED` | 403 Forbidden | Đã quá thời hạn 24 giờ cho phép chỉnh sửa phiếu kết quả |
| `ROOM_FULL` | 400 Bad Request | Tất cả các ghế khám hiện đều đang có bệnh nhân |
| `WRONG_DAY` | 400 Bad Request | Chỉ có thể tiếp nhận cho lịch khám trong ngày hôm nay |
| `CLOSED` | 400 Bad Request | Phòng khám đã hết giờ tiếp nhận bệnh nhân trong ngày |
| `ACCOUNT_LOCKED` | 403 Forbidden | Tài khoản đang bị tạm khóa. Vui lòng liên hệ quản trị viên |

