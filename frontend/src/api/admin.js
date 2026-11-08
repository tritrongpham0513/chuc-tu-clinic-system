import client from './client'

// Dashboard
export const getDashboardStats = () =>
  client.get('/admin/dashboard/stats')

// Appointments
export const getAdminAppointments = (params) =>
  client.get('/admin/appointments', { params })

export const createWalkInAppointment = (data) =>
  client.post('/admin/appointments/walk-in', data)

export const updateAppointmentStatus = (id, status, cancelReason) =>
  client.patch(`/admin/appointments/${id}/status`, null, { params: { status, cancelReason } })

export const checkInAppointment = (id) =>
  client.patch(`/admin/appointments/${id}/check-in`)

export const callInAppointment = (id) =>
  client.patch(`/admin/appointments/${id}/call-in`)

export const getDoctorQueue = (params) =>
  client.get('/admin/appointments/doctor-queue', { params })

export const assignDoctor = (id, doctorId) =>
  client.patch(`/admin/appointments/${id}/assign-doctor`, null, { params: { doctorId } })

// Patients
export const searchAdminPatients = (search) =>
  client.get('/admin/patients', { params: { search } })

export const getAdminPatientById = (id) =>
  client.get(`/admin/patients/${id}`)

export const createAdminPatient = (data) =>
  client.post('/admin/patients', data)

export const updateAdminPatient = (id, data) =>
  client.put(`/admin/patients/${id}`, data)

// Medical Records
export const saveMedicalRecord = (data) =>
  client.post('/admin/medical-records', data)

export const getMedicalRecordByAppointment = (appointmentId) =>
  client.get(`/admin/medical-records/appointment/${appointmentId}`)

export const getMedicalRecordsByPatient = (patientId) =>
  client.get(`/admin/medical-records/patient/${patientId}`)

// Queue TV
export const getQueueToday = () =>
  client.get('/queue/today')

// Articles
export const getAdminArticles = (params) =>
  client.get('/articles/admin/all', { params })

export const createAdminArticle = (data) =>
  client.post('/articles/admin', data)

export const updateAdminArticle = (id, data) =>
  client.put(`/articles/admin/${id}`, data)

export const deleteAdminArticle = (id) =>
  client.delete(`/articles/admin/${id}`)

// Doctors & Services (load danh mục cho select)
export const getAdminDoctors = () =>
  client.get('/doctors')

export const getAdminServices = () =>
  client.get('/services')

// User & Role Management (Độc quyền Admin quản lý tài khoản & phân quyền Tiếp tân / Bác sĩ)
export const getAdminUsers = (params) =>
  client.get('/admin/users', { params })

export const createAdminUser = (data) =>
  client.post('/admin/users', data)

export const updateUserRole = (id, data) =>
  client.patch(`/admin/users/${id}/role`, data)

export const toggleUserStatus = (id, isActive) =>
  client.patch(`/admin/users/${id}/status`, null, { params: { isActive } })

export const resetUserPassword = (id, newPassword) =>
  client.post(`/admin/users/${id}/reset-password`, { newPassword })

