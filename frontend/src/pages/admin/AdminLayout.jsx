import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import Icon from '../../components/Icon'
import { logo } from '../../assets'

export default function AdminLayout({ children }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  // Kiểm tra role nội bộ
  const isStaff = user && (user.role === 'ADMIN' || user.role === 'STAFF' || user.role === 'DOCTOR')

  const isTiepTan = location.pathname.startsWith('/tiep-tan')
  const isBacSi = location.pathname.startsWith('/bac-si')
  const defaultLoginUrl = user?.role === 'STAFF' || (!user && isTiepTan)
    ? '/tiep-tan/dang-nhap'
    : user?.role === 'DOCTOR' || (!user && isBacSi)
    ? '/bac-si/dang-nhap'
    : '/admin/login'
  const portalName = isTiepTan ? 'Bàn Tiếp Tân & Lễ Tân' : isBacSi ? 'Bàn Khám Bác Sĩ YHCT' : 'Quản Trị Phòng Khám'

  if (!user || !isStaff) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#FDFBF7' }}>
        <div style={{ textAlign: 'center', padding: 32, background: '#fff', borderRadius: 20, border: '2px solid #E0D3BC', maxWidth: 440 }}>
          <div style={{ fontSize: 48, marginBottom: 12 }}>🔒</div>
          <h2 style={{ fontSize: 24, fontWeight: 700, color: '#5C3A21', marginBottom: 8 }}>{portalName}</h2>
          <p style={{ fontSize: 16, color: '#6B5E4E', marginBottom: 24 }}>Vui lòng đăng nhập bằng tài khoản {isTiepTan ? 'Tiếp tân' : isBacSi ? 'Bác sĩ' : 'Quản trị viên'} để tiếp tục.</p>
          <button
            onClick={() => navigate(defaultLoginUrl)}
            style={{ width: '100%', minHeight: 48, borderRadius: 12, background: isTiepTan ? '#1D4ED8' : isBacSi ? '#15803D' : '#5C3A21', color: '#fff', fontSize: 17, fontWeight: 700, border: 'none', cursor: 'pointer' }}
          >
            Đến trang đăng nhập
          </button>
        </div>
      </div>
    )
  }

  // Danh mục Menu theo vai trò
  const role = user.role
  const navItems = []

  if (role === 'ADMIN') {
    navItems.push(
      { path: '/admin', label: '📊 Tổng quan hôm nay' },
      { path: '/admin/users', label: '👥 Quản lý tài khoản & Quyền' },
      { path: '/tiep-tan', label: '💁‍♀️ Bàn Tiếp tân (Portal)' },
      { path: '/bac-si', label: '👨‍⚕️ Bàn Khám Bác sĩ (Portal)' },
      { path: '/admin/appointments', label: '📅 Danh sách tất cả lịch hẹn' },
      { path: '/admin/patients', label: '🗂️ Hồ sơ bệnh nhân' },
      { path: '/admin/articles', label: '📰 Cẩm nang YHCT' }
    )
  } else if (role === 'STAFF') {
    navItems.push(
      { path: '/tiep-tan', label: '💁‍♀️ Bàn Tiếp tân & STT' },
      { path: '/admin/patients', label: '🗂️ Hồ sơ bệnh nhân' }
    )
  } else if (role === 'DOCTOR') {
    navItems.push(
      { path: '/bac-si', label: '👨‍⚕️ Bàn Khám Bác sĩ' },
      { path: '/admin/patients', label: '🗂️ Tra cứu Bệnh sử' },
      { path: '/admin/articles', label: '📰 Viết cẩm nang YHCT' }
    )
  }

  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: '#F8F6F0', fontFamily: 'inherit' }}>
      {/* Sidebar */}
      <aside style={{ width: 280, background: '#2B1A0F', color: '#FDFBF7', display: 'flex', flexDirection: 'column', flexShrink: 0 }}>
        {/* Brand */}
        <div style={{ padding: '24px 20px', borderBottom: '1px solid #3F291B', display: 'flex', alignItems: 'center', gap: 12 }}>
          <img src={logo} alt="Chúc Từ" style={{ width: 44, height: 44, objectFit: 'contain' }} />
          <div>
            <div style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 20, fontWeight: 700, color: '#E4EFE2' }}>Chúc Từ</div>
            <div style={{ fontSize: 13, color: '#BBAE9E' }}>Quản trị & Điều phối</div>
          </div>
        </div>

        {/* Navigation items */}
        <nav style={{ padding: '16px 12px', display: 'flex', flexDirection: 'column', gap: 6, flex: 1 }}>
          {navItems.map(item => {
            const active = location.pathname === item.path
            return (
              <Link
                key={item.path}
                to={item.path}
                style={{
                  display: 'flex', alignItems: 'center', gap: 10,
                  padding: '13px 14px', borderRadius: 12,
                  fontSize: 16, fontWeight: active ? 700 : 500,
                  color: active ? '#fff' : '#D1C7BA',
                  background: active ? '#3D2516' : 'transparent',
                  textDecoration: 'none',
                  borderLeft: active ? '4px solid #D97706' : '4px solid transparent',
                  transition: 'background 0.15s'
                }}
              >
                <span>{item.label}</span>
              </Link>
            )
          })}

          <div style={{ height: 1, background: '#3F291B', margin: '12px 4px' }} />

          {/* Màn hình TV phòng chờ */}
          <a
            href="/tv-phong-cho"
            target="_blank"
            rel="noopener noreferrer"
            style={{
              display: 'flex', alignItems: 'center', gap: 10,
              padding: '12px 14px', borderRadius: 12,
              fontSize: 15, fontWeight: 700,
              color: '#F59E0B', textDecoration: 'none',
              background: 'rgba(217, 119, 6, 0.1)'
            }}
          >
            <span>📺</span>
            <span>Mở TV Phòng chờ ↗</span>
          </a>

          <Link
            to="/"
            target="_blank"
            style={{
              display: 'flex', alignItems: 'center', gap: 10,
              padding: '12px 14px', borderRadius: 12,
              fontSize: 15, color: '#BBAE9E', textDecoration: 'none'
            }}
          >
            <span>🌐</span>
            <span>Trang chủ khách ↗</span>
          </Link>
        </nav>

        {/* User bar & Role badge (Prompt 3) */}
        <div style={{ padding: '16px 18px', borderTop: '1px solid #3F291B', background: '#22140B', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <div style={{ fontSize: 15, fontWeight: 700, color: '#fff', marginBottom: 4 }}>
              {user.name}
            </div>
            <div>
              {role === 'ADMIN' && (
                <span style={{ background: '#7F1D1D', color: '#FEF08A', padding: '2px 8px', borderRadius: 6, fontSize: 12, fontWeight: 700 }}>
                  👑 Quản trị viên
                </span>
              )}
              {role === 'STAFF' && (
                <span style={{ background: '#1E3A8A', color: '#BFDBFE', padding: '2px 8px', borderRadius: 6, fontSize: 12, fontWeight: 700 }}>
                  💁‍♀️ Lễ tân tiếp đón
                </span>
              )}
              {role === 'DOCTOR' && (
                <span style={{ background: '#14532D', color: '#BBF7D0', padding: '2px 8px', borderRadius: 6, fontSize: 12, fontWeight: 700 }}>
                  👨‍⚕️ Bác sĩ YHCT
                </span>
              )}
            </div>
          </div>
          <button
            onClick={() => { logout(); navigate(defaultLoginUrl) }}
            title="Đăng xuất"
            style={{ background: 'transparent', border: 'none', color: '#D97706', cursor: 'pointer', padding: 6 }}
          >
            <Icon name="log-out" size={20} stroke="#D97706" />
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main style={{ flex: 1, minWidth: 0, overflowY: 'auto', padding: '26px 30px' }}>
        {children}
      </main>
    </div>
  )
}
