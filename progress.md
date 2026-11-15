# Nhật ký Tiến độ Đánh giá & Sửa Lỗi Hệ Thống (Progress Log)

- **2026-11-14 14:33 - 15:04**: Khởi tạo kế hoạch, kiểm tra toàn diện hệ thống, xử lý cookie httpOnly, múi giờ Việt Nam, chuẩn hóa API.
- **2026-11-15 14:00 - 15:15**: 
  - **Phần A (P0) & B (P1)**: Xoá backdoor quick-login, sửa URL eSMS `/`, che dữ liệu hàng đợi công khai, JWT fail-fast (bắt buộc Base64 >= 32 bytes), ddl-auto: validate (migration 007), khoá tài khoản 5 lần sai (423 ACCOUNT_LOCKED), cookie ct_session/ct_refresh, tắt swagger prod, đóng endpoint tạo lịch công khai, magic bytes upload file.
  - **Phần C (P2)**: ClinicSettingService cache 5 phút, xoá 9 compat controller và ArticleController thừa, .gitignore uploads/.
  - **Phần D & E**: Cập nhật tài liệu README, smoke-test.http, API_CHANGELOG, tạo 5 bộ test IT mới (`AuthSecurityIT`, `QueuePrivacyIT`, `CapacityConsistencyIT`, `UploadIT`, `BootIT`).
  - **Kiểm chứng**: 111/111 backend tests PASS (BUILD SUCCESS), Frontend npm run build PASS (3.93s). Đã commit vào git (`4e3df03`, `3d2949e`, `64b9bdb`).

---

### TIẾN ĐỘ ĐỢT SỬA LỖI MỚI (Phiên chiều 15/11/2026 - Tạm dừng lúc 18:38, tiếp tục tối nay)

#### 1. Hạng mục đã hoàn thành:
- **A3. Sửa REGRESSION job no-show (huỷ bệnh nhân đã check-in)**:
  - `ClinicScheduledTasks.java`: Chỉ lọc `PENDING`; chỉ huỷ khi `slot.getEndTime() < now` (hoặc slot null/ended) sau mốc `no_show_cancel_at`; chuyển `status = NO_SHOW`, `cancelReason = "Không đến khám"`; nhả chỗ qua `appointmentSeatRepository.deleteByAppointmentId(id)`.
  - Tuyệt đối không đụng `CONFIRMED` (đã check-in), `IN_PROGRESS`, `COMPLETED`.
  - Thêm TODO (B6) gửi notification.
  - `ClinicScheduledTasksTest.java`: Đã kiểm chứng 5 trường hợp (PENDING đã qua -> NO_SHOW + nhả ghế; PENDING chưa qua -> giữ nguyên; CONFIRMED -> giữ nguyên; IN_PROGRESS -> giữ nguyên; chạy 2 lần -> idempotent). **Kết quả: 6/6 tests PASS (0.662s)**.

- **A1. Chuẩn hoá "khung giờ có được đặt hay không" (SlotPolicy)**:
  - Tạo `service/SlotPolicy.java` chuẩn hoá quy tắc theo đúng thứ tự ưu tiên:
    1. `date < today` -> `PAST`
    2. `date > today + maxAdvanceDays` -> `TOO_FAR` (`user_max_days`, fallback `booking_advance_days`)
    3. `slot.start < receptionOpen` HOẶC `slot.end > receptionClose` -> `OUTSIDE_RECEPTION`
    4. `date == today` và `slot.start < now + minHours*60p` -> `TOO_LATE` (PATIENT_PORTAL dùng booking_min_hours; COUNTER dùng 0)
    5. `remainingSeats <= 0` -> `FULL` (`SLOT_FULL`)
    6. Thoả mãn -> `OK`
  - Đã viết unit test `SlotPolicyTest.java`: Đã test 4 trường hợp đặc thù với `Clock` cố định -> **4/4 tests PASS (0.594s)**.
  - Đã tích hợp `SlotPolicy` vào:
    - `PatientPortalServiceImpl.java`: `getAvailability` (trả `available=false` + `reason`) và `bookAppointment` (trả 422 TOO_LATE, 422 OUTSIDE_RECEPTION, 400 TOO_FAR, 400 PAST, 422 SLOT_FULL).
    - `TimeSlotServiceImpl.java`: dùng `SlotPolicy` và `clinicSettingService.getSlotCapacity()`, chuẩn hoá đồng bộ với `getAvailability` (giải quyết luôn B2).
    - `StaffServiceImpl.java`: `createCounterAppointment` dùng `SlotPolicy` kênh `COUNTER`.
    - `AppointmentServiceImpl.java`: dùng `SlotPolicy`.
  - Đã viết sẵn test tích hợp `backend/src/test/java/com/chuctupk/it/SlotPolicyIT.java`.

---

#### 2. Các hạng mục tiếp theo đã hoàn tất (Phiên 15-16/11/2026):
- **A4. Profile `prod` an toàn**:
  - `docker-compose.yml`, `backend/Dockerfile`, `.env.example`: set `SPRING_PROFILES_ACTIVE=prod`.
  - `application.yml`: `app.otp.expose-dev-code=false`, `app.cookie.secure=true`.
  - `SecurityConfig.java`: gate `/v3/api-docs/**`, `/swagger-ui/**` yêu cầu `hasRole("ADMIN")`.
  - `docs/RENDER_DEPLOY_GUIDE.md`: xoá dev JWT secret, thay bằng lệnh `openssl rand -base64 32`.
  - Đã kiểm chứng qua `BootIT.java` (3/3 tests PASS).

- **A2. Gán bác sĩ (DoctorAssignmentService)**:
  - Tạo `DoctorAssignmentService.pickDoctor(date, slot)`: kiểm tra ngày nghỉ `DoctorDayOff`, cân bằng tải theo số ca đã nhận trong slot, tie-break ID ASC.
  - Tự động gán khi đặt lịch web (`PatientPortalServiceImpl`), tạo lịch quầy và đổi lịch (`StaffServiceImpl`).
  - Endpoint gán tay: `PATCH /api/v1/admin/appointments/{id}/doctor` và `PATCH /api/v1/staff/appointments/{id}/doctor`.
  - Hàng đợi bác sĩ: `GET /api/v1/staff/queue?doctorId={id}`.
  - Xoá `DEFAULT_DOCTOR_NAMES`: ghế trống `doctor=null`, có bệnh nhân chưa gán thì "Chưa phân bác sĩ".
  - Đã kiểm chứng qua `DoctorAssignmentIT.java` (3/3 tests PASS).

- **A5. Mô hình ghế duy nhất (Single Seat Model)**:
  - Thay hardcode 5 trong `StaffServiceImpl` bằng `clinicSettingService.getSeats()`.
  - Migration 008 (`database/008_reschedule_and_slot_capacity.sql` và `docker/mysql/init/10-reschedule-and-slot-capacity.sql`):
    + `clinic_settings.slot_capacity = 5`.
    + Thêm cột `reschedule_count INT NOT NULL DEFAULT 0` vào bảng `appointments`.
    + Cập nhật `time_slots.max_patients = 5` cho các slot > 5.

- **B1. ServiceType Whitelist & Default**:
  - `ContractMapper.toDbServiceType`: mặc định `KHAM_BENH`, whitelist 6 giá trị (`KHAM_BENH`, `CHAM_CUU`, `XOA_BOP_BAM_HUYET`, `BOC_THUOC`, `VAT_LY_TRI_LIEU`, `CHUA_RO`), giá trị lạ trả 400.
  - Đã kiểm chứng qua `ContractMapperTest.java` (3/3 tests PASS).

- **B2. Nhất quán `/time-slots` và `/me/availability`**:
  - Cả 2 endpoint đồng nhất tính theo `slot_capacity` = 5 và số chỗ đã đặt = `appointment_seats` (fallback appointments NOT CANCELLED).
  - Đã kiểm chứng qua `CapacityConsistencyIT.java` (3/3 tests PASS).

- **B3. Giờ tiếp nhận động trong checkin**:
  - `StaffServiceImpl.checkin`: dùng `clinicSettingService.getReceptionOpen()` & `getReceptionClose()`, và `getPriorityEarlyMinutes()`.

- **B4. Che tên màn hình TV công khai**:
  - `PatientDto.maskFullName`: loại bỏ đuôi số điện thoại (vd: "Bệnh nhân 7242" -> "Bệnh nhân").
  - `StaffServiceImpl.getQueueState`: ẩn `ticketCode: null` khi người xem chưa đăng nhập.

- **B5. Đổi giờ lịch hẹn (Online Reschedule)**:
  - Endpoint: `POST /api/v1/me/appointments/{id}/reschedule`.
  - Ràng buộc: chỉ `PENDING`, trước `booking_min_hours`, tối đa 1 lần (`reschedule_count < 1`), chuyển `appointment_seats`, gán bác sĩ mới qua `DoctorAssignmentService`.
  - Đã kiểm chứng qua `V1MeControllerSliceTest.java` (7/7 tests PASS).

- **B6. NotificationService & Dashboard Badges**:
  - Tạo `NotificationService`: gửi SMS xác nhận đặt lịch, gửi SMS thông báo huỷ tự động do no-show.
  - Thêm `@Scheduled` nhắc lịch 2h trước giờ khám.
  - Thêm `GET /api/v1/staff/dashboard/badges` trả `{newBookings, cancelledToday}`.

- **B7. Forwarded Headers Strategy & Timed Expiration Rate Limiter**:
  - `server.forward-headers-strategy: framework` trong `application.yml` và `application.example.yml`.
  - Rate limiter tra cứu lịch hẹn dùng dọn dẹp theo thời gian hết hạn (`removeIf`), không xoá trắng toàn bộ.

- **B8. Gợi ý đặt lại khi huỷ & cho phép đặt lại cùng ngày**:
  - `cancelAppointment`: trả về `rebookUrl: "/dat-lich?rebookFrom={id}"`.
  - `PatientPortalServiceImpl.bookAppointment`: cho phép đặt lại trên cùng ngày nếu các lịch trước đã `CANCELLED` (`hasActiveAppointmentOnDate`).

- **C. Cập nhật Tài liệu & Hợp đồng**:
  - Cập nhật `README.md` (hướng dẫn migration 008, ddl-auto: validate).
  - Cập nhật `docs/API_CHANGELOG.md` ghi nhận toàn bộ API mới và thay đổi.
  - Cập nhật `docs/smoke-test.http` bổ sung request test reschedule, badges, assign doctor.

---

### KẾT QUẢ KIỂM CHỨNG TỔNG THỂ:
1. **Backend Test Suite**: **131/131 tests PASS (0 failures, 0 errors, 0 skipped) - BUILD SUCCESS trong 32.5s**.
2. **Frontend Build**: `npm run build` thành công trong 4.31s với 0 errors. Không có bất kỳ thay đổi nào vi phạm giao diện JSX/TSX/CSS.

