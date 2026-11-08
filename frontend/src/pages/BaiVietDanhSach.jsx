import { Fragment, useState } from 'react'
import { Link } from 'react-router-dom'
import Icon from '../components/Icon'
import { facade } from '../assets'
import { CATEGORIES, POSTS } from '../data/posts'
import PublicLayout from '../components/PublicLayout'

// Kiểu nút lọc chuyên mục: đang chọn (nền xanh, có dấu ✓) / chưa chọn (nền kem, viền nâu)
const chipBase = {
  display: 'inline-flex', alignItems: 'center', gap: '8px', minHeight: '52px', padding: '0 22px',
  borderRadius: '999px', fontFamily: 'inherit', fontSize: '18px', cursor: 'pointer',
}
const chipOn = { ...chipBase, background: '#3B7145', border: '2px solid #3B7145', color: '#FFFFFF', fontWeight: 700 }
const chipOff = { ...chipBase, background: '#FFFCF6', border: '2px solid #8C7B5E', color: '#2E5B37', fontWeight: 600 }

// Nhãn chuyên mục trên từng bài: xanh (tin tức, thiện nguyện, giới thiệu) / vàng (hướng dẫn, dịch vụ)
const tagBase = { fontSize: '15px', fontWeight: 600, padding: '6px 14px', borderRadius: '999px' }
const tagStyles = {
  green: { ...tagBase, background: '#E4EFE2', color: '#2E5B37' },
  gold: { ...tagBase, background: '#F7EBD2', color: '#8A6012' },
}

export default function BaiVietDanhSach() {
  const [cat, setCat] = useState('all')

  const chips = CATEGORIES.map((c) => ({
    label: c.label,
    on: c.id === cat,
    pressed: c.id === cat ? 'true' : 'false',
    pick: () => setCat(c.id),
  }))
  const currentLabel = CATEGORIES.find((c) => c.id === cat)?.label ?? 'Tất cả'
  const posts = POSTS.filter((p) => cat === 'all' || p.cat === cat).map((p) => ({
    ...p,
    aria: 'Đọc thêm: ' + p.title,
  }))
  // Bài nổi bật phía trên thuộc chuyên mục Tin tức, cộng thêm vào số đếm
  const count = posts.length + (cat === 'all' || cat === 'tintuc' ? 1 : 0)

  return (
    <PublicLayout active="tin-tuc">
      <section style={{ width: "100%", background: "#F4EADB", borderBottom: "1px solid #E0D3BC", padding: "40px 24px 56px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexDirection: "column", gap: "16px" }}>
          <nav aria-label="Đường dẫn" style={{ fontSize: "16px", color: "#6B5E4E" }}>
            <Link to="/" style={{ color: "#4A3F33", textDecoration: "underline" }}>Trang chủ</Link>
            {" "}
            <span style={{ padding: "0 8px" }}>›</span>
            {" "}
            <span>Bài viết</span>
          </nav>
          <h1 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "44px", lineHeight: "56px", fontWeight: 700, color: "#2A2118" }}>
            Bài viết của phòng khám
          </h1>
          <p style={{ margin: 0, maxWidth: "64ch", fontSize: "20px", lineHeight: "32px", color: "#4A3F33" }}>
            Thông báo, hướng dẫn, giới thiệu dịch vụ và hoạt động thiện nguyện. Cô chú đọc được mà không cần đăng nhập.
          </p>
        </div>
      </section>
      <section style={{ width: "100%", padding: "40px 24px 0" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto" }}>
          <h2 style={{ margin: "0 0 16px", fontSize: "19px", fontWeight: 600, color: "#2A2118" }}>Lọc theo chuyên mục</h2>
          <div role="group" aria-label="Lọc bài viết theo chuyên mục" style={{ display: "flex", flexWrap: "wrap", gap: "12px" }}>
            {chips.map((chip, i) => (
              <Fragment key={i}>
                <button type="button" onClick={chip.pick} aria-pressed={chip.pressed} style={chip.on ? chipOn : chipOff}>
                  {chip.on && (
                    <>
                      <Icon name="check" size={22} strokeWidth={2} />
                    </>
                  )}
                  {" "}
                  {chip.label}
                </button>
              </Fragment>
            ))}
          </div>
          <p style={{ margin: "16px 0 0", fontSize: "17px", color: "#6B5E4E" }}>
            Đang xem chuyên mục:
            {" "}
            <strong style={{ color: "#2A2118" }}>{currentLabel}</strong>
            {" "}
            · {count} bài viết
          </p>
        </div>
      </section>
      <section style={{ width: "100%", padding: "32px 24px 0" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto" }}>
          <article className="soft-card" style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "28px", overflow: "hidden", display: "flex", flexWrap: "wrap" }}>
            <img src={facade} alt="Ảnh minh hoạ bài viết (ảnh tạm)" style={{ flex: "1 1 420px", minWidth: 0, height: "340px", objectFit: "cover", display: "block" }} />
            <div style={{ flex: "999 1 420px", minWidth: 0, padding: "36px", display: "flex", flexDirection: "column", gap: "16px", justifyContent: "center" }}>
              <div style={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: "12px" }}>
                <span style={{ background: "#E4EFE2", color: "#2E5B37", fontSize: "15px", fontWeight: 600, padding: "6px 14px", borderRadius: "999px" }}>Tin tức</span>
                <span style={{ fontSize: "16px", color: "#6B5E4E" }}>02/10/2026</span>
                <span style={{ fontSize: "16px", color: "#6B5E4E" }}>· Ban tiếp đón phòng khám</span>
              </div>
              <h2 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "30px", lineHeight: "40px", fontWeight: 700, color: "#2A2118" }}>
                Lịch khám trong dịp lễ sắp tới
              </h2>
              <p style={{ margin: 0, maxWidth: "58ch", fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Từ Thứ Hai 12/10 đến hết Thứ Tư 14/10/2026, phòng khám tạm ngưng nhận bệnh để đoàn bác sĩ đi khám thiện nguyện ở xa. Phiếu khám đã đặt rơi vào ba ngày này sẽ được gọi điện dời sang ngày khác.
              </p>
              <Link to="/tin-tuc/lich-kham-dip-le" aria-label="Đọc thêm: Lịch khám trong dịp lễ sắp tới" style={{ alignSelf: "flex-start", display: "inline-flex", alignItems: "center", gap: "8px", minHeight: "48px", fontSize: "19px", fontWeight: 600, color: "#2E5B37", textDecoration: "underline" }}>
                Đọc thêm
                <Icon name="arrow-right" size={22} />
              </Link>
            </div>
          </article>
        </div>
      </section>
      <section style={{ width: "100%", padding: "32px 24px 72px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "grid", gridTemplateColumns: "repeat(3, minmax(0, 1fr))", gap: "28px" }}>
          {posts.map((post, i) => (
            <Fragment key={i}>
              <article className="soft-card" style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", overflow: "hidden", display: "flex", flexDirection: "column" }}>
                <img src={facade} alt="Ảnh minh hoạ bài viết (ảnh tạm)" style={{ width: "100%", height: "200px", objectFit: "cover", display: "block" }} />
                <div style={{ padding: "24px", display: "flex", flexDirection: "column", gap: "12px", flexGrow: 1 }}>
                  <div style={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: "10px" }}>
                    <span style={tagStyles[post.tone]}>{post.category}</span>
                    <span style={{ fontSize: "15px", color: "#6B5E4E" }}>{post.date}</span>
                  </div>
                  <h3 style={{ margin: 0, fontSize: "21px", lineHeight: "30px", fontWeight: 600, color: "#2A2118" }}>{post.title}</h3>
                  <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>{post.summary}</p>
                  <Link to="/tin-tuc/lich-kham-dip-le" aria-label={post.aria} style={{ marginTop: "auto", fontSize: "17px", fontWeight: 600, color: "#2E5B37", textDecoration: "underline" }}>
                    Đọc thêm
                  </Link>
                </div>
              </article>
            </Fragment>
          ))}
        </div>
      </section>
      <section style={{ width: "100%", padding: "0 24px 72px" }}>
        <nav aria-label="Phân trang" style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexWrap: "wrap", alignItems: "center", justifyContent: "center", gap: "12px" }}>
          <button type="button" disabled style={{ display: "inline-flex", alignItems: "center", gap: "8px", minHeight: "52px", padding: "0 20px", background: "#F4EADB", border: "2px solid #B0A083", borderRadius: "12px", fontFamily: "inherit", fontSize: "18px", fontWeight: 600, color: "#6B5E4E", cursor: "not-allowed" }}>
            <Icon name="arrow-left" size={22} />
            Trang trước
          </button>
          <Link to="/tin-tuc" aria-current="page" style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", minWidth: "52px", minHeight: "52px", padding: "0 14px", background: "#3B7145", borderRadius: "12px", fontSize: "18px", fontWeight: 700, color: "#FFFFFF", textDecoration: "none" }}>
            1
          </Link>
          <Link to="/tin-tuc" style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", minWidth: "52px", minHeight: "52px", padding: "0 14px", background: "#FFFCF6", border: "2px solid #8C7B5E", borderRadius: "12px", fontSize: "18px", fontWeight: 600, color: "#2E5B37", textDecoration: "none" }}>
            2
          </Link>
          <Link to="/tin-tuc" style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", minWidth: "52px", minHeight: "52px", padding: "0 14px", background: "#FFFCF6", border: "2px solid #8C7B5E", borderRadius: "12px", fontSize: "18px", fontWeight: 600, color: "#2E5B37", textDecoration: "none" }}>
            3
          </Link>
          <Link to="/tin-tuc" style={{ display: "inline-flex", alignItems: "center", gap: "8px", minHeight: "52px", padding: "0 20px", background: "#FFFCF6", border: "2px solid #8C7B5E", borderRadius: "12px", fontSize: "18px", fontWeight: 600, color: "#2E5B37", textDecoration: "none" }}>
            Trang sau
            <Icon name="arrow-right" size={22} />
          </Link>
        </nav>
      </section>
    </PublicLayout>
  )
}
