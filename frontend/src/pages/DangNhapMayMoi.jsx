import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Icon from '../components/Icon'
import { facade } from '../assets'
import AuthLayout from '../components/AuthLayout'
import { useAuth } from '../context/AuthContext'
import authApi from '../api/auth'
import { cleanPhone, isValidPhone, phoneMessage, maskPhone, normalizePhone } from '../lib/phone'

// Đăng nhập trên máy mới: nhập số điện thoại -> hộp thoại nhập mã OTP.
export default function DangNhapMayMoi({ booking = false, otpOpen = false }) {
  const { loginWithOtp } = useAuth()
  const navigate = useNavigate()

  const [phone, setPhone] = useState('')
  const [moHopOtp, setMoHopOtp] = useState(otpOpen)
  const [tried, setTried] = useState(false)
  const [serverError, setServerError] = useState('')
  const [otpError, setOtpError] = useState('')
  const [isSendingOtp, setIsSendingOtp] = useState(false)
  const [isVerifying, setIsVerifying] = useState(false)
  const [otpCode, setOtpCode] = useState('')
  const [countdown, setCountdown] = useState(60)

  const digits = cleanPhone(phone)
  const phoneNormalized = normalizePhone(phone)
  const okPhone = isValidPhone(phone)
  const errPhone = tried && !okPhone
  const phoneInvalid = errPhone ? 'true' : 'false'
  const phoneMsg = phoneMessage(phone)
  const masked = maskPhone(phone)
  const subtitle =
    'Đây là máy mới nên cô chú nhập số điện thoại rồi bấm Đăng nhập. Phòng khám sẽ gửi một mã số qua tin nhắn để xác nhận.'
  const next = booking ? '/dat-lich' : '/'
  const oldDeviceTo = booking ? '/dat-lich/dang-nhap' : '/dang-nhap'

  // Đếm ngược 60 giây gửi lại mã OTP
  useEffect(() => {
    let timer
    if (moHopOtp && countdown > 0) {
      timer = setInterval(() => {
        setCountdown((prev) => prev - 1)
      }, 1000)
    }
    return () => clearInterval(timer)
  }, [moHopOtp, countdown])

  const onPhone = (e) => {
    setPhone(cleanPhone(e.target.value))
    if (serverError) setServerError('')
  }

  // Gửi OTP và mở hộp thoại
  const handleOpenOtp = async () => {
    setTried(true)
    if (!okPhone || isSendingOtp) return

    setIsSendingOtp(true)
    setServerError('')
    try {
      await authApi.sendOtp(phoneNormalized, 'LOGIN')
      setMoHopOtp(true)
      setCountdown(60)
      setOtpCode('')
      setOtpError('')
    } catch (err) {
      setServerError(err.message || 'Không thể gửi mã xác nhận. Cô chú vui lòng thử lại sau.')
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
      await authApi.sendOtp(phoneNormalized, 'LOGIN')
      setCountdown(60)
    } catch (err) {
      setOtpError(err.message || 'Không thể gửi lại mã xác nhận. Cô chú vui lòng thử lại sau.')
    } finally {
      setIsSendingOtp(false)
    }
  }

  // Xác thực OTP và đăng nhập
  const handleConfirmOtp = async (e) => {
    if (e) e.preventDefault()
    if (otpCode.length < 6 || isVerifying) return

    setIsVerifying(true)
    setOtpError('')

    try {
      await loginWithOtp(phoneNormalized, otpCode)
      setMoHopOtp(false)
      navigate(next)
    } catch (err) {
      setOtpError(err.message || 'Mã xác nhận không đúng hoặc đã hết hạn. Cô chú vui lòng kiểm tra lại.')
    } finally {
      setIsVerifying(false)
    }
  }

  const dongOtp = () => {
    setMoHopOtp(false)
    setOtpError('')
  }

  return (
    <AuthLayout
      mode="login"
      overlay={moHopOtp && (
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
            <form onSubmit={handleConfirmOtp} style={{ padding: "24px 28px 28px", display: "flex", flexDirection: "column", gap: "20px" }}>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Đây là máy mới nên phòng khám cần xác nhận lại. Phòng khám vừa gửi tin nhắn có mã 6 số tới số
                {" "}
                <strong style={{ color: "#2A2118" }}>{masked}</strong>
                .
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
                <label htmlFor="dn-otp" style={{ fontSize: "17px", fontWeight: 600, color: "#2A2118" }}>Mã xác nhận (6 số)</label>
                <input
                  id="dn-otp"
                  type="text"
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  maxLength={6}
                  pattern="[0-9]*"
                  placeholder="······"
                  value={otpCode}
                  onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ''))}
                  aria-describedby="dn-otp-note"
                  style={{ width: "100%", minHeight: "76px", padding: "0 16px", background: "#FFFFFF", border: "2px solid #8C7B5E", borderRadius: "12px", fontFamily: "inherit", fontSize: "36px", fontWeight: 700, letterSpacing: "0.45em", textAlign: "center", color: "#2A2118" }}
                />
                <p id="dn-otp-note" style={{ margin: 0, fontSize: "16px", lineHeight: "24px", color: "#6B5E4E" }}>
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

              <div style={{ display: "flex", gap: "14px", alignItems: "flex-start" }}>
                <input id="dn-ghinho" type="checkbox" defaultChecked style={{ width: "26px", height: "26px", margin: "2px 0 0", accentColor: "#3B7145", flexShrink: 0 }} />
                <label htmlFor="dn-ghinho" style={{ fontSize: "18px", lineHeight: "28px", color: "#2A2118" }}>
                  <strong>Giữ đăng nhập trên máy này.</strong>
                  {" "}
                  Lần sau cô chú không cần nhập mã.
                </label>
              </div>

              <button
                type="submit"
                disabled={otpCode.length < 6 || isVerifying}
                className="btn-primary"
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  gap: "10px",
                  width: "100%",
                  minHeight: "60px",
                  background: otpCode.length < 6 || isVerifying ? "#8BA890" : "#3B7145",
                  border: 0,
                  borderRadius: "12px",
                  fontFamily: "inherit",
                  fontSize: "20px",
                  fontWeight: 600,
                  color: "#FFFFFF",
                  cursor: otpCode.length < 6 || isVerifying ? "not-allowed" : "pointer"
                }}
              >
                <Icon name="log-in" />
                {isVerifying ? 'Đang xác nhận...' : 'Xác nhận và đăng nhập'}
              </button>

              <button className="btn-ghost" type="button" onClick={dongOtp} style={{ display: "flex", alignItems: "center", justifyContent: "center", width: "100%", minHeight: "56px", background: "#FFFCF6", border: "2px solid #8C7B5E", borderRadius: "12px", fontFamily: "inherit", fontSize: "19px", fontWeight: 600, color: "#2E5B37", cursor: "pointer" }}>
                Nhập lại số điện thoại
              </button>
            </form>
          </div>
        </div>
      )}
    >
      <section style={{ width: "100%", flexGrow: 1, padding: "56px 24px 72px" }}>
        <div style={{ maxWidth: "1060px", margin: "0 auto", display: "flex", flexWrap: "wrap", gap: "48px", alignItems: "stretch" }}>
          {/* Cột trái */}
          <div style={{ flex: "1 1 380px", minWidth: 0, display: "flex", flexDirection: "column", gap: "24px" }}>
            <img src={facade} alt="Mặt tiền Phòng khám Chúc Từ" style={{ width: "100%", height: "300px", objectFit: "cover", borderRadius: "28px", border: "4px solid #C9963F", display: "block" }} />
          </div>
          {/* Cột phải: form */}
          <div style={{ flex: "999 1 420px", minWidth: 0, background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "28px", padding: "40px", display: "flex", flexDirection: "column", gap: "24px" }}>
            <div>
              <h1 style={{ margin: "0 0 10px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "34px", lineHeight: "44px", fontWeight: 700, color: "#2A2118" }}>
                Đăng nhập máy mới
              </h1>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>{subtitle}</p>
            </div>

            {/* Thông báo lỗi nếu có */}
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
                Không cần nhớ mật khẩu. Vì là máy mới, phòng khám sẽ gửi mã xác nhận qua tin nhắn (tối đa 3 mã mỗi giờ).
                {" "}
                <Link to={oldDeviceTo} style={{ color: "#2E5B37", fontWeight: 600, textDecoration: "underline" }}>Đăng nhập trên máy đã dùng trước đó?</Link>
              </p>
            </div>

            {/* Máy mới: hiện hộp nhập mã OTP */}
            <button
              className="btn-primary"
              type="button"
              disabled={isSendingOtp}
              onClick={handleOpenOtp}
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
              <Icon name="log-in" />
              {isSendingOtp ? 'Đang gửi mã xác nhận...' : 'Đăng nhập'}
            </button>

            <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
              Chưa có tài khoản?
              {" "}
              <Link to="/dang-ky" style={{ color: "#2E5B37", fontWeight: 600, textDecoration: "underline" }}>Tạo tài khoản mới</Link>
            </p>
          </div>
        </div>
      </section>
    </AuthLayout>
  )
}
