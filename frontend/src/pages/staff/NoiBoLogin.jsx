import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { logo } from '../../assets'

export default function NoiBoLogin() {
  const [roleTab, setRoleTab] = useState('STAFF') // 'STAFF' | 'DOCTOR'
  const [username, setUsername] = useState('letan')
  const [password, setPassword] = useState('MatKhauAdmin@2026')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const { loginWithPassword, logout } = useAuth()
  const navigate = useNavigate()

  const switchTab = (tab) => {
    setRoleTab(tab)
    setError('')
    if (tab === 'STAFF') {
      setUsername('letan')
      setPassword('MatKhauAdmin@2026')
    } else {
      setUsername('bacsi')
      setPassword('MatKhauAdmin@2026')
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const loggedInUser = await loginWithPassword(username.trim(), password)
      if (loggedInUser && loggedInUser.role !== 'STAFF' && loggedInUser.role !== 'DOCTOR' && loggedInUser.role !== 'ADMIN') {
        logout()
        setError('Tài khoản bệnh nhân không được phép truy cập cổng nhân sự nội bộ.')
        return
      }

      if (loggedInUser.role === 'STAFF') {
        navigate('/tiep-tan')
      } else if (loggedInUser.role === 'DOCTOR') {
        navigate('/bac-si')
      } else {
        navigate('/admin')
      }
    } catch (err) {
      console.error('Lỗi đăng nhập nội bộ:', err)
      setError(err.message || 'Đăng nhập không thành công. Vui lòng kiểm tra lại tài khoản hoặc mật khẩu.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ minHeight: '100vh', background: '#F4F7F6', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16, fontFamily: 'inherit' }}>
      <div style={{ width: '100%', maxWidth: 480, background: '#fff', borderRadius: 24, padding: '36px 32px', boxShadow: '0 10px 40px rgba(0,0,0,0.06)', border: '2px solid #D6E0DC' }}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: 20 }}>
          <img src={logo} alt="Chúc Từ" style={{ width: 64, height: 64, objectFit: 'contain', margin: '0 auto 10px' }} />
          <h1 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 24, fontWeight: 700, color: '#1B3C2D', margin: 0 }}>
            Phòng Khám Chúc Từ
          </h1>
          <div style={{ fontSize: 15, color: '#4B5563', marginTop: 4 }}>
            Cổng Đăng Nhập Nhân Sự (Tiếp Tân & Bác Sĩ)
          </div>
        </div>

        {/* Tab chọn Tiếp tân hay Bác sĩ */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, padding: 4, background: '#EDF2F0', borderRadius: 12, marginBottom: 20 }}>
          <button
            type="button"
            onClick={() => switchTab('STAFF')}
            style={{
              padding: '10px 0', borderRadius: 10, border: 'none',
              background: roleTab === 'STAFF' ? '#1E40AF' : 'transparent',
              color: roleTab === 'STAFF' ? '#fff' : '#374151',
              fontSize: 15, fontWeight: 700, cursor: 'pointer',
              boxShadow: roleTab === 'STAFF' ? '0 2px 8px rgba(30,64,175,0.2)' : 'none'
            }}
          >
            💁‍♀️ Bàn Tiếp Tân
          </button>
          <button
            type="button"
            onClick={() => switchTab('DOCTOR')}
            style={{
              padding: '10px 0', borderRadius: 10, border: 'none',
              background: roleTab === 'DOCTOR' ? '#15803D' : 'transparent',
              color: roleTab === 'DOCTOR' ? '#fff' : '#374151',
              fontSize: 15, fontWeight: 700, cursor: 'pointer',
              boxShadow: roleTab === 'DOCTOR' ? '0 2px 8px rgba(21,128,61,0.2)' : 'none'
            }}
          >
            👨‍⚕️ Bác Sĩ YHCT
          </button>
        </div>

        {/* Khối gợi ý tài khoản */}
        <div style={{
          background: roleTab === 'STAFF' ? '#EFF6FF' : '#F0FDF4',
          border: `1.5px solid ${roleTab === 'STAFF' ? '#93C5FD' : '#86EFAC'}`,
          borderRadius: 14, padding: '12px 16px', marginBottom: 20
        }}>
          <div style={{ fontSize: 14, fontWeight: 700, color: roleTab === 'STAFF' ? '#1E40AF' : '#14532D', marginBottom: 4 }}>
            🔑 TÀI KHOẢN MẪU:
          </div>
          <div style={{ fontSize: 15, color: '#1F2937', lineHeight: 1.6 }}>
            <div>Tên đăng nhập: <strong style={{ color: roleTab === 'STAFF' ? '#1D4ED8' : '#15803D', fontFamily: 'monospace', fontSize: 16 }}>{username}</strong></div>
            <div>Mật khẩu: <strong style={{ color: roleTab === 'STAFF' ? '#1D4ED8' : '#15803D', fontFamily: 'monospace', fontSize: 16 }}>MatKhauAdmin@2026</strong></div>
          </div>
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
              placeholder="Nhập tên đăng nhập..."
              required
              style={{
                width: '100%', minHeight: 46, padding: '0 14px',
                borderRadius: 12, border: '1.5px solid #CBD5E1',
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
                borderRadius: 12, border: '1.5px solid #CBD5E1',
                fontSize: 16, fontFamily: 'inherit', boxSizing: 'border-box'
              }}
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%', minHeight: 48, borderRadius: 12,
              background: roleTab === 'STAFF' ? '#1E40AF' : '#15803D',
              color: '#fff', border: 'none', fontSize: 17, fontWeight: 700,
              cursor: 'pointer', marginTop: 6,
              boxShadow: roleTab === 'STAFF' ? '0 4px 12px rgba(30,64,175,0.25)' : '0 4px 12px rgba(21,128,61,0.25)'
            }}
          >
            {loading ? 'Đang xác thực...' : `Đăng nhập ${roleTab === 'STAFF' ? 'Bàn Tiếp Tân' : 'Bác Sĩ YHCT'}`}
          </button>
        </form>

        {/* Footer chuyển Admin */}
        <div style={{ marginTop: 24, paddingTop: 16, borderTop: '1px solid #E5E7EB', textAlign: 'center', fontSize: 14 }}>
          <span style={{ color: '#6B7280' }}>Bạn là Quản trị viên phòng khám? </span>
          <Link to="/admin/login" style={{ color: '#991B1B', fontWeight: 700, textDecoration: 'none' }}>
            Đăng nhập Quản trị Admin →
          </Link>
        </div>
      </div>
    </div>
  )
}

