// API phân hệ Bác sĩ Y học cổ truyền (Không dính dáng đến Admin)
import client from './client'

export const getDoctorQueue = (params) =>
  client.get('/doctor/my-queue', { params })

export const callInAppointment = (id) =>
  client.patch(`/doctor/appointments/${id}/call-in`)

export const saveDoctorMedicalRecord = (data) =>
  client.post('/doctor/medical-records', data)

export const getDoctorPatientHistory = (patientId) =>
  client.get(`/doctor/medical-records/patient/${patientId}`)

export const getDoctors = () =>
  client.get('/doctors')

