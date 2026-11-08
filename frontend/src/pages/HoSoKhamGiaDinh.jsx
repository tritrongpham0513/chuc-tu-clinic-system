import { useState, useEffect, useCallback } from 'react'
import { Link } from 'react-router-dom'
import PublicLayout from '../components/PublicLayout'
import Icon from '../components/Icon'
import { getMyProfiles, addProfile, updateProfile, deleteProfile } from '../api/patientProfile'

// ─── Hằng số ───────────────────────────────────────────────────────────────
const RELATIONSHIPS = ['Bản thân', 'Vợ/Chồng', 'Cha', 'Mẹ', 'Con', 'Anh/Chị/Em', 'Khác']
const GENDERS = [
  { value: 'MALE', label: 'Nam' },
  { value: 'FEMALE', label: 'Nữ' },
  { value: 'OTHER', label: 'Khác' },
]
const CURRENT_YEAR = new Date().getFullYear()

const C = {
  bg: '#FDFBF7',
  primary: '#5C3A21',
  green: '#2D5A27',
  greenLight: '#E4EFE2',
  gold: '#D97706',
  border: '#E0D3BC',
  text: '#1F1610',
  textSoft: '#6B5E4E',
  danger: '#90372A',
  dangerBg: '#FFF0EE',
}

const style = {
  page: { minHeight: '100vh', background: C.bg, paddingBottom: 120 },
  container: { maxWidth: 760, margin: '0 auto', padding: '32px 16px 24px' },
  h1: { fontFamily: "'Noto Serif', Georgia, serif", fontSize: 28, fontWeight: 700, color: C.primary, marginBottom: 6 },
  subtitle: { fontSize: 18, color: C.textSoft, marginBottom: 32 },

  // Card hồ sơ
  card: (isPrimary) => ({
    background: '#fff',
    border: `2px solid ${isPrimary ? C.green : C.border}`,
    borderRadius: 20,
    padding: '20px 24px',
    marginBottom: 16,
    display: 'flex',
    gap: 16,
    alignItems: 'flex-start',
    boxShadow: '0 2px 10px rgba(42,33,24,0.07)',
  }),
  badge: (isPrimary) => ({
    display: 'inline-block',
    padding: '3px 12px',
    borderRadius: 99,
    fontSize: 15,
    fontWeight: 600,
    background: isPrimary ? C.greenLight : '#F5EFE6',
    color: isPrimary ? C.green : C.textSoft,
    marginBottom: 6,
  }),
  cardName: { fontSize: 22, fontWeight: 700, color: C.text, marginBottom: 4 },
  cardMeta: { fontSize: 17, color: C.textSoft, lineHeight: 1.7 },

  // Nút
  btnPrimary: {
    display: 'inline-flex', alignItems: 'center', gap: 10,
    minHeight: 52, padding: '0 24px', borderRadius: 14,
    background: C.green, color: '#fff',
    border: 'none', fontSize: 19, fontWeight: 700, cursor: 'pointer',
  },
  btnSecondary: {
    display: 'inline-flex', alignItems: 'center', gap: 8,
    minHeight: 48, padding: '0 18px', borderRadius: 12,
    background: 'transparent', color: C.primary,
    border: `2px solid ${C.border}`, fontSize: 17, fontWeight: 600, cursor: 'pointer',
  },
  btnDanger: {
    display: 'inline-flex', alignItems: 'center', gap: 8,
    minHeight: 48, padding: '0 18px', borderRadius: 12,
    background: 'transparent', color: C.danger,
    border: `2px solid ${C.danger}20`, fontSize: 17, fontWeight: 600, cursor: 'pointer',
  },

  // Modal overlay
  overlay: {
    position: 'fixed', inset: 0,
    background: 'rgba(31,22,16,0.52)', backdropFilter: 'blur(2px)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    zIndex: 200, padding: 16,
  },
  modal: {
    background: C.bg, borderRadius: 24, padding: '32px 28px',
    width: '100%', maxWidth: 520, maxHeight: '90vh', overflowY: 'auto',
    boxShadow: '0 24px 64px rgba(31,22,16,0.28)',
  },
  modalTitle: { fontFamily: "'Noto Serif', Georgia, serif", fontSize: 24, fontWeight: 700, color: C.primary, marginBottom: 20 },

  // Form
  label: { display: 'block', fontSize: 17, fontWeight: 600, color: C.text, marginBottom: 6 },
  input: {
    width: '100%', minHeight: 52, padding: '0 16px',
    border: `1.5px solid ${C.border}`, borderRadius: 12,
    fontSize: 18, color: C.text, background: '#fff',
    fontFamily: 'inherit', boxSizing: 'border-box',
  },
  select: {
    width: '100%', minHeight: 52, padding: '0 16px',
    border: `1.5px solid ${C.border}`, borderRadius: 12,
    fontSize: 18, color: C.text, background: '#fff',
    fontFamily: 'inherit', cursor: 'pointer',
  },
  formGroup: { marginBottom: 18 },
  errorMsg: { fontSize: 15, color: C.danger, marginTop: 6 },
}

// ─── Component chính ────────────────────────────────────────────────────────
export default function HoSoKhamGiaDinh() {
  const [profiles, setProfiles] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // Modal thêm/sửa
  const [showForm, setShowForm] = useState(false)
  const [editTarget, setEditTarget] = useState(null)
  const [form, setForm] = useState(emptyForm())
  const [formError, setFormError] = useState({})
  const [saving, setSaving] = useState(false)

  // Modal xóa
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleting, setDeleting] = useState(false)

  // Toast
  const [toast, setToast] = useState(null)

  const showToast = (msg, type = 'ok') => {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const res = await getMyProfiles()
      setProfiles(res.data || [])
    } catch {
      setError('Không thể tải danh sách hồ sơ. Vui lòng thử lại.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  // ─── Form helpers ─────────────────────────────────────────────────────────
  function emptyForm() {
    return { fullName: '', phone: '', yearOfBirth: '', gender: 'MALE', address: '', relationship: 'Bản thân', medicalHistory: '' }
  }

  const openAdd = () => {
    setEditTarget(null)
    setForm(emptyForm())
    setFormError({})
    setShowForm(true)
  }

  const openEdit = (p) => {
    setEditTarget(p)
    setForm({
      fullName: p.fullName || '',
      phone: p.phone || '',
      yearOfBirth: p.yearOfBirth || '',
      gender: p.gender || 'MALE',
      address: p.address || '',
      relationship: p.relationship || 'Bản thân',
      medicalHistory: p.medicalHistory || '',
    })
    setFormError({})
    setShowForm(true)
  }

  const validate = () => {
    const errs = {}
    if (!form.fullName.trim()) errs.fullName = 'Vui lòng nhập họ tên'
    if (!form.phone.trim()) errs.phone = 'Vui lòng nhập số điện thoại'
    else if (!/^(0[35789])\d{8}$/.test(form.phone.trim())) errs.phone = 'Số điện thoại không đúng định dạng'
    if (!form.yearOfBirth) errs.yearOfBirth = 'Vui lòng nhập năm sinh'
    else {
      const y = parseInt(form.yearOfBirth, 10)
      if (y < 1900 || y > CURRENT_YEAR) errs.yearOfBirth = `Năm sinh phải từ 1900 đến ${CURRENT_YEAR}`
    }
    if (!form.relationship) errs.relationship = 'Vui lòng chọn quan hệ'
    return errs
  }

  const handleSave = async () => {
    const errs = validate()
    if (Object.keys(errs).length) { setFormError(errs); return }
    setSaving(true)
    try {
      const payload = { ...form, yearOfBirth: parseInt(form.yearOfBirth, 10) }
      if (editTarget) {
        await updateProfile(editTarget.id, payload)
        showToast('Cập nhật hồ sơ thành công!')
      } else {
        await addProfile(payload)
        showToast('Thêm hồ sơ thành công!')
      }
      setShowForm(false)
      load()
    } catch (err) {
      showToast(err?.response?.data?.message || 'Có lỗi xảy ra, vui lòng thử lại', 'err')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async () => {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await deleteProfile(deleteTarget.id)
      showToast('Đã xóa hồ sơ thành công!')
      setDeleteTarget(null)
      load()
    } catch (err) {
      showToast(err?.response?.data?.message || 'Có lỗi xảy ra', 'err')
      setDeleteTarget(null)
    } finally {
      setDeleting(false)
    }
  }

  // ─── Render ────────────────────────────────────────────────────────────────
  return (
    <div style={style.page}>
      <div style={style.container}>
        {/* Breadcrumb */}
        <nav style={{ fontSize: 16, color: C.textSoft, marginBottom: 20, display: 'flex', gap: 6, alignItems: 'center' }}>
          <Link to="/" style={{ color: C.green, textDecoration: 'none' }}>Trang chủ</Link>
          <span>›</span>
          <span>Hồ sơ khám gia đình</span>
        </nav>

        <h1 style={style.h1}>📋 Hồ sơ khám gia đình</h1>
        <p style={style.subtitle}>Quản lý tối đa 10 hồ sơ bệnh nhân trong gia đình bạn</p>

        {/* Nút thêm */}
        {profiles.length < 10 && (
          <button style={{ ...style.btnPrimary, marginBottom: 28 }} onClick={openAdd}>
            <Icon name="user-plus" size={22} stroke="#fff" />
            Thêm hồ sơ người thân
          </button>
        )}

        {/* Loading */}
        {loading && (
          <div style={{ textAlign: 'center', padding: 48, color: C.textSoft, fontSize: 20 }}>
            <span>⏳ Đang tải...</span>
          </div>
        )}

        {/* Lỗi */}
        {!loading && error && (
          <div style={{ background: C.dangerBg, border: `1px solid ${C.danger}30`, borderRadius: 14, padding: '18px 20px', color: C.danger, fontSize: 18, marginBottom: 24 }}>
            {error}
          </div>
        )}

        {/* Danh sách hồ sơ */}
        {!loading && !error && profiles.length === 0 && (
          <div style={{ textAlign: 'center', padding: '48px 24px', color: C.textSoft }}>
            <div style={{ fontSize: 56, marginBottom: 16 }}>👤</div>
            <div style={{ fontSize: 20, fontWeight: 600, marginBottom: 8 }}>Chưa có hồ sơ nào</div>
            <div style={{ fontSize: 17, marginBottom: 24 }}>Thêm hồ sơ của bạn hoặc người thân để đặt lịch khám.</div>
            <button style={style.btnPrimary} onClick={openAdd}>
              <Icon name="user-plus" size={22} stroke="#fff" />
              Thêm hồ sơ ngay
            </button>
          </div>
        )}

        {!loading && profiles.map(p => (
          <div key={p.id} style={style.card(p.isPrimary)}>
            {/* Avatar */}
            <div style={{
              width: 60, height: 60, borderRadius: '50%', flexShrink: 0,
              background: p.isPrimary ? C.greenLight : '#F5EFE6',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <Icon name="user" size={30} stroke={p.isPrimary ? C.green : C.primary} />
            </div>

            {/* Nội dung */}
            <div style={{ flex: 1, minWidth: 0 }}>
              <div>
                <span style={style.badge(p.isPrimary)}>{p.relationship || 'Bản thân'}</span>
                {p.isPrimary && <span style={{ marginLeft: 8, fontSize: 14, color: C.green, fontWeight: 600 }}>✓ Chính chủ</span>}
              </div>
              <div style={style.cardName}>{p.fullName}</div>
              <div style={style.cardMeta}>
                📱 {p.phone} &nbsp;·&nbsp; Năm sinh: {p.yearOfBirth} &nbsp;·&nbsp;
                {p.gender === 'MALE' ? '♂ Nam' : p.gender === 'FEMALE' ? '♀ Nữ' : '⚧ Khác'}
              </div>
              {p.address && <div style={{ ...style.cardMeta, marginTop: 2 }}>📍 {p.address}</div>}
              {p.medicalHistory && (
                <div style={{ marginTop: 6, padding: '8px 12px', background: '#FFF8F0', borderRadius: 10, fontSize: 16, color: C.textSoft }}>
                  💊 Tiền sử: {p.medicalHistory}
                </div>
              )}
            </div>

            {/* Actions */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10, flexShrink: 0 }}>
              <button style={style.btnSecondary} onClick={() => openEdit(p)} title="Chỉnh sửa hồ sơ">
                <Icon name="pencil" size={18} stroke={C.primary} />
                Sửa
              </button>
              {!p.isPrimary && (
                <button style={style.btnDanger} onClick={() => setDeleteTarget(p)} title="Xóa hồ sơ">
                  <Icon name="trash-2" size={18} stroke={C.danger} />
                  Xóa
                </button>
              )}
            </div>
          </div>
        ))}

        {/* Ghi chú */}
        {profiles.length >= 10 && (
          <div style={{ background: '#FFF8E6', border: `1px solid ${C.gold}50`, borderRadius: 14, padding: '14px 18px', fontSize: 17, color: C.primary, marginTop: 8 }}>
            ⚠️ Tài khoản đã có tối đa 10 hồ sơ. Vui lòng xóa hồ sơ cũ để thêm mới.
          </div>
        )}
      </div>

      {/* ─── STICKY BOTTOM BAR ─── */}
      <StickyBar />

      {/* ─── MODAL THÊM / SỬA ─── */}
      {showForm && (
        <div style={style.overlay} onClick={e => { if (e.target === e.currentTarget) setShowForm(false) }}>
          <div style={style.modal}>
            <h2 style={style.modalTitle}>{editTarget ? '✏️ Chỉnh sửa hồ sơ' : '➕ Thêm hồ sơ người thân'}</h2>

            <div style={style.formGroup}>
              <label style={style.label}>Họ và tên *</label>
              <input style={style.input} placeholder="Nguyễn Văn A" value={form.fullName}
                onChange={e => setForm(f => ({ ...f, fullName: e.target.value }))} />
              {formError.fullName && <div style={style.errorMsg}>{formError.fullName}</div>}
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
              <div style={style.formGroup}>
                <label style={style.label}>Số điện thoại *</label>
                <input style={style.input} placeholder="0901234567" value={form.phone}
                  onChange={e => setForm(f => ({ ...f, phone: e.target.value }))} />
                {formError.phone && <div style={style.errorMsg}>{formError.phone}</div>}
              </div>
              <div style={style.formGroup}>
                <label style={style.label}>Năm sinh *</label>
                <input style={style.input} placeholder={`${CURRENT_YEAR - 30}`} type="number"
                  min={1900} max={CURRENT_YEAR} value={form.yearOfBirth}
                  onChange={e => setForm(f => ({ ...f, yearOfBirth: e.target.value }))} />
                {formError.yearOfBirth && <div style={style.errorMsg}>{formError.yearOfBirth}</div>}
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
              <div style={style.formGroup}>
                <label style={style.label}>Giới tính *</label>
                <select style={style.select} value={form.gender}
                  onChange={e => setForm(f => ({ ...f, gender: e.target.value }))}>
                  {GENDERS.map(g => <option key={g.value} value={g.value}>{g.label}</option>)}
                </select>
              </div>
              <div style={style.formGroup}>
                <label style={style.label}>Quan hệ *</label>
                <select style={style.select} value={form.relationship}
                  onChange={e => setForm(f => ({ ...f, relationship: e.target.value }))}>
                  {RELATIONSHIPS.map(r => <option key={r} value={r}>{r}</option>)}
                </select>
                {formError.relationship && <div style={style.errorMsg}>{formError.relationship}</div>}
              </div>
            </div>

            <div style={style.formGroup}>
              <label style={style.label}>Địa chỉ</label>
              <input style={style.input} placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành"
                value={form.address} onChange={e => setForm(f => ({ ...f, address: e.target.value }))} />
            </div>

            <div style={style.formGroup}>
              <label style={style.label}>Tiền sử bệnh</label>
              <textarea style={{ ...style.input, height: 80, padding: '12px 16px', resize: 'vertical' }}
                placeholder="Ghi những bệnh mãn tính, thuốc đang dùng (nếu có)..."
                value={form.medicalHistory}
                onChange={e => setForm(f => ({ ...f, medicalHistory: e.target.value }))} />
            </div>

            <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 8 }}>
              <button style={style.btnSecondary} onClick={() => setShowForm(false)} disabled={saving}>Hủy bỏ</button>
              <button style={{ ...style.btnPrimary, opacity: saving ? 0.7 : 1 }} onClick={handleSave} disabled={saving}>
                {saving ? '⏳ Đang lưu...' : editTarget ? '💾 Cập nhật' : '✅ Thêm hồ sơ'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ─── MODAL XÓA ─── */}
      {deleteTarget && (
        <div style={style.overlay} onClick={e => { if (e.target === e.currentTarget) setDeleteTarget(null) }}>
          <div style={{ ...style.modal, maxWidth: 440 }}>
            <div style={{ fontSize: 48, textAlign: 'center', marginBottom: 16 }}>⚠️</div>
            <h2 style={{ ...style.modalTitle, textAlign: 'center', color: C.danger }}>Xóa hồ sơ?</h2>
            <p style={{ fontSize: 18, color: C.textSoft, textAlign: 'center', marginBottom: 8 }}>
              Bạn muốn xóa hồ sơ của <strong style={{ color: C.text }}>{deleteTarget.fullName}</strong>?
            </p>
            <p style={{ fontSize: 16, color: C.textSoft, textAlign: 'center', marginBottom: 24, padding: '12px', background: '#FFF8F0', borderRadius: 10 }}>
              Nếu hồ sơ này đã có lịch sử khám, hệ thống sẽ chỉ ngắt liên kết (không xóa lịch sử).
            </p>
            <div style={{ display: 'flex', gap: 12, justifyContent: 'center' }}>
              <button style={style.btnSecondary} onClick={() => setDeleteTarget(null)} disabled={deleting}>Giữ lại</button>
              <button style={{ ...style.btnDanger, background: C.dangerBg }} onClick={handleDelete} disabled={deleting}>
                {deleting ? '⏳ Đang xóa...' : '🗑️ Xác nhận xóa'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ─── TOAST ─── */}
      {toast && (
        <div style={{
          position: 'fixed', bottom: 100, left: '50%', transform: 'translateX(-50%)',
          background: toast.type === 'err' ? C.danger : C.green,
          color: '#fff', padding: '14px 28px', borderRadius: 16,
          fontSize: 18, fontWeight: 600, zIndex: 999,
          boxShadow: '0 8px 24px rgba(0,0,0,0.25)',
          animation: 'fadeIn 0.2s ease',
        }}>
          {toast.type === 'err' ? '❌' : '✅'} {toast.msg}
        </div>
      )}
    </div>
  )
}

// ─── Sticky Bottom Bar ─────────────────────────────────────────────────────
function StickyBar() {
  return (
    <div style={{
      position: 'fixed', bottom: 0, left: 0, right: 0,
      background: '#fff', borderTop: '1px solid #E0D3BC',
      display: 'flex', zIndex: 100,
      boxShadow: '0 -4px 16px rgba(42,33,24,0.10)',
    }}>
      <a href="tel:0900000000" style={{
        flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
        padding: '12px 0', color: '#fff', background: '#2D5A27',
        textDecoration: 'none', fontSize: 16, fontWeight: 700, gap: 4,
        minHeight: 64,
      }}>
        <span style={{ fontSize: 24 }}>📞</span>
        <span>Gọi Hotline</span>
      </a>
      <a
        href="https://www.google.com/maps/search/?api=1&query=Đường+Điện+Biên+Phủ+TP+Hồ+Chí+Minh"
        target="_blank" rel="noopener noreferrer"
        style={{
          flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
          padding: '12px 0', color: '#fff', background: '#5C3A21',
          textDecoration: 'none', fontSize: 16, fontWeight: 700, gap: 4,
          minHeight: 64,
        }}>
        <span style={{ fontSize: 24 }}>📍</span>
        <span>Chỉ đường</span>
      </a>
    </div>
  )
}

