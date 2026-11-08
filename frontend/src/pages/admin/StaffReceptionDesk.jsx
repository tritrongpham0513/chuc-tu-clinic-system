import { useState, useEffect, useCallback } from 'react'
import AdminLayout from './AdminLayout'
import {
  getReceptionistAppointments as getAdminAppointments,
  createWalkInAppointment,
  checkInAppointment,
  assignDoctor,
  cancelAppointment,
  getDoctors as getAdminDoctors,
  getServices as getAdminServices,
} from '../../api/receptionist'
import { searchAdminPatients } from '../../api/admin'

const STATUS_TABS = [
  { key: 'ALL', label: 'Tất cả hôm nay' },
  { key: 'PENDING', label: 'Chờ đến (Online)' },
  { key: 'CONFIRMED', label: 'Đang ngồi sảnh chờ' },
  { key: 'IN_PROGRESS', label: 'Đang khám' },
  { key: 'COMPLETED', label: 'Đã hoàn tất' },
]

export default function StaffReceptionDesk() {
  const [appointments, setAppointments] = useState([])
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState('ALL')
  const [search, setSearch] = useState('')
  const [doctors, setDoctors] = useState([])
  const [services, setServices] = useState([])

  // Modal tiếp nhận tại quầy (F1)
  const [showWalkIn, setShowWalkIn] = useState(false)
  const [walkInPhone, setWalkInPhone] = useState('')
  const [walkInName, setWalkInName] = useState('')
  const [walkInYear, setWalkInYear] = useState('1960')
  const [walkInGender, setWalkInGender] = useState('MALE')
  const [walkInAddress, setWalkInAddress] = useState('')
  const [walkInDoctor, setWalkInDoctor] = useState('')
  const [walkInService, setWalkInService] = useState('')
  const [walkInSymptoms, setWalkInSymptoms] = useState('')
  const [submittingWalkIn, setSubmittingWalkIn] = useState(false)
  const [phoneLookupMsg, setPhoneLookupMsg] = useState('')

  // Modal đổi bác sĩ
  const [assignTarget, setAssignTarget] = useState(null)
  const [newDoctorId, setNewDoctorId] = useState('')

  // Modal hủy lịch
  const [cancelTarget, setCancelTarget] = useState(null)
  const [cancelReason, setCancelReason] = useState('')

  // Thẻ cấp số STT vừa tạo
  const [justCreatedTicket, setJustCreatedTicket] = useState(null)

  // 1. Tải danh sách lịch hẹn hôm nay
  const loadAppointments = useCallback(async () => {
    try {
      const today = new Date().toISOString().split('T')[0]
      const statusParam = activeTab === 'ALL' ? undefined : activeTab
      const res = await getAdminAppointments({
        date: today,
        status: statusParam,
        search: search.trim() || undefined,
      })
      setAppointments(res.data || [])
    } catch (err) {
      console.error('Lỗi tải danh sách lịch tiếp nhận:', err)
      setAppointments([])
    } finally {
      setLoading(false)
    }
  }, [activeTab, search])

  useEffect(() => {
    loadAppointments()
  }, [loadAppointments])

  // Tải danh mục Bác sĩ & Dịch vụ
  useEffect(() => {
    getAdminDoctors().then(res => setDoctors(res.data || [])).catch(() => {})
    getAdminServices().then(res => setServices(res.data || [])).catch(() => {})
  }, [])

  // 2. Phím tắt F1 (Tiếp nhận nhanh), F2 (Tìm kiếm), Esc (Đóng modal)
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'F1') {
        e.preventDefault()
        setShowWalkIn(true)
      } else if (e.key === 'F2') {
        e.preventDefault()
        const searchInput = document.getElementById('search-patient-input')
        if (searchInput) searchInput.focus()
      } else if (e.key === 'Escape') {
        setShowWalkIn(false)
        setAssignTarget(null)
        setCancelTarget(null)
        setJustCreatedTicket(null)
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [])

  // 3. Tự động tìm bệnh nhân khi nhập đủ 10 số điện thoại ở form Tiếp nhận
  const handlePhoneChange = async (val) => {
    setWalkInPhone(val)
    setPhoneLookupMsg('')
    if (val.trim().length === 10) {
      try {
        const res = await searchAdminPatients(val.trim())
        const found = res.data && res.data.length > 0 ? res.data[0] : null
        if (found) {
          setWalkInName(found.fullName || '')
          setWalkInYear(found.yearOfBirth ? String(found.yearOfBirth) : '1960')
          setWalkInGender(found.gender || 'MALE')
          setWalkInAddress(found.address || '')
          setPhoneLookupMsg(`✓ Đã tìm thấy bệnh nhân cũ: ${found.fullName} (${found.patientCode})`)
        } else {
          setPhoneLookupMsg('ℹ Bệnh nhân mới đến lần đầu')
        }
      } catch {
        // bỏ qua
      }
    }
  }

  // 4. Xử lý tạo ca tiếp nhận tại quầy
  const handleSubmitWalkIn = async (e) => {
    e.preventDefault()
    setSubmittingWalkIn(true)
    try {
      const payload = {
        phone: walkInPhone.trim(),
        fullName: walkInName.trim(),
        yearOfBirth: parseInt(walkInYear, 10),
        gender: walkInGender,
        address: walkInAddress.trim() || undefined,
        doctorId: walkInDoctor ? parseInt(walkInDoctor, 10) : undefined,
        serviceId: walkInService ? parseInt(walkInService, 10) : undefined,
        symptoms: walkInSymptoms.trim() || 'Tiếp nhận trực tiếp tại quầy',
      }
      const res = await createWalkInAppointment(payload)
      setJustCreatedTicket(res.data)
      setShowWalkIn(false)
      // reset form
      setWalkInPhone('')
      setWalkInName('')
      setWalkInAddress('')
      setWalkInSymptoms('')
      setPhoneLookupMsg('')
      loadAppointments()
    } catch (err) {
      alert('Không thể tiếp nhận: ' + (err.message || 'Lỗi hệ thống'))
    } finally {
      setSubmittingWalkIn(false)
    }
  }

  // 5. Check-in một chạm
  const handleCheckIn = async (appointmentId) => {
    try {
      await checkInAppointment(appointmentId)
      loadAppointments()
    } catch (err) {
      alert('Lỗi điểm danh check-in: ' + err.message)
    }
  }

  // 6. Đổi bác sĩ
  const handleAssignDoctor = async (e) => {
    e.preventDefault()
    if (!assignTarget || !newDoctorId) return
    try {
      await assignDoctor(assignTarget.id, parseInt(newDoctorId, 10))
      setAssignTarget(null)
      loadAppointments()
    } catch (err) {
      alert('Lỗi chỉ định bác sĩ: ' + err.message)
    }
  }

  // 7. Hủy lịch
  const handleCancel = async (e) => {
    e.preventDefault()
    if (!cancelTarget) return
    try {
      await cancelAppointment(cancelTarget.id, cancelReason.trim() || 'Lễ tân hủy lịch')
      setCancelTarget(null)
      setCancelReason('')
      loadAppointments()
    } catch (err) {
      alert('Lỗi hủy lịch: ' + err.message)
    }
  }

  return (
    <AdminLayout>
      <div style={{ maxWidth: 1280, margin: '0 auto' }}>
        {/* Thanh tiêu đề & Nút F1 */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20, flexWrap: 'wrap', gap: 14 }}>
          <div>
            <h1 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 26, fontWeight: 700, color: '#2B1A0F', margin: 0 }}>
              💁‍♀️ Bàn Tiếp Tân & Điều Phối Phòng Khám
            </h1>
            <div style={{ fontSize: 15, color: '#6B5E4E', marginTop: 4 }}>
              Hôm nay: Ngày {new Date().toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' })}
            </div>
          </div>

          <div style={{ display: 'flex', gap: 12 }}>
            <button
              onClick={() => setShowWalkIn(true)}
              style={{
                display: 'inline-flex', alignItems: 'center', gap: 8,
                minHeight: 46, padding: '0 20px', borderRadius: 12,
                background: '#2D5A27', color: '#fff', border: 'none',
                fontSize: 16, fontWeight: 700, cursor: 'pointer',
                boxShadow: '0 4px 12px rgba(45,90,39,0.25)'
              }}
            >
              + Tiếp nhận tại quầy (F1)
            </button>
            <a
              href="/tv-phong-cho"
              target="_blank"
              rel="noopener noreferrer"
              style={{
                display: 'inline-flex', alignItems: 'center', gap: 6,
                minHeight: 46, padding: '0 16px', borderRadius: 12,
                background: '#fff', border: '1.5px solid #D97706',
                color: '#D97706', fontSize: 15, fontWeight: 700, textDecoration: 'none'
              }}
            >
              📺 Màn hình TV
            </a>
          </div>
        </div>

        {/* Thanh tìm kiếm & Tabs lọc trạng thái */}
        <div style={{ background: '#fff', padding: '16px 20px', borderRadius: 16, border: '1px solid #E0D3BC', marginBottom: 20 }}>
          <div style={{ display: 'flex', gap: 14, flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between' }}>
            {/* Tabs */}
            <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 4 }}>
              {STATUS_TABS.map(tab => {
                const isActive = activeTab === tab.key
                return (
                  <button
                    key={tab.key}
                    onClick={() => setActiveTab(tab.key)}
                    style={{
                      padding: '8px 16px', borderRadius: 10, border: 'none',
                      background: isActive ? '#2D5A27' : '#F5EFE6',
                      color: isActive ? '#fff' : '#5C3A21',
                      fontSize: 15, fontWeight: isActive ? 700 : 600,
                      cursor: 'pointer', whiteSpace: 'nowrap'
                    }}
                  >
                    {tab.label}
                  </button>
                )
              })}
            </div>

            {/* Ô tìm kiếm nhanh (F2) */}
            <div style={{ minWidth: 280, flex: '1 1 300px' }}>
              <input
                id="search-patient-input"
                type="text"
                value={search}
                onChange={e => setSearch(e.target.value)}
                placeholder="🔍 Tìm nhanh theo SĐT, Tên, Mã phiếu (F2)..."
                style={{
                  width: '100%', minHeight: 42, padding: '0 14px',
                  borderRadius: 10, border: '1.5px solid #E0D3BC',
                  fontSize: 15, outline: 'none', boxSizing: 'border-box'
                }}
              />
            </div>
          </div>
        </div>

        {/* Danh sách ca khám dạng bảng */}
        <div style={{ background: '#fff', borderRadius: 18, border: '1px solid #E0D3BC', overflow: 'hidden', boxShadow: '0 4px 20px rgba(0,0,0,0.03)' }}>
          {loading ? (
            <div style={{ padding: 40, textAlign: 'center', color: '#6B5E4E', fontSize: 17 }}>⏳ Đang tải danh sách ca khám...</div>
          ) : appointments.length === 0 ? (
            <div style={{ padding: 48, textAlign: 'center', color: '#8C7B5E' }}>
              <div style={{ fontSize: 40, marginBottom: 8 }}>📭</div>
              <div style={{ fontSize: 17, fontWeight: 600 }}>Không có lượt khám nào phù hợp bộ lọc</div>
            </div>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', minWidth: 900 }}>
                <thead>
                  <tr style={{ background: '#F5EFE6', color: '#5C3A21', fontSize: 15, borderBottom: '2px solid #E0D3BC' }}>
                    <th style={{ padding: '14px 16px', width: 70, textAlign: 'center' }}>STT</th>
                    <th style={{ padding: '14px 16px' }}>Mã phiếu</th>
                    <th style={{ padding: '14px 16px' }}>Bệnh nhân</th>
                    <th style={{ padding: '14px 16px' }}>Bác sĩ buồng</th>
                    <th style={{ padding: '14px 16px' }}>Khung giờ</th>
                    <th style={{ padding: '14px 16px' }}>Trạng thái</th>
                    <th style={{ padding: '14px 16px', textAlign: 'center' }}>Thao tác tiếp đón</th>
                  </tr>
                </thead>
                <tbody>
                  {appointments.map((a, idx) => {
                    const isPending = a.status === 'PENDING'
                    const isConfirmed = a.status === 'CONFIRMED'
                    const isInProgress = a.status === 'IN_PROGRESS'
                    const isCompleted = a.status === 'COMPLETED'
                    const isCancelled = a.status === 'CANCELLED'

                    return (
                      <tr key={a.id} style={{ borderBottom: '1px solid #F0E8DC', background: idx % 2 === 0 ? '#fff' : '#FDFAF5' }}>
                        {/* STT */}
                        <td style={{ padding: '14px 16px', textAlign: 'center' }}>
                          <span style={{
                            display: 'inline-block', width: 34, height: 34, lineHeight: '34px',
                            borderRadius: '50%', background: isConfirmed ? '#FEF3C7' : '#E5E7EB',
                            color: isConfirmed ? '#92400E' : '#374151', fontWeight: 800, fontSize: 15
                          }}>
                            {a.queueNumber || (idx + 1)}
                          </span>
                        </td>

                        {/* Mã phiếu */}
                        <td style={{ padding: '14px 16px' }}>
                          <span style={{ fontFamily: 'monospace', fontWeight: 700, fontSize: 15, color: '#2B1A0F' }}>
                            {a.bookingCode}
                          </span>
                          <div style={{ fontSize: 12, color: a.source === 'WALK_IN' ? '#2563EB' : '#15803D', fontWeight: 600 }}>
                            {a.source === 'WALK_IN' ? 'Tại quầy' : 'Đặt Online'}
                          </div>
                        </td>

                        {/* Bệnh nhân */}
                        <td style={{ padding: '14px 16px' }}>
                          <div style={{ fontSize: 16, fontWeight: 700, color: '#1F1610' }}>
                            {a.patientName} {a.patientYearOfBirth && <span style={{ color: '#6B5E4E', fontWeight: 400 }}>({new Date().getFullYear() - a.patientYearOfBirth}t)</span>}
                          </div>
                          <div style={{ fontSize: 14, color: '#6B5E4E' }}>
                            📞 {a.patientPhone}
                          </div>
                          {a.symptoms && (
                            <div style={{ fontSize: 13, color: '#854D0E', marginTop: 2 }}>
                              Triệu chứng: {a.symptoms}
                            </div>
                          )}
                        </td>

                        {/* Bác sĩ buồng */}
                        <td style={{ padding: '14px 16px' }}>
                          <div style={{ fontSize: 15, fontWeight: 600, color: '#2B1A0F' }}>
                            {a.doctorName || <span style={{ color: '#9CA3AF' }}>(Chưa phân buồng)</span>}
                          </div>
                          {a.serviceName && (
                            <div style={{ fontSize: 13, color: '#2D5A27' }}>{a.serviceName}</div>
                          )}
                          {!isCompleted && !isCancelled && (
                            <button
                              onClick={() => { setAssignTarget(a); setNewDoctorId(a.doctorId ? String(a.doctorId) : '') }}
                              style={{ border: 'none', background: 'none', color: '#2563EB', fontSize: 13, cursor: 'pointer', padding: 0, textDecoration: 'underline', marginTop: 2 }}
                            >
                              Đổi bác sĩ
                            </button>
                          )}
                        </td>

                        {/* Khung giờ */}
                        <td style={{ padding: '14px 16px', fontSize: 15, color: '#1F1610' }}>
                          {a.timeSlotDisplay || 'Trong ngày'}
                        </td>

                        {/* Trạng thái */}
                        <td style={{ padding: '14px 16px' }}>
                          {isPending && <span style={{ background: '#FEE2E2', color: '#991B1B', padding: '4px 10px', borderRadius: 999, fontSize: 13, fontWeight: 700 }}>Chưa đến quầy</span>}
                          {isConfirmed && <span style={{ background: '#FEF3C7', color: '#92400E', padding: '4px 10px', borderRadius: 999, fontSize: 13, fontWeight: 700 }}>⏳ Đang ngồi chờ</span>}
                          {isInProgress && <span style={{ background: '#DBEAFE', color: '#1E40AF', padding: '4px 10px', borderRadius: 999, fontSize: 13, fontWeight: 700 }}>🩺 Đang khám</span>}
                          {isCompleted && <span style={{ background: '#D1FAE5', color: '#065F46', padding: '4px 10px', borderRadius: 999, fontSize: 13, fontWeight: 700 }}>✓ Hoàn tất</span>}
                          {isCancelled && <span style={{ background: '#F3F4F6', color: '#6B7280', padding: '4px 10px', borderRadius: 999, fontSize: 13, fontWeight: 700 }}>Đã hủy</span>}
                        </td>

                        {/* Nút Thao tác */}
                        <td style={{ padding: '14px 16px', textAlign: 'center' }}>
                          <div style={{ display: 'inline-flex', gap: 8, alignItems: 'center' }}>
                            {isPending && (
                              <button
                                onClick={() => handleCheckIn(a.id)}
                                style={{
                                  background: '#2D5A27', color: '#fff', border: 'none',
                                  padding: '6px 14px', borderRadius: 8, fontSize: 14, fontWeight: 700, cursor: 'pointer'
                                }}
                              >
                                ✓ Check-in
                              </button>
                            )}

                            {!isCompleted && !isCancelled && (
                              <button
                                onClick={() => setCancelTarget(a)}
                                style={{
                                  background: '#FFF1F2', color: '#BE123C', border: '1px solid #FECDD3',
                                  padding: '6px 10px', borderRadius: 8, fontSize: 13, fontWeight: 600, cursor: 'pointer'
                                }}
                              >
                                Hủy lịch
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* MODAL 1: TIẾP NHẬN TẠI QUẦY (F1) */}
        {showWalkIn && (
          <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
            <div style={{ background: '#fff', borderRadius: 20, width: '100%', maxWidth: 540, padding: 28, boxShadow: '0 20px 40px rgba(0,0,0,0.2)', border: '2px solid #E0D3BC' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 }}>
                <h2 style={{ margin: 0, fontSize: 22, color: '#2B1A0F', fontFamily: "'Noto Serif', serif" }}>
                  📋 Tiếp nhận bệnh nhân tại quầy (Walk-in)
                </h2>
                <button onClick={() => setShowWalkIn(false)} style={{ border: 'none', background: 'none', fontSize: 22, cursor: 'pointer' }}>✕</button>
              </div>

              <form onSubmit={handleSubmitWalkIn} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                <div>
                  <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>
                    Số điện thoại (*)
                  </label>
                  <input
                    type="tel"
                    required
                    value={walkInPhone}
                    onChange={e => handlePhoneChange(e.target.value)}
                    placeholder="Nhập 10 chữ số..."
                    style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                  />
                  {phoneLookupMsg && (
                    <div style={{ fontSize: 13, color: phoneLookupMsg.startsWith('✓') ? '#15803D' : '#6B5E4E', marginTop: 4, fontWeight: 600 }}>
                      {phoneLookupMsg}
                    </div>
                  )}
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 100px', gap: 12 }}>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Họ và tên (*)</label>
                    <input
                      type="text"
                      required
                      value={walkInName}
                      onChange={e => setWalkInName(e.target.value)}
                      placeholder="VD: Nguyễn Văn A"
                      style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                    />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Năm sinh</label>
                    <input
                      type="number"
                      value={walkInYear}
                      onChange={e => setWalkInYear(e.target.value)}
                      style={{ width: '100%', minHeight: 44, padding: '0 10px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                    />
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Giới tính</label>
                    <select
                      value={walkInGender}
                      onChange={e => setWalkInGender(e.target.value)}
                      style={{ width: '100%', minHeight: 44, padding: '0 10px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 15 }}
                    >
                      <option value="MALE">Nam</option>
                      <option value="FEMALE">Nữ</option>
                    </select>
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Địa chỉ / Khu vực</label>
                    <input
                      type="text"
                      value={walkInAddress}
                      onChange={e => setWalkInAddress(e.target.value)}
                      placeholder="Quận/Huyện..."
                      style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 15, boxSizing: 'border-box' }}
                    />
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Chỉ định Bác sĩ</label>
                    <select
                      value={walkInDoctor}
                      onChange={e => setWalkInDoctor(e.target.value)}
                      style={{ width: '100%', minHeight: 44, padding: '0 10px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 15 }}
                    >
                      <option value="">-- Bác sĩ trực buồng --</option>
                      {doctors.map(d => (
                        <option key={d.id} value={d.id}>{d.fullName} ({d.specialty})</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Dịch vụ khám</label>
                    <select
                      value={walkInService}
                      onChange={e => setWalkInService(e.target.value)}
                      style={{ width: '100%', minHeight: 44, padding: '0 10px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 15 }}
                    >
                      <option value="">-- Chọn dịch vụ --</option>
                      {services.map(s => (
                        <option key={s.id} value={s.id}>{s.name}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Triệu chứng sơ bộ</label>
                  <input
                    type="text"
                    value={walkInSymptoms}
                    onChange={e => setWalkInSymptoms(e.target.value)}
                    placeholder="VD: Đau lưng mạn tính, mất ngủ kéo dài..."
                    style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 15, boxSizing: 'border-box' }}
                  />
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 12 }}>
                  <button
                    type="button"
                    onClick={() => setShowWalkIn(false)}
                    style={{ padding: '10px 18px', borderRadius: 10, border: '1px solid #D1D5DB', background: '#F3F4F6', fontSize: 15, cursor: 'pointer' }}
                  >
                    Hủy (Esc)
                  </button>
                  <button
                    type="submit"
                    disabled={submittingWalkIn}
                    style={{
                      padding: '10px 22px', borderRadius: 10, border: 'none',
                      background: '#2D5A27', color: '#fff', fontSize: 16, fontWeight: 700, cursor: 'pointer'
                    }}
                  >
                    {submittingWalkIn ? 'Đang tạo số...' : '✓ Cấp số thứ tự khám'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* THẺ IN SỐ THỨ TỰ VỪA TIẾP NHẬN */}
        {justCreatedTicket && (
          <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', zIndex: 1001, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
            <div style={{ background: '#fff', borderRadius: 24, width: '100%', maxWidth: 420, padding: 32, textAlign: 'center', border: '3px solid #2D5A27', boxShadow: '0 25px 50px rgba(0,0,0,0.25)' }}>
              <div style={{ fontSize: 18, color: '#2D5A27', fontWeight: 700, marginBottom: 4 }}>PHÒNG KHÁM CHÚC TỪ</div>
              <div style={{ fontSize: 14, color: '#6B5E4E', marginBottom: 16 }}>PHIẾU KHÁM BỆNH Y HỌC CỔ TRUYỀN</div>

              <div style={{ background: '#F5EFE6', borderRadius: 16, padding: '20px 16px', marginBottom: 20 }}>
                <div style={{ fontSize: 16, color: '#5C3A21', fontWeight: 600 }}>SỐ THỨ TỰ KHÁM</div>
                <div style={{ fontSize: 56, fontWeight: 900, color: '#2D5A27', lineHeight: 1.2 }}>
                  {justCreatedTicket.queueNumber || '01'}
                </div>
                <div style={{ fontSize: 16, fontWeight: 700, color: '#2B1A0F', marginTop: 4 }}>
                  {justCreatedTicket.patientName}
                </div>
                <div style={{ fontSize: 14, color: '#6B5E4E' }}>Mã phiếu: <strong>{justCreatedTicket.bookingCode}</strong></div>
                {justCreatedTicket.doctorName && (
                  <div style={{ fontSize: 14, color: '#1E40AF', marginTop: 4, fontWeight: 600 }}>
                    Buồng khám: {justCreatedTicket.doctorName}
                  </div>
                )}
              </div>

              <p style={{ fontSize: 15, color: '#4B5563', lineHeight: 1.5, marginBottom: 20 }}>
                Xin mời cô chú ngồi nghỉ tại sảnh chờ.<br />Khi số thứ tự hiển thị trên màn hình TV, xin mời cô chú bước vào phòng khám.
              </p>

              <button
                onClick={() => setJustCreatedTicket(null)}
                style={{
                  width: '100%', minHeight: 48, borderRadius: 12,
                  background: '#2D5A27', color: '#fff', border: 'none',
                  fontSize: 17, fontWeight: 700, cursor: 'pointer'
                }}
              >
                Đã gửi phiếu cho người bệnh (Esc)
              </button>
            </div>
          </div>
        )}

        {/* MODAL 2: ĐỔI BÁC SĨ BUỒNG */}
        {assignTarget && (
          <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
            <div style={{ background: '#fff', borderRadius: 18, width: '100%', maxWidth: 440, padding: 24 }}>
              <h3 style={{ margin: '0 0 14px', fontSize: 19, color: '#2B1A0F' }}>
                Đổi Bác sĩ cho bệnh nhân: {assignTarget.patientName}
              </h3>
              <form onSubmit={handleAssignDoctor}>
                <select
                  required
                  value={newDoctorId}
                  onChange={e => setNewDoctorId(e.target.value)}
                  style={{ width: '100%', minHeight: 44, padding: '0 10px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, marginBottom: 16 }}
                >
                  <option value="">-- Chọn bác sĩ tiếp nhận --</option>
                  {doctors.map(d => (
                    <option key={d.id} value={d.id}>{d.fullName} ({d.specialty})</option>
                  ))}
                </select>
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
                  <button type="button" onClick={() => setAssignTarget(null)} style={{ padding: '8px 14px', borderRadius: 8, border: '1px solid #ccc' }}>Hủy</button>
                  <button type="submit" style={{ padding: '8px 18px', borderRadius: 8, border: 'none', background: '#2D5A27', color: '#fff', fontWeight: 700 }}>Xác nhận đổi</button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* MODAL 3: HỦY LỊCH HẸN */}
        {cancelTarget && (
          <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
            <div style={{ background: '#fff', borderRadius: 18, width: '100%', maxWidth: 440, padding: 24 }}>
              <h3 style={{ margin: '0 0 10px', fontSize: 19, color: '#991B1B' }}>
                Xác nhận hủy phiếu khám: {cancelTarget.bookingCode}
              </h3>
              <p style={{ fontSize: 15, color: '#4B5563', margin: '0 0 14px' }}>
                Bệnh nhân: <strong>{cancelTarget.patientName}</strong>
              </p>
              <form onSubmit={handleCancel}>
                <textarea
                  required
                  value={cancelReason}
                  onChange={e => setCancelReason(e.target.value)}
                  placeholder="Nhập lý do hủy (VD: Bệnh nhân báo bận, hẹn ngày khác...)"
                  rows={3}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 15, boxSizing: 'border-box', marginBottom: 16 }}
                />
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
                  <button type="button" onClick={() => setCancelTarget(null)} style={{ padding: '8px 14px', borderRadius: 8, border: '1px solid #ccc' }}>Đóng</button>
                  <button type="submit" style={{ padding: '8px 18px', borderRadius: 8, border: 'none', background: '#DC2626', color: '#fff', fontWeight: 700 }}>Xác nhận hủy</button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  )
}

