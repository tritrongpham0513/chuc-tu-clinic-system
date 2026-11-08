import { Route, Routes } from 'react-router-dom'
import ScrollManager from './components/ScrollManager'
import RequireAuth from './components/RequireAuth'

// Nhóm 1 + 3: Trang công khai
import TrangChu from './pages/TrangChu'
import GioiThieu from './pages/GioiThieu'
import DichVu from './pages/DichVu'
import HuongDan from './pages/HuongDan'
import LienHe from './pages/LienHe'
import BaiVietDanhSach from './pages/BaiVietDanhSach'
import BaiVietChiTiet from './pages/BaiVietChiTiet'

// Nhóm 2: Đăng nhập & Đăng ký bệnh nhân
import DangNhap from './pages/DangNhap'
import DangNhapMayMoi from './pages/DangNhapMayMoi'
import DangKyTaiKhoan from './pages/DangKyTaiKhoan'

// Phân hệ Bệnh nhân (Elderly-First UX)
import HoSoKhamGiaDinh from './pages/HoSoKhamGiaDinh'
import DatLichKham from './pages/DatLichKham'
import PhieuKhamCuaToi from './pages/PhieuKhamCuaToi'
import LichSuKham from './pages/LichSuKham'

// Phân hệ Quản trị CMS & Điều phối
import AdminLogin from './pages/admin/AdminLogin'
import AdminDashboard from './pages/admin/AdminDashboard'
import AdminUsers from './pages/admin/AdminUsers'
import StaffReceptionDesk from './pages/admin/StaffReceptionDesk'
import DoctorDesk from './pages/admin/DoctorDesk'
import AdminAppointments from './pages/admin/AdminAppointments'
import AdminPatients from './pages/admin/AdminPatients'
import AdminArticles from './pages/admin/AdminArticles'

// Đăng nhập nhân sự độc lập
import TiepTanLogin from './pages/receptionist/TiepTanLogin'
import BacSiLogin from './pages/doctor/BacSiLogin'
import NoiBoLogin from './pages/staff/NoiBoLogin'

// Màn hình TV phòng chờ
import TvPhongCho from './pages/TvPhongCho'

import SapRaMat from './pages/SapRaMat'

export default function App() {
  return (
    <>
      <ScrollManager />
      <Routes>
        {/* ─── 1. TRANG CÔNG KHAI ─── */}
        <Route path="/" element={<TrangChu />} />
        <Route path="/gioi-thieu" element={<GioiThieu />} />
        <Route path="/dich-vu" element={<DichVu />} />
        <Route path="/huong-dan" element={<HuongDan />} />
        <Route path="/lien-he" element={<LienHe />} />
        <Route path="/tin-tuc" element={<BaiVietDanhSach />} />
        <Route path="/tin-tuc/:slug" element={<BaiVietChiTiet />} />

        {/* ─── 2. ĐĂNG NHẬP / ĐĂNG KÝ BỆNH NHÂN ─── */}
        <Route path="/dang-nhap" element={<DangNhap />} />
        <Route path="/dang-nhap/may-moi" element={<DangNhapMayMoi />} />
        <Route path="/dang-nhap/may-moi/otp" element={<DangNhapMayMoi otpOpen />} />
        <Route path="/dang-ky" element={<DangKyTaiKhoan />} />
        <Route path="/dang-ky/otp" element={<DangKyTaiKhoan otpSent />} />

        {/* Đăng nhập khi bấm Đặt lịch */}
        <Route path="/dat-lich/dang-nhap" element={<DangNhap booking />} />
        <Route path="/dat-lich/dang-nhap/may-moi" element={<DangNhapMayMoi booking />} />
        <Route path="/dat-lich/dang-nhap/may-moi/otp" element={<DangNhapMayMoi booking otpOpen />} />

        {/* ─── 3. PHÂN HỆ BỆNH NHÂN ─── */}
        <Route path="/dat-lich" element={<RequireAuth to="/dat-lich/dang-nhap"><DatLichKham /></RequireAuth>} />
        <Route path="/tai-khoan" element={<RequireAuth><HoSoKhamGiaDinh /></RequireAuth>} />
        <Route path="/tai-khoan/ho-so-kham-benh" element={<RequireAuth><HoSoKhamGiaDinh /></RequireAuth>} />
        <Route path="/phieu-kham" element={<RequireAuth><PhieuKhamCuaToi /></RequireAuth>} />
        <Route path="/lich-su-kham" element={<RequireAuth><LichSuKham /></RequireAuth>} />

        {/* ─── 4. PHÂN HỆ ĐỘC LẬP: BÀN TIẾP TÂN & BÀN BÁC SĨ ─── */}
        <Route path="/tiep-tan/dang-nhap" element={<TiepTanLogin />} />
        <Route path="/bac-si/dang-nhap" element={<BacSiLogin />} />
        <Route path="/noi-bo/dang-nhap" element={<NoiBoLogin />} />
        <Route path="/tiep-tan" element={<StaffReceptionDesk />} />
        <Route path="/bac-si" element={<DoctorDesk />} />

        {/* ─── 5. PHÂN HỆ QUẢN TRỊ ADMIN CMS (ĐỘC QUYỀN ADMIN) ─── */}
        <Route path="/admin/login" element={<AdminLogin />} />
        <Route path="/admin" element={<AdminDashboard />} />
        <Route path="/admin/users" element={<AdminUsers />} />
        <Route path="/admin/tiep-tan" element={<StaffReceptionDesk />} />
        <Route path="/admin/bac-si" element={<DoctorDesk />} />
        <Route path="/admin/appointments" element={<AdminAppointments />} />
        <Route path="/admin/patients" element={<AdminPatients />} />
        <Route path="/admin/articles" element={<AdminArticles />} />

        {/* ─── 5. MÀN HÌNH TV PHÒNG CHỜ (FULL SCREEN) ─── */}
        <Route path="/tv-phong-cho" element={<TvPhongCho />} />

        {/* 404 */}
        <Route path="*" element={<SapRaMat title="Không tìm thấy trang" />} />
      </Routes>
    </>
  )
}
