import { useState, useEffect } from 'react'
import { getQueueToday } from '../api/admin'
import { logo } from '../assets'

export default function TvPhongCho() {
  const [queueData, setQueueData] = useState(null)
  const [time, setTime] = useState(new Date())

  // Cập nhật đồng hồ mỗi giây
  useEffect(() => {
    const timer = setInterval(() => setTime(new Date()), 1000)
    return () => clearInterval(timer)
  }, [])

  // Poll danh sách hàng đợi mỗi 4 giây
  useEffect(() => {
    const fetchQueue = () => {
      getQueueToday()
        .then(res => setQueueData(res.data))
        .catch(() => {})
    }
    fetchQueue()
    const poller = setInterval(fetchQueue, 4000)
    return () => clearInterval(poller)
  }, [])

  const inProgress = queueData?.inProgressList || []
  const waiting = queueData?.waitingList || []

  return (
    <div style={{
      minHeight: '100vh',
      background: '#180E07',
      color: '#FDFBF7',
      display: 'flex',
      flexDirection: 'column',
      fontFamily: "'Segoe UI', Roboto, Helvetica, Arial, sans-serif",
      overflow: 'hidden',
      userSelect: 'none'
    }}>
      {/* ─── HEADER TV ─── */}
      <header style={{
        background: '#2B170B',
        borderBottom: '4px solid #D97706',
        padding: '16px 36px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        boxShadow: '0 8px 30px rgba(0,0,0,0.6)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 20 }}>
          <img src={logo} alt="Chúc Từ" style={{ width: 72, height: 72, objectFit: 'contain' }} />
          <div>
            <div style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 36, fontWeight: 800, color: '#FDFBF7', letterSpacing: 1 }}>
              PHÒNG KHÁM ĐÔNG Y CHÚC TỪ
            </div>
            <div style={{ fontSize: 20, color: '#D97706', fontWeight: 600 }}>
              BẢNG ĐIỀU PHỐI VÀ GỌI SỐ KHÁM BỆNH TỰ ĐỘNG
            </div>
          </div>
        </div>

        {/* Đồng hồ số to */}
        <div style={{ textAlign: 'right' }}>
          <div style={{ fontSize: 44, fontWeight: 900, color: '#34D399', letterSpacing: 2, fontFamily: 'monospace', lineHeight: 1 }}>
            {time.toLocaleTimeString('vi-VN')}
          </div>
          <div style={{ fontSize: 18, color: '#BBAE9E', marginTop: 4 }}>
            {time.toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' })}
          </div>
        </div>
      </header>

      {/* ─── NỘI DUNG 2 CỘT ─── */}
      <main style={{ flex: 1, display: 'grid', gridTemplateColumns: '1.1fr 1fr', gap: 24, padding: '24px 36px', minHeight: 0 }}>
        {/* CỘT TRÁI: ĐANG TRONG PHÒNG KHÁM */}
        <section style={{
          background: '#25140A',
          borderRadius: 24,
          border: '3px solid #059669',
          padding: 24,
          display: 'flex',
          flexDirection: 'column',
          boxShadow: '0 12px 40px rgba(0,0,0,0.5)'
        }}>
          <div style={{
            background: '#059669',
            padding: '14px 20px',
            borderRadius: 16,
            marginBottom: 20,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}>
            <span style={{ fontSize: 26, fontWeight: 800, color: '#fff', letterSpacing: 1 }}>
              🩺 ĐANG TRONG PHÒNG KHÁM
            </span>
            <span style={{ fontSize: 18, background: '#047857', padding: '4px 14px', borderRadius: 99, color: '#D1FAE5', fontWeight: 700 }}>
              {inProgress.length} phòng đang làm việc
            </span>
          </div>

          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 16, overflowY: 'auto' }}>
            {inProgress.length === 0 ? (
              <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', color: '#8C7B5E', textAlign: 'center' }}>
                <span style={{ fontSize: 64, marginBottom: 16 }}>🛋️</span>
                <span style={{ fontSize: 26, fontWeight: 600 }}>Hiện không có ca nào đang khám</span>
                <span style={{ fontSize: 20, marginTop: 8 }}>Bác sĩ đang chuẩn bị đón tiếp bệnh nhân tiếp theo</span>
              </div>
            ) : (
              inProgress.map((item, idx) => (
                <div key={item.appointmentId} style={{
                  background: '#2E1A0E',
                  borderRadius: 18,
                  border: '2px solid #10B981',
                  padding: '20px 24px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between'
                }}>
                  <div>
                    <div style={{ fontSize: 20, color: '#34D399', fontWeight: 700 }}>
                      MÃ PHIẾU: <span style={{ fontFamily: 'monospace', fontSize: 26, color: '#FDFBF7' }}>{item.bookingCode}</span>
                    </div>
                    <div style={{ fontSize: 36, fontWeight: 900, color: '#FDFBF7', marginTop: 4 }}>
                      {item.displayName}
                    </div>
                    <div style={{ fontSize: 20, color: '#D1C7BA', marginTop: 4 }}>
                      👨‍⚕️ Bác sĩ: <strong style={{ color: '#F59E0B' }}>{item.doctorName}</strong>
                    </div>
                  </div>

                  <div style={{ textAlign: 'right' }}>
                    <div style={{ background: '#047857', color: '#fff', fontSize: 18, fontWeight: 800, padding: '8px 18px', borderRadius: 12 }}>
                      {item.serviceName}
                    </div>
                    <div style={{ fontSize: 18, color: '#A7F3D0', marginTop: 8, fontWeight: 600 }}>
                      Giờ hẹn: {item.timeSlotLabel}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </section>

        {/* CỘT PHẢI: CHUẨN BỊ VÀO KHÁM */}
        <section style={{
          background: '#25140A',
          borderRadius: 24,
          border: '3px solid #D97706',
          padding: 24,
          display: 'flex',
          flexDirection: 'column',
          boxShadow: '0 12px 40px rgba(0,0,0,0.5)'
        }}>
          <div style={{
            background: '#D97706',
            padding: '14px 20px',
            borderRadius: 16,
            marginBottom: 20,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}>
            <span style={{ fontSize: 26, fontWeight: 800, color: '#fff', letterSpacing: 1 }}>
              🔔 MỜI CHUẨN BỊ TIẾP THEO
            </span>
            <span style={{ fontSize: 18, background: '#B45309', padding: '4px 14px', borderRadius: 99, color: '#FEF3C7', fontWeight: 700 }}>
              {waiting.length} người đang đợi
            </span>
          </div>

          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 12, overflowY: 'auto' }}>
            {waiting.length === 0 ? (
              <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', color: '#8C7B5E', textAlign: 'center' }}>
                <span style={{ fontSize: 64, marginBottom: 16 }}>☕</span>
                <span style={{ fontSize: 26, fontWeight: 600 }}>Phòng chờ hiện đang trống</span>
                <span style={{ fontSize: 20, marginTop: 8 }}>Bệnh nhân đến vui lòng ghé quầy tiếp tân để check-in</span>
              </div>
            ) : (
              waiting.slice(0, 5).map((item, idx) => (
                <div key={item.appointmentId} style={{
                  background: idx === 0 ? '#3A2010' : '#2B170B',
                  borderRadius: 16,
                  border: idx === 0 ? '2.5px solid #F59E0B' : '1px solid #3F291B',
                  padding: '16px 20px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between'
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 18 }}>
                    <div style={{
                      width: 52, height: 52, borderRadius: '50%',
                      background: idx === 0 ? '#F59E0B' : '#3F291B',
                      color: idx === 0 ? '#180E07' : '#D1C7BA',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: 26, fontWeight: 900
                    }}>
                      {idx + 1}
                    </div>
                    <div>
                      <div style={{ fontSize: 26, fontWeight: 800, color: idx === 0 ? '#FDE68A' : '#FDFBF7' }}>
                        {item.displayName}
                      </div>
                      <div style={{ fontSize: 16, color: '#A99B8B', marginTop: 2 }}>
                        Mã: <span style={{ fontFamily: 'monospace', fontWeight: 700, color: '#FDFBF7' }}>{item.bookingCode}</span> · Khung giờ: {item.timeSlotLabel}
                      </div>
                    </div>
                  </div>

                  <div style={{ textAlign: 'right' }}>
                    <span style={{
                      fontSize: 15, fontWeight: 700, padding: '6px 14px', borderRadius: 99,
                      background: idx === 0 ? '#F59E0B25' : '#3F291B',
                      color: idx === 0 ? '#F59E0B' : '#A99B8B',
                      border: idx === 0 ? '1px solid #F59E0B' : 'none'
                    }}>
                      {idx === 0 ? '👉 Kế tiếp' : 'Đang đợi'}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        </section>
      </main>

      {/* ─── DÒNG CHỮ CHẠY CUỐI MÀN HÌNH (MARQUEE) ─── */}
      <footer style={{
        background: '#1F1107',
        borderTop: '2px solid #3F291B',
        padding: '12px 36px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        fontSize: 19,
        color: '#D1C7BA'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span style={{ fontSize: 22 }}>📞</span>
          <span>Hotline hỗ trợ: <strong style={{ color: '#34D399', fontSize: 22 }}>0900.000.000</strong></span>
        </div>
        <div style={{ color: '#F59E0B', fontWeight: 600 }}>
          🌿 Phòng khám Chúc Từ kính chúc quý cô bác thân tâm an lạc, mau lành bệnh!
        </div>
        <div>
          <span>Đã hoàn thành hôm nay: <strong style={{ color: '#34D399' }}>{queueData?.completedTodayCount || 0} ca</strong></span>
        </div>
      </footer>
    </div>
  )
}
