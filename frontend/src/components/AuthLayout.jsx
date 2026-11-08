import AuthHeader from './AuthHeader'
import AuthFooter from './AuthFooter'

// Khung các trang đăng nhập / đăng ký. `overlay` là hộp thoại OTP phủ lên trang.
export default function AuthLayout({ mode, overlay = null, children }) {
  return (
    <div
      style={{
        position: 'relative',
        width: '100%',
        minHeight: '100%',
        background: '#FBF6EC',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <AuthHeader mode={mode} />
      {children}
      <AuthFooter />
      {overlay}
    </div>
  )
}
