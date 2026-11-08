import { useState, useEffect, useCallback } from 'react'
import { Link } from 'react-router-dom'
import Icon from '../components/Icon'
import { getMyHistory } from '../api/booking'

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
}

export default function LichSuKham() {
  const [historyList, setHistoryList] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedRecord, setSelectedRecord] = useState(null)

  const loadData = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const res = await getMyHistory()
      // Lọc các lần khám hoàn thành (COMPLETED) hoặc tất cả
      setHistoryList(res.data || [])
    } catch {
      setError('Không thể tải lịch sử khám bệnh. Vui lòng thử lại.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Lọc chỉ những lần có kết quả điều trị hoặc đã hoàn thành
  const completedList = historyList.filter(item => item.status === 'COMPLETED')

  return (
    <div style={{ minHeight: '100vh', background: C.bg, paddingBottom: 120 }}>
      <div style={{ maxWidth: 760, margin: '0 auto', padding: '32px 16px 24px' }}>
        {/* Breadcrumb */}
        <nav style={{ fontSize: 16, color: C.textSoft, marginBottom: 20, display: 'flex', gap: 6, alignItems: 'center' }}>
          <Link to="/" style={{ color: C.green, textDecoration: 'none' }}>Trang chủ</Link>
          <span>›</span>
          <Link to="/phieu-kham" style={{ color: C.green, textDecoration: 'none' }}>Phiếu khám</Link>
          <span>›</span>
          <span>Lịch sử khám & Kết quả điều trị</span>
        </nav>

        <div style={{ marginBottom: 28 }}>
          <h1 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 28, fontWeight: 700, color: C.primary, marginBottom: 6 }}>
            📜 Sổ theo dõi sức khỏe & Điều trị
          </h1>
          <p style={{ fontSize: 18, color: C.textSoft }}>
            Xem lại chẩn đoán bắt mạch, đơn thuốc thảo mộc và tiến trình trị liệu của bạn
          </p>
        </div>

        {loading && (
          <div style={{ textAlign: 'center', padding: 48, color: C.textSoft, fontSize: 19 }}>
            ⏳ Đang tải hồ sơ bệnh án...
          </div>
        )}

        {!loading && error && (
          <div style={{ background: '#FFF0EE', border: `1px solid ${C.danger}30`, borderRadius: 14, padding: '16px 20px', color: C.danger, fontSize: 18, marginBottom: 20 }}>
            {error}
          </div>
        )}

        {!loading && !error && completedList.length === 0 && (
          <div style={{ textAlign: 'center', padding: '48px 24px', background: '#fff', borderRadius: 20, border: `1px solid ${C.border}` }}>
            <div style={{ fontSize: 54, marginBottom: 16 }}>🌿</div>
            <div style={{ fontSize: 20, fontWeight: 700, color: C.text, marginBottom: 8 }}>
              Chưa có đợt khám hoàn thành nào
            </div>
            <p style={{ fontSize: 17, color: C.textSoft, marginBottom: 24 }}>
              Sau khi bạn đến phòng khám và được bác sĩ bắt mạch, kê đơn điều trị, kết quả và đơn thuốc sẽ được lưu trữ đầy đủ tại đây.
            </p>
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
          </div>
        )}

        {!loading && completedList.map((item) => (
          <div
            key={item.id}
            style={{
              background: '#fff',
              border: `2px solid ${C.border}`,
              borderRadius: 20,
              padding: '24px',
              marginBottom: 20,
              boxShadow: '0 3px 12px rgba(42,33,24,0.06)'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 10, borderBottom: `1px solid ${C.border}`, paddingBottom: 16, marginBottom: 16 }}>
              <div>
                <div style={{ fontSize: 15, color: C.textSoft }}>NGÀY KHÁM</div>
                <div style={{ fontSize: 22, fontWeight: 800, color: C.green }}>
                  {formatDateVN(item.appointmentDate)} · {item.timeSlotLabel}
                </div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <span style={{ display: 'inline-block', padding: '4px 12px', borderRadius: 99, background: C.greenLight, color: C.green, fontSize: 14, fontWeight: 700 }}>
                  Đã hoàn thành
                </span>
                <div style={{ fontSize: 14, color: C.textSoft, marginTop: 4, fontFamily: 'monospace' }}>
                  {item.bookingCode}
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 10, fontSize: 17, color: C.text, marginBottom: 18 }}>
              <div>👤 <strong>Bệnh nhân:</strong> {item.patientName} ({item.patientCode}) - {item.relationship || 'Bản thân'}</div>
              {item.doctorName && <div>👨‍⚕️ <strong>Bác sĩ phụ trách:</strong> {item.doctorName}</div>}
              {item.serviceName && <div>🌿 <strong>Dịch vụ:</strong> {item.serviceName}</div>}
            </div>

            {/* Chi tiết chẩn đoán và đơn thuốc nếu có */}
            {item.medicalResult ? (
              <div style={{ background: C.greenLight, borderRadius: 16, border: `1.5px solid ${C.green}`, padding: '18px 20px' }}>
                <h3 style={{ fontSize: 19, fontWeight: 800, color: C.green, marginBottom: 12 }}>
                  🌿 KẾT QUẢ BẮT MẠCH & ĐƠN THUỐC
                </h3>

                {item.medicalResult.diagnosis && (
                  <div style={{ marginBottom: 10, fontSize: 17 }}>
                    <strong style={{ color: C.primary }}>Chẩn đoán YHCT:</strong>
                    <div style={{ marginTop: 4, background: '#fff', padding: '10px 14px', borderRadius: 10, color: C.text }}>
                      {item.medicalResult.diagnosis}
                    </div>
                  </div>
                )}

                {item.medicalResult.prescription && (
                  <div style={{ marginBottom: 10, fontSize: 17 }}>
                    <strong style={{ color: C.primary }}>Bài thuốc thảo mộc:</strong>
                    <div style={{ marginTop: 4, background: '#fff', padding: '10px 14px', borderRadius: 10, color: C.text, whiteSpace: 'pre-line' }}>
                      {item.medicalResult.prescription}
                    </div>
                  </div>
                )}

                {item.medicalResult.notes && (
                  <div style={{ marginBottom: 10, fontSize: 17 }}>
                    <strong style={{ color: C.primary }}>Lời dặn của bác sĩ:</strong>
                    <div style={{ marginTop: 4, background: '#fff', padding: '10px 14px', borderRadius: 10, color: C.text }}>
                      {item.medicalResult.notes}
                    </div>
                  </div>
                )}

                {item.medicalResult.followUpDate && (
                  <div style={{ fontSize: 17, fontWeight: 700, color: C.gold, marginTop: 12 }}>
                    ⏰ Hẹn ngày tái khám: {formatDateVN(item.medicalResult.followUpDate)}
                  </div>
                )}
              </div>
            ) : (
              <div style={{ padding: '12px 16px', background: '#F8F6F0', borderRadius: 12, fontSize: 16, color: C.textSoft }}>
                ℹ️ Chưa có ghi nhận đơn thuốc điện tử cho lượt khám này.
              </div>
            )}
          </div>
        ))}
      </div>

      {/* ─── STICKY BOTTOM BAR ─── */}
      <StickyBar />
    </div>
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

