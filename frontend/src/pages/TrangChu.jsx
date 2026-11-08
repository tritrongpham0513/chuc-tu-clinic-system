import { Link } from 'react-router-dom'
import Icon from '../components/Icon'
import { facade } from '../assets'
import { useAuth } from '../context/AuthContext'
import PublicLayout from '../components/PublicLayout'

export default function TrangChu() {
  const { user } = useAuth()
  // Khách phải đăng nhập trước khi đặt lịch; người đã đăng nhập vào thẳng trang đặt lịch
  const bookTo = user ? '/dat-lich' : '/dat-lich/dang-nhap'
  return (
    <PublicLayout topbar>
      <section style={{ width: "100%", padding: "64px 24px 72px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexWrap: "wrap", gap: "56px", alignItems: "center" }}>
          <div style={{ flex: "999 1 460px", minWidth: 0, display: "flex", flexDirection: "column", gap: "22px" }}>
            <span style={{ alignSelf: "flex-start", display: "inline-flex", alignItems: "center", gap: "8px", background: "#E4EFE2", color: "#2E5B37", fontSize: "16px", fontWeight: 600, padding: "8px 16px", borderRadius: "999px" }}>
              <Icon name="leaf" size={20} />
              Phòng chẩn trị Y học cổ truyền từ thiện
            </span>
            <h1 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "48px", lineHeight: "60px", fontWeight: 700, color: "#2A2118" }}>
              Chúc Từ — chăm sóc sức khỏe bằng tấm lòng
            </h1>
            <p style={{ margin: 0, maxWidth: "60ch", fontSize: "20px", lineHeight: "32px", color: "#4A3F33" }}>
              Châm cứu, bấm huyệt, xoa bóp và thuốc y học cổ truyền cho bệnh cơ xương khớp, đau nhức, tai biến, mất ngủ, đau đầu. Hoàn toàn miễn phí. Đặt lịch ngay trên trang này, cô chú nhận được phiếu khám liền.
            </p>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "16px", marginTop: "4px" }}>
              <Link to={bookTo} className="btn-primary" style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", gap: "10px", minHeight: "60px", padding: "0 32px", background: "#3B7145", borderRadius: "12px", fontSize: "20px", fontWeight: 600, color: "#FFFFFF", textDecoration: "none", boxShadow: "0 1px 2px rgba(74, 63, 51, 0.08)" }}>
                <Icon name="calendar-plus" />
                Đặt lịch khám
              </Link>
              <Link to="/huong-dan" className="btn-ghost" style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", gap: "10px", minHeight: "60px", padding: "0 28px", border: "2px solid #8C7B5E", borderRadius: "12px", fontSize: "20px", fontWeight: 600, color: "#2E5B37", textDecoration: "none", background: "#FFFCF6" }}>
                <Icon name="help-circle" />
                Xem hướng dẫn đi khám
              </Link>
            </div>
            {!user && (
              <p style={{ margin: 0, display: "flex", alignItems: "center", gap: "10px", fontSize: "17px", lineHeight: "26px", color: "#6B5E4E" }}>
                <Icon name="lock" size={22} stroke="#8A6012" style={{ flexShrink: 0 }} />
                Cần
                <Link to={bookTo} style={{ color: "#2E5B37", fontWeight: 600, textDecoration: "underline" }}>đăng nhập tài khoản</Link>
                trước khi đặt lịch.
              </p>
            )}
            <ul style={{ listStyle: "none", margin: "12px 0 0", padding: 0, display: "flex", flexWrap: "wrap", gap: "28px" }}>
              <li style={{ display: "flex", alignItems: "center", gap: "12px", fontSize: "18px", color: "#4A3F33" }}>
                <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "44px", height: "44px", background: "#E4EFE2", borderRadius: "999px", flexShrink: 0 }}>
                  <Icon name="heart" stroke="#2E5B37" />
                </span>
                Không thu phí
              </li>
              <li style={{ display: "flex", alignItems: "center", gap: "12px", fontSize: "18px", color: "#4A3F33" }}>
                <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "44px", height: "44px", background: "#E4EFE2", borderRadius: "999px", flexShrink: 0 }}>
                  <Icon name="users" stroke="#2E5B37" />
                </span>
                <span>Bác sĩ & nhân viên</span>
              </li>
              <li style={{ display: "flex", alignItems: "center", gap: "12px", fontSize: "18px", color: "#4A3F33" }}>
                <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "44px", height: "44px", background: "#E4EFE2", borderRadius: "999px", flexShrink: 0 }}>
                  <Icon name="clock" stroke="#2E5B37" />
                </span>
                Ưu tiên bà con khó khăn
              </li>
            </ul>
          </div>
          <div style={{ flex: "1 1 380px", minWidth: 0, position: "relative" }}>
            <img src={facade} alt="Mặt tiền Phòng khám Chúc Từ (ảnh tạm, sẽ thay bằng ảnh chính thức)" style={{ width: "100%", height: "480px", objectFit: "cover", borderRadius: "28px", border: "4px solid #C9963F", display: "block" }} />
            <div className="clinic-hours">
              <span className="clinic-hours-icon">
                <Icon name="clock" size={24} stroke="#8A6012" />
              </span>
              <span className="clinic-hours-copy">
                <span className="clinic-hours-label">Giờ khám hôm nay</span>
                <span className="clinic-hours-time">8:00 – 16:30</span>
              </span>
            </div>
          </div>
        </div>
      </section>
      <section style={{ width: "100%", padding: "88px 24px", background: "#F4EADB" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexDirection: "column", gap: "40px" }}>
          <div style={{ maxWidth: "68ch" }}>
            <h2 style={{ margin: "0 0 12px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "38px", lineHeight: "48px", fontWeight: 700, color: "#2A2118" }}>
              Đặt lịch khám diễn ra thế nào?
            </h2>
            <div style={{ width: "72px", height: "4px", background: "#C9963F", borderRadius: "999px", marginBottom: "16px" }} />
            <p style={{ margin: 0, fontSize: "20px", lineHeight: "32px", color: "#4A3F33" }}>
              Đặt lịch xong là
              {" "}
              <strong style={{ color: "#2A2118" }}>có phiếu khám ngay</strong>
              . Ngày đi khám cô chú chỉ cần đưa phiếu khám cho tiếp tân.
            </p>
          </div>
          <ol style={{ listStyle: "none", margin: 0, padding: 0, display: "grid", gridTemplateColumns: "repeat(4, minmax(0, 1fr))", gap: "24px" }}>
            <li style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "28px", display: "flex", flexDirection: "column", gap: "12px" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "48px", height: "48px", background: "#3B7145", color: "#FFFFFF", borderRadius: "999px", fontSize: "22px", fontWeight: 700 }}>
                1
              </span>
              <h3 style={{ margin: 0, fontSize: "20px", lineHeight: "28px", fontWeight: 600, color: "#2A2118" }}>{user ? 'Đã đăng nhập' : 'Đăng nhập'}</h3>
              <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>
                {user ? 'Cô chú đang đăng nhập rồi, không cần làm lại bước này.' : 'Chưa có tài khoản thì tạo một lần, lần sau khỏi nhập lại thông tin.'}
              </p>
            </li>
            <li style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "28px", display: "flex", flexDirection: "column", gap: "12px" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "48px", height: "48px", background: "#3B7145", color: "#FFFFFF", borderRadius: "999px", fontSize: "22px", fontWeight: 700 }}>
                2
              </span>
              <h3 style={{ margin: 0, fontSize: "20px", lineHeight: "28px", fontWeight: 600, color: "#2A2118" }}>Đặt lịch</h3>
              <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>Chọn ngày giờ, người đi khám rồi bấm đặt lịch.</p>
            </li>
            <li style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "28px", display: "flex", flexDirection: "column", gap: "12px" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "48px", height: "48px", background: "#3B7145", color: "#FFFFFF", borderRadius: "999px", fontSize: "22px", fontWeight: 700 }}>
                3
              </span>
              <h3 style={{ margin: 0, fontSize: "20px", lineHeight: "28px", fontWeight: 600, color: "#2A2118" }}>Nhận phiếu khám</h3>
              <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>
                Có ngay phiếu khám kèm mã phiếu khám và giờ khám dự kiến, xem trong mục Phiếu khám bệnh.
              </p>
            </li>
            <li style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "28px", display: "flex", flexDirection: "column", gap: "12px" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "48px", height: "48px", background: "#3B7145", color: "#FFFFFF", borderRadius: "999px", fontSize: "22px", fontWeight: 700 }}>
                4
              </span>
              <h3 style={{ margin: 0, fontSize: "20px", lineHeight: "28px", fontWeight: 600, color: "#2A2118" }}>Đến khám</h3>
              <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>
                Đưa phiếu khám cho tiếp tân để check-in. Không đến được thì cô chú huỷ phiếu khám ngay trên trang.
              </p>
            </li>
          </ol>
        </div>
      </section>
      <section style={{ width: "100%", padding: "88px 24px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexDirection: "column", gap: "40px" }}>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "16px", alignItems: "flex-end", justifyContent: "space-between" }}>
            <div style={{ maxWidth: "60ch" }}>
              <h2 style={{ margin: "0 0 12px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "38px", lineHeight: "48px", fontWeight: 700, color: "#2A2118", maxWidth: "100%" }}>
                Phòng khám giúp được gì cho cô chú?
              </h2>
              <div style={{ width: "72px", height: "4px", background: "#C9963F", borderRadius: "999px" }} />
            </div>
            <Link to="/dich-vu" className="card-link" style={{ display: "inline-flex", alignItems: "center", gap: "8px", minHeight: "48px", fontSize: "19px", fontWeight: 600, color: "#2E5B37", textDecoration: "underline", textAlign: "justify" }}>
              Xem tất cả dịch vụ
              <Icon name="arrow-right" size={22} />
            </Link>
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(4, minmax(0, 1fr))", gap: "24px" }}>
            <Link to="/dich-vu#cham-cuu" className="soft-card" aria-label="Xem chi tiết châm cứu" style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "28px", display: "flex", flexDirection: "column", gap: "16px", textDecoration: "none", color: "inherit" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "64px", height: "64px", background: "#E4EFE2", borderRadius: "999px" }}>
                <Icon name="spine" size={32} stroke="#2E5B37" />
              </span>
              <h3 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "23px", lineHeight: "32px", fontWeight: 600, color: "#2A2118" }}>
                Châm cứu
              </h3>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Giảm đau vai gáy, đau lưng, tê bì chân tay và di chứng sau tai biến.
              </p>
              <span aria-hidden="true" style={{ marginTop: "auto", display: "inline-flex", alignItems: "center", justifyContent: "center", width: "44px", height: "44px", background: "#E4EFE2", borderRadius: "999px", color: "#2E5B37" }}>
                <Icon name="arrow-right" strokeWidth={2} />
              </span>
            </Link>
            <Link to="/dich-vu#vat-ly-tri-lieu" className="soft-card" aria-label="Xem chi tiết vật lý trị liệu" style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "28px", display: "flex", flexDirection: "column", gap: "16px", textDecoration: "none", color: "inherit" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "64px", height: "64px", background: "#E4EFE2", borderRadius: "999px" }}>
                <Icon name="sprout" size={32} stroke="#2E5B37" />
              </span>
              <h3 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "23px", lineHeight: "32px", fontWeight: 600, color: "#2A2118" }}>
                Vật lý trị liệu
              </h3>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Tập phục hồi vận động sau chấn thương, thoái hóa khớp và cứng khớp.
              </p>
              <span aria-hidden="true" style={{ marginTop: "auto", display: "inline-flex", alignItems: "center", justifyContent: "center", width: "44px", height: "44px", background: "#E4EFE2", borderRadius: "999px", color: "#2E5B37" }}>
                <Icon name="arrow-right" strokeWidth={2} />
              </span>
            </Link>
            <Link to="/dich-vu#xoa-bop" className="soft-card" aria-label="Xem chi tiết xoa bóp — bấm huyệt" style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "28px", display: "flex", flexDirection: "column", gap: "16px", textDecoration: "none", color: "inherit" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "64px", height: "64px", background: "#E4EFE2", borderRadius: "999px" }}>
                <Icon name="hand" size={32} stroke="#2E5B37" />
              </span>
              <h3 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "23px", lineHeight: "32px", fontWeight: 600, color: "#2A2118" }}>
                Xoa bóp — bấm huyệt
              </h3>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Thư giãn cơ, giảm mỏi và hỗ trợ giấc ngủ, thường kết hợp cùng châm cứu.
              </p>
              <span aria-hidden="true" style={{ marginTop: "auto", display: "inline-flex", alignItems: "center", justifyContent: "center", width: "44px", height: "44px", background: "#E4EFE2", borderRadius: "999px", color: "#2E5B37" }}>
                <Icon name="arrow-right" strokeWidth={2} />
              </span>
            </Link>
            <Link to="/dich-vu#boc-thuoc" className="soft-card" aria-label="Xem chi tiết bốc thuốc" style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "28px", display: "flex", flexDirection: "column", gap: "16px", textDecoration: "none", color: "inherit" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "64px", height: "64px", background: "#E4EFE2", borderRadius: "999px" }}>
                <Icon name="leaf" size={32} stroke="#2E5B37" />
              </span>
              <h3 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "23px", lineHeight: "32px", fontWeight: 600, color: "#2A2118" }}>
                Bốc thuốc
              </h3>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Bắt mạch, kê thang thuốc Nam – thuốc Bắc, cân và gói ngay tại phòng khám.
              </p>
              <span aria-hidden="true" style={{ marginTop: "auto", display: "inline-flex", alignItems: "center", justifyContent: "center", width: "44px", height: "44px", background: "#E4EFE2", borderRadius: "999px", color: "#2E5B37" }}>
                <Icon name="arrow-right" strokeWidth={2} />
              </span>
            </Link>
          </div>
          <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "28px", display: "flex", flexWrap: "wrap", gap: "20px", alignItems: "center" }}>
            <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "56px", height: "56px", background: "#F7EBD2", borderRadius: "999px", flexShrink: 0 }}>
              <Icon name="pill-jar" size={28} stroke="#8A6012" />
            </span>
            <p style={{ flex: "999 1 420px", minWidth: 0, margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
              <strong style={{ color: "#2A2118" }}>Kê toa thuốc y học cổ truyền và phục hồi chức năng.</strong>
              {" "}
              Tuỳ tình trạng, bác sĩ kê thuốc nam, thuốc bắc hoặc thuốc đã bào chế sẵn. Cô chú nhận thuốc tại chỗ, cũng không mất tiền.
            </p>
            <Link to="/dich-vu" className="card-link" style={{ flex: "0 1 auto", display: "inline-flex", alignItems: "center", gap: "8px", minHeight: "48px", fontSize: "18px", fontWeight: 600, color: "#2E5B37", textDecoration: "underline" }}>
              Xem trang Dịch vụ
            </Link>
          </div>
        </div>
      </section>
      <section style={{ width: "100%", padding: "0 24px 88px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "grid", gridTemplateColumns: "repeat(2, minmax(0, 1fr))", gap: "28px" }}>
          <div style={{ background: "#F7EBD2", borderRadius: "28px", padding: "36px", display: "flex", flexDirection: "column", gap: "20px" }}>
            <h2 style={{ margin: 0, display: "flex", alignItems: "center", gap: "12px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "28px", lineHeight: "38px", fontWeight: 700, color: "#2A2118" }}>
              <Icon name="clock" size={30} stroke="#8A6012" />
              Giờ khám
            </h2>
            <dl style={{ margin: 0 }}>
              <dt style={{ fontSize: "19px", color: "#4A3F33" }}>Tất cả các ngày trong tuần</dt>
              <dd style={{ margin: "8px 0 0", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "40px", lineHeight: "50px", fontWeight: 700, color: "#2A2118" }}>
                8:00 – 16:30
              </dd>
            </dl>
            <p style={{ margin: 0, display: "flex", gap: "10px", alignItems: "flex-start", fontSize: "18px", lineHeight: "28px", color: "#4A3F33" }}>
              <Icon name="info" size={22} stroke="#8A6012" style={{ flexShrink: 0, marginTop: "3px" }} />
              Buổi sáng thường rất đông. Cô chú nên đến sớm đầu giờ sáng để khỏi phải chờ lâu, hoặc gọi trước để hỏi phòng khám còn nhận số không.
            </p>
          </div>
          <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "28px", padding: "36px", display: "flex", flexDirection: "column", gap: "20px" }}>
            <h2 style={{ margin: 0, display: "flex", alignItems: "center", gap: "12px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "28px", lineHeight: "38px", fontWeight: 700, color: "#2A2118" }}>
              <Icon name="map-pin" size={30} stroke="#8A6012" />
              Địa chỉ & liên hệ
            </h2>
            <p style={{ margin: 0, fontSize: "19px", lineHeight: "31px", color: "#4A3F33" }}>57/8-10 Điện Biên Phủ, Phường 25, Quận Bình Thạnh, TP. Hồ Chí Minh</p>
            <p style={{ margin: 0, fontSize: "19px", lineHeight: "31px", color: "#4A3F33" }}>
              Điện thoại:
              {" "}
              <a href="tel:0397116666" style={{ color: "#2E5B37", textDecoration: "underline", fontWeight: 600 }}>0397 116 666</a>
            </p>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "12px", marginTop: "auto" }}>
              <Link to="/lien-he" className="btn-ghost" style={{ display: "inline-flex", alignItems: "center", gap: "8px", minHeight: "48px", padding: "0 20px", border: "2px solid #8C7B5E", borderRadius: "12px", fontSize: "18px", fontWeight: 600, color: "#2E5B37", textDecoration: "none" }}>
                Xem đường đi
              </Link>
              <Link to="/lien-he" className="btn-ghost" style={{ display: "inline-flex", alignItems: "center", gap: "8px", minHeight: "48px", padding: "0 20px", border: "2px solid #8C7B5E", borderRadius: "12px", fontSize: "18px", fontWeight: 600, color: "#2E5B37", textDecoration: "none" }}>
                Trang liên hệ
              </Link>
            </div>
          </div>
        </div>
      </section>
      <section style={{ width: "100%", padding: "88px 24px", background: "#F4EADB" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexDirection: "column", gap: "40px" }}>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "16px", alignItems: "flex-end", justifyContent: "space-between" }}>
            <div>
              <h2 style={{ margin: "0 0 12px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "38px", lineHeight: "48px", fontWeight: 700, color: "#2A2118" }}>
                Tin tức
              </h2>
              <div style={{ width: "72px", height: "4px", background: "#C9963F", borderRadius: "999px" }} />
            </div>
            <Link to="/tin-tuc" className="card-link" style={{ display: "inline-flex", alignItems: "center", gap: "8px", minHeight: "48px", fontSize: "19px", fontWeight: 600, color: "#2E5B37", textDecoration: "underline" }}>
              Xem tất cả bài viết
              <Icon name="arrow-right" size={22} />
            </Link>
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(3, minmax(0, 1fr))", gap: "28px" }}>
            <article className="soft-card" style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", overflow: "hidden", display: "flex", flexDirection: "column" }}>
              <img src={facade} alt="Ảnh minh hoạ bài viết (ảnh tạm)" style={{ width: "100%", height: "200px", objectFit: "cover", display: "block" }} />
              <div style={{ padding: "24px", display: "flex", flexDirection: "column", gap: "12px", flexGrow: 1 }}>
                <div style={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: "10px" }}>
                  <span style={{ background: "#E4EFE2", color: "#2E5B37", fontSize: "15px", fontWeight: 600, padding: "6px 14px", borderRadius: "999px" }}>
                    Tin tức
                  </span>
                  <span style={{ fontSize: "15px", color: "#6B5E4E" }}>02/10/2026</span>
                </div>
                <h3 style={{ margin: 0, fontSize: "21px", lineHeight: "30px", fontWeight: 600, color: "#2A2118" }}>Lịch khám trong dịp lễ sắp tới</h3>
                <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>
                  Phòng khám nghỉ hai ngày lễ, các ngày còn lại vẫn nhận bệnh bình thường 8:00 – 16:30.
                </p>
                <Link to="/tin-tuc/lich-kham-dip-le" className="card-link" aria-label="Đọc thêm: Lịch khám trong dịp lễ sắp tới" style={{ marginTop: "auto", fontSize: "17px", fontWeight: 600, color: "#2E5B37", textDecoration: "underline" }}>
                  Đọc thêm
                </Link>
              </div>
            </article>
            <article className="soft-card" style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", overflow: "hidden", display: "flex", flexDirection: "column" }}>
              <img src={facade} alt="Ảnh minh hoạ bài viết (ảnh tạm)" style={{ width: "100%", height: "200px", objectFit: "cover", display: "block" }} />
              <div style={{ padding: "24px", display: "flex", flexDirection: "column", gap: "12px", flexGrow: 1 }}>
                <div style={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: "10px" }}>
                  <span style={{ background: "#F7EBD2", color: "#8A6012", fontSize: "15px", fontWeight: 600, padding: "6px 14px", borderRadius: "999px" }}>
                    Hướng dẫn
                  </span>
                  <span style={{ fontSize: "15px", color: "#6B5E4E" }}>24/09/2026</span>
                </div>
                <h3 style={{ margin: 0, fontSize: "21px", lineHeight: "30px", fontWeight: 600, color: "#2A2118" }}>Cần chuẩn bị gì trước buổi châm cứu đầu tiên</h3>
                <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>
                  Cô chú nên ăn nhẹ, mặc đồ rộng rãi và mang theo toa thuốc đang uống khi tới khám.
                </p>
                <Link to="/tin-tuc/lich-kham-dip-le" className="card-link" aria-label="Đọc thêm: Cần chuẩn bị gì trước buổi châm cứu đầu tiên" style={{ marginTop: "auto", fontSize: "17px", fontWeight: 600, color: "#2E5B37", textDecoration: "underline" }}>
                  Đọc thêm
                </Link>
              </div>
            </article>
            <article className="soft-card" style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", overflow: "hidden", display: "flex", flexDirection: "column" }}>
              <img src={facade} alt="Ảnh minh hoạ bài viết (ảnh tạm)" style={{ width: "100%", height: "200px", objectFit: "cover", display: "block" }} />
              <div style={{ padding: "24px", display: "flex", flexDirection: "column", gap: "12px", flexGrow: 1 }}>
                <div style={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: "10px" }}>
                  <span style={{ background: "#E4EFE2", color: "#2E5B37", fontSize: "15px", fontWeight: 600, padding: "6px 14px", borderRadius: "999px" }}>
                    Thiện nguyện
                  </span>
                  <span style={{ fontSize: "15px", color: "#6B5E4E" }}>18/09/2026</span>
                </div>
                <h3 style={{ margin: 0, fontSize: "21px", lineHeight: "30px", fontWeight: 600, color: "#2A2118" }}>
                  Buổi khám thiện nguyện cùng bà con trong phường
                </h3>
                <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>
                  Hơn 120 cô cô chú trong phường được khám và phát thuốc miễn phí sáng Chủ Nhật vừa rồi.
                </p>
                <Link to="/tin-tuc/lich-kham-dip-le" className="card-link" aria-label="Đọc thêm: Buổi khám thiện nguyện cùng bà con trong phường" style={{ marginTop: "auto", fontSize: "17px", fontWeight: 600, color: "#2E5B37", textDecoration: "underline" }}>
                  Đọc thêm
                </Link>
              </div>
            </article>
          </div>
        </div>
      </section>
      <section style={{ width: "100%", padding: "72px 24px", background: "#3B7145" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexWrap: "wrap", gap: "32px", alignItems: "center", justifyContent: "space-between" }}>
          <div style={{ flex: "1 1 420px", minWidth: 0 }}>
            <h2 style={{ margin: "0 0 12px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "34px", lineHeight: "44px", fontWeight: 700, color: "#FFFFFF" }}>
              Cô chú muốn đặt lịch khám?
            </h2>
            <p style={{ margin: 0, maxWidth: "56ch", fontSize: "19px", lineHeight: "31px", color: "#FFFFFF" }}>
              {user ? 'Đặt lịch chỉ mất vài phút. Nếu thấy khó, cô chú cứ gọi điện cho phòng khám.' : 'Đăng nhập rồi đặt lịch, chỉ mất vài phút. Nếu thấy khó, cô chú cứ gọi điện cho phòng khám.'}
            </p>
          </div>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "16px" }}>
            <Link to={bookTo} className="btn-gold" style={{ display: "inline-flex", alignItems: "center", gap: "10px", minHeight: "60px", padding: "0 32px", background: "#FFFCF6", borderRadius: "12px", fontSize: "20px", fontWeight: 600, color: "#2E5B37", textDecoration: "none" }}>
              <Icon name="calendar-plus" />
              Đặt lịch khám
            </Link>
            <a href="tel:0397116666" style={{ display: "inline-flex", alignItems: "center", gap: "10px", minHeight: "60px", padding: "0 28px", border: "2px solid #FFFFFF", borderRadius: "12px", fontSize: "20px", fontWeight: 600, color: "#FFFFFF", textDecoration: "underline" }}>
              <Icon name="phone" />
              Gọi 0397 116 666
            </a>
          </div>
        </div>
      </section>
    </PublicLayout>
  )
}
