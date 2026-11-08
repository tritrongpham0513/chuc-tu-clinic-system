import { Link } from 'react-router-dom'
import Icon from '../components/Icon'
import { facade } from '../assets'
import { useAuth } from '../context/AuthContext'
import PublicLayout from '../components/PublicLayout'

export default function BaiVietChiTiet() {
  const { user } = useAuth()
  // Khách phải đăng nhập trước khi đặt lịch; người đã đăng nhập vào thẳng trang đặt lịch
  const bookTo = user ? '/dat-lich' : '/dat-lich/dang-nhap'
  return (
    <PublicLayout active="tin-tuc">
      <section style={{ width: "100%", background: "#F4EADB", borderBottom: "1px solid #E0D3BC", padding: "36px 24px 48px" }}>
        <div style={{ maxWidth: "820px", margin: "0 auto", display: "flex", flexDirection: "column", gap: "16px" }}>
          <nav aria-label="Đường dẫn" style={{ fontSize: "16px", color: "#6B5E4E" }}>
            <Link to="/" style={{ color: "#4A3F33", textDecoration: "underline" }}>Trang chủ</Link>
            {" "}
            <span style={{ padding: "0 8px" }}>›</span>
            {" "}
            <Link to="/tin-tuc" style={{ color: "#4A3F33", textDecoration: "underline" }}>Bài viết</Link>
            {" "}
            <span style={{ padding: "0 8px" }}>›</span>
            {" "}
            <span>Tin tức</span>
          </nav>
          <span style={{ alignSelf: "flex-start", background: "#E4EFE2", color: "#2E5B37", fontSize: "16px", fontWeight: 600, padding: "7px 16px", borderRadius: "999px" }}>
            Tin tức
          </span>
          <h1 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "42px", lineHeight: "54px", fontWeight: 700, color: "#2A2118" }}>
            Lịch khám trong dịp lễ sắp tới
          </h1>
          <div style={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: "20px", fontSize: "17px", color: "#6B5E4E" }}>
            <span style={{ display: "inline-flex", alignItems: "center", gap: "8px" }}>
              <Icon name="user" size={20} stroke="#6B5E4E" />
              Ban tiếp đón phòng khám
            </span>
            <span style={{ display: "inline-flex", alignItems: "center", gap: "8px" }}>
              <Icon name="calendar" size={20} stroke="#6B5E4E" />
              Đăng ngày 02/10/2026
            </span>
          </div>
        </div>
      </section>
      <article style={{ width: "100%", padding: "48px 24px 72px" }}>
        <div style={{ maxWidth: "820px", margin: "0 auto", display: "flex", flexDirection: "column", gap: "28px" }}>
          <img src={facade} alt="Ảnh đại diện bài viết (ảnh tạm, sẽ thay bằng ảnh thật)" style={{ width: "100%", height: "420px", objectFit: "cover", borderRadius: "28px", display: "block" }} />
          <p style={{ margin: 0, fontSize: "21px", lineHeight: "34px", fontWeight: 500, color: "#2A2118" }}>
            Từ Thứ Hai ngày 12/10 đến hết Thứ Tư ngày 14/10/2026, phòng khám tạm ngưng nhận bệnh tại chỗ để đoàn bác sĩ đi khám thiện nguyện ở xa. Những ngày còn lại trong tháng, phòng khám vẫn mở cửa bình thường từ 8:00 đến 16:30, kể cả Chủ Nhật.
          </p>
          <p style={{ margin: 0, fontSize: "19px", lineHeight: "33px", color: "#4A3F33" }}>
            Mỗi năm phòng khám dành ra vài ngày để đoàn bác sĩ và nhân viên về vùng xa khám bệnh, phát thuốc cho bà con không có điều kiện lên thành phố. Năm nay chuyến đi rơi đúng vào dịp lễ, nên phòng khám xin thông báo sớm để bà con chủ động sắp xếp ngày đi khám của mình.
          </p>
          <h2 style={{ margin: "12px 0 0", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "30px", lineHeight: "40px", fontWeight: 700, color: "#2A2118" }}>
            Bà con cần lưu ý mấy điều
          </h2>
          <p style={{ margin: 0, fontSize: "19px", lineHeight: "33px", color: "#4A3F33" }}>
            Cô chú không cần tự huỷ lịch trên website. Những phiếu khám đã đặt mà rơi vào ba ngày nghỉ, phòng khám sẽ gọi điện báo trước và hẹn lại ngày gần nhất còn chỗ.
          </p>
          <ul style={{ margin: 0, paddingLeft: "26px", display: "flex", flexDirection: "column", gap: "12px" }}>
            <li style={{ fontSize: "19px", lineHeight: "32px", color: "#4A3F33" }}>
              Phiếu khám đã đặt vào ngày 12, 13 hoặc 14/10 sẽ được phòng khám gọi điện dời sang ngày khác.
            </li>
            <li style={{ fontSize: "19px", lineHeight: "32px", color: "#4A3F33" }}>
              Cô chú vẫn đặt được phiếu khám cho các ngày từ 15/10 trở đi, kể cả đặt trong ba ngày nghỉ này.
            </li>
            <li style={{ fontSize: "19px", lineHeight: "32px", color: "#4A3F33" }}>
              Cô chú nào đang uống thuốc theo thang, xin ghé lấy thuốc trước Chủ Nhật ngày 11/10 để không bị đứt quãng.
            </li>
          </ul>
          <blockquote style={{ margin: 0, padding: "28px 32px", background: "#F7EBD2", borderRadius: "20px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "22px", lineHeight: "36px", color: "#2A2118" }}>
            Phòng khám mở cửa lại bình thường từ 8:00 sáng Thứ Năm, ngày 15/10/2026.
          </blockquote>
          <p style={{ margin: 0, fontSize: "19px", lineHeight: "33px", color: "#4A3F33" }}>
            Nếu cô chú chưa rõ lịch khám của mình rơi vào ngày nào, cứ gọi 0397 116 666 trong giờ làm việc, nhân viên sẽ tra giúp và hẹn lại ngày mới. Phòng khám xin cảm ơn bà con đã thông cảm.
          </p>
          {/* Hành động */}
          <div style={{ marginTop: "12px", background: "#E4EFE2", borderRadius: "28px", padding: "32px", display: "flex", flexWrap: "wrap", gap: "24px", alignItems: "center", justifyContent: "space-between" }}>
            <div style={{ flex: "1 1 320px", minWidth: 0 }}>
              <h2 style={{ margin: "0 0 8px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "26px", lineHeight: "36px", fontWeight: 700, color: "#2A2118" }}>
                Cô chú cần đi khám?
              </h2>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>{user ? 'Đặt lịch xong là có ngay phiếu khám.' : 'Đăng nhập rồi đặt lịch, có ngay phiếu khám.'}</p>
            </div>
            <Link to={bookTo} className="btn-primary" style={{ display: "inline-flex", alignItems: "center", gap: "10px", minHeight: "56px", padding: "0 28px", background: "#3B7145", borderRadius: "12px", fontSize: "19px", fontWeight: 600, color: "#FFFFFF", textDecoration: "none" }}>
              <Icon name="calendar-plus" />
              Đặt lịch khám
            </Link>
          </div>
        </div>
      </article>
      <section style={{ width: "100%", padding: "72px 24px", background: "#F4EADB" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexDirection: "column", gap: "32px" }}>
          <div>
            <h2 style={{ margin: "0 0 12px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "32px", lineHeight: "42px", fontWeight: 700, color: "#2A2118" }}>
              Bài viết cùng chuyên mục
            </h2>
            <div style={{ width: "72px", height: "4px", background: "#C9963F", borderRadius: "999px" }} />
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(3, minmax(0, 1fr))", gap: "28px" }}>
            <article className="soft-card" style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", overflow: "hidden", display: "flex", flexDirection: "column" }}>
              <img src={facade} alt="Ảnh minh hoạ bài viết (ảnh tạm)" style={{ width: "100%", height: "180px", objectFit: "cover", display: "block" }} />
              <div style={{ padding: "24px", display: "flex", flexDirection: "column", gap: "10px", flexGrow: 1 }}>
                <span style={{ alignSelf: "flex-start", background: "#E4EFE2", color: "#2E5B37", fontSize: "15px", fontWeight: 600, padding: "6px 14px", borderRadius: "999px" }}>
                  Tin tức
                </span>
                <h3 style={{ margin: 0, fontSize: "20px", lineHeight: "29px", fontWeight: 600, color: "#2A2118" }}>Thay đổi giờ khám buổi chiều từ tuần sau</h3>
                <Link to="/tin-tuc/lich-kham-dip-le" aria-label="Đọc thêm: Thay đổi giờ khám buổi chiều từ tuần sau" style={{ marginTop: "auto", fontSize: "17px", fontWeight: 600, color: "#2E5B37", textDecoration: "underline" }}>
                  Đọc thêm
                </Link>
              </div>
            </article>
            <article className="soft-card" style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", overflow: "hidden", display: "flex", flexDirection: "column" }}>
              <img src={facade} alt="Ảnh minh hoạ bài viết (ảnh tạm)" style={{ width: "100%", height: "180px", objectFit: "cover", display: "block" }} />
              <div style={{ padding: "24px", display: "flex", flexDirection: "column", gap: "10px", flexGrow: 1 }}>
                <span style={{ alignSelf: "flex-start", background: "#E4EFE2", color: "#2E5B37", fontSize: "15px", fontWeight: 600, padding: "6px 14px", borderRadius: "999px" }}>
                  Tin tức
                </span>
                <h3 style={{ margin: 0, fontSize: "20px", lineHeight: "29px", fontWeight: 600, color: "#2A2118" }}>Phòng khám tiếp nhận thêm buổi khám cuối tuần</h3>
                <Link to="/tin-tuc/lich-kham-dip-le" aria-label="Đọc thêm: Phòng khám tiếp nhận thêm buổi khám cuối tuần" style={{ marginTop: "auto", fontSize: "17px", fontWeight: 600, color: "#2E5B37", textDecoration: "underline" }}>
                  Đọc thêm
                </Link>
              </div>
            </article>
            <article className="soft-card" style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", overflow: "hidden", display: "flex", flexDirection: "column" }}>
              <img src={facade} alt="Ảnh minh hoạ bài viết (ảnh tạm)" style={{ width: "100%", height: "180px", objectFit: "cover", display: "block" }} />
              <div style={{ padding: "24px", display: "flex", flexDirection: "column", gap: "10px", flexGrow: 1 }}>
                <span style={{ alignSelf: "flex-start", background: "#F7EBD2", color: "#8A6012", fontSize: "15px", fontWeight: 600, padding: "6px 14px", borderRadius: "999px" }}>
                  Hướng dẫn
                </span>
                <h3 style={{ margin: 0, fontSize: "20px", lineHeight: "29px", fontWeight: 600, color: "#2A2118" }}>Cách đăng ký tài khoản và đặt lịch khám</h3>
                <Link to="/tin-tuc/lich-kham-dip-le" aria-label="Đọc thêm: Cách đăng ký tài khoản và đặt lịch khám" style={{ marginTop: "auto", fontSize: "17px", fontWeight: 600, color: "#2E5B37", textDecoration: "underline" }}>
                  Đọc thêm
                </Link>
              </div>
            </article>
          </div>
          <Link to="/tin-tuc" style={{ alignSelf: "flex-start", display: "inline-flex", alignItems: "center", gap: "8px", minHeight: "48px", fontSize: "19px", fontWeight: 600, color: "#2E5B37", textDecoration: "underline" }}>
            <Icon name="arrow-left" size={22} />
            Quay lại danh sách bài viết
          </Link>
        </div>
      </section>
    </PublicLayout>
  )
}
