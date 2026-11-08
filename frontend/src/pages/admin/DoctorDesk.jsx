import { useState, useEffect, useCallback } from 'react'
import AdminLayout from './AdminLayout'
import {
  getDoctorQueue,
  callInAppointment,
  saveDoctorMedicalRecord as saveMedicalRecord,
  getDoctorPatientHistory as getMedicalRecordsByPatient,
  getDoctors as getAdminDoctors,
} from '../../api/doctor'
import { useAuth } from '../../context/AuthContext'

// Danh mục thang thuốc cổ phương mẫu thông dụng tại phòng khám YHCT
const HERBAL_PRESETS = [
  {
    name: 'Độc Hoạt Ký Sinh Thang (Cơ xương khớp, thoái hóa)',
    content: 'Thang Độc Hoạt Ký Sinh Gia Giảm (10 thang)\n- Độc hoạt: 12g, Tang ký sinh: 16g, Đỗ trọng: 12g, Ngưu tất: 12g\n- Đảng sâm: 12g, Phục linh: 10g, Cam thảo: 6g, Thục địa: 12g\n- Bạch thược: 10g, Xuyên khung: 8g, Đương quy: 12g, Tần giao: 10g\nCách dùng: Mỗi ngày 1 thang, sắc 3 bát lấy 1 bát, uống ấm sau bữa ăn 30 phút.',
    diag: 'Tý chứng thể phong hàn thấp / Thoái hóa khớp'
  },
  {
    name: 'Bát Trân Thang (Bồi bổ khí huyết, suy nhược người già)',
    content: 'Thang Bát Trân Cổ Phương (7 thang)\n- Đảng sâm: 12g, Bạch truật: 12g, Bạch linh: 10g, Cam thảo: 6g\n- Thục địa: 12g, Đương quy: 12g, Bạch thược: 10g, Xuyên khung: 8g\nCách dùng: Sắc uống ngày 1 thang, chia 2 lần sáng - chiều ấm.',
    diag: 'Khí huyết lưỡng hư thể suy nhược'
  },
  {
    name: 'Bổ Trung Ích Khí Thang (Tỳ vị hư nhược, sa giáng mệt mỏi)',
    content: 'Thang Bổ Trung Ích Khí (7 thang)\n- Hoàng kỳ: 16g, Đảng sâm: 12g, Bạch truật: 12g, Đương quy: 10g\n- Trần bì: 8g, Thăng ma: 6g, Sài hồ: 6g, Cam thảo: 6g\nCách dùng: Uống ấm trước bữa ăn, ngày 1 thang.',
    diag: 'Tỳ khí hư hãm / Tiêu hóa kém'
  },
  {
    name: 'Lục Vị Địa Hoàng Thang (Can thận âm hư, hoa mắt, đau lưng)',
    content: 'Thang Lục Vị Địa Hoàng (10 thang)\n- Thục địa: 16g, Hoài sơn: 12g, Sơn thù: 10g\n- Đan bì: 8g, Bạch linh: 10g, Trạch tả: 8g\nCách dùng: Sắc nước hoặc hoàn viên, dùng ngày 2 lần.',
    diag: 'Can thận âm hư / Thận tinh bất túc'
  },
  {
    name: 'Quy Tỳ Thang (Mất ngủ, lo âu, tim đập nhanh)',
    content: 'Thang Quy Tỳ Gia Giảm (10 thang)\n- Đảng sâm: 12g, Hoàng kỳ: 12g, Bạch truật: 12g, Phục thần: 10g\n- Toan táo nhân (sao đen): 12g, Long nhãn: 10g, Đương quy: 10g, Viễn chí: 6g\nCách dùng: Uống ấm buổi tối trước khi ngủ 1 giờ.',
    diag: 'Tâm tỳ hư nhược / Thất miên (Mất ngủ mạn tính)'
  }
]

export default function DoctorDesk() {
  const { user } = useAuth()
  const [queue, setQueue] = useState([])
  const [loading, setLoading] = useState(true)
  const [selectedApp, setSelectedApp] = useState(null)
  const [doctors, setDoctors] = useState([])
  const [filterDoctorId, setFilterDoctorId] = useState('')

  // Form Bệnh án YHCT
  const [bloodPressure, setBloodPressure] = useState('120/80')
  const [pulseRate, setPulseRate] = useState(72)
  const [pulseDiagnosis, setPulseDiagnosis] = useState('Trầm nhược vô lực')
  const [tongueDiagnosis, setTongueDiagnosis] = useState('Chất lưỡi nhợt, rêu trắng mỏng')
  const [traditionalDiagnosis, setTraditionalDiagnosis] = useState('Tý chứng thể phong hàn thấp')
  const [herbalPrescription, setHerbalPrescription] = useState('')
  const [acupointTherapy, setAcupointTherapy] = useState('Châm cứu: Hợp cốc, Túc tam lý, A thị huyệt. Liệu trình 5 ngày.')
  const [dietaryAdvice, setDietaryAdvice] = useState('Kiêng đồ tanh lạnh, rau muống, da gà. Giữ ấm tay chân về đêm.')
  const [followUpDays, setFollowUpDays] = useState(10)
  const [submitting, setSubmitting] = useState(false)

  // Drawer Lịch sử bệnh án cũ
  const [showHistory, setShowHistory] = useState(false)
  const [pastRecords, setPastRecords] = useState([])
  const [loadingPast, setLoadingPast] = useState(false)

  // 1. Tải danh sách bác sĩ để chọn buồng khám
  useEffect(() => {
    getAdminDoctors().then(res => setDoctors(res.data || [])).catch(() => {})
  }, [])

  // 2. Tải hàng đợi của Bác sĩ
  const loadQueue = useCallback(async () => {
    try {
      const today = new Date().toISOString().split('T')[0]
      const res = await getDoctorQueue({
        date: today,
        doctorId: filterDoctorId ? parseInt(filterDoctorId, 10) : undefined,
      })
      const list = res.data || []
      setQueue(list)

      // Nếu đang có ca IN_PROGRESS thì tự động chọn ca đó
      const currentInProg = list.find(a => a.status === 'IN_PROGRESS')
      if (currentInProg && (!selectedApp || selectedApp.id !== currentInProg.id)) {
        setSelectedApp(currentInProg)
      } else if (!currentInProg && list.length > 0 && !selectedApp) {
        setSelectedApp(list[0])
      }
    } catch (err) {
      console.error('Lỗi tải hàng đợi bác sĩ:', err)
      setQueue([])
    } finally {
      setLoading(false)
    }
  }, [filterDoctorId, selectedApp])

  useEffect(() => {
    loadQueue()
    const timer = setInterval(loadQueue, 8000)
    return () => clearInterval(timer)
  }, [loadQueue])

  // 3. Mời ca tiếp theo vào khám (Call in)
  const handleCallIn = async (appId) => {
    try {
      const res = await callInAppointment(appId)
      setSelectedApp(res.data)
      loadQueue()
    } catch (err) {
      alert('Lỗi mời vào khám: ' + err.message)
    }
  }

  // 4. Mở Drawer xem bệnh sử cũ của bệnh nhân
  const handleOpenHistory = async () => {
    if (!selectedApp) return
    setShowHistory(true)
    setLoadingPast(true)
    try {
      const res = await getMedicalRecordsByPatient(selectedApp.patientId)
      setPastRecords(res.data || [])
    } catch {
      setPastRecords([])
    } finally {
      setLoadingPast(false)
    }
  }

  // 5. Chọn thang thuốc mẫu
  const applyPreset = (preset) => {
    setHerbalPrescription(preset.content)
    if (!traditionalDiagnosis || traditionalDiagnosis === 'Khám tổng quát YHCT') {
      setTraditionalDiagnosis(preset.diag)
    }
  }

  // 6. Hoàn tất ca khám & Lưu bệnh án YHCT
  const handleCompleteRecord = async (e) => {
    e.preventDefault()
    if (!selectedApp) {
      alert('Vui lòng chọn ca khám cần ghi bệnh án!')
      return
    }

    setSubmitting(true)
    try {
      const payload = {
        appointmentId: selectedApp.id,
        bloodPressure: bloodPressure.trim(),
        pulseRate: pulseRate ? parseInt(pulseRate, 10) : undefined,
        pulseDiagnosis: pulseDiagnosis.trim(),
        tongueDiagnosis: tongueDiagnosis.trim(),
        traditionalDiagnosis: traditionalDiagnosis.trim(),
        herbalPrescription: herbalPrescription.trim(),
        acupointTherapy: acupointTherapy.trim(),
        dietaryAdvice: dietaryAdvice.trim(),
        followUpDays: followUpDays ? parseInt(followUpDays, 10) : 0,
      }
      await saveMedicalRecord(payload)
      alert('✓ Đã lưu bệnh án & Kê đơn thuốc thành công cho bệnh nhân ' + selectedApp.patientName)

      // Xóa form và tải lại hàng đợi
      setHerbalPrescription('')
      setSelectedApp(null)
      loadQueue()
    } catch (err) {
      alert('Không thể lưu bệnh án: ' + (err.message || 'Lỗi hệ thống'))
    } finally {
      setSubmitting(false)
    }
  }

  const currentInProgress = queue.find(a => a.status === 'IN_PROGRESS')
  const waitingList = queue.filter(a => a.status === 'CONFIRMED')

  return (
    <AdminLayout>
      <div style={{ maxWidth: 1400, margin: '0 auto' }}>
        {/* Header bàn bác sĩ */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20, flexWrap: 'wrap', gap: 14 }}>
          <div>
            <h1 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 26, fontWeight: 700, color: '#2B1A0F', margin: 0 }}>
              👨‍⚕️ Bàn Khám Bệnh & Bốc Thuốc Y Học Cổ Truyền
            </h1>
            <div style={{ fontSize: 15, color: '#6B5E4E', marginTop: 4 }}>
              Người trực: <strong>{user?.name || user?.fullName || 'Bác sĩ YHCT'}</strong> | Ngày {new Date().toLocaleDateString('vi-VN')}
            </div>
          </div>

          <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            <span style={{ fontSize: 14, color: '#5C3A21', fontWeight: 600 }}>Buồng khám:</span>
            <select
              value={filterDoctorId}
              onChange={e => setFilterDoctorId(e.target.value)}
              style={{ minHeight: 40, padding: '0 10px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 14, background: '#fff' }}
            >
              <option value="">-- Tất cả buồng khám --</option>
              {doctors.map(d => (
                <option key={d.id} value={d.id}>{d.fullName} ({d.specialty})</option>
              ))}
            </select>
          </div>
        </div>

        {/* Bố cục 2 cột */}
        <div style={{ display: 'grid', gridTemplateColumns: '360px 1fr', gap: 20, alignItems: 'start' }}>
          {/* CỘT TRÁI (35%): HÀNG ĐỢI GỌI KHÁM */}
          <div style={{ background: '#fff', borderRadius: 18, border: '1px solid #E0D3BC', padding: 20, boxShadow: '0 4px 16px rgba(0,0,0,0.03)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <h2 style={{ fontSize: 18, fontWeight: 700, color: '#2B1A0F', margin: 0 }}>
                📋 Hàng đợi hôm nay ({queue.length})
              </h2>
              <button
                onClick={loadQueue}
                style={{ border: 'none', background: '#F5EFE6', color: '#5C3A21', padding: '4px 10px', borderRadius: 8, fontSize: 13, cursor: 'pointer', fontWeight: 600 }}
              >
                🔄 Cập nhật
              </button>
            </div>

            {/* Ca Đang Khám (nếu có) */}
            {currentInProgress && (
              <div style={{ background: '#EFF6FF', border: '2px solid #3B82F6', borderRadius: 14, padding: 14, marginBottom: 16 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                  <span style={{ background: '#2563EB', color: '#fff', padding: '2px 8px', borderRadius: 6, fontSize: 12, fontWeight: 800 }}>
                    ĐANG KHÁM
                  </span>
                  <span style={{ fontSize: 18, fontWeight: 900, color: '#1E40AF' }}>
                    STT #{currentInProgress.queueNumber}
                  </span>
                </div>
                <div style={{ fontSize: 17, fontWeight: 700, color: '#1E3A8A' }}>
                  {currentInProgress.patientName}
                </div>
                <div style={{ fontSize: 13, color: '#4B5563', marginTop: 2 }}>
                  📞 {currentInProgress.patientPhone}
                </div>
                {currentInProgress.symptoms && (
                  <div style={{ fontSize: 13, color: '#B45309', marginTop: 4 }}>
                    Triệu chứng: {currentInProgress.symptoms}
                  </div>
                )}
                <button
                  onClick={() => setSelectedApp(currentInProgress)}
                  style={{
                    width: '100%', marginTop: 10, padding: '6px 0', borderRadius: 8,
                    background: selectedApp?.id === currentInProgress.id ? '#1D4ED8' : '#fff',
                    color: selectedApp?.id === currentInProgress.id ? '#fff' : '#1D4ED8',
                    border: '1.5px solid #1D4ED8', fontSize: 14, fontWeight: 700, cursor: 'pointer'
                  }}
                >
                  {selectedApp?.id === currentInProgress.id ? '✓ Đang xem ca này' : 'Chuyển sang ghi bệnh án'}
                </button>
              </div>
            )}

            {/* Nút gọi nhanh ca kế tiếp */}
            {waitingList.length > 0 && (
              <button
                onClick={() => handleCallIn(waitingList[0].id)}
                style={{
                  width: '100%', minHeight: 46, borderRadius: 12,
                  background: '#2D5A27', color: '#fff', border: 'none',
                  fontSize: 16, fontWeight: 700, cursor: 'pointer',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
                  marginBottom: 16, boxShadow: '0 4px 12px rgba(45,90,39,0.2)'
                }}
              >
                📢 Mời ca tiếp theo: STT #{waitingList[0].queueNumber}
              </button>
            )}

            {/* Danh sách các ca đang ngồi sảnh chờ */}
            <div style={{ fontSize: 14, fontWeight: 700, color: '#5C3A21', marginBottom: 8 }}>
              Cô chú đang chờ ở sảnh ({waitingList.length}):
            </div>

            {loading ? (
              <div style={{ textAlign: 'center', padding: 20, color: '#6B5E4E' }}>⏳ Đang tải...</div>
            ) : waitingList.length === 0 ? (
              <div style={{ textAlign: 'center', padding: 24, color: '#9CA3AF', fontSize: 15 }}>
                Không có bệnh nhân chờ
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10, maxHeight: 480, overflowY: 'auto' }}>
                {waitingList.map(a => {
                  const isSelected = selectedApp?.id === a.id
                  return (
                    <div
                      key={a.id}
                      onClick={() => setSelectedApp(a)}
                      style={{
                        padding: 12, borderRadius: 12, cursor: 'pointer',
                        background: isSelected ? '#FEF3C7' : '#F9FAFB',
                        border: isSelected ? '1.5px solid #D97706' : '1px solid #E5E7EB',
                        transition: 'all 0.15s'
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span style={{ fontSize: 14, fontWeight: 800, color: '#B45309' }}>
                          STT #{a.queueNumber}
                        </span>
                        <span style={{ fontSize: 12, color: '#6B7280' }}>
                          {a.timeSlotDisplay || 'Sớm'}
                        </span>
                      </div>
                      <div style={{ fontSize: 16, fontWeight: 700, color: '#1F1610', marginTop: 2 }}>
                        {a.patientName} {a.patientYearOfBirth && <span style={{ fontWeight: 400, color: '#6B7280' }}>({new Date().getFullYear() - a.patientYearOfBirth}t)</span>}
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 6 }}>
                        <span style={{ fontSize: 13, color: '#6B7280' }}>📞 {a.patientPhone}</span>
                        <button
                          onClick={(e) => { e.stopPropagation(); handleCallIn(a.id) }}
                          style={{
                            background: '#2D5A27', color: '#fff', border: 'none',
                            padding: '3px 8px', borderRadius: 6, fontSize: 12, fontWeight: 700, cursor: 'pointer'
                          }}
                        >
                          Mời vào khám
                        </button>
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>

          {/* CỘT PHẢI (65%): BÀN KHÁM BỆNH & KÊ ĐƠN THUỐC ĐÔNG Y */}
          <div style={{ background: '#fff', borderRadius: 18, border: '1px solid #E0D3BC', padding: 26, boxShadow: '0 4px 16px rgba(0,0,0,0.03)' }}>
            {!selectedApp ? (
              <div style={{ padding: 60, textAlign: 'center', color: '#8C7B5E' }}>
                <div style={{ fontSize: 48, marginBottom: 12 }}>🩺</div>
                <div style={{ fontSize: 20, fontWeight: 700, color: '#5C3A21' }}>Chưa chọn ca khám nào</div>
                <p style={{ fontSize: 16, color: '#6B5E4E', maxWidth: 440, margin: '8px auto 0' }}>
                  Bác sĩ vui lòng chọn một bệnh nhân từ danh sách hàng đợi bên trái hoặc bấm nút "Mời ca tiếp theo vào khám".
                </p>
              </div>
            ) : (
              <div>
                {/* Thông tin hành chính bệnh nhân & Nút xem bệnh sử */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', borderBottom: '2px solid #F0E8DC', paddingBottom: 16, marginBottom: 20, flexWrap: 'wrap', gap: 12 }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <span style={{ background: '#FEF3C7', color: '#92400E', padding: '3px 10px', borderRadius: 8, fontSize: 14, fontWeight: 800 }}>
                        STT #{selectedApp.queueNumber || '01'}
                      </span>
                      <h2 style={{ fontSize: 24, fontWeight: 700, color: '#2B1A0F', margin: 0, fontFamily: "'Noto Serif', serif" }}>
                        {selectedApp.patientName}
                      </h2>
                      {selectedApp.patientYearOfBirth && (
                        <span style={{ fontSize: 16, color: '#6B5E4E' }}>
                          ({new Date().getFullYear() - selectedApp.patientYearOfBirth} tuổi)
                        </span>
                      )}
                    </div>
                    <div style={{ fontSize: 15, color: '#6B5E4E', marginTop: 4 }}>
                      Mã phiếu: <strong style={{ color: '#2B1A0F' }}>{selectedApp.bookingCode}</strong> | SĐT: <strong>{selectedApp.patientPhone}</strong>
                    </div>
                    {selectedApp.symptoms && (
                      <div style={{ fontSize: 15, color: '#B45309', marginTop: 4 }}>
                        Lý do khám / Triệu chứng: <strong>{selectedApp.symptoms}</strong>
                      </div>
                    )}
                  </div>

                  <button
                    onClick={handleOpenHistory}
                    style={{
                      display: 'inline-flex', alignItems: 'center', gap: 6,
                      padding: '8px 16px', borderRadius: 10,
                      background: '#FFFBEB', border: '1.5px solid #D97706',
                      color: '#B45309', fontSize: 15, fontWeight: 700, cursor: 'pointer'
                    }}
                  >
                    📜 Xem sổ bệnh sử cũ
                  </button>
                </div>

                {/* Form Bệnh án YHCT */}
                <form onSubmit={handleCompleteRecord} style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
                  {/* Nhóm 1: Tứ chẩn & Sinh hiệu */}
                  <div style={{ background: '#FDFBF7', border: '1px solid #E0D3BC', borderRadius: 14, padding: 16 }}>
                    <div style={{ fontSize: 16, fontWeight: 700, color: '#2D5A27', marginBottom: 12 }}>
                      1. SINH HIỆU & TỨ CHẨN ĐÔNG Y (Mạch lý, Thiệt chẩn)
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14, marginBottom: 12 }}>
                      <div>
                        <label style={{ display: 'block', fontSize: 14, fontWeight: 600, color: '#2B1A0F', marginBottom: 4 }}>
                          Huyết áp (mmHg)
                        </label>
                        <input
                          type="text"
                          value={bloodPressure}
                          onChange={e => setBloodPressure(e.target.value)}
                          placeholder="VD: 120/80"
                          style={{ width: '100%', minHeight: 40, padding: '0 10px', borderRadius: 8, border: '1.5px solid #E0D3BC', fontSize: 15, boxSizing: 'border-box' }}
                        />
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: 14, fontWeight: 600, color: '#2B1A0F', marginBottom: 4 }}>
                          Nhịp mạch (lần/phút)
                        </label>
                        <input
                          type="number"
                          value={pulseRate}
                          onChange={e => setPulseRate(e.target.value)}
                          placeholder="72"
                          style={{ width: '100%', minHeight: 40, padding: '0 10px', borderRadius: 8, border: '1.5px solid #E0D3BC', fontSize: 15, boxSizing: 'border-box' }}
                        />
                      </div>
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
                      <div>
                        <label style={{ display: 'block', fontSize: 14, fontWeight: 600, color: '#2B1A0F', marginBottom: 4 }}>
                          Mạch lý Đông Y
                        </label>
                        <input
                          type="text"
                          value={pulseDiagnosis}
                          onChange={e => setPulseDiagnosis(e.target.value)}
                          placeholder="VD: Trầm nhược, Trầm trì, Phù hoạt..."
                          style={{ width: '100%', minHeight: 40, padding: '0 10px', borderRadius: 8, border: '1.5px solid #E0D3BC', fontSize: 15, boxSizing: 'border-box' }}
                        />
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: 14, fontWeight: 600, color: '#2B1A0F', marginBottom: 4 }}>
                          Thiệt chẩn (Lưỡi & Sắc mặt)
                        </label>
                        <input
                          type="text"
                          value={tongueDiagnosis}
                          onChange={e => setTongueDiagnosis(e.target.value)}
                          placeholder="VD: Chất lưỡi nhạt, rêu mỏng vàng..."
                          style={{ width: '100%', minHeight: 40, padding: '0 10px', borderRadius: 8, border: '1.5px solid #E0D3BC', fontSize: 15, boxSizing: 'border-box' }}
                        />
                      </div>
                    </div>
                  </div>

                  {/* Nhóm 2: Chẩn đoán YHCT & Bát cương */}
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 700, color: '#2B1A0F', marginBottom: 6 }}>
                      2. Chẩn đoán Y học cổ truyền (Hội chứng Bát cương & Tạng phủ) (*)
                    </label>
                    <input
                      type="text"
                      required
                      value={traditionalDiagnosis}
                      onChange={e => setTraditionalDiagnosis(e.target.value)}
                      placeholder="VD: Can thận âm hư / Tý chứng thể phong hàn thấp..."
                      style={{ width: '100%', minHeight: 42, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                    />
                  </div>

                  {/* Nhóm 3: Toa thuốc thảo mộc & Thang mẫu */}
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6, flexWrap: 'wrap', gap: 6 }}>
                      <label style={{ fontSize: 15, fontWeight: 700, color: '#2B1A0F' }}>
                        3. Kê đơn thuốc thảo mộc / Bài thuốc Nam dược (*)
                      </label>
                      <span style={{ fontSize: 13, color: '#6B5E4E' }}>Chọn thang mẫu điền nhanh:</span>
                    </div>

                    {/* Presets buttons */}
                    <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 10 }}>
                      {HERBAL_PRESETS.map((p, idx) => (
                        <button
                          key={idx}
                          type="button"
                          onClick={() => applyPreset(p)}
                          style={{
                            padding: '4px 10px', borderRadius: 8,
                            background: '#F5EFE6', border: '1px solid #D6C7B2',
                            color: '#5C3A21', fontSize: 13, fontWeight: 600, cursor: 'pointer'
                          }}
                        >
                          + {p.name.split(' (')[0]}
                        </button>
                      ))}
                    </div>

                    <textarea
                      required
                      rows={5}
                      value={herbalPrescription}
                      onChange={e => setHerbalPrescription(e.target.value)}
                      placeholder="Nhập tên bài thuốc, thành phần các vị thảo mộc, số lượng gam, số thang và cách sắc uống..."
                      style={{ width: '100%', padding: '12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 15, fontFamily: 'inherit', boxSizing: 'border-box', lineHeight: 1.5 }}
                    />
                  </div>

                  {/* Nhóm 4: Liệu trình châm cứu & Dặn dò kiêng khem */}
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
                    <div>
                      <label style={{ display: 'block', fontSize: 14, fontWeight: 700, color: '#2B1A0F', marginBottom: 4 }}>
                        Liệu trình Châm cứu / Bấm huyệt
                      </label>
                      <textarea
                        rows={3}
                        value={acupointTherapy}
                        onChange={e => setAcupointTherapy(e.target.value)}
                        placeholder="Châm tả / châm bổ các huyệt vị..."
                        style={{ width: '100%', padding: '10px', borderRadius: 8, border: '1.5px solid #E0D3BC', fontSize: 14, fontFamily: 'inherit', boxSizing: 'border-box' }}
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: 14, fontWeight: 700, color: '#2B1A0F', marginBottom: 4 }}>
                        Dặn dò dinh dưỡng & Kiêng cữ
                      </label>
                      <textarea
                        rows={3}
                        value={dietaryAdvice}
                        onChange={e => setDietaryAdvice(e.target.value)}
                        placeholder="Thực phẩm kiêng khem, chế độ vận động..."
                        style={{ width: '100%', padding: '10px', borderRadius: 8, border: '1.5px solid #E0D3BC', fontSize: 14, fontFamily: 'inherit', boxSizing: 'border-box' }}
                      />
                    </div>
                  </div>

                  {/* Nhóm 5: Hẹn tái khám */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
                    <span style={{ fontSize: 15, fontWeight: 700, color: '#2B1A0F' }}>Hẹn tái khám sau:</span>
                    {[7, 10, 14, 30].map(days => (
                      <button
                        key={days}
                        type="button"
                        onClick={() => setFollowUpDays(days)}
                        style={{
                          padding: '6px 14px', borderRadius: 8,
                          background: followUpDays === days ? '#2D5A27' : '#F5EFE6',
                          color: followUpDays === days ? '#fff' : '#5C3A21',
                          border: 'none', fontSize: 14, fontWeight: 700, cursor: 'pointer'
                        }}
                      >
                        {days} ngày
                      </button>
                    ))}
                    <span style={{ fontSize: 14, color: '#6B5E4E' }}>
                      (Dự kiến ngày {new Date(Date.now() + followUpDays * 86400000).toLocaleDateString('vi-VN')})
                    </span>
                  </div>

                  {/* Nút Submit Hoàn tất */}
                  <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, borderTop: '1px solid #E0D3BC', paddingTop: 18, marginTop: 8 }}>
                    <button
                      type="submit"
                      disabled={submitting}
                      style={{
                        minHeight: 50, padding: '0 32px', borderRadius: 12,
                        background: '#2D5A27', color: '#fff', border: 'none',
                        fontSize: 18, fontWeight: 700, cursor: 'pointer',
                        boxShadow: '0 6px 16px rgba(45,90,39,0.3)'
                      }}
                    >
                      {submitting ? 'Đang lưu bệnh án...' : '✓ Hoàn tất ca khám & Kê đơn thuốc'}
                    </button>
                  </div>
                </form>
              </div>
            )}
          </div>
        </div>

        {/* DRAWER XEM BỆNH SỬ CŨ TRỌN ĐỜI */}
        {showHistory && (
          <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', zIndex: 1100, display: 'flex', justifyContent: 'flex-end' }}>
            <div style={{ background: '#fff', width: '100%', maxWidth: 520, height: '100%', padding: 28, overflowY: 'auto', boxShadow: '-10px 0 30px rgba(0,0,0,0.2)', display: 'flex', flexDirection: 'column' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20, borderBottom: '1px solid #E0D3BC', paddingBottom: 14 }}>
                <h3 style={{ margin: 0, fontSize: 20, color: '#2B1A0F', fontFamily: "'Noto Serif', serif" }}>
                  📜 Sổ Bệnh Sử: {selectedApp?.patientName}
                </h3>
                <button onClick={() => setShowHistory(false)} style={{ border: 'none', background: 'none', fontSize: 24, cursor: 'pointer' }}>✕</button>
              </div>

              {loadingPast ? (
                <div style={{ padding: 40, textAlign: 'center', color: '#6B5E4E' }}>⏳ Đang tải bệnh sử...</div>
              ) : pastRecords.length === 0 ? (
                <div style={{ textAlign: 'center', padding: 40, color: '#9CA3AF' }}>
                  Chưa có lịch sử khám trước đây (Bệnh nhân mới)
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                  {pastRecords.map((r, i) => (
                    <div key={r.id || i} style={{ background: '#FDFBF7', border: '1.5px solid #E0D3BC', borderRadius: 14, padding: 16 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                        <span style={{ fontSize: 14, fontWeight: 700, color: '#2D5A27' }}>
                          📅 Ngày {r.appointmentDate}
                        </span>
                        <span style={{ fontSize: 13, color: '#6B5E4E' }}>
                          Bác sĩ: {r.doctorName || 'Bác sĩ YHCT'}
                        </span>
                      </div>
                      <div style={{ fontSize: 14, color: '#2B1A0F', marginBottom: 4 }}>
                        <strong>Mạch lý / HA:</strong> {r.pulseBp || 'Không ghi'}
                      </div>
                      <div style={{ fontSize: 14, color: '#2B1A0F', marginBottom: 6 }}>
                        <strong>Chẩn đoán:</strong> {r.diagnosis}
                      </div>
                      <div style={{ background: '#fff', border: '1px dashed #D6C7B2', borderRadius: 8, padding: 10, fontSize: 13, whiteSpace: 'pre-wrap', color: '#1F1610' }}>
                        <strong>Toa thuốc:</strong>\n{r.prescription}
                      </div>
                      {r.advice && (
                        <div style={{ fontSize: 13, color: '#854D0E', marginTop: 6 }}>
                          <strong>Dặn dò:</strong> {r.advice}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  )
}

