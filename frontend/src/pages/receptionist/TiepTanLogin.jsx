import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { logo } from '../../assets'

export default function TiepTanLogin() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const { loginWithPassword, logout } = useAuth()
  const navigate = useNavigate()

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
      if (loggedInUser && loggedInUser.role !== 'STAFF' && loggedInUser.role !== 'ADMIN') {
        logout()
        setError('Tài khoản này không thuộc bộ phận Tiếp tân / Lễ tân. Bác sĩ vui lòng đăng nhập tại cổng Bác sĩ.')
        return
      }
      navigate('/tiep-tan')
    } catch (err) {
      console.error('Lỗi đăng nhập tiếp tân:', err)
      setError(err.message || 'Tên đăng nhập hoặc mật khẩu không chính xác.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #F0F5FA 0%, #E2ECF6 100%)',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '32px 16px',
      fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"
    }}>
      {/* Card đăng nhập chính */}
      <div style={{
        width: '100%',
        maxWidth: 440,
        background: '#FFFFFF',
        borderRadius: 20,
        padding: '36px 32px 28px',
        boxShadow: '0 20px 40px -12px rgba(30, 58, 138, 0.1), 0 0 1px 1px rgba(30, 58, 138, 0.06)',
        border: '1px solid #D1E1F3',
        boxSizing: 'border-box'
      }}>
        {/* Header thương hiệu */}
        <div style={{ textAlign: 'center', marginBottom: 26 }}>
          <img
            src={logo}
            alt="Phòng Khám Chúc Từ"
            style={{ width: 68, height: 68, objectFit: 'contain', margin: '0 auto 12px', display: 'block' }}
          />
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 6,
            background: '#EFF6FF',
            color: '#1D4ED8',
            padding: '4px 12px',
            borderRadius: 999,
            fontSize: 12,
            fontWeight: 700,
            letterSpacing: '0.04em',
            textTransform: 'uppercase',
            marginBottom: 8
          }}>
            <span>💁‍♀️</span> Bàn Tiếp Tân & Đón Tiếp
          </div>
          <h1 style={{
            fontFamily: "'Noto Serif', Georgia, serif",
            fontSize: 24,
            fontWeight: 700,
            color: '#1E3A8A',
            margin: '0 0 4px',
            letterSpacing: '-0.02em'
          }}>
            Phòng Khám Chúc Từ
          </h1>
          <p style={{ fontSize: 14, color: '#4B5563', margin: 0 }}>
            Tiếp nhận người bệnh, Điểm danh & Cấp số thứ tự (STT)
          </p>
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
              Tên đăng nhập
            </label>
            <input
              type="text"
              value={username}
              onChange={e => { setUsername(e.target.value); setError('') }}
              placeholder="Nhập tên đăng nhập tiếp tân..."
              required
              autoFocus
              style={{
                width: '100%',
                height: 48,
                padding: '0 14px',
                borderRadius: 10,
                border: '1.5px solid #BFDBFE',
                fontSize: 15,
                color: '#111827',
                outline: 'none',
                boxSizing: 'border-box',
                transition: 'border-color 0.2s, box-shadow 0.2s'
              }}
              onFocus={e => { e.target.style.borderColor = '#2563EB'; e.target.style.boxShadow = '0 0 0 3px rgba(37,99,235,0.12)' }}
              onBlur={e => { e.target.style.borderColor = '#BFDBFE'; e.target.style.boxShadow = 'none' }}
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
                  border: '1.5px solid #BFDBFE',
                  fontSize: 15,
                  color: '#111827',
                  outline: 'none',
                  boxSizing: 'border-box',
                  transition: 'border-color 0.2s, box-shadow 0.2s'
                }}
                onFocus={e => { e.target.style.borderColor = '#2563EB'; e.target.style.boxShadow = '0 0 0 3px rgba(37,99,235,0.12)' }}
                onBlur={e => { e.target.style.borderColor = '#BFDBFE'; e.target.style.boxShadow = 'none' }}
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
              background: '#1D4ED8',
              color: '#FFFFFF',
              border: 'none',
              fontSize: 16,
              fontWeight: 700,
              cursor: loading ? 'not-allowed' : 'pointer',
              marginTop: 4,
              boxShadow: '0 4px 14px rgba(29, 78, 216, 0.28)',
              transition: 'background 0.2s, transform 0.1s',
              opacity: loading ? 0.7 : 1
            }}
            onMouseEnter={e => { if (!loading) e.target.style.background = '#1E40AF' }}
            onMouseLeave={e => { if (!loading) e.target.style.background = '#1D4ED8' }}
          >
            {loading ? 'Đang xác thực...' : 'Đăng nhập Bàn Tiếp tân'}
          </button>
        </form>

        {/* Chuyển hướng các cổng khác */}
        <div style={{
          marginTop: 26,
          paddingTop: 18,
          borderTop: '1px solid #E5E7EB',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          fontSize: 13,
          color: '#6B7280'
        }}>
          <Link
            to="/bac-si/dang-nhap"
            style={{ color: '#059669', fontWeight: 600, textDecoration: 'none' }}
          >
            👨‍⚕️ Cổng Bác sĩ →
          </Link>
          <Link
            to="/admin/login"
            style={{ color: '#5C3A21', fontWeight: 600, textDecoration: 'none' }}
          >
            👑 Cổng Quản trị Admin →
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
