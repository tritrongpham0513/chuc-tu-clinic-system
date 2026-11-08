// API calls for Module 1 — Hồ sơ khám gia đình
import client from './client'

/** Lấy danh sách hồ sơ của tài khoản đang đăng nhập */
export const getMyProfiles = () =>
  client.get('/patient-profiles')

/** Chi tiết một hồ sơ */
export const getProfileById = (id) =>
  client.get(`/patient-profiles/${id}`)

/** Thêm hồ sơ người thân */
export const addProfile = (data) =>
  client.post('/patient-profiles', data)

/** Cập nhật hồ sơ */
export const updateProfile = (id, data) =>
  client.put(`/patient-profiles/${id}`, data)

/** Xóa / ngắt liên kết hồ sơ */
export const deleteProfile = (id) =>
  client.delete(`/patient-profiles/${id}`)
