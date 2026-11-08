import { Link } from 'react-router-dom'
import { logo } from '../assets'

// Header gọn dùng cho các trang đăng nhập / đăng ký: mode = 'login' | 'register'
export default function AuthHeader({ mode }) {
  const isLogin = mode === 'login'
  return (
    <header style={{ width: "100%", background: "#FFFCF6", borderBottom: "1px solid #E0D3BC" }}>
      <div style={{ maxWidth: "1280px", margin: "0 auto", padding: "14px 24px", display: "flex", flexWrap: "wrap", alignItems: "center", gap: "12px 16px" }}>
        <Link to="/" aria-label="Về trang chủ Phòng khám Chúc Từ" style={{ display: "inline-flex", alignItems: "center", gap: "12px", textDecoration: "none" }}>
          <img src={logo} alt="" style={{ width: "52px", height: "52px", objectFit: "contain" }} />
          <span style={{ display: "flex", flexDirection: "column", lineHeight: 1.2 }}>
            <span style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: "23px", fontWeight: 700, color: "#2E5B37" }}>Chúc Từ</span>
            <span style={{ fontSize: "14px", color: "#6B5E4E" }}>Y học cổ truyền từ thiện</span>
          </span>
        </Link>
        <div style={{ marginLeft: "auto", display: "flex", alignItems: "center", gap: "12px", flexShrink: 0 }}>
          <span style={{ fontSize: "17px", color: "#4A3F33", whiteSpace: "nowrap" }}>{isLogin ? 'Chưa có tài khoản?' : 'Đã có tài khoản?'}</span>
          <Link to={isLogin ? '/dang-ky' : '/dang-nhap'} className="btn-ghost" style={{ display: "inline-flex", alignItems: "center", minHeight: "48px", padding: "0 18px", border: "2px solid #8C7B5E", borderRadius: "12px", fontSize: "18px", fontWeight: 600, color: "#2E5B37", textDecoration: "none" }}>
            {isLogin ? 'Tạo tài khoản' : 'Đăng nhập'}
          </Link>
        </div>
      </div>
    </header>
  )
}
