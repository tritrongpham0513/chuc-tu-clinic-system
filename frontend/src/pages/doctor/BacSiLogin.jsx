import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { logo } from '../../assets'

export default function BacSiLogin() {
  const [username, setUsername] = useState('bacsi')
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
      if (loggedInUser && loggedInUser.role !== 'DOCTOR' && loggedInUser.role !== 'ADMIN') {
        logout()
        setError('Tài khoản này không thuộc vai trò Bác sĩ Y học cổ truyền. Lễ tân vui lòng đăng nhập tại cổng Tiếp tân.')
        return
      }
      navigate('/bac-si')
    } catch (err) {
      console.error('Lỗi đăng nhập bác sĩ:', err)
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
    <div style={{ minHeight: '100vh', background: '#F2F7F2', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16, fontFamily: 'inherit' }}>
      <div style={{ width: '100%', maxWidth: 460, background: '#fff', borderRadius: 24, padding: '36px 32px', boxShadow: '0 10px 40px rgba(20,83,45,0.08)', border: '2px solid #BBF7D0' }}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <img src={logo} alt="Chúc Từ" style={{ width: 64, height: 64, objectFit: 'contain', margin: '0 auto 12px' }} />
          <div style={{ display: 'inline-block', background: '#DCFCE7', color: '#14532D', padding: '4px 14px', borderRadius: 999, fontSize: 13, fontWeight: 800, marginBottom: 8 }}>
            👨‍⚕️ CỔNG BÁC SĨ Y HỌC CỔ TRUYỀN
          </div>
          <h1 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 24, fontWeight: 700, color: '#14532D', margin: 0 }}>
            Phòng Khám Chúc Từ
          </h1>
          <div style={{ fontSize: 15, color: '#4B5563', marginTop: 4 }}>
            Bàn Bắt Mạch, Châm Cứu & Kê Đơn Thuốc Nam Dược
          </div>
        </div>

        {/* Khối gợi ý tài khoản mẫu */}
        <div style={{ background: '#F0FDF4', border: '1.5px solid #86EFAC', borderRadius: 14, padding: '12px 16px', marginBottom: 20 }}>
          <div style={{ fontSize: 14, fontWeight: 700, color: '#14532D', marginBottom: 4 }}>
            🔑 TÀI KHOẢN BÁC SĨ MẪU:
          </div>
          <div style={{ fontSize: 15, color: '#1F2937', lineHeight: 1.6 }}>
            <div>Tên đăng nhập: <strong style={{ color: '#15803D', fontFamily: 'monospace', fontSize: 16 }}>bacsi</strong></div>
            <div>Mật khẩu: <strong style={{ color: '#15803D', fontFamily: 'monospace', fontSize: 16 }}>MatKhauAdmin@2026</strong></div>
          </div>
          <button
            type="button"
            onClick={() => fillAccount('bacsi', 'MatKhauAdmin@2026')}
            style={{ marginTop: 8, padding: '6px 12px', borderRadius: 8, background: '#15803D', color: '#fff', border: 'none', fontSize: 13, fontWeight: 600, cursor: 'pointer' }}
          >
            ✓ Tự động điền tài khoản Bác sĩ
          </button>
        </div>

        {error && (
          <div style={{ background: '#FFF1F2', border: '1px solid #FECDD3', borderRadius: 12, padding: '12px 16px', color: '#991B1B', fontSize: 15, marginBottom: 20 }}>
            ❌ {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div>
            <label style={{ display: 'block', fontSize: 15, fontWeight: 600, color: '#1F2937', marginBottom: 6 }}>
              Tên đăng nhập
            </label>
            <input
              type="text"
              value={username}
              onChange={e => setUsername(e.target.value)}
              placeholder="Nhập tên đăng nhập bác sĩ..."
              required
              style={{
                width: '100%', minHeight: 46, padding: '0 14px',
                borderRadius: 12, border: '1.5px solid #BBF7D0',
                fontSize: 16, fontFamily: 'inherit', boxSizing: 'border-box'
              }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: 15, fontWeight: 600, color: '#1F2937', marginBottom: 6 }}>
              Mật khẩu
            </label>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="Nhập mật khẩu..."
              required
              style={{
                width: '100%', minHeight: 46, padding: '0 14px',
                borderRadius: 12, border: '1.5px solid #BBF7D0',
                fontSize: 16, fontFamily: 'inherit', boxSizing: 'border-box'
              }}
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%', minHeight: 48, borderRadius: 12,
              background: '#15803D', color: '#fff', border: 'none',
              fontSize: 17, fontWeight: 700, cursor: 'pointer',
              marginTop: 6, boxShadow: '0 4px 12px rgba(21,128,61,0.25)'
            }}
          >
            {loading ? 'Đang xác thực...' : 'Đăng nhập Bàn Khám Bác sĩ'}
          </button>
        </form>

        {/* Footer chuyển cổng */}
        <div style={{ marginTop: 24, paddingTop: 16, borderTop: '1px solid #E5E7EB', display: 'flex', justifyContent: 'space-between', fontSize: 14 }}>
          <Link to="/tiep-tan/dang-nhap" style={{ color: '#1E40AF', fontWeight: 600, textDecoration: 'none' }}>
            💁‍♀️ Cổng Tiếp tân →
          </Link>
          <Link to="/admin/login" style={{ color: '#991B1B', fontWeight: 600, textDecoration: 'none' }}>
            👑 Cổng Quản trị Admin →
          </Link>
        </div>
      </div>
    </div>
  )
}

