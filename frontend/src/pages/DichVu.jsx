import { Link } from 'react-router-dom'
import Icon from '../components/Icon'
import { facade } from '../assets'
import { useAuth } from '../context/AuthContext'
import PublicLayout from '../components/PublicLayout'

export default function DichVu() {
  const { user } = useAuth()
  // Khách phải đăng nhập trước khi đặt lịch; người đã đăng nhập vào thẳng trang đặt lịch
  const bookTo = user ? '/dat-lich' : '/dat-lich/dang-nhap'
  return (
    <PublicLayout active="dich-vu">
      <section style={{ width: "100%", background: "#F4EADB", borderBottom: "1px solid #E0D3BC", padding: "40px 24px 56px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexDirection: "column", gap: "16px" }}>
          <nav aria-label="Đường dẫn" style={{ fontSize: "16px", color: "#6B5E4E" }}>
            <Link to="/" style={{ color: "#4A3F33", textDecoration: "underline" }}>Trang chủ</Link>
            {" "}
            <span style={{ padding: "0 8px" }}>›</span>
            {" "}
            <span>Dịch vụ</span>
          </nav>
          <h1 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "44px", lineHeight: "56px", fontWeight: 700, color: "#2A2118" }}>
            Dịch vụ khám và điều trị
          </h1>
          <p style={{ margin: 0, maxWidth: "72ch", fontSize: "20px", lineHeight: "32px", color: "#4A3F33" }}>
            Phòng khám chuyên điều trị và phục hồi chức năng bằng y học cổ truyền: bệnh cơ xương khớp, đau nhức, tai biến, mất ngủ, đau đầu. Bác sĩ xem mạch và chẩn đoán trước, rồi mới chọn phương pháp. Tất cả đều không thu phí.
          </p>
        </div>
      </section>
      <section id="cham-cuu" style={{ width: "100%", padding: "72px 24px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexWrap: "wrap", gap: "56px", alignItems: "center" }}>
          <div style={{ flex: "1 1 380px", minWidth: 0 }}>
            <img src={facade} alt="Khu châm cứu của phòng khám (ảnh tạm, sẽ thay bằng ảnh chính thức)" style={{ width: "100%", height: "380px", objectFit: "cover", borderRadius: "28px", display: "block" }} />
          </div>
          <div style={{ flex: "999 1 440px", minWidth: 0, display: "flex", flexDirection: "column", gap: "18px" }}>
            <span style={{ alignSelf: "flex-start", display: "inline-flex", alignItems: "center", gap: "8px", background: "#E4EFE2", color: "#2E5B37", fontSize: "16px", fontWeight: 600, padding: "8px 16px", borderRadius: "999px" }}>
              <Icon name="spine" size={20} />
              Châm cứu
            </span>
            <h2 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "34px", lineHeight: "44px", fontWeight: 700, color: "#2A2118" }}>
              Châm cứu
            </h2>
            <p style={{ margin: 0, maxWidth: "60ch", fontSize: "19px", lineHeight: "31px", color: "#4A3F33" }}>
              Dùng kim châm vào các huyệt để giảm đau và lưu thông khí huyết. Bác sĩ khám mạch, hỏi bệnh rồi mới chọn huyệt cho từng người.
            </p>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "24px", display: "flex", flexDirection: "column", gap: "12px" }}>
              <h3 style={{ margin: 0, fontSize: "19px", fontWeight: 600, color: "#2A2118" }}>Thường dùng cho</h3>
              <ul style={{ listStyle: "none", margin: 0, padding: 0, display: "flex", flexDirection: "column", gap: "10px" }}>
                <li style={{ display: "flex", gap: "10px", alignItems: "flex-start", fontSize: "18px", lineHeight: "28px", color: "#4A3F33" }}>
                  <Icon name="check" stroke="#2E5B37" strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
                  Đau vai gáy, đau lưng, đau thần kinh toạ
                </li>
                <li style={{ display: "flex", gap: "10px", alignItems: "flex-start", fontSize: "18px", lineHeight: "28px", color: "#4A3F33" }}>
                  <Icon name="check" stroke="#2E5B37" strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
                  Tê bì chân tay, mất ngủ kéo dài, đau đầu
                </li>
                <li style={{ display: "flex", gap: "10px", alignItems: "flex-start", fontSize: "18px", lineHeight: "28px", color: "#4A3F33" }}>
                  <Icon name="check" stroke="#2E5B37" strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
                  Phục hồi di chứng sau tai biến
                </li>
              </ul>
              <div style={{ display: "flex", flexWrap: "wrap", gap: "24px", paddingTop: "12px", borderTop: "1px solid #E0D3BC", fontSize: "17px", color: "#4A3F33" }}>
                <span>
                  <strong style={{ color: "rgb(42, 33, 24)" }}>Thời lượng:</strong>
                  {" "}
                  30 phút/buổi
                </span>
                <span>
                  <strong style={{ color: "rgb(42, 33, 24)" }}>Liệu trình:</strong>
                  {" "}
                  2-3 buổi
                </span>
              </div>
            </div>
            {user && (
              <Link to="/dat-lich" className="btn-primary" style={{ alignSelf: "flex-start", display: "inline-flex", alignItems: "center", gap: "10px", minHeight: "56px", padding: "0 28px", background: "#3B7145", borderRadius: "12px", fontSize: "19px", fontWeight: 600, color: "#FFFFFF", textDecoration: "none" }}>
                Đặt lịch khám
              </Link>
            )}
          </div>
        </div>
      </section>
      <section id="vat-ly-tri-lieu" style={{ width: "100%", padding: "72px 24px", background: "#F4EADB" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexWrap: "wrap-reverse", gap: "56px", alignItems: "center" }}>
          <div style={{ flex: "999 1 440px", minWidth: 0, display: "flex", flexDirection: "column", gap: "18px" }}>
            <span style={{ alignSelf: "flex-start", display: "inline-flex", alignItems: "center", gap: "8px", background: "#FFFCF6", color: "#2E5B37", fontSize: "16px", fontWeight: 600, padding: "8px 16px", borderRadius: "999px", border: "1px solid #E0D3BC" }}>
              <Icon name="sprout" size={20} />
              Vật lý trị liệu
            </span>
            <h2 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "34px", lineHeight: "44px", fontWeight: 700, color: "#2A2118" }}>
              Vật lý trị liệu — phục hồi chức năng
            </h2>
            <p style={{ margin: 0, maxWidth: "60ch", fontSize: "19px", lineHeight: "31px", color: "#4A3F33" }}>
              Các bài tập và kỹ thuật phục hồi vận động, có kỹ thuật viên đi cùng suốt buổi tập để chỉnh tư thế và theo dõi sức chịu đựng.
            </p>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "24px", display: "flex", flexDirection: "column", gap: "12px" }}>
              <h3 style={{ margin: 0, fontSize: "19px", fontWeight: 600, color: "#2A2118" }}>Thường dùng cho</h3>
              <ul style={{ listStyle: "none", margin: 0, padding: 0, display: "flex", flexDirection: "column", gap: "10px" }}>
                <li style={{ display: "flex", gap: "10px", alignItems: "flex-start", fontSize: "18px", lineHeight: "28px", color: "#4A3F33" }}>
                  <Icon name="check" stroke="#2E5B37" strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
                  Thoái hóa khớp gối, cứng khớp vai
                </li>
                <li style={{ display: "flex", gap: "10px", alignItems: "flex-start", fontSize: "18px", lineHeight: "28px", color: "#4A3F33" }}>
                  <Icon name="check" stroke="#2E5B37" strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
                  Phục hồi sau chấn thương, sau bó bột
                </li>
                <li style={{ display: "flex", gap: "10px", alignItems: "flex-start", fontSize: "18px", lineHeight: "28px", color: "#4A3F33" }}>
                  <Icon name="check" stroke="#2E5B37" strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
                  Tập đi lại cho người cao tuổi yếu chân
                </li>
              </ul>
              <div style={{ display: "flex", flexWrap: "wrap", gap: "24px", paddingTop: "12px", borderTop: "1px solid #E0D3BC", fontSize: "17px", color: "#4A3F33" }}>
                <span>
                  <strong style={{ color: "rgb(42, 33, 24)" }}>Thời lượng:</strong>
                  {" "}
                  30 phút/buổi
                </span>
                <span>
                  <strong style={{ color: "rgb(42, 33, 24)" }}>Liệu trình:</strong>
                  {" "}
                  2-3 buổi
                </span>
              </div>
            </div>
            {user && (
              <Link to="/dat-lich" className="btn-primary" style={{ alignSelf: "flex-start", display: "inline-flex", alignItems: "center", gap: "10px", minHeight: "56px", padding: "0 28px", background: "#3B7145", borderRadius: "12px", fontSize: "19px", fontWeight: 600, color: "#FFFFFF", textDecoration: "none" }}>
                Đặt lịch khám
              </Link>
            )}
          </div>
          <div style={{ flex: "1 1 380px", minWidth: 0 }}>
            <img src={facade} alt="Khu tập vật lý trị liệu (ảnh tạm, sẽ thay bằng ảnh chính thức)" style={{ width: "100%", height: "380px", objectFit: "cover", borderRadius: "28px", display: "block" }} />
          </div>
        </div>
      </section>
      <section id="xoa-bop" style={{ width: "100%", padding: "72px 24px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexWrap: "wrap", gap: "56px", alignItems: "center" }}>
          <div style={{ flex: "1 1 380px", minWidth: 0 }}>
            <img src={facade} alt="Phòng xoa bóp bấm huyệt (ảnh tạm, sẽ thay bằng ảnh chính thức)" style={{ width: "100%", height: "380px", objectFit: "cover", borderRadius: "28px", display: "block" }} />
          </div>
          <div style={{ flex: "999 1 440px", minWidth: 0, display: "flex", flexDirection: "column", gap: "18px" }}>
            <span style={{ alignSelf: "flex-start", display: "inline-flex", alignItems: "center", gap: "8px", background: "#E4EFE2", color: "#2E5B37", fontSize: "16px", fontWeight: 600, padding: "8px 16px", borderRadius: "999px" }}>
              <Icon name="hand" size={20} />
              Xoa bóp — bấm huyệt
            </span>
            <h2 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "34px", lineHeight: "44px", fontWeight: 700, color: "#2A2118" }}>
              Xoa bóp — bấm huyệt
            </h2>
            <p style={{ margin: 0, maxWidth: "60ch", fontSize: "19px", lineHeight: "31px", color: "#4A3F33" }}>
              Tác động bằng tay lên cơ và huyệt đạo để giảm co cứng, giảm mỏi và giúp ngủ ngon hơn. Thường kết hợp cùng châm cứu.
            </p>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "24px", display: "flex", flexDirection: "column", gap: "12px" }}>
              <h3 style={{ margin: 0, fontSize: "19px", fontWeight: 600, color: "#2A2118" }}>Thường dùng cho</h3>
              <ul style={{ listStyle: "none", margin: 0, padding: 0, display: "flex", flexDirection: "column", gap: "10px" }}>
                <li style={{ display: "flex", gap: "10px", alignItems: "flex-start", fontSize: "18px", lineHeight: "28px", color: "#4A3F33" }}>
                  <Icon name="check" stroke="#2E5B37" strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
                  Mỏi cổ vai gáy do ngồi lâu
                </li>
                <li style={{ display: "flex", gap: "10px", alignItems: "flex-start", fontSize: "18px", lineHeight: "28px", color: "#4A3F33" }}>
                  <Icon name="check" stroke="#2E5B37" strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
                  Co cứng cơ, chuột rút ban đêm
                </li>
                <li style={{ display: "flex", gap: "10px", alignItems: "flex-start", fontSize: "18px", lineHeight: "28px", color: "#4A3F33" }}>
                  <Icon name="check" stroke="#2E5B37" strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
                  Khó ngủ, căng thẳng kéo dài
                </li>
              </ul>
              <div style={{ display: "flex", flexWrap: "wrap", gap: "24px", paddingTop: "12px", borderTop: "1px solid #E0D3BC", fontSize: "17px", color: "#4A3F33" }}>
                <span>
                  <strong style={{ color: "rgb(42, 33, 24)" }}>Thời lượng:</strong>
                  {" "}
                  30 phút/buổi
                </span>
                <span>
                  <strong style={{ color: "rgb(42, 33, 24)" }}>Liệu trình:</strong>
                  {" "}
                  3 buổi
                </span>
              </div>
            </div>
            {user && (
              <Link to="/dat-lich" className="btn-primary" style={{ alignSelf: "flex-start", display: "inline-flex", alignItems: "center", gap: "10px", minHeight: "56px", padding: "0 28px", background: "#3B7145", borderRadius: "12px", fontSize: "19px", fontWeight: 600, color: "#FFFFFF", textDecoration: "none" }}>
                Đặt lịch khám
              </Link>
            )}
          </div>
        </div>
      </section>
      <section id="boc-thuoc" style={{ width: "100%", padding: "72px 24px", background: "#F4EADB" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexWrap: "wrap-reverse", gap: "56px", alignItems: "center" }}>
          <div style={{ flex: "999 1 440px", minWidth: 0, display: "flex", flexDirection: "column", gap: "18px" }}>
            <span style={{ alignSelf: "flex-start", display: "inline-flex", alignItems: "center", gap: "8px", background: "#FFFCF6", color: "#2E5B37", fontSize: "16px", fontWeight: 600, padding: "8px 16px", borderRadius: "999px", border: "1px solid #E0D3BC" }}>
              <Icon name="leaf" size={20} />
              Bốc thuốc
            </span>
            <h2 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "34px", lineHeight: "44px", fontWeight: 700, color: "#2A2118" }}>
              Bốc thuốc — thuốc Nam, thuốc Bắc
            </h2>
            <p style={{ margin: 0, maxWidth: "60ch", fontSize: "19px", lineHeight: "31px", color: "#4A3F33" }}>
              Bác sĩ bắt mạch, chẩn đoán rồi kê thang thuốc thảo dược hợp với thể trạng từng người. Thuốc được cân và gói ngay tại phòng khám, cô chú mang về sắc uống theo lời dặn.
            </p>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "24px", display: "flex", flexDirection: "column", gap: "12px" }}>
              <h3 style={{ margin: 0, fontSize: "19px", fontWeight: 600, color: "#2A2118" }}>Thường dùng cho</h3>
              <ul style={{ listStyle: "none", margin: 0, padding: 0, display: "flex", flexDirection: "column", gap: "10px" }}>
                <li style={{ display: "flex", gap: "10px", alignItems: "flex-start", fontSize: "18px", lineHeight: "28px", color: "#4A3F33" }}>
                  <Icon name="check" stroke="#2E5B37" strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
                  Đau nhức xương khớp kéo dài, hay tái phát
                </li>
                <li style={{ display: "flex", gap: "10px", alignItems: "flex-start", fontSize: "18px", lineHeight: "28px", color: "#4A3F33" }}>
                  <Icon name="check" stroke="#2E5B37" strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
                  Suy nhược cơ thể, ăn ngủ kém
                </li>
                <li style={{ display: "flex", gap: "10px", alignItems: "flex-start", fontSize: "18px", lineHeight: "28px", color: "#4A3F33" }}>
                  <Icon name="check" stroke="#2E5B37" strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
                  Hỗ trợ hồi phục sau tai biến
                </li>
              </ul>
              <div style={{ display: "flex", flexWrap: "wrap", gap: "24px", paddingTop: "12px", borderTop: "1px solid #E0D3BC", fontSize: "17px", color: "#4A3F33" }}>
                <span>
                  <strong style={{ color: "rgb(42, 33, 24)" }}>Thời lượng:</strong>
                  {" "}
                  15 phút bắt mạch
                </span>
                <span>
                  <strong style={{ color: "rgb(42, 33, 24)" }}>Liệu trình:</strong>
                  {" "}
                  5-7 thang mỗi đợt
                </span>
              </div>
            </div>
            {user && (
              <Link to="/dat-lich" className="btn-primary" style={{ alignSelf: "flex-start", display: "inline-flex", alignItems: "center", gap: "10px", minHeight: "56px", padding: "0 28px", background: "#3B7145", borderRadius: "12px", fontSize: "19px", fontWeight: 600, color: "#FFFFFF", textDecoration: "none" }}>
                Đặt lịch khám
              </Link>
            )}
          </div>
          <div style={{ flex: "1 1 380px", minWidth: 0 }}>
            <img src={facade} alt="Quầy bốc thuốc Nam, thuốc Bắc của phòng khám (ảnh tạm, sẽ thay bằng ảnh chính thức)" style={{ width: "100%", height: "380px", objectFit: "cover", borderRadius: "28px", display: "block" }} />
          </div>
        </div>
      </section>
      <section style={{ width: "100%", padding: "72px 24px 80px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", background: "#F7EBD2", border: "1px solid #E0D3BC", borderRadius: "28px", padding: "36px", display: "flex", flexWrap: "wrap", gap: "24px", alignItems: "flex-start" }}>
          <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "64px", height: "64px", background: "#FFFCF6", borderRadius: "999px", flexShrink: 0 }}>
            <Icon name="info" size={32} stroke="#8A6012" />
          </span>
          <div style={{ flex: "1 1 520px", minWidth: 0, display: "flex", flexDirection: "column", gap: "12px" }}>
            <h2 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "26px", lineHeight: "36px", fontWeight: 700, color: "#2A2118" }}>
              Những trường hợp cần nói trước với bác sĩ
            </h2>
            <p style={{ margin: 0, maxWidth: "68ch", fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
              Hãy báo với bác sĩ ngay khi đến nếu cô chú thuộc một trong những trường hợp dưới đây — phòng khám sẽ chọn cách điều trị khác an toàn hơn.
            </p>
            <ul style={{ listStyle: "none", margin: "8px 0 0", padding: 0, display: "flex", flexDirection: "column", gap: "18px" }}>
              <li style={{ display: "flex", gap: "16px", alignItems: "flex-start", maxWidth: "68ch", fontSize: "18px", lineHeight: "30px", color: "#4A3F33" }}>
                <span aria-hidden="true" style={{ display: "inline-block", width: "10px", height: "10px", marginTop: "10px", background: "#8A6012", borderRadius: "999px", flexShrink: 0 }} />
                <span>
                  Phụ nữ
                  {" "}
                  <strong style={{ color: "rgb(42, 33, 24)" }}>đang mang thai</strong>
                  .
                </span>
              </li>
              <li style={{ display: "flex", gap: "16px", alignItems: "flex-start", maxWidth: "68ch", fontSize: "18px", lineHeight: "30px", color: "#4A3F33" }}>
                <span aria-hidden="true" style={{ display: "inline-block", width: "10px", height: "10px", marginTop: "10px", background: "#8A6012", borderRadius: "999px", flexShrink: 0 }} />
                <span>
                  Cô chú
                  {" "}
                  <strong style={{ color: "#2A2118" }}>đang dùng thuốc chống đông máu</strong>
                  .
                </span>
              </li>
              <li style={{ display: "flex", gap: "16px", alignItems: "flex-start", maxWidth: "68ch", fontSize: "18px", lineHeight: "30px", color: "#4A3F33" }}>
                <span aria-hidden="true" style={{ display: "inline-block", width: "10px", height: "10px", marginTop: "10px", background: "#8A6012", borderRadius: "999px", flexShrink: 0 }} />
                <span>
                  Cô chú
                  {" "}
                  <strong style={{ color: "#2A2118" }}>có máy tạo nhịp tim</strong>
                  .
                </span>
              </li>
              <li style={{ display: "flex", gap: "16px", alignItems: "flex-start", maxWidth: "68ch", fontSize: "18px", lineHeight: "30px", color: "#4A3F33" }}>
                <span aria-hidden="true" style={{ display: "inline-block", width: "10px", height: "10px", marginTop: "10px", background: "#8A6012", borderRadius: "999px", flexShrink: 0 }} />
                <span>
                  Cô chú
                  {" "}
                  <strong style={{ color: "#2A2118" }}>đang sốt cao</strong>
                  , hoặc có
                  {" "}
                  <strong style={{ color: "#2A2118" }}>vết thương hở</strong>
                  {" "}
                  ở vùng cần điều trị.
                </span>
              </li>
              <li style={{ display: "flex", gap: "16px", alignItems: "flex-start", maxWidth: "68ch", fontSize: "18px", lineHeight: "30px", color: "#4A3F33" }}>
                <span aria-hidden="true" style={{ display: "inline-block", width: "10px", height: "10px", marginTop: "10px", background: "#8A6012", borderRadius: "999px", flexShrink: 0 }} />
                <span>
                  Đừng để bụng quá đói khi đi châm cứu — dễ bị “vựng châm”, tức chóng mặt, cồn cào, vã mồ hôi. Nhưng cũng đừng ăn quá no ngay trước giờ khám.
                </span>
              </li>
            </ul>
            {!user && (
              <Link to={bookTo} className="btn-primary" style={{ alignSelf: "flex-start", marginTop: "12px", display: "inline-flex", alignItems: "center", gap: "10px", minHeight: "56px", padding: "0 28px", background: "#3B7145", borderRadius: "12px", fontSize: "19px", fontWeight: 600, color: "#FFFFFF", textDecoration: "none" }}>
                Đặt lịch khám
              </Link>
            )}
            <p style={{ margin: "20px 0 0", paddingTop: "20px", borderTop: "1px solid #E0D3BC", maxWidth: "68ch", fontSize: "17px", lineHeight: "28px", color: "#6B5E4E" }}>
              Thông tin trên trang chỉ mang tính tham khảo và không thay thế chẩn đoán trực tiếp của thầy thuốc.
            </p>
          </div>
        </div>
      </section>
    </PublicLayout>
  )
}
