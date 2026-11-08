import { Link } from 'react-router-dom'
import Icon from './Icon'
import { logo } from '../assets'
import { NAV } from '../lib/nav'
import { useAuth } from '../context/AuthContext'

export default function UserHeader({ active }) {
  const { user, logout } = useAuth()
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
        <nav aria-label="Điều hướng chính" style={{ display: "flex", flex: "1 1 340px", minWidth: 0, flexWrap: "nowrap", alignItems: "center", gap: "4px", marginLeft: "12px" }}>
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
                  padding: '12px 12px',
                  borderRadius: '12px',
                  ...(on ? { background: '#E4EFE2' } : null),
                }}
              >
                {item.label}
              </Link>
            )
          })}
        </nav>
        <div className="user-menu" style={{ position: "relative", flexShrink: 0, marginLeft: "auto" }}>
          <button className="user-btn" type="button" aria-haspopup="menu" aria-expanded="false" style={{ display: "inline-flex", alignItems: "center", gap: "10px", minHeight: "48px", padding: "0 12px", background: "transparent", border: "2px solid transparent", borderRadius: "12px", fontFamily: "inherit", fontSize: "18px", fontWeight: 600, color: "#2E5B37", cursor: "pointer", whiteSpace: "nowrap" }}>
            <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "40px", height: "40px", background: "#E4EFE2", borderRadius: "999px", flexShrink: 0 }}>
              <Icon name="user" size={22} stroke="#2E5B37" />
            </span>
            {user.name}
            <Icon name="chevron-down" size={20} strokeWidth={2.25} />
          </button>
          <div className="menu-list" role="menu" style={{ position: "absolute", top: "calc(100% + 4px)", right: 0, minWidth: "300px", background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "16px", boxShadow: "0 18px 44px rgba(42, 33, 24, 0.22)", padding: "8px", flexDirection: "column", gap: "2px", zIndex: 60 }}>
            <Link to="/tai-khoan" className="menu-item" role="menuitem" style={{ display: "flex", alignItems: "center", gap: "14px", minHeight: "56px", padding: "0 14px", borderRadius: "12px", textDecoration: "none" }}>
              <Icon name="user" size={26} stroke="#2E5B37" style={{ flexShrink: 0 }} />
              <span style={{ display: "flex", flexDirection: "column", minWidth: 0 }}>
                <span style={{ fontSize: "18px", lineHeight: "26px", fontWeight: 600, color: "#2A2118" }}>Thông tin tài khoản</span>
              </span>
            </Link>
            <Link to="/tai-khoan/ho-so-kham-benh" className="menu-item" role="menuitem" style={{ display: "flex", alignItems: "center", gap: "14px", minHeight: "56px", padding: "0 14px", borderRadius: "12px", textDecoration: "none" }}>
              <Icon name="building" size={26} stroke="#2E5B37" style={{ flexShrink: 0 }} />
              <span style={{ display: "flex", flexDirection: "column", minWidth: 0 }}>
                <span style={{ fontSize: "18px", lineHeight: "26px", fontWeight: 600, color: "#2A2118" }}>Hồ sơ khám bệnh</span>
              </span>
            </Link>
            <Link to="/phieu-kham" className="menu-item" role="menuitem" style={{ display: "flex", alignItems: "center", gap: "14px", minHeight: "56px", padding: "0 14px", borderRadius: "12px", textDecoration: "none" }}>
              <Icon name="calendar-days" size={26} stroke="#2E5B37" style={{ flexShrink: 0 }} />
              <span style={{ display: "flex", flexDirection: "column", minWidth: 0 }}>
                <span style={{ fontSize: "18px", lineHeight: "26px", fontWeight: 600, color: "#2A2118" }}>Phiếu khám bệnh</span>
              </span>
            </Link>
            <Link to="/lich-su-kham" className="menu-item" role="menuitem" style={{ display: "flex", alignItems: "center", gap: "14px", minHeight: "56px", padding: "0 14px", borderRadius: "12px", textDecoration: "none" }}>
              <Icon name="file-text" size={26} stroke="#2E5B37" style={{ flexShrink: 0 }} />
              <span style={{ display: "flex", flexDirection: "column", minWidth: 0 }}>
                <span style={{ fontSize: "18px", lineHeight: "26px", fontWeight: 600, color: "#2A2118" }}>Lịch sử khám</span>
              </span>
            </Link>
            <span aria-hidden="true" style={{ height: "1px", background: "#E0D3BC", margin: "6px 8px" }} />
            <Link to="/" onClick={logout} className="menu-item" role="menuitem" style={{ display: "flex", alignItems: "center", gap: "14px", minHeight: "56px", padding: "0 14px", borderRadius: "12px", fontSize: "18px", fontWeight: 600, color: "#90372A", textDecoration: "none" }}>
              <Icon name="log-out" size={26} stroke="#90372A" style={{ flexShrink: 0 }} />
              Đăng xuất
            </Link>
          </div>
        </div>
      </div>
    </header>
  )
}
