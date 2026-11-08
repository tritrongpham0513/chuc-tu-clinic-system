import { Link } from 'react-router-dom'
import Icon from './Icon'
import { logo } from '../assets'

export default function UserFooter() {
  return (
    <footer style={{ width: "100%", marginTop: "auto", background: "#F4EADB", borderTop: "4px solid #C9963F" }}>
      <div style={{ width: "100%", padding: "44px 24px 40px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexWrap: "wrap", gap: "40px 48px" }}>
          <div style={{ flex: "1 1 300px", minWidth: 0, display: "flex", flexDirection: "column", gap: "16px" }}>
            <Link to="/" aria-label="Về trang chủ" style={{ display: "inline-flex", alignItems: "center", gap: "12px", textDecoration: "none" }}>
              <img src={logo} alt="" style={{ width: "52px", height: "52px", objectFit: "contain" }} />
              <span style={{ display: "flex", flexDirection: "column", lineHeight: 1.2 }}>
                <span style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: "24px", fontWeight: 700, color: "#2E5B37" }}>Chúc Từ</span>
                <span style={{ fontSize: "15px", color: "#6B5E4E" }}>Y học cổ truyền · Từ thiện</span>
              </span>
            </Link>
            <p style={{ margin: 0, maxWidth: "40ch", fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>
              Khám và điều trị không thu phí cho bà con. Cần gì cô chú cứ gọi, có người trực nghe máy.
            </p>
          </div>
          <div style={{ flex: "0 1 230px", display: "flex", flexDirection: "column", gap: "10px" }}>
            <h3 style={{ margin: "0 0 4px", fontSize: "16px", fontWeight: 700, letterSpacing: "0.04em", textTransform: "uppercase", color: "#6B5E4E" }}>
              Tài khoản của tôi
            </h3>
            <Link to="/tai-khoan" style={{ fontSize: "17px", lineHeight: "26px", color: "#4A3F33", textDecoration: "underline" }}>Thông tin tài khoản</Link>
            <Link to="/tai-khoan/ho-so-kham-benh" style={{ fontSize: "17px", lineHeight: "26px", color: "#4A3F33", textDecoration: "underline" }}>Hồ sơ khám bệnh</Link>
            <Link to="/phieu-kham" style={{ fontSize: "17px", lineHeight: "26px", color: "#4A3F33", textDecoration: "underline" }}>Phiếu khám bệnh</Link>
            <Link to="/lich-su-kham" style={{ fontSize: "17px", lineHeight: "26px", color: "#4A3F33", textDecoration: "underline" }}>Lịch sử khám</Link>
            <Link to="/dat-lich" style={{ fontSize: "17px", lineHeight: "26px", fontWeight: 700, color: "#2E5B37", textDecoration: "underline" }}>Đặt lịch khám mới</Link>
          </div>
          <div style={{ flex: "0 1 220px", display: "flex", flexDirection: "column", gap: "10px" }}>
            <h3 style={{ margin: "0 0 4px", fontSize: "16px", fontWeight: 700, letterSpacing: "0.04em", textTransform: "uppercase", color: "#6B5E4E" }}>Cần giúp đỡ</h3>
            <Link to="/huong-dan" style={{ fontSize: "17px", lineHeight: "26px", color: "#4A3F33", textDecoration: "underline" }}>Hướng dẫn đặt lịch</Link>
            <Link to="/huong-dan" style={{ fontSize: "17px", lineHeight: "26px", color: "#4A3F33", textDecoration: "underline" }}>Câu hỏi thường gặp</Link>
            <Link to="/lien-he" style={{ fontSize: "17px", lineHeight: "26px", color: "#4A3F33", textDecoration: "underline" }}>Liên hệ phòng khám</Link>
            <Link to="/" style={{ fontSize: "17px", lineHeight: "26px", color: "#90372A", textDecoration: "underline" }}>Đăng xuất</Link>
          </div>
          <div style={{ flex: "1 1 300px", minWidth: 0, display: "flex", flexDirection: "column", gap: "14px" }}>
            <h3 style={{ margin: "0 0 4px", fontSize: "16px", fontWeight: 700, letterSpacing: "0.04em", textTransform: "uppercase", color: "#6B5E4E" }}>Phòng khám</h3>
            <div style={{ display: "flex", gap: "12px", alignItems: "flex-start" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "40px", height: "40px", background: "#E4EFE2", borderRadius: "999px", flexShrink: 0 }}>
                <Icon name="map-pin" size={22} stroke="#2E5B37" />
              </span>
              <p style={{ margin: 0, fontSize: "17px", lineHeight: "26px", color: "#4A3F33" }}>57/8-10 Điện Biên Phủ, Phường 25, Quận Bình Thạnh, TP. Hồ Chí Minh</p>
            </div>
            <div style={{ display: "flex", gap: "12px", alignItems: "flex-start" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "40px", height: "40px", background: "#E4EFE2", borderRadius: "999px", flexShrink: 0 }}>
                <Icon name="clock" size={22} stroke="#2E5B37" />
              </span>
              <p style={{ margin: 0, fontSize: "17px", lineHeight: "26px", color: "#4A3F33" }}>
                Mỗi ngày kể cả Chủ Nhật:
                <br />
                <strong style={{ color: "#2A2118" }}>8:00 – 16:30</strong>
              </p>
            </div>
            <div style={{ display: "flex", gap: "12px", alignItems: "center" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "40px", height: "40px", background: "#E4EFE2", borderRadius: "999px", flexShrink: 0 }}>
                <Icon name="phone" size={22} stroke="#2E5B37" />
              </span>
              <a href="tel:0397116666" style={{ fontSize: "22px", fontWeight: 700, color: "#2E5B37", textDecoration: "underline" }}>0397 116 666</a>
            </div>
          </div>
        </div>
      </div>
      <div style={{ width: "100%", background: "#2E5B37", padding: "22px 24px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexWrap: "wrap", gap: "12px 32px", alignItems: "center", justifyContent: "space-between" }}>
          <p style={{ margin: 0, fontSize: "16px", lineHeight: "26px", color: "#FFFFFF" }}>© 2026 Phòng chẩn trị Y học cổ truyền từ thiện Chúc Từ.</p>
          <p style={{ margin: 0, maxWidth: "60ch", fontSize: "16px", lineHeight: "26px", color: "#FFFFFF" }}>
            Thông tin trên trang chỉ mang tính tham khảo, không thay thế chẩn đoán của thầy thuốc.
          </p>
        </div>
      </div>
    </footer>
  )
}
