import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { logo } from '../../assets'

export default function NoiBoLogin() {
  const [roleTab, setRoleTab] = useState('STAFF') // 'STAFF' | 'DOCTOR'
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const { loginWithPassword, logout } = useAuth()
  const navigate = useNavigate()

  const switchTab = (tab) => {
    setRoleTab(tab)
    setError('')
    setUsername('')
    setPassword('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!username.trim() || !password) {
      setError('Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.')
      return
    }

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
      setError(err.message || 'Tên đăng nhập hoặc mật khẩu không chính xác.')
    } finally {
      setLoading(false)
    }
  }

  const isStaff = roleTab === 'STAFF'
  const primaryColor = isStaff ? '#1E40AF' : '#15803D'
  const borderColor = isStaff ? '#BFDBFE' : '#BBF7D0'
  const focusShadow = isStaff ? 'rgba(30, 64, 175, 0.12)' : 'rgba(21, 128, 61, 0.12)'

  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #F3F6F5 0%, #E3EBE7 100%)',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '32px 16px',
      fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"
    }}>
      {/* Card đăng nhập */}
      <div style={{
        width: '100%',
        maxWidth: 450,
        background: '#FFFFFF',
        borderRadius: 20,
        padding: '36px 32px 28px',
        boxShadow: '0 20px 40px -12px rgba(0, 0, 0, 0.08), 0 0 1px 1px rgba(0, 0, 0, 0.05)',
        border: '1px solid #D6E0DC',
        boxSizing: 'border-box'
      }}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: 22 }}>
          <img
            src={logo}
            alt="Phòng Khám Chúc Từ"
            style={{ width: 68, height: 68, objectFit: 'contain', margin: '0 auto 12px', display: 'block' }}
          />
          <h1 style={{
            fontFamily: "'Noto Serif', Georgia, serif",
            fontSize: 24,
            fontWeight: 700,
            color: '#1B3C2D',
            margin: '0 0 4px',
            letterSpacing: '-0.02em'
          }}>
            Phòng Khám Chúc Từ
          </h1>
          <p style={{ fontSize: 14, color: '#4B5563', margin: 0 }}>
            Cổng Đăng Nhập Nhân Sự Nội Bộ
          </p>
        </div>

        {/* Tab chọn Tiếp tân hay Bác sĩ */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: 6,
          padding: 4,
          background: '#F1F5F3',
          borderRadius: 12,
          marginBottom: 22
        }}>
          <button
            type="button"
            onClick={() => switchTab('STAFF')}
            style={{
              padding: '10px 0',
              borderRadius: 8,
              border: 'none',
              background: isStaff ? '#1E40AF' : 'transparent',
              color: isStaff ? '#FFFFFF' : '#4B5563',
              fontSize: 14,
              fontWeight: 700,
              cursor: 'pointer',
              transition: 'all 0.2s',
              boxShadow: isStaff ? '0 2px 6px rgba(30,64,175,0.25)' : 'none'
            }}
          >
            💁‍♀️ Bàn Tiếp Tân
          </button>
          <button
            type="button"
            onClick={() => switchTab('DOCTOR')}
            style={{
              padding: '10px 0',
              borderRadius: 8,
              border: 'none',
              background: !isStaff ? '#15803D' : 'transparent',
              color: !isStaff ? '#FFFFFF' : '#4B5563',
              fontSize: 14,
              fontWeight: 700,
              cursor: 'pointer',
              transition: 'all 0.2s',
              boxShadow: !isStaff ? '0 2px 6px rgba(21,128,61,0.25)' : 'none'
            }}
          >
            👨‍⚕️ Bác Sĩ YHCT
          </button>
        </div>

        {/* Thông báo lỗi */}
        {error && (
          <div style={{
            background: '#FEF2F2',
            border: '1px solid #FECACA',
            borderRadius: 12,
            padding: '12px 14px',
            color: '#991B1B',
            fontSize: 14,
            lineHeight: 1.5,
            marginBottom: 20,
            display: 'flex',
            alignItems: 'flex-start',
            gap: 8
          }}>
            <span style={{ fontSize: 16 }}>⚠️</span>
            <span style={{ flex: 1 }}>{error}</span>
          </div>
        )}

        {/* Form đăng nhập */}
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
          <div>
            <label style={{ display: 'block', fontSize: 14, fontWeight: 600, color: '#1F2937', marginBottom: 6 }}>
              Tên đăng nhập {isStaff ? 'tiếp tân' : 'bác sĩ'}
            </label>
            <input
              type="text"
              value={username}
              onChange={e => { setUsername(e.target.value); setError('') }}
              placeholder={isStaff ? 'Nhập tên đăng nhập tiếp tân...' : 'Nhập tên đăng nhập bác sĩ...'}
              required
              autoFocus
              style={{
                width: '100%',
                height: 48,
                padding: '0 14px',
                borderRadius: 10,
                border: `1.5px solid ${borderColor}`,
                fontSize: 15,
                color: '#111827',
                outline: 'none',
                boxSizing: 'border-box',
                transition: 'border-color 0.2s, box-shadow 0.2s'
              }}
              onFocus={e => { e.target.style.borderColor = primaryColor; e.target.style.boxShadow = `0 0 0 3px ${focusShadow}` }}
              onBlur={e => { e.target.style.borderColor = borderColor; e.target.style.boxShadow = 'none' }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: 14, fontWeight: 600, color: '#1F2937', marginBottom: 6 }}>
              Mật khẩu
            </label>
            <div style={{ position: 'relative' }}>
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={e => { setPassword(e.target.value); setError('') }}
                placeholder="Nhập mật khẩu..."
                required
                style={{
                  width: '100%',
                  height: 48,
                  padding: '0 44px 0 14px',
                  borderRadius: 10,
                  border: `1.5px solid ${borderColor}`,
                  fontSize: 15,
                  color: '#111827',
                  outline: 'none',
                  boxSizing: 'border-box',
                  transition: 'border-color 0.2s, box-shadow 0.2s'
                }}
                onFocus={e => { e.target.style.borderColor = primaryColor; e.target.style.boxShadow = `0 0 0 3px ${focusShadow}` }}
                onBlur={e => { e.target.style.borderColor = borderColor; e.target.style.boxShadow = 'none' }}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                tabIndex={-1}
                title={showPassword ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu'}
                style={{
                  position: 'absolute',
                  right: 8,
                  top: '50%',
                  transform: 'translateY(-50%)',
                  background: 'transparent',
                  border: 'none',
                  color: '#6B7280',
                  padding: 8,
                  fontSize: 16,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}
              >
                {showPassword ? '🙈' : '👁️'}
              </button>
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%',
              height: 48,
              borderRadius: 10,
              background: primaryColor,
              color: '#FFFFFF',
              border: 'none',
              fontSize: 16,
              fontWeight: 700,
              cursor: loading ? 'not-allowed' : 'pointer',
              marginTop: 4,
              boxShadow: `0 4px 14px ${focusShadow}`,
              transition: 'background 0.2s, transform 0.1s',
              opacity: loading ? 0.7 : 1
            }}
          >
            {loading ? 'Đang xác thực...' : `Đăng nhập ${isStaff ? 'Bàn Tiếp Tân' : 'Bàn Bác Sĩ'}`}
          </button>
        </form>

        {/* Link chuyển Admin */}
        <div style={{
          marginTop: 26,
          paddingTop: 18,
          borderTop: '1px solid #E5E7EB',
          textAlign: 'center',
          fontSize: 13
        }}>
          <Link
            to="/admin/login"
            style={{ color: '#5C3A21', fontWeight: 600, textDecoration: 'none' }}
          >
            👑 Dành cho Quản trị viên Admin →
          </Link>
        </div>
      </div>

      {/* Nút về trang chủ */}
      <div style={{ marginTop: 20, textAlign: 'center' }}>
        <Link
          to="/"
          style={{
            color: '#4B5563',
            fontSize: 14,
            fontWeight: 500,
            textDecoration: 'none',
            display: 'inline-flex',
            alignItems: 'center',
            gap: 6
          }}
        >
          <span>←</span> Quay lại Trang chủ phòng khám
        </Link>
      </div>
    </div>
  )
}
