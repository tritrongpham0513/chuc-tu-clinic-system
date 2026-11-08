import { Link } from 'react-router-dom'
import Icon from './Icon'
import { logo } from '../assets'

export default function GuestFooter() {
  return (
    <footer style={{ width: "100%", background: "#F4EADB", borderTop: "4px solid #C9963F" }}>
      {/* Dải thông tin liên hệ */}
      <div style={{ width: "100%", padding: "48px 24px 40px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "grid", gridTemplateColumns: "repeat(3, minmax(0, 1fr))", gap: "20px" }}>
          <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "24px", display: "flex", gap: "16px", alignItems: "flex-start" }}>
            <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "48px", height: "48px", background: "#E4EFE2", borderRadius: "999px", flexShrink: 0 }}>
              <Icon name="map-pin" stroke="#2E5B37" />
            </span>
            <div style={{ minWidth: 0 }}>
              <h3 style={{ margin: "0 0 6px", fontSize: "17px", fontWeight: 700, color: "#2A2118" }}>Địa chỉ phòng khám</h3>
              <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>57/8-10 Điện Biên Phủ, Phường 25, Quận Bình Thạnh, TP. Hồ Chí Minh</p>
            </div>
          </div>
          <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "24px", display: "flex", gap: "16px", alignItems: "flex-start" }}>
            <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "48px", height: "48px", background: "#E4EFE2", borderRadius: "999px", flexShrink: 0 }}>
              <Icon name="clock" stroke="#2E5B37" />
            </span>
            <div style={{ minWidth: 0 }}>
              <h3 style={{ margin: "0 0 6px", fontSize: "17px", fontWeight: 700, color: "#2A2118" }}>Giờ khám</h3>
              <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>
                Tất cả các ngày trong tuần,
                <br />
                kể cả Chủ Nhật:
                <strong style={{ color: "#2A2118" }}>8:00 – 16:30</strong>
              </p>
            </div>
          </div>
          <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "24px", display: "flex", gap: "16px", alignItems: "flex-start" }}>
            <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "48px", height: "48px", background: "#E4EFE2", borderRadius: "999px", flexShrink: 0 }}>
              <Icon name="phone" stroke="#2E5B37" />
            </span>
            <div style={{ minWidth: 0 }}>
              <h3 style={{ margin: "0 0 6px", fontSize: "17px", fontWeight: 700, color: "#2A2118" }}>Gọi cho phòng khám</h3>
              <a href="tel:0397116666" style={{ display: "inline-block", fontSize: "22px", fontWeight: 700, color: "#2E5B37", textDecoration: "underline" }}>
                0397 116 666
              </a>
              <p style={{ margin: "4px 0 0", fontSize: "16px", lineHeight: "25px", color: "#6B5E4E" }}>Cô chú cứ gọi, có người trực nghe máy.</p>
            </div>
          </div>
        </div>
      </div>
      {/* Thương hiệu và liên kết */}
      <div style={{ width: "100%", borderTop: "1px solid #E0D3BC", padding: "40px 24px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexWrap: "wrap", gap: "48px" }}>
          <div style={{ flex: "1 1 340px", minWidth: 0, display: "flex", flexDirection: "column", gap: "16px" }}>
            <Link to="/" aria-label="Về trang chủ" style={{ display: "inline-flex", alignItems: "center", gap: "12px", textDecoration: "none" }}>
              <img src={logo} alt="" style={{ width: "52px", height: "52px", objectFit: "contain" }} />
              <span style={{ display: "flex", flexDirection: "column" }}>
                <span style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: "24px", fontWeight: 700, color: "#2E5B37" }}>Chúc Từ</span>
                <span style={{ fontSize: "15px", color: "#6B5E4E" }}>Y học cổ truyền · Từ thiện</span>
              </span>
            </Link>
            <p style={{ margin: 0, maxWidth: "44ch", fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>
              Phòng chẩn trị Y học cổ truyền từ thiện Chúc Từ — khám và điều trị không thu phí cho bà con.
            </p>
          </div>
          <div style={{ flex: "0 1 200px", display: "flex", flexDirection: "column", gap: "12px" }}>
            <h3 style={{ margin: "0 0 4px", fontSize: "16px", fontWeight: 700, letterSpacing: "0.04em", textTransform: "uppercase", color: "#6B5E4E" }}>Nội dung</h3>
            <Link to="/gioi-thieu" style={{ fontSize: "17px", color: "#4A3F33", textDecoration: "underline" }}>Giới thiệu</Link>
            <Link to="/dich-vu" style={{ fontSize: "17px", color: "#4A3F33", textDecoration: "underline" }}>Dịch vụ</Link>
            <Link to="/tin-tuc" style={{ fontSize: "17px", color: "#4A3F33", textDecoration: "underline" }}>Tin tức</Link>
            <Link to="/huong-dan" style={{ fontSize: "17px", color: "#4A3F33", textDecoration: "underline" }}>Hướng dẫn</Link>
            <Link to="/lien-he" style={{ fontSize: "17px", color: "#4A3F33", textDecoration: "underline" }}>Liên hệ</Link>
          </div>
          <div style={{ flex: "0 1 220px", display: "flex", flexDirection: "column", gap: "12px" }}>
            <h3 style={{ margin: "0 0 4px", fontSize: "16px", fontWeight: 700, letterSpacing: "0.04em", textTransform: "uppercase", color: "#6B5E4E" }}>Tài khoản</h3>
            <Link to="/dang-nhap" style={{ fontSize: "17px", color: "#4A3F33", textDecoration: "underline" }}>Đăng nhập</Link>
            <Link to="/dang-ky" style={{ fontSize: "17px", color: "#4A3F33", textDecoration: "underline" }}>Tạo tài khoản</Link>
          </div>
          <div style={{ flex: "0 1 230px", display: "flex", flexDirection: "column", gap: "12px" }}>
            <h3 style={{ margin: "0 0 4px", fontSize: "16px", fontWeight: 700, letterSpacing: "0.04em", textTransform: "uppercase", color: "#6B5E4E" }}>Cần giúp đỡ</h3>
            <Link to="/huong-dan" style={{ fontSize: "17px", color: "#4A3F33", textDecoration: "underline" }}>Hướng dẫn đặt lịch</Link>
            <Link to="/huong-dan" style={{ fontSize: "17px", color: "#4A3F33", textDecoration: "underline" }}>Câu hỏi thường gặp</Link>
          </div>
        </div>
      </div>
      {/* Dòng cuối */}
      <div style={{ width: "100%", background: "#2E5B37", padding: "24px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexWrap: "wrap", gap: "16px 32px", alignItems: "center", justifyContent: "space-between" }}>
          <p style={{ margin: 0, fontSize: "16px", lineHeight: "26px", color: "#FFFFFF" }}>© 2026 Phòng chẩn trị Y học cổ truyền từ thiện Chúc Từ.</p>
          <p style={{ margin: 0, maxWidth: "60ch", fontSize: "16px", lineHeight: "26px", color: "#FFFFFF" }}>
            Thông tin trên trang chỉ mang tính tham khảo, không thay thế chẩn đoán của thầy thuốc.
          </p>
        </div>
      </div>
    </footer>
  )
}
