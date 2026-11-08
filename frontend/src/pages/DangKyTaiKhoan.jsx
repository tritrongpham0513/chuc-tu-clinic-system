import { Fragment, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Icon from '../components/Icon'
import { facade } from '../assets'
import AuthLayout from '../components/AuthLayout'
import { useAuth } from '../context/AuthContext'
import authApi from '../api/auth'
import { cleanPhone, isValidPhone, phoneMessage, normalizePhone } from '../lib/phone'
import { DIA_GIOI } from '../data/diaGioi'

// Số ngày tối đa của một tháng (có xét năm nhuận khi đã chọn năm)
function soNgayToiDa(thang, nam) {
  if (thang === '') return 31
  const t = +thang
  if ([4, 6, 9, 11].includes(t)) return 30
  if (t !== 2) return 31
  const nhuan = nam !== '' && ((+nam % 4 === 0 && +nam % 100 !== 0) || +nam % 400 === 0)
  return nam !== '' && !nhuan ? 28 : 29
}

export default function DangKyTaiKhoan({ otpSent: otpSentInit = false }) {
  const { register } = useAuth()
  const navigate = useNavigate()

  // Form states
  const [fullName, setFullName] = useState('')
  const [phone, setPhone] = useState('')
  const [tinh, setTinh] = useState('hcm')
  const [xa, setXa] = useState('')
  const [diaChiChiTiet, setDiaChiChiTiet] = useState('')
  const [ngay0, setNgay] = useState('')
  const [thang, setThang] = useState('')
  const [nam, setNam] = useState('1960')
  const [gioiTinh, setGioiTinh] = useState('FEMALE') // 'MALE' | 'FEMALE'
  const [dongY, setDongY] = useState(true)

  // Validation & status
  const [tried, setTried] = useState(false)
  const [formError, setFormError] = useState('')
  const [otpSent, setOtpSent] = useState(otpSentInit)
  const [otpCode, setOtpCode] = useState('')
  const [otpError, setOtpError] = useState('')
  const [isSendingOtp, setIsSendingOtp] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [countdown, setCountdown] = useState(60)

  // Số điện thoại
  const digits = cleanPhone(phone)
  const phoneNormalized = normalizePhone(phone)
  const okPhone = isValidPhone(phone)
  const errPhone = tried && !okPhone
  const phoneInvalid = errPhone ? 'true' : 'false'
  const phoneMsg = phoneMessage(phone)
  const onPhone = (e) => {
    setPhone(cleanPhone(e.target.value))
    if (formError) setFormError('')
  }

  // Ngày sinh
  const maxNgay = soNgayToiDa(thang, nam)
  const ngay = ngay0 !== '' && +ngay0 > maxNgay ? '' : ngay0
  const ngayList = Array.from({ length: maxNgay }, (_, i) => String(i + 1))
  const thangList = Array.from({ length: 12 }, (_, i) => ({ v: String(i + 1), t: 'Tháng ' + (i + 1) }))
  const namList = Array.from({ length: 2026 - 1920 + 1 }, (_, i) => String(2026 - i))
  const nsNote =
    ngay && thang && nam
      ? `Cô chú sinh ngày ${ngay} tháng ${thang} năm ${nam}.`
      : 'Cô chú chọn lần lượt ngày, tháng, năm sinh.'

  // Địa chỉ
  const found = DIA_GIOI.find((t) => t.id === tinh)
  const tinhList = DIA_GIOI
  const xaList = found ? found.xa : []
  const xaPlaceholder = found ? '— Chọn phường / xã —' : '— Chọn tỉnh / thành phố trước —'
  const xaNote = found
    ? `Danh sách phường / xã của ${found.ten}.`
    : 'Chọn tỉnh / thành phố trước thì danh sách này mới hiện.'

  // Đếm ngược 60 giây gửi lại mã OTP
  useEffect(() => {
    let timer
    if (otpSent && countdown > 0) {
      timer = setInterval(() => {
        setCountdown((prev) => prev - 1)
      }, 1000)
    }
    return () => clearInterval(timer)
  }, [otpSent, countdown])

  // Gửi OTP đăng ký
  const handleStartRegister = async (e) => {
    if (e) e.preventDefault()
    setTried(true)
    setFormError('')

    if (!fullName.trim()) {
      setFormError('Cô chú vui lòng điền Họ và tên.')
      return
    }
    if (!okPhone) {
      setFormError('Cô chú vui lòng kiểm tra lại số điện thoại.')
      return
    }
    if (!nam) {
      setFormError('Cô chú vui lòng chọn năm sinh.')
      return
    }
    if (!dongY) {
      setFormError('Cô chú vui lòng tích đồng ý điều khoản để phòng khám lưu thông tin.')
      return
    }

    setIsSendingOtp(true)
    try {
      await authApi.sendOtp(phoneNormalized, 'REGISTER')
      setOtpSent(true)
      setCountdown(60)
      setOtpCode('')
      setOtpError('')
    } catch (err) {
      setFormError(err.message || 'Không thể gửi mã xác nhận. Cô chú vui lòng thử lại sau.')
    } finally {
      setIsSendingOtp(false)
    }
  }

  // Gửi lại mã OTP
  const handleResendOtp = async () => {
    if (countdown > 0 || isSendingOtp) return
    setIsSendingOtp(true)
    setOtpError('')
    try {
      await authApi.sendOtp(phoneNormalized, 'REGISTER')
      setCountdown(60)
    } catch (err) {
      setOtpError(err.message || 'Không thể gửi lại mã xác nhận. Cô chú vui lòng thử lại sau.')
    } finally {
      setIsSendingOtp(false)
    }
  }

  // Xác nhận OTP và tạo tài khoản
  const handleConfirmRegister = async (e) => {
    if (e) e.preventDefault()
    if (otpCode.length < 6 || isSubmitting) return

    setIsSubmitting(true)
    setOtpError('')

    // Ghép địa chỉ
    const parts = []
    if (diaChiChiTiet.trim()) parts.push(diaChiChiTiet.trim())
    if (xa) parts.push(xa)
    if (found?.ten) parts.push(found.ten)
    const fullAddress = parts.join(', ')

    try {
      await register({
        fullName: fullName.trim(),
        phone: phoneNormalized,
        yearOfBirth: parseInt(nam, 10),
        gender: gioiTinh,
        address: fullAddress,
        otpCode: otpCode.trim(),
      })

      setOtpSent(false)
      navigate('/')
    } catch (err) {
      setOtpError(err.message || 'Mã xác nhận không đúng hoặc đã hết hạn. Cô chú vui lòng kiểm tra lại.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const dongOtp = () => {
    setOtpSent(false)
    setOtpError('')
  }

  return (
    <AuthLayout
      mode="register"
      overlay={otpSent && (
        <div style={{ position: "fixed", top: 0, right: 0, bottom: 0, left: 0, zIndex: 70, background: "rgba(42, 33, 24, 0.55)", overflowY: "auto", padding: "24px 20px", display: "flex", alignItems: "flex-start", justifyContent: "center" }}>
          <div role="dialog" aria-modal="true" aria-labelledby="otp-title" style={{ margin: "auto", width: "100%", maxWidth: "560px", background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "28px", boxShadow: "0 24px 60px rgba(42, 33, 24, 0.3)", overflow: "hidden" }}>
            <div style={{ padding: "24px 28px", background: "#3B7145", display: "flex", alignItems: "center", gap: "16px" }}>
              <h2 id="otp-title" style={{ margin: 0, flexGrow: 1, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "25px", lineHeight: "34px", fontWeight: 700, color: "#FFFFFF" }}>
                Nhập mã xác nhận
              </h2>
              <button type="button" onClick={dongOtp} aria-label="Đóng" style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "48px", height: "48px", background: "transparent", border: "2px solid rgba(255, 255, 255, 0.6)", borderRadius: "12px", color: "#FFFFFF", cursor: "pointer", flexShrink: 0 }}>
                <Icon name="x-2" strokeWidth={2} />
              </button>
            </div>
            <form onSubmit={handleConfirmRegister} style={{ padding: "24px 28px 28px", display: "flex", flexDirection: "column", gap: "20px" }}>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Phòng khám vừa gửi tin nhắn có mã 6 số tới số điện thoại cô chú vừa nhập. Cô chú nhập mã đó để xác nhận số này là của mình.
              </p>

              {/* Thông báo lỗi OTP nếu có */}
              {otpError && (
                <div role="alert" style={{ background: "#FDF2F2", border: "2px solid #E0A6A0", borderRadius: "14px", padding: "14px 18px", display: "flex", gap: "10px", alignItems: "flex-start" }}>
                  <Icon name="alert-circle" size={22} stroke="#90372A" style={{ flexShrink: 0, marginTop: "2px" }} />
                  <p style={{ margin: 0, fontSize: "16px", lineHeight: "24px", fontWeight: 600, color: "#90372A" }}>
                    {otpError}
                  </p>
                </div>
              )}

              <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                <label htmlFor="dk-otp" style={{ fontSize: "17px", fontWeight: 600, color: "#2A2118" }}>Mã xác nhận (6 số)</label>
                <input
                  id="dk-otp"
                  type="text"
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  maxLength={6}
                  pattern="[0-9]*"
                  placeholder="······"
                  value={otpCode}
                  onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ''))}
                  aria-describedby="dk-otp-note"
                  style={{ width: "100%", minHeight: "76px", padding: "0 16px", background: "#FFFFFF", border: "2px solid #8C7B5E", borderRadius: "12px", fontFamily: "inherit", fontSize: "36px", fontWeight: 700, letterSpacing: "0.45em", textAlign: "center", color: "#2A2118" }}
                />
                <p id="dk-otp-note" style={{ margin: 0, fontSize: "16px", lineHeight: "24px", color: "#6B5E4E" }}>
                  Tin nhắn thường đến trong vài giây. Điện thoại có thể tự điền mã giúp cô chú.
                </p>
              </div>

              <div style={{ display: "flex", flexWrap: "wrap", gap: "14px", alignItems: "center", justifyContent: "space-between", background: "#F4EADB", borderRadius: "16px", padding: "16px 20px" }}>
                <span style={{ fontSize: "18px", lineHeight: "28px", color: "#2A2118" }}>
                  {countdown > 0 ? (
                    <>Gửi lại mã sau <strong>{countdown} giây</strong></>
                  ) : (
                    <>Cô chú chưa nhận được mã?</>
                  )}
                </span>
                <button
                  type="button"
                  onClick={handleResendOtp}
                  disabled={countdown > 0 || isSendingOtp}
                  style={{
                    display: "inline-flex",
                    alignItems: "center",
                    minHeight: "52px",
                    padding: "0 22px",
                    background: countdown > 0 ? "#FFFCF6" : "#3B7145",
                    border: countdown > 0 ? "2px solid #B0A083" : 0,
                    borderRadius: "12px",
                    fontFamily: "inherit",
                    fontSize: "18px",
                    fontWeight: 600,
                    color: countdown > 0 ? "#6B5E4E" : "#FFFFFF",
                    cursor: countdown > 0 ? "not-allowed" : "pointer"
                  }}
                >
                  {isSendingOtp ? 'Đang gửi...' : 'Gửi lại mã'}
                </button>
              </div>

              <p style={{ margin: 0, fontSize: "16px", lineHeight: "24px", color: "#6B5E4E" }}>
                Không nhận được tin nhắn? Cô chú gọi
                {" "}
                <a href="tel:0397116666" style={{ color: "#2E5B37", fontWeight: 600, textDecoration: "underline" }}>0397 116 666</a>
                {" "}
                để phòng khám giúp.
              </p>

              <button
                type="submit"
                disabled={otpCode.length < 6 || isSubmitting}
                className="btn-primary"
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  gap: "10px",
                  width: "100%",
                  minHeight: "60px",
                  background: otpCode.length < 6 || isSubmitting ? "#8BA890" : "#3B7145",
                  border: 0,
                  borderRadius: "12px",
                  fontFamily: "inherit",
                  fontSize: "20px",
                  fontWeight: 600,
                  color: "#FFFFFF",
                  cursor: otpCode.length < 6 || isSubmitting ? "not-allowed" : "pointer"
                }}
              >
                <Icon name="user-plus" />
                {isSubmitting ? 'Đang tạo tài khoản...' : 'Xác nhận và tạo tài khoản'}
              </button>

              <button className="btn-ghost" type="button" onClick={dongOtp} style={{ display: "flex", alignItems: "center", justifyContent: "center", width: "100%", minHeight: "56px", background: "#FFFCF6", border: "2px solid #8C7B5E", borderRadius: "12px", fontFamily: "inherit", fontSize: "19px", fontWeight: 600, color: "#2E5B37", cursor: "pointer" }}>
                Quay lại sửa thông tin
              </button>
            </form>
          </div>
        </div>
      )}
    >
      <section style={{ width: "100%", flexGrow: 1, padding: "56px 24px 72px" }}>
        <div style={{ maxWidth: "1060px", margin: "0 auto", display: "flex", flexWrap: "wrap", gap: "48px", alignItems: "flex-start" }}>
          {/* Cột trái */}
          <div style={{ flex: "1 1 320px", minWidth: 0, display: "flex", flexDirection: "column", gap: "24px" }}>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "28px", padding: "28px", display: "flex", gap: "16px", alignItems: "flex-start" }}>
              <Icon name="lock" size={30} stroke="#8A6012" style={{ flexShrink: 0 }} />
              <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>
                Phòng khám chỉ dùng thông tin của cô chú cho việc khám chữa bệnh, không chia sẻ cho bên nào khác.
              </p>
            </div>
            <img src={facade} alt="Mặt tiền Phòng khám Chúc Từ" style={{ width: "100%", height: "240px", objectFit: "cover", borderRadius: "28px", display: "block" }} />
          </div>

          {/* Cột phải: form */}
          <form onSubmit={handleStartRegister} style={{ flex: "999 1 460px", minWidth: 0, background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "28px", padding: "40px", display: "flex", flexDirection: "column", gap: "24px" }}>
            <div>
              <h1 style={{ margin: "0 0 10px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "34px", lineHeight: "44px", fontWeight: 700, color: "#2A2118" }}>
                Tạo tài khoản
              </h1>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Cô chú điền giúp mấy ô dưới đây. Ô có dấu
                {" "}
                <span style={{ color: "#90372A", fontWeight: 700 }}>*</span>
                {" "}
                là bắt buộc.
              </p>
            </div>

            {/* Thông báo lỗi nếu có */}
            {formError && (
              <div role="alert" style={{ background: "#FDF2F2", border: "2px solid #E0A6A0", borderRadius: "14px", padding: "16px 20px", display: "flex", gap: "12px", alignItems: "flex-start" }}>
                <Icon name="alert-circle" size={24} stroke="#90372A" style={{ flexShrink: 0, marginTop: "2px" }} />
                <p style={{ margin: 0, fontSize: "17px", lineHeight: "26px", fontWeight: 600, color: "#90372A" }}>
                  {formError}
                </p>
              </div>
            )}

            <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
              <label htmlFor="dk-hoten" style={{ fontSize: "17px", fontWeight: 600, color: "#2A2118" }}>
                Họ và tên
                {" "}
                <span style={{ color: "#90372A" }}>*</span>
              </label>
              <input
                id="dk-hoten"
                type="text"
                placeholder="Ví dụ: Nguyễn Thị Hoa"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                style={{ width: "100%", minHeight: "60px", padding: "0 16px", background: "#FFFFFF", border: "2px solid #8C7B5E", borderRadius: "12px", fontFamily: "inherit", fontSize: "20px", color: "#2A2118" }}
              />
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
              <label htmlFor="dk-sodienthoai" style={{ fontSize: "17px", fontWeight: 600, color: "#2A2118" }}>
                Số điện thoại
                {" "}
                <span style={{ color: "#90372A" }}>*</span>
              </label>
              <div className="sdt-wrap" style={{ minHeight: "60px" }}>
                <span className="sdt-pre" aria-hidden="true" style={{ fontSize: "20px" }}>+84</span>
                <input id="dk-sodienthoai" type="tel" inputMode="numeric" maxLength={10} className="sdt-in" placeholder="912345678" value={phone} onChange={onPhone} aria-invalid={phoneInvalid} aria-describedby="dk-sdt-loi dk-sdt-note" style={{ fontSize: "20px" }} />
              </div>
              {errPhone && (
                <p id="dk-sdt-loi" role="alert" style={{ margin: 0, display: "flex", gap: "8px", alignItems: "flex-start", fontSize: "17px", lineHeight: "26px", fontWeight: 600, color: "#90372A" }}>
                  <Icon name="alert-circle" size={22} strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
                  {phoneMsg}
                </p>
              )}
              <p id="dk-sdt-note" style={{ margin: 0, fontSize: "16px", lineHeight: "24px", color: "#6B5E4E" }}>
                Phòng khám gửi mã xác nhận tới số này. Không cần đặt mật khẩu.
              </p>
            </div>

            <div role="group" aria-labelledby="dk-ns-label" style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
              <span id="dk-ns-label" style={{ fontSize: "17px", fontWeight: 600, color: "#2A2118" }}>
                Ngày sinh <span style={{ color: "#90372A" }}>*</span>
              </span>
              <div style={{ display: "flex", flexWrap: "wrap", gap: "14px" }}>
                <div style={{ flex: "1 1 120px", minWidth: 0, display: "flex", flexDirection: "column", gap: "6px" }}>
                  <label htmlFor="dk-ngay" style={{ fontSize: "16px", fontWeight: 600, color: "#4A3F33" }}>Ngày</label>
                  <select id="dk-ngay" value={ngay} onChange={(e) => setNgay(e.target.value)} aria-describedby="dk-ns-note" style={{ width: "100%", minHeight: "60px", padding: "0 16px", background: "#FFFFFF", border: "2px solid #8C7B5E", borderRadius: "12px", fontFamily: "inherit", fontSize: "20px", color: "#2A2118" }}>
                    <option value="">Chọn ngày</option>
                    {ngayList.map((n, i) => (
                      <option key={i} value={n}>{n}</option>
                    ))}
                  </select>
                </div>
                <div style={{ flex: "1.3 1 160px", minWidth: 0, display: "flex", flexDirection: "column", gap: "6px" }}>
                  <label htmlFor="dk-thang" style={{ fontSize: "16px", fontWeight: 600, color: "#4A3F33" }}>Tháng</label>
                  <select id="dk-thang" value={thang} onChange={(e) => setThang(e.target.value)} aria-describedby="dk-ns-note" style={{ width: "100%", minHeight: "60px", padding: "0 16px", background: "#FFFFFF", border: "2px solid #8C7B5E", borderRadius: "12px", fontFamily: "inherit", fontSize: "20px", color: "#2A2118" }}>
                    <option value="">Chọn tháng</option>
                    {thangList.map((t, i) => (
                      <option key={i} value={t.v}>{t.t}</option>
                    ))}
                  </select>
                </div>
                <div style={{ flex: "1.2 1 140px", minWidth: 0, display: "flex", flexDirection: "column", gap: "6px" }}>
                  <label htmlFor="dk-nam" style={{ fontSize: "16px", fontWeight: 600, color: "#4A3F33" }}>Năm</label>
                  <select id="dk-nam" value={nam} onChange={(e) => setNam(e.target.value)} aria-describedby="dk-ns-note" style={{ width: "100%", minHeight: "60px", padding: "0 16px", background: "#FFFFFF", border: "2px solid #8C7B5E", borderRadius: "12px", fontFamily: "inherit", fontSize: "20px", color: "#2A2118" }}>
                    <option value="">Chọn năm</option>
                    {namList.map((y, i) => (
                      <option key={i} value={y}>{y}</option>
                    ))}
                  </select>
                </div>
              </div>
              <p id="dk-ns-note" aria-live="polite" style={{ margin: 0, fontSize: "16px", lineHeight: "24px", color: "#6B5E4E" }}>{nsNote}</p>
            </div>

            <div role="radiogroup" aria-labelledby="dk-gioitinh-label" style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
              <span id="dk-gioitinh-label" style={{ fontSize: "17px", fontWeight: 600, color: "#2A2118" }}>
                Giới tính
                {" "}
                <span style={{ color: "#90372A" }}>*</span>
              </span>
              <div style={{ display: "flex", flexWrap: "wrap", gap: "14px" }}>
                <label style={{ flex: "1 1 160px", minWidth: 0, display: "flex", gap: "12px", alignItems: "center", minHeight: "60px", padding: "0 18px", background: "#FFFFFF", border: gioiTinh === 'MALE' ? "2px solid #3B7145" : "2px solid #8C7B5E", borderRadius: "12px", fontSize: "19px", color: "#2A2118", cursor: "pointer" }}>
                  <input
                    type="radio"
                    name="dk-gioitinh"
                    checked={gioiTinh === 'MALE'}
                    onChange={() => setGioiTinh('MALE')}
                    style={{ width: "26px", height: "26px", margin: 0, accentColor: "#3B7145", flexShrink: 0 }}
                  />
                  Nam
                </label>
                <label style={{ flex: "1 1 160px", minWidth: 0, display: "flex", gap: "12px", alignItems: "center", minHeight: "60px", padding: "0 18px", background: "#FFFFFF", border: gioiTinh === 'FEMALE' ? "2px solid #3B7145" : "2px solid #8C7B5E", borderRadius: "12px", fontSize: "19px", color: "#2A2118", cursor: "pointer" }}>
                  <input
                    type="radio"
                    name="dk-gioitinh"
                    checked={gioiTinh === 'FEMALE'}
                    onChange={() => setGioiTinh('FEMALE')}
                    style={{ width: "26px", height: "26px", margin: 0, accentColor: "#3B7145", flexShrink: 0 }}
                  />
                  Nữ
                </label>
              </div>
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: "16px", background: "#F4EADB", borderRadius: "20px", padding: "24px" }}>
              <h2 style={{ margin: 0, fontSize: "20px", fontWeight: 700, color: "#2A2118" }}>Địa chỉ</h2>
              <div style={{ display: "flex", flexWrap: "wrap", gap: "20px" }}>
                <div style={{ flex: "1 1 240px", minWidth: 0, display: "flex", flexDirection: "column", gap: "8px" }}>
                  <label htmlFor="dk-tinh" style={{ fontSize: "17px", fontWeight: 600, color: "#2A2118" }}>
                    Tỉnh / Thành phố
                    {" "}
                    <span style={{ color: "#90372A" }}>*</span>
                  </label>
                  <select id="dk-tinh" value={tinh} onChange={(e) => { setTinh(e.target.value); setXa(''); }} style={{ width: "100%", minHeight: "60px", padding: "0 16px", background: "#FFFFFF", border: "2px solid #8C7B5E", borderRadius: "12px", fontFamily: "inherit", fontSize: "20px", color: "#2A2118" }}>
                    <option value="">— Chọn tỉnh / thành phố —</option>
                    {tinhList.map((t, i) => (
                      <option key={i} value={t.id}>{t.ten}</option>
                    ))}
                  </select>
                </div>
                <div style={{ flex: "1 1 240px", minWidth: 0, display: "flex", flexDirection: "column", gap: "8px" }}>
                  <label htmlFor="dk-xa" style={{ fontSize: "17px", fontWeight: 600, color: "#2A2118" }}>
                    Phường / Xã
                    {" "}
                    <span style={{ color: "#90372A" }}>*</span>
                  </label>
                  <select id="dk-xa" value={xa} onChange={(e) => setXa(e.target.value)} aria-describedby="dk-xa-note" style={{ width: "100%", minHeight: "60px", padding: "0 16px", background: "#FFFFFF", border: "2px solid #8C7B5E", borderRadius: "12px", fontFamily: "inherit", fontSize: "20px", color: "#2A2118" }}>
                    <option value="">{xaPlaceholder}</option>
                    {xaList.map((x, i) => (
                      <option key={i} value={x}>{x}</option>
                    ))}
                  </select>
                  <p id="dk-xa-note" style={{ margin: 0, fontSize: "16px", lineHeight: "24px", color: "#6B5E4E" }}>{xaNote}</p>
                </div>
              </div>
              <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                <label htmlFor="dk-diachi" style={{ fontSize: "17px", fontWeight: 600, color: "#2A2118" }}>Địa chỉ cụ thể</label>
                <input
                  id="dk-diachi"
                  type="text"
                  placeholder="Số nhà, tên đường, tổ / ấp"
                  value={diaChiChiTiet}
                  onChange={(e) => setDiaChiChiTiet(e.target.value)}
                  style={{ width: "100%", minHeight: "60px", padding: "0 16px", background: "#FFFFFF", border: "2px solid #8C7B5E", borderRadius: "12px", fontFamily: "inherit", fontSize: "20px", color: "#2A2118" }}
                />
              </div>
            </div>

            <div style={{ display: "flex", gap: "14px", alignItems: "flex-start", background: "#F4EADB", borderRadius: "16px", padding: "18px 20px" }}>
              <input
                id="dk-dongy"
                type="checkbox"
                checked={dongY}
                onChange={(e) => setDongY(e.target.checked)}
                style={{ width: "26px", height: "26px", margin: "2px 0 0", accentColor: "#3B7145", flexShrink: 0 }}
              />
              <label htmlFor="dk-dongy" style={{ fontSize: "18px", lineHeight: "28px", color: "#2A2118" }}>
                Tôi đồng ý để phòng khám lưu thông tin này phục vụ việc khám chữa bệnh.
              </label>
            </div>

            <button
              className="btn-primary"
              type="submit"
              disabled={isSendingOtp}
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                gap: "10px",
                width: "100%",
                minHeight: "60px",
                background: isSendingOtp ? "#8BA890" : "#3B7145",
                border: 0,
                borderRadius: "12px",
                fontFamily: "inherit",
                fontSize: "20px",
                fontWeight: 600,
                color: "#FFFFFF",
                cursor: isSendingOtp ? "not-allowed" : "pointer"
              }}
            >
              <Icon name="user-plus" />
              {isSendingOtp ? 'Đang gửi mã xác nhận...' : 'Tạo tài khoản'}
            </button>

            <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
              Đã có tài khoản?
              {" "}
              <Link to="/dang-nhap" style={{ color: "#2E5B37", fontWeight: 600, textDecoration: "underline" }}>Đăng nhập tại đây</Link>
            </p>
          </form>
        </div>
      </section>
    </AuthLayout>
  )
}
