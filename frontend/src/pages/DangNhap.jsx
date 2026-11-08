import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Icon from '../components/Icon'
import { facade } from '../assets'
import AuthLayout from '../components/AuthLayout'
import { useAuth } from '../context/AuthContext'
import { cleanPhone, isValidPhone, phoneMessage, normalizePhone } from '../lib/phone'

// Đăng nhập trên máy đã từng đăng nhập: chỉ cần số điện thoại.
// booking = true: luồng "Đặt lịch khám" (đăng nhập xong đi tiếp tới trang đặt lịch).
export default function DangNhap({ booking = false }) {
  const { quickLogin } = useAuth()
  const navigate = useNavigate()
  const [phone, setPhone] = useState('')
  const [tried, setTried] = useState(false)
  const [serverError, setServerError] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  const digits = cleanPhone(phone)
  const phoneNormalized = normalizePhone(phone)
  const okPhone = isValidPhone(phone)
  const errPhone = tried && !okPhone
  const phoneInvalid = errPhone ? 'true' : 'false'
  const phoneMsg = phoneMessage(phone)
  const subtitle = 'Máy này đã từng đăng nhập nên cô chú chỉ cần nhập số điện thoại đã đăng ký rồi bấm Đăng nhập.'
  const next = booking ? '/dat-lich' : '/'
  const newDeviceTo = booking ? '/dat-lich/dang-nhap/may-moi' : '/dang-nhap/may-moi'

  const onPhone = (e) => {
    setPhone(cleanPhone(e.target.value))
    if (serverError) setServerError('')
  }

  const handleLogin = async (e) => {
    if (e) e.preventDefault()
    setTried(true)
    if (!okPhone || isLoading) return

    setIsLoading(true)
    setServerError('')

    try {
      await quickLogin(phoneNormalized)
      navigate(next)
    } catch (err) {
      setServerError(err.message || 'Không thể đăng nhập. Cô chú vui lòng kiểm tra lại số điện thoại hoặc chọn Đăng nhập máy mới.')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <AuthLayout mode="login">
      <section style={{ width: "100%", flexGrow: 1, padding: "56px 24px 72px" }}>
        <div style={{ maxWidth: "1060px", margin: "0 auto", display: "flex", flexWrap: "wrap", gap: "48px", alignItems: "stretch" }}>
          {/* Cột trái */}
          <div style={{ flex: "1 1 380px", minWidth: 0, display: "flex", flexDirection: "column", gap: "24px" }}>
            <img src={facade} alt="Mặt tiền Phòng khám Chúc Từ" style={{ width: "100%", height: "300px", objectFit: "cover", borderRadius: "28px", border: "4px solid #C9963F", display: "block" }} />
          </div>
          {/* Cột phải: form */}
          <form onSubmit={handleLogin} style={{ flex: "999 1 420px", minWidth: 0, background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "28px", padding: "40px", display: "flex", flexDirection: "column", gap: "24px" }}>
            <div>
              <h1 style={{ margin: "0 0 10px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "34px", lineHeight: "44px", fontWeight: 700, color: "#2A2118" }}>
                Đăng nhập
              </h1>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>{subtitle}</p>
            </div>

            {/* Thông báo lỗi từ máy chủ nếu có */}
            {serverError && (
              <div role="alert" style={{ background: "#FDF2F2", border: "2px solid #E0A6A0", borderRadius: "14px", padding: "16px 20px", display: "flex", gap: "12px", alignItems: "flex-start" }}>
                <Icon name="alert-circle" size={24} stroke="#90372A" style={{ flexShrink: 0, marginTop: "2px" }} />
                <p style={{ margin: 0, fontSize: "17px", lineHeight: "26px", fontWeight: 600, color: "#90372A" }}>
                  {serverError}
                </p>
              </div>
            )}

            {/* Nhập số điện thoại và đăng nhập */}
            <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
              <label htmlFor="dn-sdt" style={{ fontSize: "17px", fontWeight: 600, color: "#2A2118" }}>Số điện thoại</label>
              <div className="sdt-wrap" style={{ minHeight: "60px" }}>
                <span className="sdt-pre" aria-hidden="true" style={{ fontSize: "20px" }}>+84</span>
                <input id="dn-sdt" type="tel" inputMode="numeric" maxLength={10} className="sdt-in" autoComplete="tel" value={phone} onChange={onPhone} aria-invalid={phoneInvalid} placeholder="912345678" aria-describedby="dn-sdt-loi dn-sdt-note" style={{ fontSize: "20px" }} />
              </div>
              {errPhone && (
                <p id="dn-sdt-loi" role="alert" style={{ margin: 0, display: "flex", gap: "8px", alignItems: "flex-start", fontSize: "17px", lineHeight: "26px", fontWeight: 600, color: "#90372A" }}>
                  <Icon name="alert-circle" size={22} strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
                  {phoneMsg}
                </p>
              )}
              <p id="dn-sdt-note" style={{ margin: 0, fontSize: "16px", lineHeight: "24px", color: "#6B5E4E" }}>
                Không cần nhớ mật khẩu và không cần nhập mã.
                {" "}
                <Link to={newDeviceTo} style={{ color: "#2E5B37", fontWeight: 600, textDecoration: "underline" }}>Đăng nhập trên máy mới?</Link>
              </p>
            </div>

            {/* Nút đăng nhập */}
            <button
              type="submit"
              disabled={isLoading}
              className="btn-primary"
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                gap: "10px",
                width: "100%",
                minHeight: "60px",
                background: isLoading ? "#8BA890" : "#3B7145",
                border: 0,
                borderRadius: "12px",
                fontFamily: "inherit",
                fontSize: "20px",
                fontWeight: 600,
                color: "#FFFFFF",
                cursor: isLoading ? "not-allowed" : "pointer"
              }}
            >
              <Icon name="log-in" />
              {isLoading ? 'Đang kiểm tra đăng nhập...' : 'Đăng nhập'}
            </button>

            <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
              Chưa có tài khoản?
              {" "}
              <Link to="/dang-ky" style={{ color: "#2E5B37", fontWeight: 600, textDecoration: "underline" }}>Tạo tài khoản mới</Link>
            </p>
          </form>
        </div>
      </section>
    </AuthLayout>
  )
}
