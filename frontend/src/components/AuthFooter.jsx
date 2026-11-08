import { Link } from 'react-router-dom'

export default function AuthFooter() {
  return (
    <footer style={{ width: "100%", background: "#F4EADB", borderTop: "1px solid #E0D3BC", padding: "28px 24px" }}>
      <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexWrap: "wrap", gap: "24px", alignItems: "center", justifyContent: "space-between", fontSize: "16px", color: "#6B5E4E" }}>
        <span>© 2026 Phòng khám Chúc Từ · 57/8-10 Điện Biên Phủ, Phường 25, Quận Bình Thạnh, TP. Hồ Chí Minh</span>
        <Link to="/lien-he" style={{ color: "#2E5B37", textDecoration: "underline" }}>Cần giúp đỡ? Liên hệ phòng khám</Link>
      </div>
    </footer>
  )
}
