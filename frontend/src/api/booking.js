// API calls for Module 6 — Đặt lịch khám
import client from './client'

/** Lấy danh sách khung giờ khả dụng theo ngày (YYYY-MM-DD) */
export const getAvailableSlots = (date) =>
  client.get('/booking/slots', { params: { date } })

/** Đặt lịch khám */
export const bookAppointment = (data) =>
  client.post('/booking', data)

/** Danh sách phiếu sắp tới */
export const getMyUpcoming = () =>
  client.get('/booking/my/upcoming')

/** Lịch sử khám */
export const getMyHistory = () =>
  client.get('/booking/my/history')

/** Chi tiết phiếu khám */
export const getMyAppointmentById = (id) =>
  client.get(`/booking/my/${id}`)

/** Hủy phiếu khám */
export const cancelMyAppointment = (id, cancelReason = '') =>
  client.post(`/booking/my/${id}/cancel`, { cancelReason })
