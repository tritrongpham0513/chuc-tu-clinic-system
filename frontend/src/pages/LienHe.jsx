import { Link } from 'react-router-dom'
import Icon from '../components/Icon'
import { facade } from '../assets'
import { useAuth } from '../context/AuthContext'
import PublicLayout from '../components/PublicLayout'

export default function LienHe() {
  const { user } = useAuth()
  // Khách phải đăng nhập trước khi đặt lịch; người đã đăng nhập vào thẳng trang đặt lịch
  const bookTo = user ? '/dat-lich' : '/dat-lich/dang-nhap'
  return (
    <PublicLayout active="lien-he">
      <section style={{ width: "100%", background: "#F4EADB", borderBottom: "1px solid #E0D3BC", padding: "40px 24px 56px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexWrap: "wrap", gap: "48px", alignItems: "center" }}>
          <div style={{ flex: "999 1 460px", minWidth: 0, display: "flex", flexDirection: "column", gap: "16px" }}>
            <nav aria-label="Đường dẫn" style={{ fontSize: "16px", color: "#6B5E4E" }}>
              <Link to="/" style={{ color: "#4A3F33", textDecoration: "underline" }}>Trang chủ</Link>
              {" "}
              <span style={{ padding: "0 8px" }}>›</span>
              {" "}
              <span>Liên hệ</span>
            </nav>
            <h1 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "44px", lineHeight: "56px", fontWeight: 700, color: "#2A2118" }}>
              Liên hệ phòng khám
            </h1>
            <p style={{ margin: 0, maxWidth: "56ch", fontSize: "20px", lineHeight: "32px", color: "#4A3F33" }}>
              Gọi điện là cách nhanh nhất — có người trực nghe máy trong giờ làm việc. Cô chú cũng có thể tới thẳng phòng khám, không cần hẹn trước.
            </p>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "12px", marginTop: "4px" }}>
              <span style={{ display: "inline-flex", alignItems: "center", gap: "8px", background: "#E4EFE2", color: "#2E5B37", fontSize: "16px", fontWeight: 600, padding: "8px 16px", borderRadius: "999px" }}>
                <Icon name="clock" size={18} strokeWidth={2} />
                Mở cửa tất cả các ngày, kể cả Chủ Nhật
              </span>
              <span style={{ display: "inline-flex", alignItems: "center", gap: "8px", background: "#F7EBD2", color: "#8A6012", fontSize: "16px", fontWeight: 600, padding: "8px 16px", borderRadius: "999px" }}>
                <Icon name="heart" size={18} strokeWidth={2} />
                Khám và bốc thuốc không thu phí
              </span>
            </div>
          </div>
          <div style={{ flex: "1 1 380px", minWidth: 0 }}>
            <img src={facade} alt="Mặt tiền Phòng khám Chúc Từ, có hai chữ Từ Bi và Trí Tuệ trên hai cột (ảnh tạm)" style={{ width: "100%", height: "320px", objectFit: "cover", borderRadius: "28px", border: "4px solid #C9963F", display: "block" }} />
          </div>
        </div>
      </section>
      <section style={{ width: "100%", padding: "72px 24px 0" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexDirection: "column", gap: "32px" }}>
          <div>
            <h2 style={{ margin: "0 0 12px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "34px", lineHeight: "44px", fontWeight: 700, color: "#2A2118" }}>
              Cô chú liên hệ bằng cách nào cũng được
            </h2>
            <div style={{ width: "72px", height: "4px", background: "#C9963F", borderRadius: "999px" }} />
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(2, minmax(0, 1fr))", gap: "28px", alignItems: "stretch" }}>
            {/* Gọi điện */}
            <div style={{ background: "#F7EBD2", borderRadius: "28px", padding: "36px", display: "flex", flexDirection: "column", gap: "20px" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "64px", height: "64px", background: "#FFFCF6", borderRadius: "999px" }}>
                <Icon name="phone" size={32} stroke="#8A6012" />
              </span>
              <h3 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "28px", lineHeight: "38px", fontWeight: 700, color: "#2A2118" }}>
                Gọi cho phòng khám
              </h3>
              <p style={{ margin: 0, fontSize: "19px", lineHeight: "31px", color: "#4A3F33" }}>
                Hỏi gì cũng được: hôm nay còn nhận số không, đường đi thế nào, hay nhờ đặt lịch giúp.
              </p>
              <a className="btn-primary" href="tel:0397116666" style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "12px", minHeight: "72px", padding: "0 28px", background: "#3B7145", borderRadius: "16px", fontSize: "26px", fontWeight: 700, color: "#FFFFFF", textDecoration: "none" }}>
                <Icon name="phone" size={30} />
                0397 116 666
              </a>
              <div style={{ marginTop: "auto", background: "#FFFCF6", borderRadius: "16px", padding: "18px 20px" }}>
                <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>Máy bận thì cô chú gọi số thứ hai:</p>
                <a href="tel:0835555115" style={{ display: "inline-block", marginTop: "4px", fontSize: "22px", fontWeight: 700, color: "#2E5B37", textDecoration: "underline" }}>
                  0835 555 115
                </a>
                <p style={{ margin: "2px 0 0", fontSize: "16px", lineHeight: "25px", color: "#6B5E4E" }}>Sư cô Chúc Từ</p>
              </div>
            </div>
            {/* Đến trực tiếp */}
            <div style={{ background: "#E4EFE2", borderRadius: "28px", padding: "36px", display: "flex", flexDirection: "column", gap: "20px" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "64px", height: "64px", background: "#FFFCF6", borderRadius: "999px" }}>
                <Icon name="map-pin" size={32} stroke="#2E5B37" />
              </span>
              <h3 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "28px", lineHeight: "38px", fontWeight: 700, color: "#2A2118" }}>
                Đến thẳng phòng khám
              </h3>
              <p style={{ margin: 0, fontSize: "19px", lineHeight: "31px", color: "#4A3F33" }}>
                Không có hẹn trước cô chú vẫn được tiếp đón. Nhân viên sẽ sắp xếp lượt khám cho cô chú.
              </p>
              <p style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "25px", lineHeight: "38px", fontWeight: 700, color: "#2A2118" }}>
                57/8-10 Điện Biên Phủ, Phường 25, Quận Bình Thạnh, TP. Hồ Chí Minh
              </p>
              <a className="btn-ghost" href="#tim-duong" style={{ marginTop: "auto", display: "inline-flex", alignItems: "center", justifyContent: "center", gap: "10px", minHeight: "56px", padding: "0 24px", background: "#FFFCF6", border: "2px solid #2E5B37", borderRadius: "12px", fontSize: "19px", fontWeight: 600, color: "#2E5B37", textDecoration: "none" }}>
                <Icon name="send" />
                Xem hướng dẫn đường đi
              </a>
            </div>
          </div>
        </div>
      </section>
      <section style={{ width: "100%", padding: "40px 24px 0" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "grid", gridTemplateColumns: "repeat(3, minmax(0, 1fr))", gap: "20px" }}>
          <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "26px 28px" }}>
            <p style={{ margin: "0 0 6px", fontSize: "16px", fontWeight: 700, letterSpacing: "0.04em", textTransform: "uppercase", color: "#6B5E4E" }}>Giờ khám</p>
            <p style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "30px", lineHeight: "40px", fontWeight: 700, color: "#2A2118" }}>
              8:00 – 16:30
            </p>
            <p style={{ margin: "4px 0 0", fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>Buổi sáng rất đông, cô chú nên đi sớm.</p>
          </div>
          <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "26px 28px" }}>
            <p style={{ margin: "0 0 6px", fontSize: "16px", fontWeight: 700, letterSpacing: "0.04em", textTransform: "uppercase", color: "#6B5E4E" }}>
              Ngày làm việc
            </p>
            <p style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "30px", lineHeight: "40px", fontWeight: 700, color: "#2A2118" }}>
              Cả 7 ngày
            </p>
            <p style={{ margin: "4px 0 0", fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>Thứ Hai đến Chủ Nhật, không nghỉ cuối tuần.</p>
          </div>
          <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "26px 28px" }}>
            <p style={{ margin: "0 0 6px", fontSize: "16px", fontWeight: 700, letterSpacing: "0.04em", textTransform: "uppercase", color: "#6B5E4E" }}>Chi phí</p>
            <p style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "30px", lineHeight: "40px", fontWeight: 700, color: "#2E5B37" }}>0 đồng</p>
            <p style={{ margin: "4px 0 0", fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>Khám, trị liệu và thuốc mang về đều miễn phí.</p>
          </div>
        </div>
      </section>
      <section id="tim-duong" style={{ width: "100%", padding: "72px 24px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexDirection: "column", gap: "32px" }}>
          <div>
            <h2 style={{ margin: "0 0 12px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "34px", lineHeight: "44px", fontWeight: 700, color: "#2A2118" }}>
              Tìm đường tới phòng khám
            </h2>
            <div style={{ width: "72px", height: "4px", background: "#C9963F", borderRadius: "999px" }} />
          </div>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "28px", alignItems: "stretch" }}>
            {/* Khung bản đồ */}
            <div style={{ flex: "999 1 560px", minWidth: 0, background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "28px", overflow: "hidden", display: "flex", flexDirection: "column" }}>
              <div style={{ flexGrow: 1, minHeight: "360px", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: "14px", padding: "40px 32px", textAlign: "center", background: "repeating-linear-gradient(0deg, #F4EADB, #F4EADB 39px, #E0D3BC 39px, #E0D3BC 40px), repeating-linear-gradient(90deg, rgba(0,0,0,0), rgba(0,0,0,0) 39px, #E0D3BC 39px, #E0D3BC 40px)" }}>
                <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "72px", height: "72px", background: "#3B7145", borderRadius: "999px", boxShadow: "0 10px 24px rgba(42, 33, 24, 0.22)" }}>
                  <Icon name="map-pin" size={38} stroke="#FFFFFF" />
                </span>
                <p style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "23px", lineHeight: "32px", fontWeight: 700, color: "#2A2118" }}>
                  Phòng khám Chúc Từ
                </p>
                <p style={{ margin: 0, maxWidth: "40ch", fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>
                  57/8-10 Điện Biên Phủ, Phường 25, Quận Bình Thạnh, TP. Hồ Chí Minh
                </p>
                <p style={{ margin: 0, fontSize: "15px", color: "#6B5E4E" }}>Khu vực nhúng bản đồ — sẽ thay bằng bản đồ thật khi lập trình</p>
              </div>
              <div style={{ padding: "20px 28px", borderTop: "1px solid #E0D3BC", display: "flex", flexWrap: "wrap", gap: "16px", alignItems: "center", justifyContent: "space-between" }}>
                <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>Cô chú đưa địa chỉ này cho tài xế xe ôm hoặc taxi là tới nơi.</p>
                <a className="btn-ghost" href="#tim-duong" style={{ display: "inline-flex", alignItems: "center", gap: "8px", minHeight: "48px", padding: "0 20px", background: "#FFFCF6", border: "2px solid #8C7B5E", borderRadius: "12px", fontSize: "17px", fontWeight: 600, color: "#2E5B37", textDecoration: "none", whiteSpace: "nowrap" }}>
                  Mở bản đồ
                  <Icon name="arrow-right" size={20} />
                </a>
              </div>
            </div>
            {/* Mốc nhận đường */}
            <div style={{ flex: "1 1 320px", minWidth: 0, background: "#F4EADB", borderRadius: "28px", padding: "32px", display: "flex", flexDirection: "column", gap: "20px" }}>
              <h3 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "24px", lineHeight: "34px", fontWeight: 700, color: "#2A2118" }}>
                Mốc nhận đường
              </h3>
              <ol style={{ listStyle: "none", margin: 0, padding: 0, display: "flex", flexDirection: "column", gap: "18px" }}>
                <li style={{ display: "flex", gap: "14px", alignItems: "flex-start" }}>
                  <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "38px", height: "38px", background: "#3B7145", color: "#FFFFFF", borderRadius: "999px", fontSize: "18px", fontWeight: 700, flexShrink: 0 }}>
                    1
                  </span>
                  <p style={{ margin: 0, paddingTop: "5px", fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                    Đi dọc đường
                    {" "}
                    <strong style={{ color: "rgb(42, 33, 24)" }}>Điện Biên Phủ</strong>
                  </p>
                </li>
                <li style={{ display: "flex", gap: "14px", alignItems: "flex-start" }}>
                  <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "38px", height: "38px", background: "#3B7145", color: "#FFFFFF", borderRadius: "999px", fontSize: "18px", fontWeight: 700, flexShrink: 0 }}>
                    2
                  </span>
                  <p style={{ margin: 0, paddingTop: "5px", fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                    Rẽ vào
                    {" "}
                    <strong style={{ color: "#2A2118" }}>hẻm 57</strong>
                    , đi thêm một đoạn ngắn.
                  </p>
                </li>
                <li style={{ display: "flex", gap: "14px", alignItems: "flex-start" }}>
                  <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "38px", height: "38px", background: "#3B7145", color: "#FFFFFF", borderRadius: "999px", fontSize: "18px", fontWeight: 700, flexShrink: 0 }}>
                    3
                  </span>
                  <p style={{ margin: 0, paddingTop: "5px", fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                    Phòng khám ở số
                    {" "}
                    <strong style={{ color: "#2A2118" }}>8-10</strong>
                    , bên phải lối vào.
                  </p>
                </li>
              </ol>
              <div style={{ marginTop: "auto", background: "#FFFCF6", borderLeft: "4px solid #C9963F", borderRadius: "12px", padding: "16px 18px" }}>
                <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>
                  Lạc đường thì cô chú cứ gọi
                  {" "}
                  <a href="tel:0397116666" style={{ color: "#2E5B37", fontWeight: 700, textDecoration: "underline" }}>0397 116 666</a>
                  , phòng khám chỉ đường cho.
                </p>
              </div>
            </div>
          </div>
          {/* Tới bằng phương tiện gì */}
          <div style={{ display: "grid", gridTemplateColumns: "repeat(3, minmax(0, 1fr))", gap: "20px" }}>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "26px", display: "flex", gap: "16px", alignItems: "flex-start" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "52px", height: "52px", background: "#E4EFE2", borderRadius: "999px", flexShrink: 0 }}>
                <Icon name="bike" size={26} stroke="#2E5B37" />
              </span>
              <div style={{ minWidth: 0 }}>
                <h3 style={{ margin: "0 0 6px", fontSize: "19px", fontWeight: 600, color: "#2A2118" }}>Đi xe máy</h3>
                <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>
                  Gửi xe ngay trong sân phòng khám, nhân viên trông giúp và không thu phí.
                </p>
              </div>
            </div>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "26px", display: "flex", gap: "16px", alignItems: "flex-start" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "52px", height: "52px", background: "#E4EFE2", borderRadius: "999px", flexShrink: 0 }}>
                <Icon name="bus" size={26} stroke="#2E5B37" />
              </span>
              <div style={{ minWidth: 0 }}>
                <h3 style={{ margin: "0 0 6px", fontSize: "19px", fontWeight: 600, color: "#2A2118" }}>Đi xe buýt</h3>
                <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>
                  Các tuyến chạy dọc Điện Biên Phủ, xuống trạm gần số 57 rồi đi bộ chừng 3 phút vào hẻm.
                </p>
              </div>
            </div>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "26px", display: "flex", gap: "16px", alignItems: "flex-start" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "52px", height: "52px", background: "#E4EFE2", borderRadius: "999px", flexShrink: 0 }}>
                <Icon name="car" size={26} stroke="#2E5B37" />
              </span>
              <div style={{ minWidth: 0 }}>
                <h3 style={{ margin: "0 0 6px", fontSize: "19px", fontWeight: 600, color: "#2A2118" }}>Đi taxi hoặc xe công nghệ</h3>
                <p style={{ margin: 0, fontSize: "17px", lineHeight: "27px", color: "#4A3F33" }}>
                  Đọc cho tài xế: “Phòng khám Chúc Từ, 57/8-10 Điện Biên Phủ, Phường 25, Quận Bình Thạnh, TP. Hồ Chí Minh”.
                </p>
              </div>
            </div>
          </div>
          {/* Nhận biết phòng khám */}
          <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderLeft: "6px solid #C9963F", borderRadius: "28px", padding: "32px", display: "flex", flexWrap: "wrap", gap: "32px" }}>
            <div style={{ flex: "1 1 380px", minWidth: 0 }}>
              <h3 style={{ margin: "0 0 10px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "23px", lineHeight: "32px", fontWeight: 600, color: "#2A2118" }}>
                Nhận biết phòng khám
              </h3>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Cổng màu vàng, mái ngói đỏ, hai cột trước cửa khắc chữ “Từ Bi” và “Trí Tuệ”. Bảng hiệu ghi “Phòng Chẩn Trị Y Học Cổ Truyền Từ Thiện Chúc Từ”.
              </p>
            </div>
            <div style={{ flex: "1 1 380px", minWidth: 0 }}>
              <h3 style={{ margin: "0 0 10px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "23px", lineHeight: "32px", fontWeight: 600, color: "#2A2118" }}>
                Tới nơi thì làm gì
              </h3>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Nhân viên tiếp đón, ghi thông tin và xếp lượt khám. Cô chú đợi tới lượt gặp bác sĩ xem mạch, qua khu trị liệu nếu có chỉ định, cuối cùng nhận thuốc rồi ra về.
              </p>
            </div>
          </div>
        </div>
      </section>
      <section style={{ width: "100%", padding: "0 24px 72px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexDirection: "column", gap: "28px" }}>
          <h2 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "30px", lineHeight: "40px", fontWeight: 700, color: "#2A2118" }}>
            Mấy điều bà con hay hỏi
          </h2>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(3, minmax(0, 1fr))", gap: "24px" }}>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "28px" }}>
              <h3 style={{ margin: "0 0 10px", fontSize: "20px", lineHeight: "30px", fontWeight: 700, color: "#2A2118" }}>Gọi lúc nào thì có người nghe?</h3>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Trong giờ làm việc 8:00 – 16:30, tất cả các ngày. Ngoài giờ cô chú để lại tin nhắn, phòng khám gọi lại sau.
              </p>
            </div>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "28px" }}>
              <h3 style={{ margin: "0 0 10px", fontSize: "20px", lineHeight: "30px", fontWeight: 700, color: "#2A2118" }}>Không rành điện thoại thì sao?</h3>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Cô chú nhờ con cháu đăng ký giúp, hoặc gọi điện để nhân viên đặt lịch hộ.
              </p>
            </div>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "28px" }}>
              <h3 style={{ margin: "0 0 10px", fontSize: "20px", lineHeight: "30px", fontWeight: 700, color: "#2A2118" }}>Có phải trả khoản nào không?</h3>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Không. Thăm khám, châm cứu, trị liệu và thuốc mang về đều miễn phí hoàn toàn.
              </p>
            </div>
          </div>
        </div>
      </section>
      <section style={{ width: "100%", padding: "72px 24px", background: "#3B7145" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexWrap: "wrap", gap: "32px", alignItems: "center", justifyContent: "space-between" }}>
          <div style={{ flex: "1 1 420px", minWidth: 0 }}>
            <h2 style={{ margin: "0 0 12px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "32px", lineHeight: "42px", fontWeight: 700, color: "#FFFFFF" }}>
              Muốn giữ chỗ trước cho đỡ phải chờ?
            </h2>
            <p style={{ margin: 0, maxWidth: "56ch", fontSize: "19px", lineHeight: "31px", color: "#FFFFFF" }}>
              {user ? 'Cô chú đặt lịch là có ngay phiếu khám kèm giờ khám dự kiến.' : 'Cô chú đăng nhập rồi đặt lịch, có ngay phiếu khám kèm giờ khám dự kiến.'}
            </p>
          </div>
          <Link to={bookTo} style={{ display: "inline-flex", alignItems: "center", gap: "10px", minHeight: "60px", padding: "0 32px", background: "#FFFCF6", borderRadius: "12px", fontSize: "20px", fontWeight: 600, color: "#2E5B37", textDecoration: "none" }}>
            <Icon name="calendar-plus" />
            Đặt lịch khám
          </Link>
        </div>
      </section>
    </PublicLayout>
  )
}
