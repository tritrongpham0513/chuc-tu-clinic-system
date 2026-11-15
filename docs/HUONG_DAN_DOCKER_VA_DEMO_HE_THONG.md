# 🏥 HƯỚNG DẪN KHỞI CHẠY BẰNG DOCKER & KỊCH BẢN DEMO TOÀN DIỆN
## HỆ THỐNG QUẢN LÝ PHÒNG KHÁM Y HỌC CỔ TRUYỀN CHÚC TỪ

> **Mục tiêu**: Cung cấp tài liệu chi tiết từ A–Z giúp bất kỳ ai (Giảng viên, Hội đồng chấm thi, Lập trình viên mới, Khách hàng) có thể tự khởi chạy hệ thống chỉ bằng Docker và thực hiện kịch bản demo sống động, trải nghiệm 100% chức năng của toàn bộ hệ thống.

---

## PHẦN 1: HƯỚNG DẪN KHỞI CHẠY HỆ THỐNG BẰNG DOCKER

Hệ thống Phòng Khám Chúc Từ được đóng gói thành **3 container độc lập** chạy qua Docker Compose:
1. **`chuctupk-mysql`**: MySQL 8.0 (InnoDB, UTF-8 chuẩn y tế), tự động nạp cấu trúc 27 bảng và dữ liệu mẫu (migrations `01` đến `10`).
2. **`chuctupk-backend`**: Spring Boot 4 / Java 17, tích hợp xác thực JWT qua HTTP-Only Cookie, phân bổ bác sĩ, bảo mật y tế và API Swagger.
3. **`chuctupk-frontend`**: React 18 + Vite được đóng gói trên nền Nginx Alpine (đóng vai trò Reverse Proxy chuyển tiếp `/api/**` an toàn, chống chặn Cookie).

---

### 1.1. Yêu cầu chuẩn bị
- Đã cài đặt **Docker Desktop** (hoặc Docker Engine trên Linux): [Tải Docker Desktop tại đây](https://www.docker.com/products/docker-desktop/).
- Đảm bảo Docker Desktop đang mở (biểu tượng cá voi màu xanh lá cây ở góc màn hình taskbar).
- Cổng cần sử dụng trên máy bạn:
  - Cổng **`3000`**: Giao diện Web (Frontend)
  - Cổng **`4000`**: Cổng API Backend
  - Cổng **`3307`**: Cổng MySQL nội bộ (được đổi sang 3307 để không bị xung đột nếu máy bạn đã cài sẵn MySQL 3306).

---

### 1.2. Khởi chạy bằng 1 lệnh duy nhất

Mở **PowerShell** hoặc **Terminal** tại thư mục gốc dự án (`d:\clinic-management-system`) và thực hiện:

#### Bước 1: Tạo file cấu hình môi trường Docker
```powershell
# Trên Windows PowerShell:
Copy-Item .env.docker.example .env
```
*(Hoặc sao chép file `.env.docker.example` thành `.env`)*

#### Bước 2: Chạy lệnh Docker Compose
```bash
docker compose up --build -d
```
*(Nếu máy dùng Docker Compose phiên bản cũ, dùng cú pháp: `docker-compose up --build -d`)*

Docker sẽ tự động:
1. Tải image MySQL 8.0 và nạp toàn bộ 10 file SQL trong `docker/mysql/init/`.
2. Biên dịch mã nguồn Backend Java 17 qua multi-stage build Maven.
3. Build bundle Frontend React Vite và đưa vào Nginx.
4. Thiết lập mạng ảo `chuctupk-network` để các dịch vụ tự kết nối với nhau.

---

### 1.3. Kiểm tra trạng thái hoạt động

Sau khoảng 1–2 phút, kiểm tra danh sách container:
```bash
docker compose ps
```
Kết quả hiển thị trạng thái `healthy` hoặc `running` như sau:
```text
NAME                IMAGE               COMMAND                  SERVICE             STATUS
chuctupk-backend    backend             "java -Djava.securit…"   backend             Up (healthy)   0.0.0.0:4000->4000/tcp
chuctupk-frontend   frontend            "/docker-entrypoint.…"   frontend            Up             0.0.0.0:3000->80/tcp
chuctupk-mysql      mysql:8.0           "docker-entrypoint.s…"   mysql               Up (healthy)   0.0.0.0:3307->3306/tcp
```

---

### 1.4. Bảng địa chỉ truy cập & Tài khoản thử nghiệm

| Phân hệ / Dịch vụ | Địa chỉ URL | Ghi chú |
| :--- | :--- | :--- |
| 🌐 **Cổng Bệnh nhân (Public Portal)** | [http://localhost:3000](http://localhost:3000) | Đặt lịch, tra cứu, xem cẩm nang sức khỏe |
| 📺 **Màn hình TV Hàng đợi công khai** | [http://localhost:3000/hang-doi](http://localhost:3000/hang-doi) | Đã che PII, ẩn ticketCode, cập nhật số ghế |
| 🏥 **Cổng Quản trị & Bàn làm việc Nội bộ**| [http://localhost:3000/noi-bo/](http://localhost:3000/noi-bo/) | Lễ tân, Bác sĩ, Quản trị viên (Admin) |
| ⚙️ **Backend Healthcheck API** | [http://localhost:4000/api/v1/health](http://localhost:4000/api/v1/health) | Kiểm tra trạng thái máy chủ |

#### Danh sách tài khoản mẫu có sẵn trong hệ thống:
| Vai trò | Tên đăng nhập | Mật khẩu | Phạm vi chức năng |
| :--- | :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `admin` | `MatKhauAdmin@2026` | Quản lý tài khoản, gán bác sĩ, biên tập bài viết, cấu hình |
| **Tiếp tân (Lễ tân)** | `letan` | `MatKhauAdmin@2026` | Điểm danh check-in, tiếp nhận tại quầy, mời bệnh nhân vào ghế |
| **Bác sĩ YHCT (Doctor)** | `bacsi` | `MatKhauAdmin@2026` | Khám bệnh, chẩn đoán Tứ chẩn/Bát cương, kê đơn thuốc YHCT |
| **Bệnh nhân (Patient)** | Số điện thoại cá nhân | OTP `123456` | Đăng ký nhanh qua SĐT (chế độ demo hỗ trợ mã OTP cố định) |

---

### 1.5. Các lệnh bảo trì thường dùng

- **Xem log thời gian thực của Backend**:
  ```bash
  docker compose logs -f backend
  ```
- **Tạm dừng hệ thống**:
  ```bash
  docker compose down
  ```
- **Xóa sạch và nạp lại Database từ đầu**:
  ```bash
  docker compose down -v
  docker compose up --build -d
  ```

---

## PHẦN 2: KỊCH BẢN DEMO TOÀN BỘ CHỨC NĂNG HỆ THỐNG

Quy trình demo được thiết kế theo đúng **vòng đời một ca khám chữa bệnh thực tế** tại Phòng Khám Chúc Từ: từ lúc người bệnh ở nhà đặt lịch, đến phòng khám được tiếp đón, vào phòng bác sĩ bắt mạch kê đơn, và ban quản trị quản lý giám sát.

---

### 🎬 KỊCH BẢN 1: BỆNH NHÂN ĐẶT LỊCH VÀ TRẢI NGHIỆM TRỰC TUYẾN
*(Thời lượng gợi ý: 3–4 phút)*

#### Bước 1.1: Trải nghiệm Giao diện Cao tuổi (Elderly-First UX)
1. Mở trình duyệt vào [http://localhost:3000](http://localhost:3000).
2. **Điểm nhấn cần giới thiệu**:
   - **Tông màu**: Nâu gỗ ấm (`#78350F`) tượng trưng cho bàn bắt mạch và tủ thuốc bắc, kết hợp xanh lá thảo mộc (`#15803D`) tượng trưng cho cây thuốc nam. Nền giấy xuyến chỉ (`#FDFBF7`) không chói mắt.
   - **Cỡ chữ**: To rõ ràng (tối thiểu `18px - 20px`), độ tương phản cao đạt chuẩn WCAG AAA.
   - **Nút khẩn cấp cố định (Sticky Bottom Bar)**: Ở cạnh dưới màn hình luôn có 2 nút nổi bật:
     - 📞 **Nút Hotline**: Gọi ngay bác sĩ trực (`tel:...`).
     - 📍 **Nút Chỉ đường**: Mở bản đồ Google Maps chỉ đường tới phòng khám tại Điện Biên Phủ, TP.HCM.

#### Bước 1.2: Xem Cẩm nang Sức khỏe Y học Cổ truyền
1. Bấm vào mục **Cẩm nang sức khỏe** trên thanh điều hướng.
2. Mở một bài viết mẫu (ví dụ: *"Phương pháp xoa bóp bấm huyệt giảm đau mỏi vai gáy cho người lớn tuổi"*).
3. Người dùng có thể tìm kiếm theo từ khóa hoặc chuyên mục.

#### Bước 1.3: Đăng ký & Đăng nhập Bệnh nhân
1. Bấm nút **Đăng nhập** ở góc phải màn hình.
2. Nhập số điện thoại bệnh nhân (ví dụ: `0988123456`).
3. Hệ thống gửi mã OTP. Trong môi trường kiểm thử/demo, nhập mã OTP `123456` và bấm **Xác nhận**.
4. Bệnh nhân đăng nhập thành công, Cookie bảo mật `ct_session` tự động kích hoạt.

#### Bước 1.4: Đặt lịch khám bệnh trực tuyến (Kiểm chứng SlotPolicy)
1. Bấm nút **Đặt lịch khám ngay**.
2. Chọn đối tượng khám: Chọn chính mình hoặc thêm hồ sơ người thân (hỗ trợ tối đa 5 hồ sơ).
3. Chọn ngày khám: Chọn một ngày trong tương lai (trong vòng 30 ngày).
4. Chọn khung giờ khám:
   - **Điểm kỹ thuật đắt giá**: Khung giờ được đánh giá tự động bởi `SlotPolicy` (Single Source of Truth). Khung giờ đã quá hạn hoặc đã đủ 5 người khám sẽ tự động bị ẩn hoặc đánh dấu `Hết chỗ`.
5. Chọn dịch vụ khám: Bắt mạch kê đơn, Châm cứu trị liệu, Xoa bóp bấm huyệt...
6. Nhập mô tả triệu chứng bệnh: *"Thường xuyên mất ngủ, đau nhức khớp gối khi trời lạnh"*.
7. Bấm **Xác nhận đặt lịch**: Hệ thống sinh ngay mã phiếu hẹn định dạng `PKCT-YYYYMMDD-XXXX` (Ví dụ: `PKCT-20261118-8421`).
8. Hệ thống tự động gán Bác sĩ trực ca thông qua thuật toán cân bằng tải `DoctorAssignmentService`.

#### Bước 1.5: Đổi lịch khám trực tuyến (Online Reschedule)
1. Vào mục **Lịch hẹn của tôi**.
2. Tìm phiếu hẹn vừa tạo (trạng thái `PENDING` - Chờ tiếp nhận).
3. Bấm nút **Đổi lịch khám**.
4. Chọn ngày và khung giờ mới còn trống chỗ.
5. Bấm **Xác nhận đổi lịch**:
   - Hệ thống tự động giải phóng ghế cũ, cấp ghế mới và cập nhật `reschedule_count = 1`.
   - Nếu bệnh nhân cố gắng đổi lần thứ 2, hệ thống sẽ lịch sự thông báo: *"Mỗi phiếu hẹn chỉ được đổi lịch tối đa 1 lần, cô chú vui lòng liên hệ hotline để được hỗ trợ"*.

#### Bước 1.6: Hủy lịch hẹn & Gợi ý đặt lại
1. Chọn phiếu hẹn và bấm **Hủy lịch khám**.
2. Nhập lý do (ví dụ: *"Bận việc gia đình"*).
3. Hệ thống cập nhật trạng thái `CANCELLED`, nhả ghế ngay lập tức và hiển thị đường dẫn tiện ích: `Đặt lại lịch khám mới` (`rebookUrl`).
4. Bệnh nhân có thể bấm vào link để đặt lại một khung giờ khác trong cùng ngày mà không bị chặn.

#### Bước 1.7: Màn hình TV Hàng đợi công khai (Queue Display)
1. Mở tab mới truy cập: [http://localhost:3000/hang-doi](http://localhost:3000/hang-doi).
2. **Quan sát tính bảo mật PII**:
   - Tên bệnh nhân được che chuẩn mực: *"Nguyễn Văn ***"*, tên mặc định *"Bệnh nhân 7242"* được cắt bỏ đuôi SĐT thành *"Bệnh nhân"*.
   - Mã phiếu (`ticketCode`) bị ẩn hoàn toàn (`null`) đối với người xem công khai để chống lộ danh tính.
   - Hiển thị trực quan trạng thái 5 ghế khám vật lý tại phòng khám.

---

### 🎬 KỊCH BẢN 2: TIẾP TÂN TIẾP ĐÓN & ĐIỀU PHỐI TẠI QUẦY
*(Thời lượng gợi ý: 2–3 phút)*

#### Bước 2.1: Đăng nhập Cổng Nội bộ Lễ tân
1. Truy cập [http://localhost:3000/noi-bo/](http://localhost:3000/noi-bo/).
2. Nhập tài khoản:
   - Tên đăng nhập: `letan`
   - Mật khẩu: `MatKhauAdmin@2026`
3. Đăng nhập thành công, giao diện chuyển đến **Bàn Tiếp Tân**.

#### Bước 2.2: Theo dõi Badges Realtime trên Dashboard
1. Nhìn lên góc thanh điều hướng và bảng điều khiển:
   - Thấy ngay các huy hiệu (badges) cập nhật tức thì: **Số ca đặt mới** (`newBookings`) và **Số ca hủy hôm nay** (`cancelledToday`).

#### Bước 2.3: Tiếp nhận ca khám tại quầy (Walk-in)
1. Có bệnh nhân lớn tuổi đến khám trực tiếp mà không biết dùng điện thoại đặt trước.
2. Lễ tân bấm nút **Tiếp nhận tại quầy**.
3. Điền thông tin:
   - Họ và tên: *"Cụ Hoàng Thị Mai"*
   - Năm sinh: `1952` (74 tuổi)
   - Số điện thoại: `0913987654`
   - Nội dung khám: *"Châm cứu phục hồi chức năng"*
4. Bấm **Lưu tiếp nhận**: Hệ thống tự động tạo mã bệnh nhân mới dạng `BN-YYYY-XXXX` và sinh phiếu khám gán ngay vào ca trực hiện tại.

#### Bước 2.4: Điểm danh (Check-in) bệnh nhân có hẹn
1. Tìm phiếu hẹn của bệnh nhân đặt trực tuyến ở Kịch bản 1.
2. Bấm nút **Điểm danh (Check-in)**.
3. Hệ thống kiểm tra giờ tiếp nhận (đọc động từ cấu hình phòng khám, ví dụ `08:00 - 16:30`) và chuyển trạng thái từ `PENDING` sang `CONFIRMED`.
4. Ghế khám được xác nhận an toàn, bệnh nhân được đưa vào hàng đợi chờ khám.

#### Bước 2.5: Mời bệnh nhân vào phòng khám
1. Trên màn hình quản lý ghế (5 ghế khám), bấm **Mời ca tiếp theo** (`Invite Next`).
2. Ca khám chuyển trạng thái sang `IN_PROGRESS` (Đang khám), hiển thị số ghế tương ứng trên màn hình TV sảnh chờ.

---

### 🎬 KỊCH BẢN 3: BÁC SĨ BẮT MẠCH, CHẨN ĐOÁN & KÊ ĐƠN
*(Thời lượng gợi ý: 2–3 phút)*

#### Bước 3.1: Đăng nhập Bàn Bác sĩ
1. Đăng xuất hoặc mở tab ẩn danh truy cập: [http://localhost:3000/noi-bo/](http://localhost:3000/noi-bo/).
2. Nhập tài khoản:
   - Tên đăng nhập: `bacsi`
   - Mật khẩu: `MatKhauAdmin@2026`
3. Chuyển vào **Bàn Khám Bệnh của Bác sĩ**.

#### Bước 3.2: Xem hàng đợi bệnh nhân thuộc ca của mình
1. Hệ thống tự động lọc danh sách theo `doctorId` của bác sĩ đang đăng nhập. Bác sĩ chỉ thấy các ca được phân công cho mình, không bị trùng lẫn với bác sĩ khác.
2. Thấy bệnh nhân *"Hoàng Thị Mai"* đang ở trạng thái `IN_PROGRESS` tại Ghế số 1.

#### Bước 3.3: Lập Bệnh án Y học Cổ truyền chuyên sâu
1. Bấm nút **Vào khám / Ghi bệnh án**.
2. Điền thông tin theo cấu trúc chuyên môn Đông y:
   - **Tứ Chẩn**:
     - *Vọng chẩn* (Nhìn): Sắc mặt nhợt, chất lưỡi bệu có dấu răng, rêu lưỡi trắng mỏng.
     - *Văn chẩn* (Nghe/Ngửi): Tiếng nói nhỏ, hơi thở ngắn.
     - *Vấn chẩn* (Hỏi): Đau lưng mỏi gối, sợ lạnh, tiểu đêm 3-4 lần.
     - *Thiết chẩn* (Sờ/Bắt mạch): Mạch trầm tế, vô lực.
   - **Bát Cương**: Lý chứng, Hư chứng, Hàn chứng, Âm chứng.
   - **Chẩn đoán YHCT**: Tỳ Thận dương hư / Chứng Tý.
   - **Chẩn đoán YHHĐ**: Thoái hóa cột sống thắt lưng kèm thoái hóa khớp gối.

#### Bước 3.4: Kê đơn thuốc & Chỉ định trị liệu
1. **Chỉ định điều trị**:
   - Châm cứu các huyệt: Thận du, Mệnh môn, Độc tỵ, Túc tam lý (cứu ấm 20 phút).
   - Xoa bóp bấm huyệt vùng cột sống thắt lưng.
2. **Kê đơn thuốc thang**:
   - Thang thuốc: *"Bát vị tri mẫu gia giảm"* (10 thang, sắc uống ngày 1 thang).
   - Lời dặn: Giữ ấm đôi chân, tập dưỡng sinh nhẹ nhàng buổi sáng, kiêng đồ ăn sống lạnh.

#### Bước 3.5: Hoàn tất ca khám
1. Bấm **Lưu & Hoàn thành ca khám**.
2. Trạng thái phiếu hẹn chuyển thành `COMPLETED`.
3. Bệnh án được lưu vĩnh viễn vào hồ sơ bệnh nhân, hỗ trợ tra cứu lịch sử điều trị ở các lần tái khám sau. Ghế khám số 1 được tự động nhả trống để tiếp nhận người tiếp theo.

---

### 🎬 KỊCH BẢN 4: QUẢN TRỊ VIÊN ĐIỀU HÀNH & BẢO MẬT HỆ THỐNG
*(Thời lượng gợi ý: 2 phút)*

#### Bước 4.1: Đăng nhập Cổng Admin
1. Đăng nhập bằng tài khoản:
   - Tên đăng nhập: `admin`
   - Mật khẩu: `MatKhauAdmin@2026`
2. Truy cập vào phân hệ **Quản trị phòng khám** (`/noi-bo/admin/`).

#### Bước 4.2: Điều chuyển & Gán lại Bác sĩ thủ công
1. Vào danh sách **Quản lý lịch hẹn**.
2. Tìm một ca khám và bấm biểu tượng **Đổi bác sĩ phụ trách**.
3. Chọn một bác sĩ khác trong danh sách đang trực.
4. Bấm **Cập nhật**: Hệ thống gọi API `PATCH /api/v1/admin/appointments/{id}/doctor`, xác thực lịch trực và chuyển giao ca khám ngay tức khắc.

#### Bước 4.3: Quản lý Bài viết Cẩm nang với Rich Editor
1. Vào mục **Quản lý bài viết** ➡️ Bấm **Viết bài mới**.
2. Thử nghiệm các tính năng cao cấp:
   - Soạn thảo trực quan với TipTap Rich Editor (in đậm, nghiêng, trích dẫn, bảng biểu).
   - Upload ảnh bìa: Hệ thống kiểm tra magic bytes thực tế của file ảnh (chống tấn công upload shell/script giả mạo ảnh).
   - Hỗ trợ Import nội dung tự động từ file Word `.docx`.
3. Bấm **Đăng bài**: Bài viết lập tức xuất hiện trên trang chủ phía bệnh nhân.

#### Bước 4.4: Bảo mật & Khóa tài khoản sau 5 lần sai mật khẩu
1. Thử đăng xuất và cố tình gõ sai mật khẩu tài khoản bất kỳ 5 lần liên tiếp.
2. Hệ thống trả về mã lỗi HTTP `423 LOCKED: "Tài khoản của bạn đã bị khóa tạm thời 15 phút do nhập sai mật khẩu quá 5 lần"`.
3. Bảo vệ an toàn tuyệt đối chống tấn công Brute-force vét cạn mật khẩu.

---

## PHẦN 3: TỔNG KẾT & CÂU HỎI THƯỜNG GẶP (Q&A)

### Q1: Làm thế nào để chứng minh hệ thống đang chạy với 5 ghế khám thực tế?
- **Trả lời**: Bạn có thể kiểm tra trực tiếp qua API:
  `GET http://localhost:4000/api/v1/meta`
  Sẽ thấy `slotCapacity: 5` và `seats: 5`. Đồng thời trên màn hình `/hang-doi` luôn hiển thị chính xác 5 ô ghế vật lý.

### Q2: Nếu tắt máy hoặc khởi động lại container thì dữ liệu có bị mất không?
- **Trả lời**: Không. Dữ liệu được lưu bền vững trên Docker Volume `chuctupk_mysql_data` và thư mục upload ảnh `chuctupk_uploads`. Khi chạy `docker compose down` rồi `docker compose up -d`, toàn bộ dữ liệu khám bệnh vẫn nguyên vẹn.

### Q3: Muốn kiểm tra tài liệu API kỹ thuật thì vào đâu?
- **Trả lời**: Truy cập tài liệu chuẩn OpenAPI / Swagger tại:
  `http://localhost:4000/swagger-ui.html` (được bảo vệ phân quyền trên môi trường Production).

---
*Tài liệu được phát triển và kiểm chứng bởi Đội ngũ Kỹ thuật Phòng Khám Chúc Từ.*
