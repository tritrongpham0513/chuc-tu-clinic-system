# Website Phòng khám Chúc Từ — ReactJS (nhóm giao diện 1 · 2 · 3)

Chuyển từ bản thiết kế "Website Phòng khám Chúc Từ" sang React (Vite + React Router).
Giữ đúng màu, font, khoảng cách, chữ và bố cục của thiết kế; đã chụp màn hình so với bản gốc
(cả 1360px và 390px) — lệch dưới 0,04% điểm ảnh, chỉ là khử răng cưa chữ.

## Chạy

```bash
npm install
npm run dev        # http://localhost:5173
npm run build      # xuất ra thư mục dist/
```

## Phạm vi

| Nhóm thiết kế | Màn hình | File React |
|---|---|---|
| 1 · Khách | Trang chủ, Giới thiệu, Dịch vụ, Hướng dẫn, Liên hệ, Danh sách bài viết, Chi tiết bài viết | `TrangChu`, `GioiThieu`, `DichVu`, `HuongDan`, `LienHe`, `BaiVietDanhSach`, `BaiVietChiTiet` |
| 2 · Đăng nhập & đăng ký | Đăng nhập, Đăng nhập máy mới (OTP), Đăng ký, Đăng ký nhập OTP, hai bản đăng nhập khi đặt lịch | `DangNhap`, `DangNhapMayMoi`, `DangKyTaiKhoan` |
| 3 · Người dùng (đã đăng nhập) | 7 trang công khai như nhóm 1 nhưng có menu tài khoản | dùng chung 7 file của nhóm 1 |

Các trang bản "Người dùng" **dùng chung đường dẫn** với bản Khách: layout tự đổi header/footer
và vài dòng chữ / link theo trạng thái đăng nhập (xem `PublicLayout`, `useAuth`).

Các màn hình nhóm 4 trở đi (đặt lịch, tài khoản, phiếu khám, tiếp tân, admin, bản điện thoại riêng)
**chưa làm**; link tới chúng đi vào trang giữ chỗ `SapRaMat`.

## Đường dẫn

| Đường dẫn | Màn hình thiết kế |
|---|---|
| `/` `/gioi-thieu` `/dich-vu` `/huong-dan` `/lien-he` | Trang chủ, Giới thiệu, Dịch vụ, Hướng dẫn, Liên hệ |
| `/tin-tuc` `/tin-tuc/:slug` | Danh sách / Chi tiết bài viết |
| `/dang-nhap` | Đăng nhập |
| `/dang-nhap/may-moi` | Đăng nhập máy mới (bấm Đăng nhập thì mở hộp OTP) |
| `/dang-nhap/may-moi/otp` | Như trên, mở sẵn hộp OTP (đúng ảnh thiết kế) |
| `/dang-ky` · `/dang-ky/otp` | Đăng ký · Đăng ký nhập OTP |
| `/dat-lich/dang-nhap` (+ `/may-moi`, `/may-moi/otp`) | Đăng nhập khi bấm "Đặt lịch khám" — đăng nhập xong đi tiếp `/dat-lich` |

## Cấu trúc

```
src/
  pages/        mỗi file = một màn hình thiết kế
  components/   Header/Footer (Guest, User, Auth), TopBar, Icon, các Layout
  context/      AuthContext — đăng nhập giả lập, lưu localStorage
  data/         bài viết mẫu, danh sách tỉnh / phường xã
  lib/          kiểm tra số điện thoại, menu điều hướng, css()
  styles/       global.css — CSS gộp từ thiết kế (gồm cả responsive)
```

## Lưu ý

- **Style inline được giữ nguyên** như thiết kế: file `global.css` có các luật responsive dựa
  vào chuỗi style (vd. `[style*="font-size: 48px"]`), nên đừng đổi sang CSS khác khi chưa sửa
  khối `RESP-v1` trong đó.
- Đăng nhập chỉ là giả lập (tên cố định "Nguyễn Thị Hoa"), mã OTP chưa kiểm tra thật. Khi có API,
  sửa trong `context/AuthContext.jsx` và các hàm `onSubmit` / `login(...)` ở trang đăng nhập, đăng ký.
- Ảnh mặt tiền là ảnh tạm của thiết kế (`src/assets/mat-tien.jpg`).
- Font Be Vietnam Pro và Noto Serif tải từ Google Fonts (link trong `index.html`).
