// API phân hệ Tiếp tân & Điều phối (Không dính dáng đến Admin)
import client from './client'

export const getReceptionistAppointments = (params) =>
  client.get('/receptionist/appointments', { params })

export const createWalkInAppointment = (data) =>
  client.post('/receptionist/walk-in', data)

export const checkInAppointment = (id) =>
  client.patch(`/receptionist/appointments/${id}/check-in`)

export const assignDoctor = (id, doctorId) =>
  client.patch(`/receptionist/appointments/${id}/assign-doctor`, null, { params: { doctorId } })

export const cancelAppointment = (id, cancelReason) =>
  client.patch(`/receptionist/appointments/${id}/cancel`, null, { params: { cancelReason } })

export const getDoctors = () =>
  client.get('/doctors')

export const getServices = () =>
  client.get('/services')

