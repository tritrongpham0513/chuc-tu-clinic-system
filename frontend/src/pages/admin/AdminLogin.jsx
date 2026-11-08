import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { logo } from '../../assets'

export default function AdminLogin() {
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('MatKhauAdmin@2026')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const { loginWithPassword, logout } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const loggedInUser = await loginWithPassword(username.trim(), password)
      if (loggedInUser && loggedInUser.role !== 'ADMIN') {
        logout()
        if (loggedInUser.role === 'STAFF') {
          setError('Tài khoản của bạn là Tiếp tân. Vui lòng đăng nhập tại Cổng Tiếp tân (/tiep-tan/dang-nhap).')
        } else if (loggedInUser.role === 'DOCTOR') {
          setError('Tài khoản của bạn là Bác sĩ. Vui lòng đăng nhập tại Cổng Bác sĩ (/bac-si/dang-nhap).')
        } else {
          setError('Tài khoản bệnh nhân không có quyền truy cập trang quản trị.')
        }
        return
      }
      navigate('/admin')
    } catch (err) {
      console.error('Lỗi đăng nhập admin:', err)
      setError(err.message || 'Đăng nhập không thành công. Vui lòng kiểm tra lại tài khoản hoặc mật khẩu.')
    } finally {
      setLoading(false)
    }
  }

  const fillAccount = (u, p) => {
    setUsername(u)
    setPassword(p)
    setError('')
  }

  return (
    <div style={{ minHeight: '100vh', background: '#F5EFE6', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
      <div style={{ width: '100%', maxWidth: 460, background: '#fff', borderRadius: 24, padding: '36px 32px', boxShadow: '0 10px 40px rgba(42,33,24,0.12)', border: '2px solid #E0D3BC' }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <img src={logo} alt="Chúc Từ" style={{ width: 64, height: 64, objectFit: 'contain', margin: '0 auto 12px' }} />
          <div style={{ display: 'inline-block', background: '#FEE2E2', color: '#991B1B', padding: '4px 14px', borderRadius: 999, fontSize: 13, fontWeight: 800, marginBottom: 8 }}>
            👑 CỔNG QUẢN TRỊ VIÊN ADMIN
          </div>
          <h1 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 26, fontWeight: 700, color: '#5C3A21', margin: 0 }}>
            Phòng Khám Chúc Từ
          </h1>
          <div style={{ fontSize: 15, color: '#6B5E4E', marginTop: 4 }}>
            Hệ thống Quản lý Vận hành, Tài khoản & Phân quyền
          </div>
        </div>

        {/* Khối gợi ý tài khoản admin */}
        <div style={{ background: '#FFF7ED', border: '1.5px solid #F97316', borderRadius: 14, padding: '14px 16px', marginBottom: 20 }}>
          <div style={{ fontSize: 14, fontWeight: 700, color: '#C2410C', marginBottom: 4 }}>
            🔑 TÀI KHOẢN QUẢN TRỊ VIÊN:
          </div>
          <div style={{ fontSize: 15, color: '#1F1610', lineHeight: 1.6 }}>
            <div>Tên đăng nhập: <strong style={{ color: '#C2410C', fontFamily: 'monospace', fontSize: 16 }}>admin</strong></div>
            <div>Mật khẩu: <strong style={{ color: '#C2410C', fontFamily: 'monospace', fontSize: 16 }}>MatKhauAdmin@2026</strong></div>
          </div>
          <button
            type="button"
            onClick={() => fillAccount('admin', 'MatKhauAdmin@2026')}
            style={{ marginTop: 8, padding: '6px 12px', borderRadius: 8, background: '#C2410C', color: '#fff', border: 'none', fontSize: 13, fontWeight: 600, cursor: 'pointer' }}
          >
            ✓ Tự động điền tài khoản Quản trị
          </button>
        </div>

        {error && (
          <div style={{ background: '#FFF0EE', border: '1px solid #90372A30', borderRadius: 12, padding: '12px 16px', color: '#90372A', fontSize: 15, marginBottom: 20 }}>
            ❌ {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
          <div>
            <label style={{ display: 'block', fontSize: 16, fontWeight: 600, color: '#1F1610', marginBottom: 6 }}>
              Tên đăng nhập Quản trị
            </label>
            <input
              type="text"
              value={username}
              onChange={e => setUsername(e.target.value)}
              placeholder="admin"
              required
              style={{
                width: '100%', minHeight: 48, padding: '0 14px',
                borderRadius: 12, border: '1.5px solid #E0D3BC',
                fontSize: 17, fontFamily: 'inherit', boxSizing: 'border-box'
              }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: 16, fontWeight: 600, color: '#1F1610', marginBottom: 6 }}>
              Mật khẩu
            </label>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="Nhập mật khẩu..."
              required
              style={{
                width: '100%', minHeight: 48, padding: '0 14px',
                borderRadius: 12, border: '1.5px solid #E0D3BC',
                fontSize: 17, fontFamily: 'inherit', boxSizing: 'border-box'
              }}
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%', minHeight: 50, borderRadius: 12,
              background: '#5C3A21', color: '#fff', border: 'none',
              fontSize: 18, fontWeight: 700, cursor: 'pointer',
              marginTop: 4, boxShadow: '0 4px 12px rgba(92,58,33,0.25)'
            }}
          >
            {loading ? 'Đang xác thực...' : 'Đăng nhập Quản trị Admin'}
          </button>
        </form>

        {/* Footer chuyển cổng Tiếp tân & Bác sĩ */}
        <div style={{ marginTop: 24, paddingTop: 16, borderTop: '1px solid #E0D3BC', display: 'flex', justifyContent: 'space-between', fontSize: 14 }}>
          <Link to="/tiep-tan/dang-nhap" style={{ color: '#1E40AF', fontWeight: 700, textDecoration: 'none' }}>
            💁‍♀️ Cổng Tiếp tân →
          </Link>
          <Link to="/bac-si/dang-nhap" style={{ color: '#15803D', fontWeight: 700, textDecoration: 'none' }}>
            👨‍⚕️ Cổng Bác sĩ →
          </Link>
        </div>
      </div>
    </div>
  )
}
