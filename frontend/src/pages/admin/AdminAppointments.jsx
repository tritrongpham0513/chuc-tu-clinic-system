import { useState, useEffect, useCallback } from 'react'
import AdminLayout from './AdminLayout'
import {
  getAdminAppointments,
  createWalkInAppointment,
  updateAppointmentStatus,
  saveMedicalRecord,
  getAdminDoctors,
  getAdminServices
} from '../../api/admin'
import { getAvailableSlots } from '../../api/booking'
import Icon from '../../components/Icon'

const STATUS_FILTERS = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'PENDING', label: 'Chờ tiếp nhận' },
  { value: 'CONFIRMED', label: 'Đang ngồi chờ' },
  { value: 'IN_PROGRESS', label: 'Đang trong phòng khám' },
  { value: 'COMPLETED', label: 'Đã hoàn thành' },
  { value: 'CANCELLED', label: 'Đã hủy' },
]

export default function AdminAppointments() {
  const [appointments, setAppointments] = useState([])
  const [loading, setLoading] = useState(true)
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10))
  const [status, setStatus] = useState('')
  const [search, setSearch] = useState('')

  // Danh mục bác sĩ, dịch vụ, khung giờ
  const [doctors, setDoctors] = useState([])
  const [services, setServices] = useState([])
  const [slots, setSlots] = useState([])

  // Modal tiếp nhận tại quầy (walk-in)
  const [showWalkIn, setShowWalkIn] = useState(false)
  const [walkInForm, setWalkInForm] = useState({
    fullName: '', phone: '', yearOfBirth: 1970, gender: 'MALE',
    address: '', timeSlotId: '', doctorId: '', serviceId: '', symptoms: ''
  })
  const [submittingWalkIn, setSubmittingWalkIn] = useState(false)

  // Modal ghi bệnh án
  const [recordTarget, setRecordTarget] = useState(null)
  const [recordForm, setRecordForm] = useState({
    pulseBp: 'Mạch trầm tế, HA 120/80 mmHg',
    diagnosis: '',
    treatment: 'Châm cứu các huyệt: Hợp cốc, Túc tam lý, Thái xung. Dùng bài thuốc thảo mộc bồi bổ khí huyết.',
    prescription: 'Độc hoạt 12g, Tang ký sinh 16g, Phòng phong 10g, Đương quy 12g, Bạch thược 12g.\nSắc uống ngày 1 thang, chia làm 2 lần sáng - chiều sau ăn.',
    advice: 'Kiêng ăn đồ lạnh, cay nóng. Vận động nhẹ nhàng mỗi sáng 20 phút.',
    followUpDate: ''
  })
  const [savingRecord, setSavingRecord] = useState(false)

  // Toast
  const [toast, setToast] = useState(null)
  const showToast = (msg, type = 'ok') => {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getAdminAppointments({ date, status: status || undefined, search: search.trim() || undefined })
      setAppointments(res.data || [])
    } catch {
      setAppointments([])
    } finally {
      setLoading(false)
    }
  }, [date, status, search])

  useEffect(() => { loadData() }, [loadData])

  useEffect(() => {
    getAdminDoctors().then(r => setDoctors(r.data || [])).catch(() => {})
    getAdminServices().then(r => setServices(r.data || [])).catch(() => {})
    getAvailableSlots(date).then(r => {
      setSlots(r.data || [])
      if (r.data?.length > 0 && !walkInForm.timeSlotId) {
        setWalkInForm(f => ({ ...f, timeSlotId: r.data[0].id }))
      }
    }).catch(() => {})
  }, [date])

  const handleStatusChange = async (id, newStatus, cancelReason) => {
    try {
      await updateAppointmentStatus(id, newStatus, cancelReason)
      showToast('Cập nhật trạng thái thành công')
      loadData()
    } catch (err) {
      showToast(err?.response?.data?.message || 'Có lỗi xảy ra', 'err')
    }
  }

  const handleWalkInSubmit = async (e) => {
    e.preventDefault()
    setSubmittingWalkIn(true)
    try {
      await createWalkInAppointment({
        ...walkInForm,
        yearOfBirth: parseInt(walkInForm.yearOfBirth, 10),
        timeSlotId: parseInt(walkInForm.timeSlotId, 10),
        doctorId: walkInForm.doctorId ? parseInt(walkInForm.doctorId, 10) : undefined,
        serviceId: walkInForm.serviceId ? parseInt(walkInForm.serviceId, 10) : undefined,
      })
      showToast('Đã tiếp nhận bệnh nhân tại quầy!')
      setShowWalkIn(false)
      loadData()
    } catch (err) {
      showToast(err?.response?.data?.message || 'Có lỗi xảy ra', 'err')
    } finally {
      setSubmittingWalkIn(false)
    }
  }

  const handleSaveMedicalRecord = async (e) => {
    e.preventDefault()
    if (!recordTarget) return
    setSavingRecord(true)
    try {
      await saveMedicalRecord({
        appointmentId: recordTarget.id,
        ...recordForm,
        followUpDate: recordForm.followUpDate || undefined
      })
      showToast('Đã lưu bệnh án & hoàn thành ca khám!')
      setRecordTarget(null)
      loadData()
    } catch (err) {
      showToast(err?.response?.data?.message || 'Có lỗi khi lưu bệnh án', 'err')
    } finally {
      setSavingRecord(false)
    }
  }

  return (
    <AdminLayout>
      <div style={{ maxWidth: 1200, margin: '0 auto' }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24, flexWrap: 'wrap', gap: 16 }}>
          <div>
            <h1 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 28, fontWeight: 700, color: '#2B1A0F', margin: 0 }}>
              📋 Tiếp nhận & Quản lý lịch khám
            </h1>
            <div style={{ fontSize: 16, color: '#6B5E4E', marginTop: 4 }}>
              Check-in, điều phối vào phòng khám và hoàn tất bệnh án
            </div>
          </div>

          <button
            onClick={() => setShowWalkIn(true)}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 8,
              minHeight: 48, padding: '0 22px', borderRadius: 12,
              background: '#2D5A27', color: '#fff', fontSize: 17, fontWeight: 700, border: 'none', cursor: 'pointer'
            }}
          >
            + Tiếp nhận bệnh nhân tại quầy
          </button>
        </div>

        {/* Thanh tìm kiếm & bộ lọc */}
        <div style={{ background: '#fff', padding: '16px 20px', borderRadius: 16, border: '1px solid #E0D3BC', marginBottom: 24, display: 'flex', flexWrap: 'wrap', gap: 14, alignItems: 'center' }}>
          <div>
            <label style={{ display: 'block', fontSize: 13, fontWeight: 700, color: '#6B5E4E', marginBottom: 4 }}>NGÀY KHÁM</label>
            <input
              type="date"
              value={date}
              onChange={e => setDate(e.target.value)}
              style={{ minHeight: 42, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, fontFamily: 'inherit' }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: 13, fontWeight: 700, color: '#6B5E4E', marginBottom: 4 }}>TRẠNG THÁI</label>
            <select
              value={status}
              onChange={e => setStatus(e.target.value)}
              style={{ minHeight: 42, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, fontFamily: 'inherit' }}
            >
              {STATUS_FILTERS.map(f => <option key={f.value} value={f.value}>{f.label}</option>)}
            </select>
          </div>

          <div style={{ flex: '1 1 240px' }}>
            <label style={{ display: 'block', fontSize: 13, fontWeight: 700, color: '#6B5E4E', marginBottom: 4 }}>TÌM THEO TÊN, SĐT HOẶC MÃ PHIẾU</label>
            <input
              type="text"
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Nhập tên bệnh nhân, SĐT hoặc mã PKCT-..."
              style={{ width: '100%', minHeight: 42, padding: '0 14px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, fontFamily: 'inherit', boxSizing: 'border-box' }}
            />
          </div>

          <button
            onClick={loadData}
            style={{ alignSelf: 'flex-end', minHeight: 42, padding: '0 16px', borderRadius: 10, background: '#5C3A21', color: '#fff', border: 'none', fontSize: 16, fontWeight: 600, cursor: 'pointer' }}
          >
            Lọc dữ liệu
          </button>
        </div>

        {/* Bảng danh sách */}
        {loading ? (
          <div style={{ textAlign: 'center', padding: 48, color: '#6B5E4E', fontSize: 18 }}>⏳ Đang tải danh sách lịch khám...</div>
        ) : appointments.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 48, background: '#fff', borderRadius: 16, border: '1px solid #E0D3BC' }}>
            <div style={{ fontSize: 48, marginBottom: 12 }}>📭</div>
            <div style={{ fontSize: 20, fontWeight: 700, color: '#2B1A0F', marginBottom: 6 }}>Không có lượt hẹn nào</div>
            <div style={{ fontSize: 16, color: '#6B5E4E' }}>Thử chọn ngày khác hoặc xóa từ khóa tìm kiếm.</div>
          </div>
        ) : (
          <div style={{ background: '#fff', borderRadius: 16, border: '1px solid #E0D3BC', overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', minWidth: 900 }}>
              <thead>
                <tr style={{ background: '#F8F5EE', borderBottom: '1.5px solid #E0D3BC', color: '#5C3A21', fontSize: 15, fontWeight: 700 }}>
                  <th style={{ padding: '14px 16px' }}>Mã phiếu</th>
                  <th style={{ padding: '14px 16px' }}>Bệnh nhân</th>
                  <th style={{ padding: '14px 16px' }}>Khung giờ</th>
                  <th style={{ padding: '14px 16px' }}>Bác sĩ / Dịch vụ</th>
                  <th style={{ padding: '14px 16px' }}>Trạng thái</th>
                  <th style={{ padding: '14px 16px', textAlign: 'right' }}>Thao tác điều phối</th>
                </tr>
              </thead>
              <tbody>
                {appointments.map((a) => (
                  <tr key={a.id} style={{ borderBottom: '1px solid #F0EAE0', fontSize: 16 }}>
                    <td style={{ padding: '14px 16px' }}>
                      <span style={{ fontFamily: 'monospace', fontWeight: 800, color: '#2D5A27' }}>{a.bookingCode}</span>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <div style={{ fontWeight: 700, color: '#1F1610' }}>{a.patientName}</div>
                      <div style={{ fontSize: 14, color: '#6B5E4E' }}>Mã: {a.patientCode} · {a.relationship || 'Bản thân'}</div>
                      {a.symptoms && <div style={{ fontSize: 13, color: '#B45309', marginTop: 2 }}>💬 {a.symptoms}</div>}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <span style={{ fontWeight: 600 }}>{a.timeSlotLabel}</span>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <div>{a.doctorName || <span style={{ color: '#999' }}>Chưa chỉ định</span>}</div>
                      <div style={{ fontSize: 13, color: '#6B5E4E' }}>{a.serviceName || 'Khám YHCT'}</div>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <StatusBadge status={a.status} label={a.statusLabel} />
                    </td>
                    <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: 6, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                        {/* PENDING -> Check-in */}
                        {a.status === 'PENDING' && (
                          <button
                            onClick={() => handleStatusChange(a.id, 'CONFIRMED')}
                            style={{ padding: '6px 12px', borderRadius: 8, background: '#2D5A27', color: '#fff', border: 'none', fontSize: 14, fontWeight: 700, cursor: 'pointer' }}
                          >
                            ✓ Check-in
                          </button>
                        )}

                        {/* CONFIRMED -> Gọi vào khám */}
                        {a.status === 'CONFIRMED' && (
                          <button
                            onClick={() => handleStatusChange(a.id, 'IN_PROGRESS')}
                            style={{ padding: '6px 12px', borderRadius: 8, background: '#047857', color: '#fff', border: 'none', fontSize: 14, fontWeight: 700, cursor: 'pointer' }}
                          >
                            🩺 Vào khám
                          </button>
                        )}

                        {/* IN_PROGRESS -> Kê đơn & Hoàn thành */}
                        {a.status === 'IN_PROGRESS' && (
                          <button
                            onClick={() => {
                              setRecordTarget(a)
                              setRecordForm(f => ({ ...f, diagnosis: a.symptoms ? 'Viêm khớp / Đau nhức do phong hàn - ' + a.symptoms : 'Viêm khớp thoái hóa YHCT' }))
                            }}
                            style={{ padding: '6px 12px', borderRadius: 8, background: '#D97706', color: '#fff', border: 'none', fontSize: 14, fontWeight: 700, cursor: 'pointer' }}
                          >
                            📝 Kê đơn & Bệnh án
                          </button>
                        )}

                        {/* Hủy phiếu */}
                        {(a.status === 'PENDING' || a.status === 'CONFIRMED') && (
                          <button
                            onClick={() => {
                              const reason = prompt('Nhập lý do hủy phiếu:')
                              if (reason !== null) handleStatusChange(a.id, 'CANCELLED', reason)
                            }}
                            style={{ padding: '6px 10px', borderRadius: 8, background: 'transparent', color: '#90372A', border: '1px solid #90372A40', fontSize: 14, cursor: 'pointer' }}
                          >
                            Hủy
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* ─── MODAL TIẾP NHẬN TẠI QUẦY (WALK-IN) ─── */}
        {showWalkIn && (
          <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 300, padding: 16 }}>
            <div style={{ background: '#FDFBF7', borderRadius: 20, padding: '28px 24px', width: '100%', maxWidth: 540, maxHeight: '90vh', overflowY: 'auto' }}>
              <h2 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 24, fontWeight: 700, color: '#5C3A21', marginBottom: 18 }}>
                ➕ Tiếp nhận bệnh nhân tại quầy
              </h2>

              <form onSubmit={handleWalkInSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                <div>
                  <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Họ và tên bệnh nhân *</label>
                  <input
                    type="text" required placeholder="Nguyễn Văn A"
                    value={walkInForm.fullName} onChange={e => setWalkInForm({ ...walkInForm, fullName: e.target.value })}
                    style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Số điện thoại *</label>
                    <input
                      type="tel" required placeholder="0901234567"
                      value={walkInForm.phone} onChange={e => setWalkInForm({ ...walkInForm, phone: e.target.value })}
                      style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                    />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Năm sinh *</label>
                    <input
                      type="number" required min={1910} max={new Date().getFullYear()}
                      value={walkInForm.yearOfBirth} onChange={e => setWalkInForm({ ...walkInForm, yearOfBirth: e.target.value })}
                      style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                    />
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Giới tính *</label>
                    <select
                      value={walkInForm.gender} onChange={e => setWalkInForm({ ...walkInForm, gender: e.target.value })}
                      style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16 }}
                    >
                      <option value="MALE">Nam</option>
                      <option value="FEMALE">Nữ</option>
                      <option value="OTHER">Khác</option>
                    </select>
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Khung giờ khám *</label>
                    <select
                      required value={walkInForm.timeSlotId} onChange={e => setWalkInForm({ ...walkInForm, timeSlotId: e.target.value })}
                      style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16 }}
                    >
                      {slots.map(s => <option key={s.id} value={s.id}>{s.slotLabel}</option>)}
                    </select>
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Bác sĩ khám</label>
                    <select
                      value={walkInForm.doctorId} onChange={e => setWalkInForm({ ...walkInForm, doctorId: e.target.value })}
                      style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16 }}
                    >
                      <option value="">Tự động / Bác sĩ trực</option>
                      {doctors.map(d => <option key={d.id} value={d.id}>{d.fullName}</option>)}
                    </select>
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Dịch vụ</label>
                    <select
                      value={walkInForm.serviceId} onChange={e => setWalkInForm({ ...walkInForm, serviceId: e.target.value })}
                      style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16 }}
                    >
                      <option value="">Khám YHCT thông thường</option>
                      {services.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                  </div>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Triệu chứng ban đầu</label>
                  <textarea
                    rows={2} placeholder="Đau mỏi vai gáy, mất ngủ..."
                    value={walkInForm.symptoms} onChange={e => setWalkInForm({ ...walkInForm, symptoms: e.target.value })}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                  />
                </div>

                <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 12 }}>
                  <button type="button" onClick={() => setShowWalkIn(false)} style={{ padding: '0 18px', minHeight: 46, borderRadius: 10, background: '#fff', border: '1.5px solid #E0D3BC', fontSize: 16, cursor: 'pointer' }}>
                    Hủy bỏ
                  </button>
                  <button type="submit" disabled={submittingWalkIn} style={{ padding: '0 24px', minHeight: 46, borderRadius: 10, background: '#2D5A27', color: '#fff', border: 'none', fontSize: 16, fontWeight: 700, cursor: 'pointer' }}>
                    {submittingWalkIn ? 'Đang tiếp nhận...' : 'Xác nhận tiếp nhận'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* ─── MODAL KÊ ĐƠN & GHI BỆNH ÁN ĐIỆN TỬ ─── */}
        {recordTarget && (
          <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 310, padding: 16 }}>
            <div style={{ background: '#FDFBF7', borderRadius: 20, padding: '28px 24px', width: '100%', maxWidth: 640, maxHeight: '92vh', overflowY: 'auto' }}>
              <div style={{ borderBottom: '1px solid #E0D3BC', paddingBottom: 14, marginBottom: 16 }}>
                <h2 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 24, fontWeight: 700, color: '#2D5A27', margin: 0 }}>
                  🌿 Kê đơn & Bệnh án Y học cổ truyền
                </h2>
                <div style={{ fontSize: 16, color: '#6B5E4E', marginTop: 4 }}>
                  Bệnh nhân: <strong>{recordTarget.patientName}</strong> ({recordTarget.patientCode}) · Phiếu: {recordTarget.bookingCode}
                </div>
              </div>

              <form onSubmit={handleSaveMedicalRecord} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                <div>
                  <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Bắt mạch & Huyết áp</label>
                  <input
                    type="text" value={recordForm.pulseBp} onChange={e => setRecordForm({ ...recordForm, pulseBp: e.target.value })}
                    style={{ width: '100%', minHeight: 42, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Chẩn đoán Y học cổ truyền & Hiện đại *</label>
                  <input
                    type="text" required value={recordForm.diagnosis} onChange={e => setRecordForm({ ...recordForm, diagnosis: e.target.value })}
                    placeholder="Ví dụ: Tý chứng thể phong hàn thấp / Thoái hóa đốt sống cổ"
                    style={{ width: '100%', minHeight: 42, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Phương pháp điều trị (Châm cứu, Xoa bóp, Vật lý trị liệu)</label>
                  <textarea
                    rows={2} value={recordForm.treatment} onChange={e => setRecordForm({ ...recordForm, treatment: e.target.value })}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Bài thuốc thang thảo mộc (Vị thuốc, liều lượng, cách sắc)</label>
                  <textarea
                    rows={4} value={recordForm.prescription} onChange={e => setRecordForm({ ...recordForm, prescription: e.target.value })}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 12 }}>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Lời dặn dò dưỡng sinh</label>
                    <input
                      type="text" value={recordForm.advice} onChange={e => setRecordForm({ ...recordForm, advice: e.target.value })}
                      style={{ width: '100%', minHeight: 42, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                    />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Hẹn tái khám</label>
                    <input
                      type="date" value={recordForm.followUpDate} onChange={e => setRecordForm({ ...recordForm, followUpDate: e.target.value })}
                      style={{ width: '100%', minHeight: 42, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                    />
                  </div>
                </div>

                <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 14 }}>
                  <button type="button" onClick={() => setRecordTarget(null)} style={{ padding: '0 18px', minHeight: 46, borderRadius: 10, background: '#fff', border: '1.5px solid #E0D3BC', fontSize: 16, cursor: 'pointer' }}>
                    Đóng lại
                  </button>
                  <button type="submit" disabled={savingRecord} style={{ padding: '0 24px', minHeight: 46, borderRadius: 10, background: '#2D5A27', color: '#fff', border: 'none', fontSize: 16, fontWeight: 700, cursor: 'pointer' }}>
                    {savingRecord ? 'Đang lưu...' : '✓ Lưu bệnh án & Hoàn thành ca khám'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Toast */}
        {toast && (
          <div style={{ position: 'fixed', bottom: 32, right: 32, background: toast.type === 'err' ? '#90372A' : '#2D5A27', color: '#fff', padding: '14px 24px', borderRadius: 14, fontSize: 16, fontWeight: 600, zIndex: 999 }}>
            {toast.type === 'err' ? '❌' : '✅'} {toast.msg}
          </div>
        )}
      </div>
    </AdminLayout>
  )
}

function StatusBadge({ status, label }) {
  let bg = '#F0EAE0'
  let color = '#4A3F33'
  if (status === 'PENDING') { bg = '#FFFBEB'; color = '#B45309' }
  else if (status === 'CONFIRMED') { bg = '#EFF6FF'; color = '#1E40AF' }
  else if (status === 'IN_PROGRESS') { bg = '#ECFDF5'; color = '#047857' }
  else if (status === 'COMPLETED') { bg = '#F0FDF4'; color = '#15803D' }
  else if (status === 'CANCELLED') { bg = '#FFF0EE'; color = '#90372A' }

  return (
    <span style={{ display: 'inline-block', padding: '4px 10px', borderRadius: 99, fontSize: 13, fontWeight: 700, background: bg, color }}>
      {label || status}
    </span>
  )
}

