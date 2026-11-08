import { useState, useEffect, useCallback } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Icon from '../components/Icon'
import { getAvailableSlots } from '../api/booking'
import { getMyProfiles } from '../api/patientProfile'
import { bookAppointment } from '../api/booking'

// ─── Màu sắc & style ───────────────────────────────────────────────────────
const C = {
  bg: '#FDFBF7', primary: '#5C3A21', green: '#2D5A27', greenLight: '#E4EFE2',
  gold: '#D97706', border: '#E0D3BC', text: '#1F1610', textSoft: '#6B5E4E',
  danger: '#90372A', dangerBg: '#FFF0EE',
}

// Sinh danh sách ngày (hôm nay → +14)
function buildDateList(advanceDays = 14) {
  const days = []
  const today = new Date()
  const weekdays = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']
  const months = ['Th.1','Th.2','Th.3','Th.4','Th.5','Th.6','Th.7','Th.8','Th.9','Th.10','Th.11','Th.12']
  for (let i = 0; i <= advanceDays; i++) {
    const d = new Date(today)
    d.setDate(today.getDate() + i)
    const yyyy = d.getFullYear()
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    days.push({
      iso: `${yyyy}-${mm}-${dd}`,
      dow: weekdays[d.getDay()],
      day: dd,
      month: months[d.getMonth()],
      isToday: i === 0,
    })
  }
  return days
}

export default function DatLichKham() {
  const navigate = useNavigate()
  // Stepper: 0=Chọn ngày/giờ, 1=Chọn hồ sơ, 2=Xác nhận, 3=Thành công
  const [step, setStep] = useState(0)

  const dateList = buildDateList(14)
  const [selectedDate, setSelectedDate] = useState(dateList[0].iso)
  const [slots, setSlots] = useState([])
  const [slotsLoading, setSlotsLoading] = useState(false)
  const [selectedSlot, setSelectedSlot] = useState(null)

  const [profiles, setProfiles] = useState([])
  const [profilesLoading, setProfilesLoading] = useState(false)
  const [selectedProfile, setSelectedProfile] = useState(null)
  const [symptoms, setSymptoms] = useState('')

  const [booking, setBooking] = useState(false)
  const [bookingResult, setBookingResult] = useState(null)
  const [bookingError, setBookingError] = useState('')

  // Load slots khi chọn ngày
  const loadSlots = useCallback(async (date) => {
    setSlotsLoading(true)
    setSelectedSlot(null)
    try {
      const res = await getAvailableSlots(date)
      setSlots(res.data || [])
    } catch {
      setSlots([])
    } finally {
      setSlotsLoading(false)
    }
  }, [])

  useEffect(() => { loadSlots(selectedDate) }, [selectedDate, loadSlots])

  // Load profiles ở bước 1
  useEffect(() => {
    if (step === 1) {
      setProfilesLoading(true)
      getMyProfiles().then(res => setProfiles(res.data || [])).catch(() => setProfiles([])).finally(() => setProfilesLoading(false))
    }
  }, [step])

  const handleBook = async () => {
    if (!selectedSlot || !selectedProfile) return
    setBooking(true)
    setBookingError('')
    try {
      const res = await bookAppointment({
        appointmentDate: selectedDate,
        timeSlotId: selectedSlot.id,
        patientId: selectedProfile.id,
        symptoms: symptoms.trim() || null,
      })
      setBookingResult(res.data)
      setStep(3)
    } catch (err) {
      setBookingError(err?.response?.data?.message || 'Có lỗi xảy ra, vui lòng thử lại!')
    } finally {
      setBooking(false)
    }
  }

  // ─── Shared style ──────────────────────────────────────────────────────────
  const S = {
    page: { minHeight: '100vh', background: C.bg, paddingBottom: 120 },
    container: { maxWidth: 720, margin: '0 auto', padding: '28px 16px 24px' },
    h1: { fontFamily: "'Noto Serif', Georgia, serif", fontSize: 28, fontWeight: 700, color: C.primary, marginBottom: 6 },
    btnPrimary: (disabled) => ({
      display: 'inline-flex', alignItems: 'center', gap: 10,
      minHeight: 56, padding: '0 32px', borderRadius: 16,
      background: disabled ? '#ccc' : C.green, color: '#fff',
      border: 'none', fontSize: 20, fontWeight: 700, cursor: disabled ? 'not-allowed' : 'pointer',
      width: '100%', justifyContent: 'center',
    }),
    btnSecondary: {
      display: 'inline-flex', alignItems: 'center', gap: 8,
      minHeight: 52, padding: '0 24px', borderRadius: 14,
      background: 'transparent', color: C.primary,
      border: `2px solid ${C.border}`, fontSize: 18, fontWeight: 600, cursor: 'pointer',
    },
  }

  // ─── Stepper Header ────────────────────────────────────────────────────────
  const steps = ['Chọn ngày & giờ', 'Chọn hồ sơ', 'Xác nhận']
  const StepBar = () => (
    <div style={{ display: 'flex', gap: 0, marginBottom: 32, borderRadius: 14, overflow: 'hidden', border: `1.5px solid ${C.border}` }}>
      {steps.map((s, i) => (
        <div key={i} style={{
          flex: 1, padding: '14px 8px', textAlign: 'center',
          fontSize: 16, fontWeight: 600,
          background: i === step ? C.green : i < step ? C.greenLight : '#fff',
          color: i === step ? '#fff' : i < step ? C.green : C.textSoft,
          borderRight: i < 2 ? `1px solid ${C.border}` : 'none',
        }}>
          <span style={{ display: 'block', fontSize: 13, marginBottom: 2 }}>Bước {i + 1}</span>
          {s}
        </div>
      ))}
    </div>
  )

  // ─── BƯỚC 0: Chọn ngày & giờ ─────────────────────────────────────────────
  if (step === 0) return (
    <div style={S.page}>
      <div style={S.container}>
        <nav style={{ fontSize: 16, color: C.textSoft, marginBottom: 20, display: 'flex', gap: 6 }}>
          <Link to="/" style={{ color: C.green, textDecoration: 'none' }}>Trang chủ</Link>
          <span>›</span><span>Đặt lịch khám</span>
        </nav>
        <h1 style={S.h1}>🗓️ Đặt lịch khám</h1>
        <p style={{ fontSize: 18, color: C.textSoft, marginBottom: 28 }}>Chọn ngày và khung giờ phù hợp với bạn</p>

        <StepBar />

        {/* Chọn ngày */}
        <section style={{ marginBottom: 28 }}>
          <h2 style={{ fontSize: 20, fontWeight: 700, color: C.primary, marginBottom: 14 }}>📅 Chọn ngày khám</h2>
          <div style={{ display: 'flex', gap: 10, overflowX: 'auto', paddingBottom: 8 }}>
            {dateList.map(d => {
              const sel = d.iso === selectedDate
              return (
                <button key={d.iso} onClick={() => setSelectedDate(d.iso)} style={{
                  flexShrink: 0, minWidth: 68, padding: '12px 10px',
                  borderRadius: 14, border: `2px solid ${sel ? C.green : C.border}`,
                  background: sel ? C.green : '#fff', cursor: 'pointer',
                  display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2,
                }}>
                  <span style={{ fontSize: 14, color: sel ? '#fff' : C.textSoft, fontWeight: 600 }}>{d.dow}</span>
                  <span style={{ fontSize: 24, fontWeight: 800, color: sel ? '#fff' : C.text, lineHeight: 1 }}>{d.day}</span>
                  <span style={{ fontSize: 13, color: sel ? '#cfe8cc' : C.textSoft }}>{d.month}</span>
                  {d.isToday && <span style={{ fontSize: 11, color: sel ? '#cfe8cc' : C.gold, fontWeight: 700 }}>Hôm nay</span>}
                </button>
              )
            })}
          </div>
        </section>

        {/* Chọn khung giờ */}
        <section style={{ marginBottom: 32 }}>
          <h2 style={{ fontSize: 20, fontWeight: 700, color: C.primary, marginBottom: 14 }}>🕐 Chọn khung giờ</h2>

          {slotsLoading ? (
            <div style={{ textAlign: 'center', padding: 32, color: C.textSoft, fontSize: 18 }}>⏳ Đang tải...</div>
          ) : (
            <>
              {/* Buổi sáng */}
              {slots.filter(s => s.period === 'MORNING').length > 0 && (
                <div style={{ marginBottom: 20 }}>
                  <div style={{ fontSize: 17, fontWeight: 600, color: C.textSoft, marginBottom: 10 }}>☀️ Buổi sáng</div>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
                    {slots.filter(s => s.period === 'MORNING').map(sl => <SlotBtn key={sl.id} sl={sl} selected={selectedSlot?.id === sl.id} onSelect={setSelectedSlot} />)}
                  </div>
                </div>
              )}
              {/* Buổi chiều */}
              {slots.filter(s => s.period === 'AFTERNOON').length > 0 && (
                <div>
                  <div style={{ fontSize: 17, fontWeight: 600, color: C.textSoft, marginBottom: 10 }}>🌤️ Buổi chiều</div>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
                    {slots.filter(s => s.period === 'AFTERNOON').map(sl => <SlotBtn key={sl.id} sl={sl} selected={selectedSlot?.id === sl.id} onSelect={setSelectedSlot} />)}
                  </div>
                </div>
              )}
              {slots.length === 0 && (
                <div style={{ textAlign: 'center', padding: 32, color: C.textSoft, fontSize: 18 }}>
                  Không có khung giờ khả dụng vào ngày này.
                </div>
              )}
            </>
          )}
        </section>

        <button
          style={S.btnPrimary(!selectedSlot)}
          disabled={!selectedSlot}
          onClick={() => setStep(1)}>
          Tiếp theo: Chọn hồ sơ bệnh nhân →
        </button>
      </div>
      <StickyBar />
    </div>
  )

  // ─── BƯỚC 1: Chọn hồ sơ ──────────────────────────────────────────────────
  if (step === 1) return (
    <div style={S.page}>
      <div style={S.container}>
        <h1 style={S.h1}>🗓️ Đặt lịch khám</h1>
        <StepBar />

        <div style={{ background: C.greenLight, borderRadius: 14, padding: '14px 18px', marginBottom: 24, fontSize: 17 }}>
          <strong>Ngày khám:</strong> {selectedDate} &nbsp;·&nbsp;
          <strong>Khung giờ:</strong> {selectedSlot?.slotLabel}
        </div>

        <h2 style={{ fontSize: 20, fontWeight: 700, color: C.primary, marginBottom: 16 }}>👤 Chọn hồ sơ bệnh nhân</h2>

        {profilesLoading ? (
          <div style={{ textAlign: 'center', padding: 32, color: C.textSoft, fontSize: 18 }}>⏳ Đang tải...</div>
        ) : profiles.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 32, color: C.textSoft }}>
            <div style={{ fontSize: 18, marginBottom: 16 }}>Bạn chưa có hồ sơ nào. Vui lòng thêm hồ sơ trước.</div>
            <Link to="/tai-khoan/ho-so-kham-benh" style={{ ...S.btnPrimary(false), display: 'inline-flex', width: 'auto', textDecoration: 'none' }}>
              Thêm hồ sơ ngay
            </Link>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginBottom: 28 }}>
            {profiles.map(p => {
              const sel = selectedProfile?.id === p.id
              return (
                <button key={p.id} onClick={() => setSelectedProfile(p)} style={{
                  textAlign: 'left', border: `2px solid ${sel ? C.green : C.border}`,
                  borderRadius: 16, padding: '16px 20px', background: sel ? C.greenLight : '#fff',
                  cursor: 'pointer', display: 'flex', gap: 16, alignItems: 'center',
                }}>
                  <div style={{ width: 48, height: 48, borderRadius: '50%', background: sel ? C.green : '#F5EFE6', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                    <Icon name="user" size={24} stroke={sel ? '#fff' : C.primary} />
                  </div>
                  <div>
                    <div style={{ fontSize: 20, fontWeight: 700, color: C.text }}>{p.fullName}</div>
                    <div style={{ fontSize: 16, color: C.textSoft }}>
                      {p.relationship} · Năm sinh: {p.yearOfBirth} · {p.phone}
                    </div>
                  </div>
                  {sel && <span style={{ marginLeft: 'auto', color: C.green, fontSize: 24 }}>✓</span>}
                </button>
              )
            })}
          </div>
        )}

        {selectedProfile && (
          <div style={{ marginBottom: 24 }}>
            <label style={{ display: 'block', fontSize: 18, fontWeight: 600, color: C.text, marginBottom: 8 }}>
              💬 Triệu chứng / Lý do khám (tùy chọn)
            </label>
            <textarea
              style={{ width: '100%', minHeight: 90, padding: '12px 16px', border: `1.5px solid ${C.border}`, borderRadius: 12, fontSize: 18, fontFamily: 'inherit', resize: 'vertical', boxSizing: 'border-box' }}
              placeholder="Mô tả triệu chứng để bác sĩ chuẩn bị tốt hơn..."
              value={symptoms}
              onChange={e => setSymptoms(e.target.value)}
            />
          </div>
        )}

        <div style={{ display: 'flex', gap: 12 }}>
          <button style={S.btnSecondary} onClick={() => setStep(0)}>← Quay lại</button>
          <button style={{ ...S.btnPrimary(!selectedProfile), flex: 1 }} disabled={!selectedProfile} onClick={() => setStep(2)}>
            Tiếp theo: Xác nhận →
          </button>
        </div>
      </div>
      <StickyBar />
    </div>
  )

  // ─── BƯỚC 2: Xác nhận ────────────────────────────────────────────────────
  if (step === 2) return (
    <div style={S.page}>
      <div style={S.container}>
        <h1 style={S.h1}>🗓️ Đặt lịch khám</h1>
        <StepBar />

        <h2 style={{ fontSize: 22, fontWeight: 700, color: C.primary, marginBottom: 20 }}>✅ Xác nhận thông tin</h2>

        {/* Phiếu xác nhận kiểu cuống vé */}
        <div style={{
          background: '#fff', border: `2px solid ${C.green}`, borderRadius: 20,
          overflow: 'hidden', marginBottom: 28,
          boxShadow: '0 4px 20px rgba(45,90,39,0.12)',
        }}>
          <div style={{ background: C.green, padding: '18px 24px', color: '#fff' }}>
            <div style={{ fontSize: 15, opacity: 0.85 }}>Phòng Khám Chúc Từ · Y học cổ truyền</div>
            <div style={{ fontSize: 24, fontWeight: 800, marginTop: 4 }}>PHIẾU KHÁM BỆNH</div>
          </div>
          <div style={{ padding: '20px 24px' }}>
            <Row label="📅 Ngày khám" value={formatDateVN(selectedDate)} />
            <Row label="🕐 Khung giờ" value={selectedSlot?.slotLabel} />
            <Row label="👤 Bệnh nhân" value={selectedProfile?.fullName} />
            <Row label="📱 Số điện thoại" value={selectedProfile?.phone} />
            <Row label="🎂 Năm sinh" value={selectedProfile?.yearOfBirth} />
            <Row label="🔗 Quan hệ" value={selectedProfile?.relationship} />
            {symptoms && <Row label="💬 Triệu chứng" value={symptoms} />}
          </div>
          <div style={{ borderTop: `1px dashed ${C.border}`, padding: '14px 24px', fontSize: 15, color: C.textSoft }}>
            Phiếu sẽ được xác nhận ngay sau khi đặt thành công.
          </div>
        </div>

        {bookingError && (
          <div style={{ background: '#FFF0EE', border: `1px solid ${C.danger}30`, borderRadius: 12, padding: '14px 18px', color: C.danger, fontSize: 18, marginBottom: 20 }}>
            ❌ {bookingError}
          </div>
        )}

        <div style={{ display: 'flex', gap: 12 }}>
          <button style={S.btnSecondary} onClick={() => setStep(1)} disabled={booking}>← Quay lại</button>
          <button style={{ ...S.btnPrimary(booking), flex: 1 }} onClick={handleBook} disabled={booking}>
            {booking ? '⏳ Đang đặt lịch...' : '🎉 Xác nhận đặt lịch'}
          </button>
        </div>
      </div>
      <StickyBar />
    </div>
  )

  // ─── BƯỚC 3: Thành công ───────────────────────────────────────────────────
  if (step === 3 && bookingResult) return (
    <div style={S.page}>
      <div style={S.container}>
        <div style={{ textAlign: 'center', paddingTop: 32 }}>
          <div style={{ fontSize: 72, marginBottom: 16 }}>🎉</div>
          <h1 style={{ ...S.h1, textAlign: 'center', color: C.green }}>Đặt lịch thành công!</h1>
          <p style={{ fontSize: 19, color: C.textSoft, marginBottom: 32 }}>
            Chúng tôi đã ghi nhận lịch khám của bạn. Vui lòng đến đúng giờ.
          </p>

          {/* Mã phiếu */}
          <div style={{ background: C.greenLight, border: `2px solid ${C.green}`, borderRadius: 20, padding: '24px', marginBottom: 28, display: 'inline-block', minWidth: 320 }}>
            <div style={{ fontSize: 15, color: C.textSoft, marginBottom: 6 }}>Mã phiếu khám của bạn</div>
            <div style={{ fontSize: 32, fontWeight: 900, color: C.green, letterSpacing: 2, fontFamily: 'monospace' }}>
              {bookingResult.bookingCode}
            </div>
            <div style={{ marginTop: 12, fontSize: 17, color: C.text }}>
              📅 {formatDateVN(bookingResult.appointmentDate)} &nbsp;·&nbsp; 🕐 {bookingResult.timeSlotLabel}
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <Link to="/phieu-kham" style={{ ...S.btnPrimary(false), textDecoration: 'none', display: 'flex' }}>
              📋 Xem phiếu khám của tôi
            </Link>
            <button style={S.btnSecondary} onClick={() => { setStep(0); setSelectedSlot(null); setSelectedProfile(null); setSymptoms(''); setBookingResult(null) }}>
              ➕ Đặt thêm lịch khám
            </button>
          </div>
        </div>
      </div>
      <StickyBar />
    </div>
  )

  return null
}

// ─── SlotBtn component ─────────────────────────────────────────────────────
function SlotBtn({ sl, selected, onSelect }) {
  const full = sl.isFull
  return (
    <button
      onClick={() => !full && onSelect(sl)}
      disabled={full}
      style={{
        minWidth: 120, padding: '14px 12px', borderRadius: 14,
        border: `2px solid ${selected ? '#2D5A27' : full ? '#E0D3BC' : '#E0D3BC'}`,
        background: selected ? '#2D5A27' : full ? '#F5F5F5' : '#fff',
        cursor: full ? 'not-allowed' : 'pointer',
        display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4,
        opacity: full ? 0.5 : 1,
      }}>
      <span style={{ fontSize: 18, fontWeight: 700, color: selected ? '#fff' : '#1F1610' }}>{sl.slotLabel}</span>
      <span style={{ fontSize: 14, color: selected ? '#cfe8cc' : full ? '#999' : '#6B5E4E', fontWeight: 500 }}>
        {full ? 'Hết chỗ' : `Còn ${sl.available} chỗ`}
      </span>
    </button>
  )
}

// ─── Row helper ───────────────────────────────────────────────────────────
function Row({ label, value }) {
  return (
    <div style={{ display: 'flex', gap: 12, padding: '10px 0', borderBottom: '1px solid #F0EAE0' }}>
      <span style={{ width: 160, flexShrink: 0, fontSize: 17, color: '#6B5E4E', fontWeight: 600 }}>{label}</span>
      <span style={{ fontSize: 18, color: '#1F1610', fontWeight: 500 }}>{value || '—'}</span>
    </div>
  )
}

// Format ngày VN
function formatDateVN(iso) {
  if (!iso) return ''
  const [y, m, d] = iso.split('-')
  return `${d}/${m}/${y}`
}

// ─── Sticky Bottom Bar ─────────────────────────────────────────────────────
function StickyBar() {
  return (
    <div style={{ position: 'fixed', bottom: 0, left: 0, right: 0, background: '#fff', borderTop: '1px solid #E0D3BC', display: 'flex', zIndex: 100, boxShadow: '0 -4px 16px rgba(42,33,24,0.10)' }}>
      <a href="tel:0900000000" style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '12px 0', color: '#fff', background: '#2D5A27', textDecoration: 'none', fontSize: 16, fontWeight: 700, gap: 4, minHeight: 64 }}>
        <span style={{ fontSize: 24 }}>📞</span><span>Gọi Hotline</span>
      </a>
      <a href="https://www.google.com/maps/search/?api=1&query=Đường+Điện+Biên+Phủ+TP+Hồ+Chí+Minh" target="_blank" rel="noopener noreferrer" style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '12px 0', color: '#fff', background: '#5C3A21', textDecoration: 'none', fontSize: 16, fontWeight: 700, gap: 4, minHeight: 64 }}>
        <span style={{ fontSize: 24 }}>📍</span><span>Chỉ đường</span>
      </a>
    </div>
  )
}

