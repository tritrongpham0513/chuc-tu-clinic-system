import { Link } from 'react-router-dom'
import Icon from '../components/Icon'
import { facade } from '../assets'
import { useAuth } from '../context/AuthContext'
import PublicLayout from '../components/PublicLayout'

export default function GioiThieu() {
  const { user } = useAuth()
  // Khách phải đăng nhập trước khi đặt lịch; người đã đăng nhập vào thẳng trang đặt lịch
  const bookTo = user ? '/dat-lich' : '/dat-lich/dang-nhap'
  return (
    <PublicLayout active="gioi-thieu">
      <section style={{ width: "100%", background: "#F4EADB", borderBottom: "1px solid #E0D3BC", padding: "40px 24px 56px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexDirection: "column", gap: "16px" }}>
          <nav aria-label="Đường dẫn" style={{ fontSize: "16px", color: "#6B5E4E" }}>
            <Link to="/" style={{ color: "#4A3F33", textDecoration: "underline" }}>Trang chủ</Link>
            {" "}
            <span style={{ padding: "0 8px" }}>›</span>
            {" "}
            <span>Giới thiệu</span>
          </nav>
          <h1 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "44px", lineHeight: "56px", fontWeight: 700, color: "#2A2118" }}>
            Về Phòng khám Chúc Từ
          </h1>
          <p style={{ margin: 0, maxWidth: "64ch", fontSize: "20px", lineHeight: "32px", color: "#4A3F33" }}>
            Phòng Chẩn Trị Y Học Cổ Truyền Từ Thiện Chúc Từ — khám bệnh, trị liệu và bốc thuốc hoàn toàn miễn phí cho người già neo đơn, người lao động nghèo và bà con có hoàn cảnh khó khăn. Phòng khám mở cửa tất cả các ngày trong tuần, kể cả Chủ Nhật, từ 8:00 đến 16:30.
          </p>
        </div>
      </section>
      <section style={{ width: "100%", padding: "80px 24px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexWrap: "wrap", gap: "56px", alignItems: "center" }}>
          <div style={{ flex: "1 1 400px", minWidth: 0 }}>
            <img src={facade} alt="Mặt tiền Phòng khám Chúc Từ với hai chữ Từ Bi và Trí Tuệ trên hai cột (ảnh tạm)" style={{ width: "100%", height: "440px", objectFit: "cover", borderRadius: "28px", border: "4px solid #C9963F", display: "block" }} />
          </div>
          <div style={{ flex: "999 1 440px", minWidth: 0, display: "flex", flexDirection: "column", gap: "20px" }}>
            <h2 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "34px", lineHeight: "44px", fontWeight: 700, color: "#2A2118" }}>
              Từ bi — Trí tuệ
            </h2>
            <div style={{ width: "72px", height: "4px", background: "#C9963F", borderRadius: "999px" }} />
            <p style={{ margin: 0, maxWidth: "62ch", fontSize: "19px", lineHeight: "31px", color: "#4A3F33" }}>
              Nằm lặng lẽ trong một con ngõ nhỏ trên đường Điện Biên Phủ, phòng khám từ lâu đã là chỗ dựa quen thuộc của bà con quanh vùng — nơi tình thương khơi nguồn, nơi nỗi đau được xoa dịu.
            </p>
            <p style={{ margin: 0, maxWidth: "62ch", fontSize: "19px", lineHeight: "31px", color: "#4A3F33" }}>
              Phòng khám khai trương ngày
              {" "}
              <strong style={{ color: "#2A2118" }}>08/11/2025</strong>
              {" "}
              (19/09 Âm lịch), do
              {" "}
              <strong style={{ color: "#2A2118" }}>Sư cô Chúc Từ</strong>
              {" "}
              sáng lập và phụ trách, với một tâm nguyện duy nhất: dùng y học cổ truyền chữa bệnh cứu người, không mưu cầu danh lợi.
            </p>
            <p style={{ margin: 0, maxWidth: "62ch", fontSize: "19px", lineHeight: "31px", color: "#4A3F33" }}>
              Hai chữ khắc trên hai cột trước cửa cũng là cách phòng khám làm việc mỗi ngày: thăm khám cẩn thận, giải thích rõ ràng bằng lời dễ hiểu, và không để ai phải ngại vì hoàn cảnh của mình.
            </p>
            <p style={{ margin: 0, maxWidth: "62ch", fontSize: "19px", lineHeight: "31px", color: "#4A3F33" }}>
              Thăm khám, trị liệu và cả thang thuốc mang về đều
              {" "}
              <strong style={{ color: "#2E5B37" }}>0 đồng</strong>
              , nhờ tấm lòng y đức của các y bác sĩ tình nguyện và sự chung tay của các Mạnh Thường Quân.
            </p>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "16px", marginTop: "4px" }}>
              <div style={{ flex: "1 1 220px", minWidth: 0, background: "#FFFCF6", border: "1px solid #E0D3BC", borderLeft: "4px solid #C9963F", borderRadius: "12px", padding: "18px 20px" }}>
                <h3 style={{ margin: "0 0 4px", fontSize: "18px", fontWeight: 700, color: "#2A2118" }}>Mái ấm Chúc Từ</h3>
                <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>
                  Cưu mang những mảnh đời cơ nhỡ, được phòng khám chăm sóc sức khoẻ thường xuyên.
                </p>
              </div>
              <div style={{ flex: "1 1 220px", minWidth: 0, background: "#FFFCF6", border: "1px solid #E0D3BC", borderLeft: "4px solid #C9963F", borderRadius: "12px", padding: "18px 20px" }}>
                <h3 style={{ margin: "0 0 4px", fontSize: "18px", fontWeight: 700, color: "#2A2118" }}>Bếp ăn tình thương</h3>
                <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>
                  Bữa cơm ấm lòng cho bà con tới khám, hoạt động song song với phòng khám.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>
      <section style={{ width: "100%", padding: "80px 24px", background: "#F4EADB" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexDirection: "column", gap: "40px" }}>
          <div>
            <h2 style={{ margin: "0 0 12px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "34px", lineHeight: "44px", fontWeight: 700, color: "#2A2118" }}>
              Phòng khám làm việc theo ba điều
            </h2>
            <div style={{ width: "72px", height: "4px", background: "#C9963F", borderRadius: "999px" }} />
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(3, minmax(0, 1fr))", gap: "28px" }}>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "32px", display: "flex", flexDirection: "column", gap: "14px" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "64px", height: "64px", background: "#F7EBD2", borderRadius: "999px" }}>
                <Icon name="heart" size={32} stroke="#8A6012" />
              </span>
              <h3 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "23px", lineHeight: "32px", fontWeight: 600, color: "#2A2118" }}>
                Không thu phí
              </h3>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Khám, châm cứu và trị liệu đều miễn phí. Bà con không phải chuẩn bị tiền trước khi đến.
              </p>
            </div>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "32px", display: "flex", flexDirection: "column", gap: "14px" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "64px", height: "64px", background: "#F7EBD2", borderRadius: "999px" }}>
                <Icon name="check-circle" size={32} stroke="#8A6012" />
              </span>
              <h3 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "23px", lineHeight: "32px", fontWeight: 600, color: "#2A2118" }}>
                Nói cho dễ hiểu
              </h3>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Bác sĩ giải thích bệnh và cách điều trị bằng lời thường ngày, hỏi lại đến khi cô chú thật sự hiểu.
              </p>
            </div>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "32px", display: "flex", flexDirection: "column", gap: "14px" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "64px", height: "64px", background: "#F7EBD2", borderRadius: "999px" }}>
                <Icon name="users" size={32} stroke="#8A6012" />
              </span>
              <h3 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "23px", lineHeight: "32px", fontWeight: 600, color: "#2A2118" }}>
                Ưu tiên bà con khó khăn
              </h3>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Nhân viên tiếp đón, hướng dẫn và sẵn lòng ghi hộ đơn đăng ký qua điện thoại cho người cao tuổi.
              </p>
            </div>
          </div>
        </div>
      </section>
      <section style={{ width: "100%", padding: "80px 24px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexDirection: "column", gap: "40px" }}>
          <div>
            <h2 style={{ margin: "0 0 12px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "34px", lineHeight: "44px", fontWeight: 700, color: "#2A2118" }}>
              Đội ngũ phòng khám
            </h2>
            <div style={{ width: "72px", height: "4px", background: "#C9963F", borderRadius: "999px", marginBottom: "16px" }} />
            <p style={{ margin: 0, maxWidth: "64ch", fontSize: "19px", lineHeight: "31px", color: "#4A3F33" }}>
              Bác sĩ và nhân viên làm việc tại phòng khám theo lịch cố định trong tuần.
            </p>
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(3, minmax(0, 1fr))", gap: "28px" }}>
            <article style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", overflow: "hidden" }}>
              <img src={facade} alt="Ảnh chân dung (ảnh tạm, sẽ thay bằng ảnh thành viên)" style={{ width: "100%", height: "240px", objectFit: "cover", display: "block" }} />
              <div style={{ padding: "24px", display: "flex", flexDirection: "column", gap: "8px" }}>
                <h3 style={{ margin: 0, fontSize: "21px", lineHeight: "30px", fontWeight: 600, color: "#2A2118" }}>Trần Long</h3>
                <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#6B5E4E" }}>Châm cứu, vật lý trị liệu và hỗ trợ bốc thuốc</p>
              </div>
            </article>
            <article style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", overflow: "hidden" }}>
              <img src={facade} alt="Ảnh chân dung (ảnh tạm, sẽ thay bằng ảnh thành viên)" style={{ width: "100%", height: "240px", objectFit: "cover", display: "block" }} />
              <div style={{ padding: "24px", display: "flex", flexDirection: "column", gap: "8px" }}>
                <h3 style={{ margin: 0, fontSize: "21px", lineHeight: "30px", fontWeight: 600, color: "#2A2118" }}>Nguyễn Văn Hậu</h3>
                <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#6B5E4E" }}>Bắt mạch, kê đơn và bốc thuốc Nam – thuốc Bắc</p>
              </div>
            </article>
            <article style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", overflow: "hidden" }}>
              <img src={facade} alt="Ảnh chân dung (ảnh tạm, sẽ thay bằng ảnh thành viên)" style={{ width: "100%", height: "240px", objectFit: "cover", display: "block" }} />
              <div style={{ padding: "24px", display: "flex", flexDirection: "column", gap: "8px" }}>
                <h3 style={{ margin: 0, fontSize: "21px", lineHeight: "30px", fontWeight: 600, color: "#2A2118" }}>Trần Thị Mai</h3>
                <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#6B5E4E" }}>Nhân viên tiếp đón · Phát số, ghi hộ đơn đăng ký</p>
              </div>
            </article>
          </div>
        </div>
      </section>
      <section style={{ width: "100%", padding: "72px 24px", background: "#3B7145" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexWrap: "wrap", gap: "32px", alignItems: "center", justifyContent: "space-between" }}>
          <h2 style={{ margin: 0, flex: "1 1 420px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "32px", lineHeight: "42px", fontWeight: 700, color: "#FFFFFF" }}>
            Cô chú đã sẵn sàng đặt lịch khám?
          </h2>
          <Link to={bookTo} style={{ display: "inline-flex", alignItems: "center", gap: "10px", minHeight: "60px", padding: "0 32px", background: "#FFFCF6", borderRadius: "12px", fontSize: "20px", fontWeight: 600, color: "#2E5B37", textDecoration: "none" }}>
            <Icon name="calendar-plus" />
            Đặt lịch khám
          </Link>
        </div>
      </section>
    </PublicLayout>
  )
}
