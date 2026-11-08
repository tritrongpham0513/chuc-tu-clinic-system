import { useState, useEffect, useCallback } from 'react'
import { Link } from 'react-router-dom'
import Icon from '../components/Icon'
import { getMyUpcoming, getMyHistory, cancelMyAppointment } from '../api/booking'

const C = {
  bg: '#FDFBF7',
  primary: '#5C3A21',
  green: '#2D5A27',
  greenLight: '#E4EFE2',
  gold: '#D97706',
  goldBg: '#FFFBEB',
  border: '#E0D3BC',
  text: '#1F1610',
  textSoft: '#6B5E4E',
  danger: '#90372A',
  dangerBg: '#FFF0EE',
  blue: '#1E40AF',
  blueBg: '#EFF6FF',
}

export default function PhieuKhamCuaToi() {
  const [activeTab, setActiveTab] = useState('upcoming') // 'upcoming' | 'history'
  const [upcomingList, setUpcomingList] = useState([])
  const [historyList, setHistoryList] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // Modal hủy phiếu
  const [cancelTarget, setCancelTarget] = useState(null)
  const [cancelReason, setCancelReason] = useState('')
  const [cancelling, setCancelling] = useState(false)

  // Modal xem chi tiết
  const [detailTarget, setDetailTarget] = useState(null)

  // Toast
  const [toast, setToast] = useState(null)
  const showToast = (msg, type = 'ok') => {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const loadData = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [upRes, histRes] = await Promise.all([
        getMyUpcoming(),
        getMyHistory()
      ])
      setUpcomingList(upRes.data || [])
      setHistoryList(histRes.data || [])
    } catch {
      setError('Không thể tải danh sách phiếu khám. Vui lòng kiểm tra lại mạng.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleConfirmCancel = async () => {
    if (!cancelTarget) return
    setCancelling(true)
    try {
      await cancelMyAppointment(cancelTarget.id, cancelReason.trim())
      showToast('Đã hủy phiếu khám thành công')
      setCancelTarget(null)
      setCancelReason('')
      loadData()
    } catch (err) {
      showToast(err?.response?.data?.message || 'Có lỗi xảy ra khi hủy phiếu', 'err')
    } finally {
      setCancelling(false)
    }
  }

  const currentList = activeTab === 'upcoming' ? upcomingList : historyList

  return (
    <div style={{ minHeight: '100vh', background: C.bg, paddingBottom: 120 }}>
      <div style={{ maxWidth: 760, margin: '0 auto', padding: '32px 16px 24px' }}>
        {/* Breadcrumb */}
        <nav style={{ fontSize: 16, color: C.textSoft, marginBottom: 20, display: 'flex', gap: 6, alignItems: 'center' }}>
          <Link to="/" style={{ color: C.green, textDecoration: 'none' }}>Trang chủ</Link>
          <span>›</span>
          <span>Phiếu khám của tôi</span>
        </nav>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16, marginBottom: 24 }}>
          <div>
            <h1 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 28, fontWeight: 700, color: C.primary, marginBottom: 6 }}>
              🎫 Phiếu khám của tôi
            </h1>
            <p style={{ fontSize: 18, color: C.textSoft }}>
              Theo dõi lịch hẹn và phiếu khám chữa bệnh tại phòng khám
            </p>
          </div>
          <Link
            to="/dat-lich"
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 8,
              minHeight: 48, padding: '0 20px', borderRadius: 12,
              background: C.green, color: '#fff',
              fontSize: 17, fontWeight: 700, textDecoration: 'none'
            }}
          >
            <Icon name="calendar-days" size={20} stroke="#fff" />
            + Đặt lịch mới
          </Link>
        </div>

        {/* 2 Tabs */}
        <div style={{ display: 'flex', gap: 8, marginBottom: 24, background: '#F0EAE0', padding: 6, borderRadius: 14 }}>
          <button
            onClick={() => setActiveTab('upcoming')}
            style={{
              flex: 1, minHeight: 48, borderRadius: 10, border: 'none',
              background: activeTab === 'upcoming' ? '#fff' : 'transparent',
              color: activeTab === 'upcoming' ? C.green : C.textSoft,
              fontSize: 18, fontWeight: 700, cursor: 'pointer',
              boxShadow: activeTab === 'upcoming' ? '0 2px 8px rgba(0,0,0,0.08)' : 'none',
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8
            }}
          >
            <span>Lịch sắp tới</span>
            {upcomingList.length > 0 && (
              <span style={{ background: C.green, color: '#fff', fontSize: 13, padding: '2px 8px', borderRadius: 99 }}>
                {upcomingList.length}
              </span>
            )}
          </button>
          <button
            onClick={() => setActiveTab('history')}
            style={{
              flex: 1, minHeight: 48, borderRadius: 10, border: 'none',
              background: activeTab === 'history' ? '#fff' : 'transparent',
              color: activeTab === 'history' ? C.primary : C.textSoft,
              fontSize: 18, fontWeight: 700, cursor: 'pointer',
              boxShadow: activeTab === 'history' ? '0 2px 8px rgba(0,0,0,0.08)' : 'none',
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8
            }}
          >
            <span>Lịch sử khám</span>
            {historyList.length > 0 && (
              <span style={{ background: C.textSoft, color: '#fff', fontSize: 13, padding: '2px 8px', borderRadius: 99 }}>
                {historyList.length}
              </span>
            )}
          </button>
        </div>

        {/* Loading state */}
        {loading && (
          <div style={{ textAlign: 'center', padding: 48, color: C.textSoft, fontSize: 19 }}>
            ⏳ Đang tải danh sách phiếu khám...
          </div>
        )}

        {/* Error */}
        {!loading && error && (
          <div style={{ background: C.dangerBg, border: `1px solid ${C.danger}30`, borderRadius: 14, padding: '16px 20px', color: C.danger, fontSize: 18, marginBottom: 20 }}>
            {error}
          </div>
        )}

        {/* Empty state */}
        {!loading && !error && currentList.length === 0 && (
          <div style={{ textAlign: 'center', padding: '48px 24px', background: '#fff', borderRadius: 20, border: `1px solid ${C.border}` }}>
            <div style={{ fontSize: 54, marginBottom: 16 }}>📋</div>
            <div style={{ fontSize: 20, fontWeight: 700, color: C.text, marginBottom: 8 }}>
              {activeTab === 'upcoming' ? 'Không có lịch khám sắp tới' : 'Chưa có lịch sử khám'}
            </div>
            <p style={{ fontSize: 17, color: C.textSoft, marginBottom: 24 }}>
              {activeTab === 'upcoming'
                ? 'Khi bạn đặt lịch hẹn khám tại phòng khám, thông tin phiếu sẽ hiển thị ở đây.'
                : 'Các đợt khám bệnh đã hoàn thành hoặc đã hủy sẽ lưu tại đây.'}
            </p>
            {activeTab === 'upcoming' && (
              <Link
                to="/dat-lich"
                style={{
                  display: 'inline-flex', alignItems: 'center', gap: 8,
                  minHeight: 52, padding: '0 28px', borderRadius: 14,
                  background: C.green, color: '#fff',
                  fontSize: 18, fontWeight: 700, textDecoration: 'none'
                }}
              >
                Đặt lịch khám ngay
              </Link>
            )}
          </div>
        )}

        {/* Danh sách thẻ phiếu khám */}
        {!loading && currentList.map((item) => (
          <div
            key={item.id}
            style={{
              background: '#fff',
              border: `2px solid ${item.status === 'PENDING' ? C.green : C.border}`,
              borderRadius: 20,
              marginBottom: 20,
              boxShadow: '0 3px 12px rgba(42,33,24,0.06)',
              overflow: 'hidden',
              display: 'flex',
              flexDirection: 'column'
            }}
          >
            {/* Cuống vé đầu trên */}
            <div style={{
              background: item.status === 'PENDING' ? C.greenLight : '#F7F4EE',
              padding: '14px 20px',
              borderBottom: `1.5px dashed ${C.border}`,
              display: 'flex', justifyContent: 'space-between', alignItems: 'center',
              flexWrap: 'wrap', gap: 8
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <span style={{ fontSize: 15, fontWeight: 700, color: C.textSoft }}>MÃ PHIẾU:</span>
                <span style={{ fontFamily: 'monospace', fontSize: 19, fontWeight: 800, color: C.primary, letterSpacing: 1 }}>
                  {item.bookingCode}
                </span>
              </div>
              <StatusBadge status={item.status} label={item.statusLabel} />
            </div>

            {/* Thân vé */}
            <div style={{ padding: '20px', display: 'flex', flexWrap: 'wrap', gap: 16 }}>
              <div style={{ flex: '1 1 300px' }}>
                <div style={{ fontSize: 22, fontWeight: 700, color: C.text, marginBottom: 6 }}>
                  {item.patientName}
                </div>
                <div style={{ fontSize: 16, color: C.textSoft, marginBottom: 12 }}>
                  Mã BN: <strong>{item.patientCode}</strong> &nbsp;·&nbsp; Quan hệ: <strong>{item.relationship || 'Bản thân'}</strong>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: 6, fontSize: 17, color: C.text }}>
                  <div>📅 <strong>Ngày khám:</strong> {formatDateVN(item.appointmentDate)}</div>
                  <div>🕐 <strong>Khung giờ:</strong> {item.timeSlotLabel}</div>
                  {item.doctorName && <div>👨‍⚕️ <strong>Bác sĩ:</strong> {item.doctorName}</div>}
                  {item.serviceName && <div>🌿 <strong>Dịch vụ:</strong> {item.serviceName}</div>}
                  {item.symptoms && (
                    <div style={{ marginTop: 6, padding: '8px 12px', background: '#FBF8F2', borderRadius: 8, fontSize: 16, color: C.textSoft }}>
                      💬 Triệu chứng: {item.symptoms}
                    </div>
                  )}
                  {item.cancelReason && (
                    <div style={{ marginTop: 6, padding: '8px 12px', background: C.dangerBg, borderRadius: 8, fontSize: 16, color: C.danger }}>
                      ❌ Lý do hủy: {item.cancelReason}
                    </div>
                  )}
                </div>
              </div>

              {/* Nút hành động */}
              <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: 10, minWidth: 160 }}>
                <button
                  onClick={() => setDetailTarget(item)}
                  style={{
                    minHeight: 48, padding: '0 16px', borderRadius: 12,
                    background: '#fff', border: `2px solid ${C.border}`,
                    color: C.primary, fontSize: 16, fontWeight: 600, cursor: 'pointer',
                    display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6
                  }}
                >
                  <Icon name="file-text" size={18} stroke={C.primary} />
                  Xem chi tiết
                </button>

                {item.status === 'COMPLETED' && item.medicalResult && (
                  <Link
                    to="/lich-su-kham"
                    style={{
                      minHeight: 48, padding: '0 16px', borderRadius: 12,
                      background: C.greenLight, border: `2px solid ${C.green}`,
                      color: C.green, fontSize: 16, fontWeight: 700, textDecoration: 'none',
                      display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6
                    }}
                  >
                    💊 Đơn thuốc & Kết quả
                  </Link>
                )}

                {item.status === 'PENDING' && (
                  <button
                    onClick={() => { setCancelTarget(item); setCancelReason('') }}
                    style={{
                      minHeight: 48, padding: '0 16px', borderRadius: 12,
                      background: 'transparent', border: `2px solid ${C.danger}30`,
                      color: C.danger, fontSize: 16, fontWeight: 600, cursor: 'pointer',
                      display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6
                    }}
                  >
                    <Icon name="x" size={18} stroke={C.danger} />
                    Hủy phiếu này
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* ─── MODAL CHI TIẾT PHIẾU KHÁM ─── */}
      {detailTarget && (
        <div
          style={{
            position: 'fixed', inset: 0, background: 'rgba(31,22,16,0.55)',
            backdropFilter: 'blur(3px)', display: 'flex', alignItems: 'center',
            justifyContent: 'center', zIndex: 250, padding: 16
          }}
          onClick={(e) => { if (e.target === e.currentTarget) setDetailTarget(null) }}
        >
          <div style={{
            background: C.bg, borderRadius: 24, padding: '28px 24px',
            width: '100%', maxWidth: 540, maxHeight: '90vh', overflowY: 'auto',
            boxShadow: '0 20px 60px rgba(0,0,0,0.3)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
              <h2 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 24, fontWeight: 700, color: C.primary }}>
                Chi tiết phiếu khám
              </h2>
              <button
                onClick={() => setDetailTarget(null)}
                style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 8 }}
              >
                <Icon name="x" size={24} stroke={C.textSoft} />
              </button>
            </div>

            <div style={{ background: '#fff', borderRadius: 16, border: `1px solid ${C.border}`, padding: '16px 20px', marginBottom: 20 }}>
              <div style={{ fontSize: 15, color: C.textSoft }}>Mã đặt khám</div>
              <div style={{ fontSize: 22, fontWeight: 800, color: C.green, fontFamily: 'monospace', marginBottom: 12 }}>
                {detailTarget.bookingCode}
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '130px 1fr', gap: '10px 12px', fontSize: 17 }}>
                <span style={{ color: C.textSoft }}>Trạng thái:</span>
                <span><StatusBadge status={detailTarget.status} label={detailTarget.statusLabel} /></span>

                <span style={{ color: C.textSoft }}>Bệnh nhân:</span>
                <span style={{ fontWeight: 700 }}>{detailTarget.patientName} ({detailTarget.patientCode})</span>

                <span style={{ color: C.textSoft }}>Quan hệ:</span>
                <span>{detailTarget.relationship || 'Bản thân'}</span>

                <span style={{ color: C.textSoft }}>Ngày hẹn:</span>
                <span style={{ fontWeight: 600 }}>{formatDateVN(detailTarget.appointmentDate)}</span>

                <span style={{ color: C.textSoft }}>Khung giờ:</span>
                <span style={{ fontWeight: 600 }}>{detailTarget.timeSlotLabel}</span>

                {detailTarget.doctorName && (
                  <>
                    <span style={{ color: C.textSoft }}>Bác sĩ khám:</span>
                    <span>{detailTarget.doctorName}</span>
                  </>
                )}

                {detailTarget.serviceName && (
                  <>
                    <span style={{ color: C.textSoft }}>Dịch vụ:</span>
                    <span>{detailTarget.serviceName}</span>
                  </>
                )}

                {detailTarget.symptoms && (
                  <>
                    <span style={{ color: C.textSoft }}>Triệu chứng:</span>
                    <span>{detailTarget.symptoms}</span>
                  </>
                )}

                {detailTarget.cancelReason && (
                  <>
                    <span style={{ color: C.danger }}>Lý do hủy:</span>
                    <span style={{ color: C.danger }}>{detailTarget.cancelReason}</span>
                  </>
                )}
              </div>
            </div>

            {/* Nếu có kết quả y tế */}
            {detailTarget.medicalResult && (
              <div style={{ background: C.greenLight, borderRadius: 16, border: `1.5px solid ${C.green}`, padding: '16px 20px', marginBottom: 20 }}>
                <h3 style={{ fontSize: 19, fontWeight: 700, color: C.green, marginBottom: 12 }}>
                  🌿 Kết quả chẩn đoán & Điều trị
                </h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8, fontSize: 17 }}>
                  {detailTarget.medicalResult.diagnosis && (
                    <div><strong>Chẩn đoán:</strong> {detailTarget.medicalResult.diagnosis}</div>
                  )}
                  {detailTarget.medicalResult.prescription && (
                    <div><strong>Bài thuốc / Đơn thuốc:</strong> {detailTarget.medicalResult.prescription}</div>
                  )}
                  {detailTarget.medicalResult.notes && (
                    <div><strong>Lời dặn:</strong> {detailTarget.medicalResult.notes}</div>
                  )}
                  {detailTarget.medicalResult.followUpDate && (
                    <div><strong>Hẹn tái khám:</strong> {formatDateVN(detailTarget.medicalResult.followUpDate)}</div>
                  )}
                </div>
              </div>
            )}

            <button
              onClick={() => setDetailTarget(null)}
              style={{
                width: '100%', minHeight: 52, borderRadius: 14,
                background: C.primary, color: '#fff',
                fontSize: 18, fontWeight: 700, border: 'none', cursor: 'pointer'
              }}
            >
              Đóng cửa sổ
            </button>
          </div>
        </div>
      )}

      {/* ─── MODAL HỦY PHIẾU KHÁM ─── */}
      {cancelTarget && (
        <div
          style={{
            position: 'fixed', inset: 0, background: 'rgba(31,22,16,0.55)',
            backdropFilter: 'blur(3px)', display: 'flex', alignItems: 'center',
            justifyContent: 'center', zIndex: 260, padding: 16
          }}
          onClick={(e) => { if (e.target === e.currentTarget) setCancelTarget(null) }}
        >
          <div style={{
            background: C.bg, borderRadius: 24, padding: '28px 24px',
            width: '100%', maxWidth: 460,
            boxShadow: '0 20px 60px rgba(0,0,0,0.3)'
          }}>
            <div style={{ textAlign: 'center', fontSize: 48, marginBottom: 12 }}>⚠️</div>
            <h2 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 24, fontWeight: 700, color: C.danger, textAlign: 'center', marginBottom: 10 }}>
              Xác nhận hủy lịch khám?
            </h2>
            <p style={{ fontSize: 17, color: C.textSoft, textAlign: 'center', marginBottom: 20 }}>
              Bạn đang yêu cầu hủy phiếu khám mã <strong>{cancelTarget.bookingCode}</strong> vào ngày <strong>{formatDateVN(cancelTarget.appointmentDate)}</strong>.
            </p>

            <div style={{ marginBottom: 20 }}>
              <label style={{ display: 'block', fontSize: 16, fontWeight: 600, color: C.text, marginBottom: 6 }}>
                Lý do hủy (tùy chọn):
              </label>
              <textarea
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                placeholder="Ví dụ: Bận đột xuất, đã khỏe lại..."
                style={{
                  width: '100%', minHeight: 80, padding: '12px 14px',
                  borderRadius: 12, border: `1.5px solid ${C.border}`,
                  fontSize: 17, fontFamily: 'inherit', resize: 'none',
                  boxSizing: 'border-box'
                }}
              />
            </div>

            <div style={{ display: 'flex', gap: 12 }}>
              <button
                onClick={() => setCancelTarget(null)}
                disabled={cancelling}
                style={{
                  flex: 1, minHeight: 50, borderRadius: 12,
                  background: '#fff', border: `2px solid ${C.border}`,
                  color: C.primary, fontSize: 17, fontWeight: 600, cursor: 'pointer'
                }}
              >
                Giữ lại lịch
              </button>
              <button
                onClick={handleConfirmCancel}
                disabled={cancelling}
                style={{
                  flex: 1, minHeight: 50, borderRadius: 12,
                  background: C.danger, border: 'none',
                  color: '#fff', fontSize: 17, fontWeight: 700, cursor: 'pointer'
                }}
              >
                {cancelling ? 'Đang hủy...' : 'Đồng ý hủy'}
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

      {/* ─── STICKY BOTTOM BAR ─── */}
      <StickyBar />
    </div>
  )
}

function StatusBadge({ status, label }) {
  let bg = '#F0EAE0'
  let color = '#4A3F33'
  if (status === 'PENDING') { bg = '#FFFBEB'; color = '#B45309' }
  else if (status === 'CONFIRMED') { bg = '#E4EFE2'; color = '#2D5A27' }
  else if (status === 'IN_PROGRESS') { bg = '#EFF6FF'; color = '#1E40AF' }
  else if (status === 'COMPLETED') { bg = '#E4EFE2'; color = '#15803D' }
  else if (status === 'CANCELLED') { bg = '#FFF0EE'; color = '#90372A' }
  else if (status === 'NO_SHOW') { bg = '#F3F4F6'; color = '#4B5563' }

  return (
    <span style={{
      display: 'inline-block', padding: '4px 12px', borderRadius: 99,
      fontSize: 14, fontWeight: 700, background: bg, color
    }}>
      {label || status}
    </span>
  )
}

function formatDateVN(iso) {
  if (!iso) return ''
  const parts = String(iso).split('-')
  if (parts.length === 3) return `${parts[2]}/${parts[1]}/${parts[0]}`
  return iso
}

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

