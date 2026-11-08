import client from './client'

export const authApi = {
  /**
   * Gửi mã OTP xác thực qua tin nhắn SMS
   * @param {string} phone Số điện thoại (10 chữ số)
   * @param {'REGISTER' | 'LOGIN'} otpType Loại xác thực
   */
  async sendOtp(phone, otpType = 'REGISTER') {
    return client.post('/auth/send-otp', {
      phone,
      otpType,
    })
  },

  /**
   * Xác thực mã OTP 6 số
   * @param {string} phone Số điện thoại
   * @param {string} otpCode Mã 6 chữ số
   * @param {'REGISTER' | 'LOGIN'} otpType
   */
  async verifyOtp(phone, otpCode, otpType = 'REGISTER') {
    return client.post('/auth/verify-otp', {
      phone,
      otpCode,
      otpType,
    })
  },

  /**
   * Đăng ký tài khoản bệnh nhân mới
   * @param {object} payload { fullName, phone, yearOfBirth, gender, address, otpCode, password? }
   */
  async register(payload) {
    return client.post('/auth/register', payload)
  },

  /**
   * Đăng nhập bằng Số điện thoại + Mật khẩu
   * @param {string} usernameOrPhone
   * @param {string} password
   */
  async login(usernameOrPhone, password) {
    return client.post('/auth/login', {
      usernameOrPhone,
      password,
    })
  },

  /**
   * Đăng nhập máy mới bằng Số điện thoại + Mã OTP 6 số
   * @param {string} phone
   * @param {string} otpCode
   */
  async loginWithOtp(phone, otpCode) {
    return client.post('/auth/login-otp', {
      phone,
      otpCode,
    })
  },

  /**
   * Đăng nhập nhanh trên máy đã ghi nhớ bằng Số điện thoại
   * @param {string} phone
   */
  async quickLogin(phone) {
    return client.post('/auth/quick-login', {
      phone,
      otpType: 'LOGIN',
    })
  },

  /**
   * Lấy thông tin tài khoản hiện tại từ JWT
   */
  async getMe() {
    return client.get('/auth/me')
  },
}

export const login = (usernameOrPhone, password) => {
  if (typeof usernameOrPhone === 'object') {
    return authApi.login(usernameOrPhone.username || usernameOrPhone.usernameOrPhone || usernameOrPhone.phone, usernameOrPhone.password)
  }
  return authApi.login(usernameOrPhone, password)
}

export const sendOtp = authApi.sendOtp
export const verifyOtp = authApi.verifyOtp
export const register = authApi.register
export const loginWithOtp = authApi.loginWithOtp
export const quickLogin = authApi.quickLogin
export const getMe = authApi.getMe

export default authApi
