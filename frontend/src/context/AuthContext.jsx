import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import authApi from '../api/auth'

const AuthContext = createContext(null)
const USER_KEY = 'chuc-tu:user'
const TOKEN_KEY = 'chuc-tu:token'

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY)) || null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readStoredUser)
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY) || null)
  const [loading, setLoading] = useState(true)

  // Đồng bộ phiên làm việc khi tải lại trang (nếu có token)
  useEffect(() => {
    async function checkAuth() {
      const storedToken = localStorage.getItem(TOKEN_KEY)
      if (storedToken) {
        try {
          const res = await authApi.getMe()
          if (res?.data) {
            const userData = {
              ...res.data,
              name: res.data.fullName || res.data.username || 'Bệnh nhân',
            }
            setUser(userData)
            localStorage.setItem(USER_KEY, JSON.stringify(userData))
          }
        } catch (err) {
          console.warn('Phiên đăng nhập đã hết hạn hoặc không hợp lệ:', err.message)
          localStorage.removeItem(TOKEN_KEY)
          localStorage.removeItem(USER_KEY)
          setUser(null)
          setToken(null)
        }
      }
      setLoading(false)
    }

    checkAuth()
  }, [])

  function saveAuthData(accessToken, userData) {
    const formattedUser = {
      ...userData,
      name: userData.fullName || userData.username || 'Bệnh nhân',
    }
    localStorage.setItem(TOKEN_KEY, accessToken)
    localStorage.setItem(USER_KEY, JSON.stringify(formattedUser))
    setToken(accessToken)
    setUser(formattedUser)
    return formattedUser
  }

  const value = useMemo(
    () => ({
      user,
      token,
      loading,
      isAuthenticated: !!user,

      // Đăng nhập nhanh bằng SĐT (cho máy đã dùng trước đó)
      async quickLogin(phone) {
        const res = await authApi.quickLogin(phone)
        if (res?.data?.accessToken) {
          return saveAuthData(res.data.accessToken, res.data.user)
        }
        throw new Error(res?.message || 'Đăng nhập không thành công.')
      },

      // Đăng nhập bằng mã OTP (máy mới)
      async loginWithOtp(phone, otpCode) {
        const res = await authApi.loginWithOtp(phone, otpCode)
        if (res?.data?.accessToken) {
          return saveAuthData(res.data.accessToken, res.data.user)
        }
        throw new Error(res?.message || 'Đăng nhập bằng mã xác nhận không thành công.')
      },

      // Đăng nhập bằng SĐT và Mật khẩu
      async loginWithPassword(usernameOrPhone, password) {
        const res = await authApi.login(usernameOrPhone, password)
        if (res?.data?.accessToken) {
          return saveAuthData(res.data.accessToken, res.data.user)
        }
        throw new Error(res?.message || 'Số điện thoại hoặc mật khẩu không chính xác.')
      },

      // Lưu thông tin đăng nhập từ dữ liệu phản hồi API
      loginWithResponse(authData) {
        if (authData?.accessToken) {
          return saveAuthData(authData.accessToken, authData.user)
        }
        throw new Error('Dữ liệu xác thực không hợp lệ.')
      },

      // Đăng ký tài khoản mới
      async register(payload) {
        const res = await authApi.register(payload)
        if (res?.data?.accessToken) {
          return saveAuthData(res.data.accessToken, res.data.user)
        }
        throw new Error(res?.message || 'Đăng ký tài khoản không thành công.')
      },

      // Hàm login cũ (tương thích ngược)
      async login(phone) {
        try {
          return await this.quickLogin(phone)
        } catch (err) {
          // Fallback giả lập nếu server chưa kết nối được
          const next = { name: 'Bệnh nhân', phone }
          localStorage.setItem(USER_KEY, JSON.stringify(next))
          setUser(next)
          return next
        }
      },

      // Đăng xuất
      logout() {
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(USER_KEY)
        setToken(null)
        setUser(null)
      },
    }),
    [user, token, loading],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth phải nằm trong <AuthProvider>')
  return ctx
}
