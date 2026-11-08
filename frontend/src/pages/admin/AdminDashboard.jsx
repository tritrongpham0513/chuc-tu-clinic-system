import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import AdminLayout from './AdminLayout'
import { getDashboardStats } from '../../api/admin'

export default function AdminDashboard() {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getDashboardStats()
      .then(res => setStats(res.data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  return (
    <AdminLayout>
      <div style={{ maxWidth: 1200, margin: '0 auto' }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 28, flexWrap: 'wrap', gap: 16 }}>
          <div>
            <h1 style={{ fontFamily: "'Noto Serif', Georgia, serif", fontSize: 28, fontWeight: 700, color: '#2B1A0F', margin: 0 }}>
              📊 Tổng quan phòng khám hôm nay
            </h1>
            <div style={{ fontSize: 16, color: '#6B5E4E', marginTop: 4 }}>
              Ngày {new Date().toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' })}
            </div>
          </div>

          <div style={{ display: 'flex', gap: 12 }}>
            <Link
              to="/admin/appointments?action=walkin"
              style={{
                display: 'inline-flex', alignItems: 'center', gap: 8,
                minHeight: 46, padding: '0 20px', borderRadius: 12,
                background: '#2D5A27', color: '#fff',
                fontSize: 16, fontWeight: 700, textDecoration: 'none'
              }}
            >
              + Tiếp nhận tại quầy
            </Link>
            <a
              href="/tv-phong-cho"
              target="_blank"
              rel="noopener noreferrer"
              style={{
                display: 'inline-flex', alignItems: 'center', gap: 8,
                minHeight: 46, padding: '0 18px', borderRadius: 12,
                background: '#fff', border: '2px solid #D97706',
                color: '#D97706', fontSize: 16, fontWeight: 700, textDecoration: 'none'
              }}
            >
              📺 Bật TV phòng chờ
            </a>
          </div>
        </div>

        {loading ? (
          <div style={{ padding: 48, textAlign: 'center', color: '#6B5E4E', fontSize: 18 }}>⏳ Đang tải số liệu...</div>
        ) : (
          <>
            {/* Lưới thẻ thống kê */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 18, marginBottom: 32 }}>
              <StatCard
                title="Tổng ca hôm nay"
                value={stats?.totalToday || 0}
                icon="📅"
                color="#2B1A0F"
                bg="#fff"
              />
              <StatCard
                title="Chờ xác nhận (Online)"
                value={stats?.pendingCount || 0}
                icon="⏳"
                color="#B45309"
                bg="#FFFBEB"
              />
              <StatCard
                title="Đang ngồi phòng chờ"
                value={stats?.waitingCount || 0}
                icon="🛋️"
                color="#1E40AF"
                bg="#EFF6FF"
              />
              <StatCard
                title="Đang trong phòng khám"
                value={stats?.inProgressCount || 0}
                icon="🩺"
                color="#047857"
                bg="#ECFDF5"
              />
              <StatCard
                title="Đã hoàn thành"
                value={stats?.completedCount || 0}
                icon="✅"
                color="#15803D"
                bg="#F0FDF4"
              />
              <StatCard
                title="Đã hủy / Vắng"
                value={stats?.cancelledCount || 0}
                icon="❌"
                color="#90372A"
                bg="#FFF0EE"
              />
            </div>

            {/* Khối tóm tắt hệ thống */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
              <div style={{ background: '#fff', borderRadius: 20, padding: 24, border: '1px solid #E0D3BC' }}>
                <h2 style={{ fontSize: 20, fontWeight: 700, color: '#5C3A21', marginBottom: 14 }}>
                  👥 Quy mô hồ sơ & Bác sĩ
                </h2>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 12, fontSize: 17 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #F0EAE0', paddingBottom: 8 }}>
                    <span style={{ color: '#6B5E4E' }}>Tổng số hồ sơ bệnh nhân:</span>
                    <strong style={{ color: '#2B1A0F' }}>{stats?.totalPatients || 0} bệnh nhân</strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #F0EAE0', paddingBottom: 8 }}>
                    <span style={{ color: '#6B5E4E' }}>Bác sĩ YHCT trong danh sách:</span>
                    <strong style={{ color: '#2B1A0F' }}>{stats?.totalDoctors || 0} bác sĩ</strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #F0EAE0', paddingBottom: 8 }}>
                    <span style={{ color: '#6B5E4E' }}>Bài viết cẩm nang sức khỏe:</span>
                    <strong style={{ color: '#2B1A0F' }}>{stats?.totalArticles || 0} bài</strong>
                  </div>
                </div>
              </div>

              <div style={{ background: '#E4EFE2', borderRadius: 20, padding: 24, border: '1.5px solid #2D5A27' }}>
                <h2 style={{ fontSize: 20, fontWeight: 700, color: '#2D5A27', marginBottom: 12 }}>
                  🌿 Hướng dẫn quy trình tiếp nhận
                </h2>
                <ol style={{ paddingLeft: 20, margin: 0, fontSize: 16, lineHeight: 1.8, color: '#1F1610' }}>
                  <li><strong>Bệnh nhân đặt trước:</strong> Tìm theo mã phiếu hoặc SĐT, bấm <em>"Tiếp nhận / Check-in"</em> để chuyển sang danh sách chờ khám.</li>
                  <li><strong>Bệnh nhân đến trực tiếp:</strong> Bấm nút <em>"+ Tiếp nhận tại quầy"</em> để tạo phiếu và cấp số thứ tự ngay.</li>
                  <li><strong>Bác sĩ khám:</strong> Bấm <em>"Gọi vào khám"</em> (TV phòng chờ sẽ tự động cập nhật), sau đó ghi bệnh án và đơn thuốc.</li>
                </ol>
              </div>
            </div>
          </>
        )}
      </div>
    </AdminLayout>
  )
}

function StatCard({ title, value, icon, color, bg }) {
  return (
    <div style={{
      background: bg, borderRadius: 18, padding: '20px 22px',
      border: `1.5px solid ${color}25`,
      boxShadow: '0 2px 10px rgba(0,0,0,0.03)',
      display: 'flex', flexDirection: 'column', gap: 8
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontSize: 15, fontWeight: 600, color: '#6B5E4E' }}>{title}</span>
        <span style={{ fontSize: 24 }}>{icon}</span>
      </div>
      <div style={{ fontSize: 34, fontWeight: 800, color, lineHeight: 1 }}>{value}</div>
    </div>
  )
}

