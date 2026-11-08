import { Link } from 'react-router-dom'
import Icon from './Icon'
import { logo } from '../assets'
import { NAV } from '../lib/nav'

export default function GuestHeader({ active }) {
  return (
    <header style={{ width: "100%", background: "#FFFCF6", borderBottom: "1px solid #E0D3BC", boxShadow: "0 4px 14px rgba(74, 63, 51, 0.06)" }}>
      <div style={{ maxWidth: "1280px", margin: "0 auto", padding: "14px 24px", display: "flex", flexWrap: "wrap", alignItems: "center", gap: "12px 16px" }}>
        <Link to="/" aria-label="Về trang chủ Phòng khám Chúc Từ" style={{ display: "inline-flex", alignItems: "center", gap: "12px", textDecoration: "none", padding: "4px", borderRadius: "12px", flexShrink: 0 }}>
          <img src={logo} alt="" style={{ width: "52px", height: "52px", objectFit: "contain" }} />
          <span style={{ display: "flex", flexDirection: "column", lineHeight: 1.2 }}>
            <span style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: "23px", fontWeight: 700, color: "#2E5B37" }}>Chúc Từ</span>
            <span style={{ fontSize: "14px", color: "#6B5E4E" }}>Y học cổ truyền từ thiện</span>
          </span>
        </Link>
        <label htmlFor="m-t" className="m-burger pub" role="button" aria-label="Mở hoặc đóng menu">
          <Icon name="menu" strokeWidth={2} join={false} className="ic-o" />
          <Icon name="x" strokeWidth={2} join={false} className="ic-x" />
          <span className="m-lbl">Menu</span>
        </label>
        <nav aria-label="Điều hướng chính" style={{ display: "flex", flex: "1 1 340px", minWidth: 0, flexWrap: "nowrap", alignItems: "center", gap: "4px", marginLeft: "24px" }}>
          {NAV.map((item) => {
            const on = item.key === active
            return (
              <Link
                key={item.key}
                to={item.to}
                className="nav-a"
                aria-current={on ? 'page' : undefined}
                style={{
                  fontSize: '18px',
                  fontWeight: on ? 600 : 500,
                  color: on ? '#2E5B37' : '#4A3F33',
                  textDecoration: 'none',
                  padding: '12px 14px',
                  borderRadius: '12px',
                  ...(on ? { background: '#E4EFE2' } : null),
                }}
              >
                {item.label}
              </Link>
            )
          })}
        </nav>
        <div style={{ marginLeft: "auto", display: "flex", alignItems: "center", gap: "12px", flexShrink: 0 }}>
          <Link to="/dang-nhap" className="btn-ghost" style={{ display: "inline-flex", alignItems: "center", gap: "8px", minHeight: "48px", padding: "0 20px", border: "2px solid #8C7B5E", borderRadius: "12px", fontSize: "18px", fontWeight: 600, color: "#2E5B37", textDecoration: "none", whiteSpace: "nowrap" }}>
            <Icon name="user" size={22} />
            Đăng nhập
          </Link>
        </div>
      </div>
    </header>
  )
}
