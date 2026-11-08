import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { logo } from '../../assets'

export default function TiepTanLogin() {
  const [username, setUsername] = useState('letan')
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
      if (loggedInUser && loggedInUser.role !== 'STAFF' && loggedInUser.role !== 'ADMIN') {
        logout()
        setError('Tài khoản này không thuộc bộ phận Tiếp tân / Lễ tân. Bác sĩ vui lòng đăng nhập tại cổng Bác sĩ.')
        return
      }
      navigate('/tiep-tan')
    } catch (err) {
      console.error('Lỗi đăng nhập tiếp tân:', err)
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
    <div style={{ minHeight: '100vh', background: '#F0F4F8', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16, fontFamily: 'inherit' }}>
      <div style={{ width: '100%', maxWidth: 460, background: '#fff', borderRadius: 24, padding: '36px 32px', boxShadow: '0 10px 40px rgba(30,58,138,0.08)', border: '2px solid #BFDBFE' }}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <img src={logo} alt="Chúc Từ" style={{ width: 64, height: 64, objectFit: 'contain', margin: '0 auto 12px' }} />
          <div style={{ display: 'inline-block', background: '#DBEAFE', color: '#1E40AF', padding: '4px 14px', borderRadius: 999, fontSize: 13, fontWeight: 800, marginBottom: 8 }}>
            💁‍♀️ CỔNG TIẾP TÂN & ĐIỀU PHỐI
          </div>
          <h1 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 24, fontWeight: 700, color: '#1E3A8A', margin: 0 }}>
            Phòng Khám Chúc Từ
          </h1>
          <div style={{ fontSize: 15, color: '#4B5563', marginTop: 4 }}>
            Bàn Đón Tiếp, Điểm Danh & Cấp Số Thứ Tự (STT)
          </div>
        </div>

        {/* Khối gợi ý tài khoản mẫu */}
        <div style={{ background: '#EFF6FF', border: '1.5px solid #93C5FD', borderRadius: 14, padding: '12px 16px', marginBottom: 20 }}>
          <div style={{ fontSize: 14, fontWeight: 700, color: '#1E40AF', marginBottom: 4 }}>
            🔑 TÀI KHOẢN TIẾP TÂN MẪU:
          </div>
          <div style={{ fontSize: 15, color: '#1F2937', lineHeight: 1.6 }}>
            <div>Tên đăng nhập: <strong style={{ color: '#1D4ED8', fontFamily: 'monospace', fontSize: 16 }}>letan</strong></div>
            <div>Mật khẩu: <strong style={{ color: '#1D4ED8', fontFamily: 'monospace', fontSize: 16 }}>MatKhauAdmin@2026</strong></div>
          </div>
          <button
            type="button"
            onClick={() => fillAccount('letan', 'MatKhauAdmin@2026')}
            style={{ marginTop: 8, padding: '6px 12px', borderRadius: 8, background: '#1D4ED8', color: '#fff', border: 'none', fontSize: 13, fontWeight: 600, cursor: 'pointer' }}
          >
            ✓ Tự động điền tài khoản Tiếp tân
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
              placeholder="Nhập tên đăng nhập tiếp tân..."
              required
              style={{
                width: '100%', minHeight: 46, padding: '0 14px',
                borderRadius: 12, border: '1.5px solid #BFDBFE',
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
                borderRadius: 12, border: '1.5px solid #BFDBFE',
                fontSize: 16, fontFamily: 'inherit', boxSizing: 'border-box'
              }}
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%', minHeight: 48, borderRadius: 12,
              background: '#1D4ED8', color: '#fff', border: 'none',
              fontSize: 17, fontWeight: 700, cursor: 'pointer',
              marginTop: 6, boxShadow: '0 4px 12px rgba(29,78,216,0.25)'
            }}
          >
            {loading ? 'Đang xác thực...' : 'Đăng nhập Bàn Tiếp tân'}
          </button>
        </form>

        {/* Footer chuyển cổng */}
        <div style={{ marginTop: 24, paddingTop: 16, borderTop: '1px solid #E5E7EB', display: 'flex', justifyContent: 'space-between', fontSize: 14 }}>
          <Link to="/bac-si/dang-nhap" style={{ color: '#047857', fontWeight: 600, textDecoration: 'none' }}>
            👨‍⚕️ Cổng Bác sĩ →
          </Link>
          <Link to="/admin/login" style={{ color: '#991B1B', fontWeight: 600, textDecoration: 'none' }}>
            👑 Cổng Quản trị Admin →
          </Link>
        </div>
      </div>
    </div>
  )
}

