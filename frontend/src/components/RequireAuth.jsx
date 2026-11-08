import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

// Chặn trang cần đăng nhập: chưa đăng nhập thì chuyển sang trang đăng nhập.
export default function RequireAuth({ to = '/dang-nhap', children }) {
  const { user } = useAuth()
  return user ? children : <Navigate to={to} replace />
}
