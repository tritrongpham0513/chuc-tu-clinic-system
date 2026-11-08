import Icon from './Icon'

export default function TopBar() {
  return (
    <div className="m-topbar" style={{ width: "100%", background: "#F7EBD2", borderBottom: "1px solid #E0D3BC" }}>
      <div style={{ maxWidth: "1160px", margin: "0 auto", padding: "10px 24px", display: "flex", flexWrap: "wrap", alignItems: "center", gap: "24px", fontSize: "16px", color: "#4A3F33" }}>
        <span style={{ display: "inline-flex", alignItems: "center", gap: "8px" }}>
          <Icon name="phone" size={20} stroke="#8A6012" />
          <a href="tel:0397116666" style={{ color: "#4A3F33", textDecoration: "underline" }}>0397 116 666</a>
        </span>
        <span style={{ display: "inline-flex", alignItems: "center", gap: "8px" }}>
          <Icon name="map-pin" size={20} stroke="#8A6012" />
          57/8-10 Điện Biên Phủ, Phường 25, Quận Bình Thạnh, TP. Hồ Chí Minh
        </span>
        <span style={{ marginLeft: "auto", display: "inline-flex", alignItems: "center", gap: "8px", fontWeight: 600, color: "#8A6012" }}>
          <Icon name="heart" size={20} stroke="#8A6012" />
          Khám và điều trị từ thiện — không thu phí
        </span>
      </div>
    </div>
  )
}
