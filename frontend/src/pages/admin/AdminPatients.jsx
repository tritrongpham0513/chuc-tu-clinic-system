import { useState, useEffect, useCallback } from 'react'
import AdminLayout from './AdminLayout'
import { searchAdminPatients, createAdminPatient, updateAdminPatient, getMedicalRecordsByPatient } from '../../api/admin'
import Icon from '../../components/Icon'

export default function AdminPatients() {
  const [patients, setPatients] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)

  // Modal Thêm/Sửa
  const [showModal, setShowModal] = useState(false)
  const [editTarget, setEditTarget] = useState(null)
  const [patientForm, setPatientForm] = useState({
    fullName: '', phone: '', yearOfBirth: 1960, gender: 'MALE',
    address: '', relationship: 'Bản thân', medicalHistory: ''
  })
  const [saving, setSaving] = useState(false)

  // Modal xem lịch sử bệnh án
  const [historyTarget, setHistoryTarget] = useState(null)
  const [medicalRecords, setMedicalRecords] = useState([])
  const [loadingRecords, setLoadingRecords] = useState(false)

  // Toast
  const [toast, setToast] = useState(null)
  const showToast = (msg, type = 'ok') => {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await searchAdminPatients(search.trim() || undefined)
      setPatients(res.data || [])
    } catch {
      setPatients([])
    } finally {
      setLoading(false)
    }
  }, [search])

  useEffect(() => { loadData() }, [loadData])

  const openAdd = () => {
    setEditTarget(null)
    setPatientForm({ fullName: '', phone: '', yearOfBirth: 1960, gender: 'MALE', address: '', relationship: 'Bản thân', medicalHistory: '' })
    setShowModal(true)
  }

  const openEdit = (p) => {
    setEditTarget(p)
    setPatientForm({
      fullName: p.fullName || '',
      phone: p.phone || '',
      yearOfBirth: p.yearOfBirth || 1960,
      gender: p.gender || 'MALE',
      address: p.address || '',
      relationship: p.relationship || 'Bản thân',
      medicalHistory: p.medicalHistory || ''
    })
    setShowModal(true)
  }

  const handleSave = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      const payload = { ...patientForm, yearOfBirth: parseInt(patientForm.yearOfBirth, 10) }
      if (editTarget) {
        await updateAdminPatient(editTarget.id, payload)
        showToast('Cập nhật hồ sơ bệnh nhân thành công!')
      } else {
        await createAdminPatient(payload)
        showToast('Tạo hồ sơ bệnh nhân mới thành công!')
      }
      setShowModal(false)
      loadData()
    } catch (err) {
      showToast(err?.response?.data?.message || 'Có lỗi xảy ra', 'err')
    } finally {
      setSaving(false)
    }
  }

  const viewHistory = async (p) => {
    setHistoryTarget(p)
    setLoadingRecords(true)
    try {
      const res = await getMedicalRecordsByPatient(p.id)
      setMedicalRecords(res.data || [])
    } catch {
      setMedicalRecords([])
    } finally {
      setLoadingRecords(false)
    }
  }

  return (
    <AdminLayout>
      <div style={{ maxWidth: 1200, margin: '0 auto' }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24, flexWrap: 'wrap', gap: 16 }}>
          <div>
            <h1 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 28, fontWeight: 700, color: '#2B1A0F', margin: 0 }}>
              👥 Quản lý hồ sơ bệnh nhân & Bệnh án
            </h1>
            <div style={{ fontSize: 16, color: '#6B5E4E', marginTop: 4 }}>
              Tra cứu thông tin cá nhân, tiền sử bệnh và lịch sử các lần điều trị
            </div>
          </div>

          <button
            onClick={openAdd}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 8,
              minHeight: 48, padding: '0 22px', borderRadius: 12,
              background: '#2D5A27', color: '#fff', fontSize: 17, fontWeight: 700, border: 'none', cursor: 'pointer'
            }}
          >
            + Thêm bệnh nhân mới
          </button>
        </div>

        {/* Thanh tìm kiếm */}
        <div style={{ background: '#fff', padding: '16px 20px', borderRadius: 16, border: '1px solid #E0D3BC', marginBottom: 24, display: 'flex', gap: 14 }}>
          <input
            type="text"
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Tra cứu theo Tên, Số điện thoại hoặc Mã bệnh nhân (BN-...)"
            style={{ flex: 1, minHeight: 44, padding: '0 16px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, fontFamily: 'inherit' }}
          />
          <button
            onClick={loadData}
            style={{ minHeight: 44, padding: '0 20px', borderRadius: 10, background: '#5C3A21', color: '#fff', border: 'none', fontSize: 16, fontWeight: 600, cursor: 'pointer' }}
          >
            Tìm kiếm
          </button>
        </div>

        {/* Bảng danh sách */}
        {loading ? (
          <div style={{ textAlign: 'center', padding: 48, color: '#6B5E4E', fontSize: 18 }}>⏳ Đang tải dữ liệu bệnh nhân...</div>
        ) : patients.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 48, background: '#fff', borderRadius: 16, border: '1px solid #E0D3BC' }}>
            <div style={{ fontSize: 48, marginBottom: 12 }}>👤</div>
            <div style={{ fontSize: 20, fontWeight: 700, color: '#2B1A0F', marginBottom: 6 }}>Không tìm thấy bệnh nhân nào</div>
            <div style={{ fontSize: 16, color: '#6B5E4E' }}>Vui lòng thử tìm với từ khóa khác hoặc bấm "+ Thêm bệnh nhân mới".</div>
          </div>
        ) : (
          <div style={{ background: '#fff', borderRadius: 16, border: '1px solid #E0D3BC', overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', minWidth: 850 }}>
              <thead>
                <tr style={{ background: '#F8F5EE', borderBottom: '1.5px solid #E0D3BC', color: '#5C3A21', fontSize: 15, fontWeight: 700 }}>
                  <th style={{ padding: '14px 16px' }}>Mã BN</th>
                  <th style={{ padding: '14px 16px' }}>Họ và tên</th>
                  <th style={{ padding: '14px 16px' }}>Số điện thoại</th>
                  <th style={{ padding: '14px 16px' }}>Năm sinh / Giới tính</th>
                  <th style={{ padding: '14px 16px' }}>Địa chỉ</th>
                  <th style={{ padding: '14px 16px', textAlign: 'right' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {patients.map((p) => (
                  <tr key={p.id} style={{ borderBottom: '1px solid #F0EAE0', fontSize: 16 }}>
                    <td style={{ padding: '14px 16px' }}>
                      <span style={{ fontFamily: 'monospace', fontWeight: 800, color: '#2D5A27' }}>{p.patientCode}</span>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <strong style={{ color: '#1F1610' }}>{p.fullName}</strong>
                      {p.relationship && <span style={{ marginLeft: 6, fontSize: 13, background: '#F0EAE0', padding: '2px 8px', borderRadius: 99 }}>{p.relationship}</span>}
                    </td>
                    <td style={{ padding: '14px 16px' }}>{p.phone}</td>
                    <td style={{ padding: '14px 16px' }}>
                      {p.yearOfBirth} · {p.gender === 'MALE' ? 'Nam' : p.gender === 'FEMALE' ? 'Nữ' : 'Khác'}
                    </td>
                    <td style={{ padding: '14px 16px', color: '#6B5E4E' }}>{p.address || '—'}</td>
                    <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: 8 }}>
                        <button
                          onClick={() => viewHistory(p)}
                          style={{ padding: '6px 12px', borderRadius: 8, background: '#E4EFE2', color: '#2D5A27', border: '1px solid #2D5A27', fontSize: 14, fontWeight: 700, cursor: 'pointer' }}
                        >
                          📜 Bệnh án ({p.patientCode})
                        </button>
                        <button
                          onClick={() => openEdit(p)}
                          style={{ padding: '6px 12px', borderRadius: 8, background: '#fff', color: '#5C3A21', border: '1px solid #E0D3BC', fontSize: 14, fontWeight: 600, cursor: 'pointer' }}
                        >
                          Sửa
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* ─── MODAL THÊM / SỬA HỒ SƠ ─── */}
        {showModal && (
          <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 300, padding: 16 }}>
            <div style={{ background: '#FDFBF7', borderRadius: 20, padding: '28px 24px', width: '100%', maxWidth: 540, maxHeight: '90vh', overflowY: 'auto' }}>
              <h2 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 24, fontWeight: 700, color: '#5C3A21', marginBottom: 18 }}>
                {editTarget ? '✏️ Chỉnh sửa hồ sơ bệnh nhân' : '➕ Thêm hồ sơ bệnh nhân mới'}
              </h2>

              <form onSubmit={handleSave} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                <div>
                  <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Họ và tên *</label>
                  <input
                    type="text" required value={patientForm.fullName} onChange={e => setPatientForm({ ...patientForm, fullName: e.target.value })}
                    style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Số điện thoại *</label>
                    <input
                      type="tel" required value={patientForm.phone} onChange={e => setPatientForm({ ...patientForm, phone: e.target.value })}
                      style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                    />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Năm sinh *</label>
                    <input
                      type="number" required min={1900} max={new Date().getFullYear()}
                      value={patientForm.yearOfBirth} onChange={e => setPatientForm({ ...patientForm, yearOfBirth: e.target.value })}
                      style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                    />
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Giới tính *</label>
                    <select
                      value={patientForm.gender} onChange={e => setPatientForm({ ...patientForm, gender: e.target.value })}
                      style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16 }}
                    >
                      <option value="MALE">Nam</option>
                      <option value="FEMALE">Nữ</option>
                      <option value="OTHER">Khác</option>
                    </select>
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Quan hệ</label>
                    <input
                      type="text" value={patientForm.relationship} onChange={e => setPatientForm({ ...patientForm, relationship: e.target.value })}
                      style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                    />
                  </div>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Địa chỉ cư trú</label>
                  <input
                    type="text" value={patientForm.address} onChange={e => setPatientForm({ ...patientForm, address: e.target.value })}
                    style={{ width: '100%', minHeight: 44, padding: '0 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: 15, fontWeight: 600, marginBottom: 4 }}>Tiền sử bệnh & Dị ứng thuốc</label>
                  <textarea
                    rows={2} value={patientForm.medicalHistory} onChange={e => setPatientForm({ ...patientForm, medicalHistory: e.target.value })}
                    placeholder="Tiểu đường, dị ứng thuốc kháng sinh..."
                    style={{ width: '100%', padding: '10px 12px', borderRadius: 10, border: '1.5px solid #E0D3BC', fontSize: 16, boxSizing: 'border-box' }}
                  />
                </div>

                <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 12 }}>
                  <button type="button" onClick={() => setShowModal(false)} style={{ padding: '0 18px', minHeight: 46, borderRadius: 10, background: '#fff', border: '1.5px solid #E0D3BC', fontSize: 16, cursor: 'pointer' }}>
                    Hủy bỏ
                  </button>
                  <button type="submit" disabled={saving} style={{ padding: '0 24px', minHeight: 46, borderRadius: 10, background: '#2D5A27', color: '#fff', border: 'none', fontSize: 16, fontWeight: 700, cursor: 'pointer' }}>
                    {saving ? 'Đang lưu...' : 'Lưu hồ sơ'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* ─── MODAL XEM LỊCH SỬ BỆNH ÁN ─── */}
        {historyTarget && (
          <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 320, padding: 16 }}>
            <div style={{ background: '#FDFBF7', borderRadius: 20, padding: '28px 24px', width: '100%', maxWidth: 700, maxHeight: '90vh', overflowY: 'auto' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', borderBottom: '1px solid #E0D3BC', paddingBottom: 14, marginBottom: 18 }}>
                <div>
                  <h2 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 24, fontWeight: 700, color: '#2D5A27', margin: 0 }}>
                    📜 Lịch sử khám bệnh & Đơn thuốc
                  </h2>
                  <div style={{ fontSize: 16, color: '#6B5E4E', marginTop: 4 }}>
                    Bệnh nhân: <strong>{historyTarget.fullName}</strong> ({historyTarget.patientCode}) · SĐT: {historyTarget.phone}
                  </div>
                </div>
                <button onClick={() => setHistoryTarget(null)} style={{ background: 'transparent', border: 'none', fontSize: 24, cursor: 'pointer', padding: 4 }}>×</button>
              </div>

              {loadingRecords ? (
                <div style={{ textAlign: 'center', padding: 32, color: '#6B5E4E' }}>⏳ Đang tải bệnh án...</div>
              ) : medicalRecords.length === 0 ? (
                <div style={{ textAlign: 'center', padding: 32, background: '#fff', borderRadius: 12 }}>
                  Bệnh nhân chưa có ghi nhận bệnh án điện tử nào từ bác sĩ.
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                  {medicalRecords.map((r) => (
                    <div key={r.id} style={{ background: '#fff', borderRadius: 14, border: '1.5px solid #2D5A27', padding: '16px 20px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10, borderBottom: '1px dashed #E0D3BC', paddingBottom: 8 }}>
                        <span style={{ fontWeight: 700, color: '#2D5A27', fontSize: 17 }}>
                          📅 Lần khám ngày: {r.appointmentDate}
                        </span>
                        <span style={{ fontSize: 14, color: '#6B5E4E' }}>Bác sĩ: <strong>{r.doctorName || 'Bác sĩ trực'}</strong></span>
                      </div>

                      <div style={{ display: 'flex', flexDirection: 'column', gap: 6, fontSize: 16 }}>
                        {r.pulseBp && <div><strong>Mạch & Huyết áp:</strong> {r.pulseBp}</div>}
                        <div><strong>Chẩn đoán YHCT:</strong> {r.diagnosis}</div>
                        {r.treatment && <div><strong>Liệu trình trị liệu:</strong> {r.treatment}</div>}
                        {r.prescription && (
                          <div style={{ background: '#F8F6F0', padding: '10px 14px', borderRadius: 10, marginTop: 4 }}>
                            <strong>Đơn thuốc:</strong>
                            <div style={{ whiteSpace: 'pre-line', marginTop: 4, color: '#5C3A21' }}>{r.prescription}</div>
                          </div>
                        )}
                        {r.advice && <div style={{ color: '#6B5E4E' }}><strong>Dặn dò:</strong> {r.advice}</div>}
                        {r.followUpDate && <div style={{ color: '#D97706', fontWeight: 600 }}>⏰ Hẹn tái khám: {r.followUpDate}</div>}
                      </div>
                    </div>
                  ))}
                </div>
              )}

              <div style={{ textAlign: 'right', marginTop: 20 }}>
                <button onClick={() => setHistoryTarget(null)} style={{ padding: '0 24px', minHeight: 46, borderRadius: 10, background: '#5C3A21', color: '#fff', border: 'none', fontSize: 16, fontWeight: 700, cursor: 'pointer' }}>
                  Đóng cửa sổ
                </button>
              </div>
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

