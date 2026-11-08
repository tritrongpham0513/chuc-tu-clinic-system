import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

export const client = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
})

// Tự động gắn Bearer Token vào header nếu đã đăng nhập
client.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('chuc-tu:token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// Xử lý dữ liệu trả về và bắt lỗi thân thiện
client.interceptors.response.use(
  (response) => {
    // Trả về data (chuẩn ApiResponse: { success, statusCode, message, data })
    return response.data
  },
  (error) => {
    const status = error.response?.status
    const serverMessage = error.response?.data?.message

    // Phiên làm việc hết hạn hoặc không có quyền
    if (status === 401) {
      localStorage.removeItem('chuc-tu:token')
      localStorage.removeItem('chuc-tu:user')
    }

    const message = serverMessage || 'Không thể kết nối đến máy chủ phòng khám. Cô chú vui lòng thử lại sau.'
    return Promise.reject(new Error(message))
  },
)

export default client

