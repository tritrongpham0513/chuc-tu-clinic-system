import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import TopBar from './TopBar'
import GuestHeader from './GuestHeader'
import UserHeader from './UserHeader'
import GuestFooter from './GuestFooter'
import UserFooter from './UserFooter'

// Khung các trang công khai. Khách thấy GuestHeader/GuestFooter,
// người đã đăng nhập thấy UserHeader (menu tài khoản) / UserFooter.
export default function PublicLayout({ active, topbar = false, children }) {
  const { user } = useAuth()
  const { pathname } = useLocation()

  // Đóng menu mobile (checkbox #m-t) mỗi khi chuyển trang
  useEffect(() => {
    const toggle = document.getElementById('m-t')
    if (toggle) toggle.checked = false
  }, [pathname])

  const Header = user ? UserHeader : GuestHeader
  const Footer = user ? UserFooter : GuestFooter

  return (
    <div style={{ width: '100%', minHeight: '100%', background: '#FBF6EC', display: 'flex', flexDirection: 'column' }}>
      {/* CSS responsive điều khiển menu mobile bằng checkbox này — phải là anh em liền trước header */}
      <input type="checkbox" id="m-t" className="m-t" aria-label="Mở hoặc đóng menu" />
      {topbar && <TopBar />}
      <Header active={active} />
      {children}
      <Footer />
    </div>
  )
}
