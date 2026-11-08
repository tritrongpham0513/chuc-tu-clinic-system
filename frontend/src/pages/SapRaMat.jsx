import { Link } from 'react-router-dom'
import PublicLayout from '../components/PublicLayout'

// Trang giữ chỗ cho các màn hình thuộc nhóm thiết kế 4–5 (tài khoản, đặt lịch, phiếu khám)
// — chưa nằm trong phạm vi nhóm 1, 2, 3.
export default function SapRaMat({ title }) {
  return (
    <PublicLayout>
      <section style={{ width: '100%', flexGrow: 1, padding: '72px 24px' }}>
        <div style={{ maxWidth: '1160px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '16px', alignItems: 'flex-start' }}>
          <h1 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: '38px', lineHeight: '48px', fontWeight: 700, color: '#2A2118' }}>
            {title}
          </h1>
          <div style={{ width: '72px', height: '4px', background: '#C9963F', borderRadius: '999px' }} />
          <p style={{ margin: 0, maxWidth: '68ch', fontSize: '20px', lineHeight: '32px', color: '#4A3F33' }}>
            Màn hình này thuộc nhóm thiết kế tiếp theo, chưa được làm trong đợt này.
          </p>
          <Link
            to="/"
            className="btn-ghost"
            style={{ display: 'inline-flex', alignItems: 'center', minHeight: '48px', padding: '0 20px', border: '2px solid #8C7B5E', borderRadius: '12px', fontSize: '18px', fontWeight: 600, color: '#2E5B37', textDecoration: 'none' }}
          >
            Về trang chủ
          </Link>
        </div>
      </section>
    </PublicLayout>
  )
}
