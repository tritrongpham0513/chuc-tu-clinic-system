import { Link } from 'react-router-dom'
import Icon from '../components/Icon'
import { useAuth } from '../context/AuthContext'
import PublicLayout from '../components/PublicLayout'

export default function HuongDan() {
  const { user } = useAuth()
  // Khách phải đăng nhập trước khi đặt lịch; người đã đăng nhập vào thẳng trang đặt lịch
  const bookTo = user ? '/dat-lich' : '/dat-lich/dang-nhap'
  return (
    <PublicLayout active="huong-dan">
      <section style={{ width: "100%", background: "#F4EADB", borderBottom: "1px solid #E0D3BC", padding: "40px 24px 56px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexDirection: "column", gap: "16px" }}>
          <nav aria-label="Đường dẫn" style={{ fontSize: "16px", color: "#6B5E4E" }}>
            <Link to="/" style={{ color: "#4A3F33", textDecoration: "underline" }}>Trang chủ</Link>
            {" "}
            <span style={{ padding: "0 8px" }}>›</span>
            {" "}
            <span>Hướng dẫn</span>
          </nav>
          <h1 style={{ margin: 0, fontFamily: "'Noto Serif', Georgia, serif", fontSize: "44px", lineHeight: "56px", fontWeight: 700, color: "#2A2118" }}>
            Hướng dẫn đi khám
          </h1>
          <p style={{ margin: 0, maxWidth: "64ch", fontSize: "20px", lineHeight: "32px", color: "#4A3F33" }}>
            Cô chú có thể đăng ký trước trên mạng cho đỡ phải chờ, hoặc cứ tới thẳng phòng khám trong giờ làm việc. Cách nào cũng được.
          </p>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "12px", marginTop: "8px" }}>
            <a href="#dang-ky-truc-tuyen" style={{ display: "inline-flex", alignItems: "center", minHeight: "48px", padding: "0 18px", background: "#FFFCF6", border: "2px solid #8C7B5E", borderRadius: "999px", fontSize: "17px", fontWeight: 600, color: "#2E5B37", textDecoration: "underline" }}>
              Đăng ký khám trực tuyến
            </a>
            <a href="#di-kham-truc-tiep" style={{ display: "inline-flex", alignItems: "center", minHeight: "48px", padding: "0 18px", background: "#FFFCF6", border: "2px solid #8C7B5E", borderRadius: "999px", fontSize: "17px", fontWeight: 600, color: "#2E5B37", textDecoration: "underline" }}>
              Quy trình đi khám trực tiếp
            </a>
            <a href="#luu-y" style={{ display: "inline-flex", alignItems: "center", minHeight: "48px", padding: "0 18px", background: "#FFFCF6", border: "2px solid #8C7B5E", borderRadius: "999px", fontSize: "17px", fontWeight: 600, color: "#2E5B37", textDecoration: "underline" }}>
              Lưu ý khi đi khám
            </a>
          </div>
        </div>
      </section>
      <section id="dang-ky-truc-tuyen" style={{ width: "100%", padding: "72px 24px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexDirection: "column", gap: "32px" }}>
          <div>
            <p style={{ margin: "0 0 8px", fontSize: "16px", fontWeight: 700, letterSpacing: "0.06em", textTransform: "uppercase", color: "#8A6012" }}>
              Cách thứ nhất
            </p>
            <h2 style={{ margin: "0 0 12px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "34px", lineHeight: "44px", fontWeight: 700, color: "#2A2118" }}>
              Hướng dẫn đăng ký khám trực tuyến
            </h2>
            <div style={{ width: "72px", height: "4px", background: "#C9963F", borderRadius: "999px", marginBottom: "16px" }} />
            <p style={{ margin: 0, maxWidth: "68ch", fontSize: "19px", lineHeight: "31px", color: "#4A3F33" }}>
              Đăng ký trước trên trang web để giữ chỗ. Đặt lịch xong là cô chú
              {" "}
              <strong style={{ color: "#2A2118" }}>có phiếu khám ngay</strong>
              {" "}
              — không cần chờ phòng khám xác nhận.
            </p>
          </div>
          <ol style={{ listStyle: "none", margin: 0, padding: 0, display: "flex", flexDirection: "column", gap: "16px" }}>
            <li style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "26px 28px", display: "flex", flexWrap: "wrap", gap: "22px", alignItems: "flex-start" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "52px", height: "52px", background: "#3B7145", color: "#FFFFFF", borderRadius: "999px", fontSize: "22px", fontWeight: 700, flexShrink: 0 }}>
                1
              </span>
              <div style={{ flex: "999 1 420px", minWidth: 0 }}>
                <h3 style={{ margin: "0 0 6px", fontSize: "22px", lineHeight: "31px", fontWeight: 600, color: "#2A2118" }}>Tạo tài khoản — chỉ làm một lần</h3>
                <p style={{ margin: 0, maxWidth: "66ch", fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                  Bấm
                  {" "}
                  <strong style={{ color: "#2A2118" }}>Đăng nhập</strong>
                  {" "}
                  ở góc trên bên phải, rồi chọn
                  {" "}
                  <strong style={{ color: "#2A2118" }}>Tạo tài khoản</strong>
                  . Cô chú nhập họ tên, ngày sinh, địa chỉ và số điện thoại, rồi nhập mã 6 số mà phòng khám gửi về tin nhắn. Không cần đặt hay nhớ mật khẩu.
                </p>
              </div>
              {user ? (
                <span style={{ display: "inline-flex", alignItems: "center", minHeight: "48px", padding: "0 18px", background: "#E4EFE2", borderRadius: "12px", fontSize: "17px", fontWeight: 600, color: "#2E5B37", whiteSpace: "nowrap" }}> Cô chú đã làm xong bước này </span>
              ) : (
                <Link to="/dang-ky" style={{ display: "inline-flex", alignItems: "center", minHeight: "48px", padding: "0 18px", border: "2px solid #8C7B5E", borderRadius: "12px", fontSize: "17px", fontWeight: 600, color: "#2E5B37", textDecoration: "none", whiteSpace: "nowrap" }}>
                Mở trang tạo tài khoản
              </Link>
              )}
            </li>
            <li style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "26px 28px", display: "flex", flexWrap: "wrap", gap: "22px", alignItems: "flex-start" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "52px", height: "52px", background: "#3B7145", color: "#FFFFFF", borderRadius: "999px", fontSize: "22px", fontWeight: 700, flexShrink: 0 }}>
                2
              </span>
              <div style={{ flex: "999 1 420px", minWidth: 0 }}>
                <h3 style={{ margin: "0 0 6px", fontSize: "22px", lineHeight: "31px", fontWeight: 600, color: "#2A2118" }}>Đăng nhập vào tài khoản</h3>
                <p style={{ margin: 0, maxWidth: "66ch", fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                  Phải đăng nhập mới đặt được lịch. Cô chú nhập số điện thoại, chờ tin nhắn rồi nhập mã 6 số. Chọn giữ đăng nhập thì lần sau không cần nhập mã. Không nhận được tin nhắn, đổi số hoặc mất SIM thì cô chú gọi phòng khám, nhân viên sẽ xác minh và giúp.
                </p>
              </div>
            </li>
            <li style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "26px 28px", display: "flex", flexWrap: "wrap", gap: "22px", alignItems: "flex-start" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "52px", height: "52px", background: "#3B7145", color: "#FFFFFF", borderRadius: "999px", fontSize: "22px", fontWeight: 700, flexShrink: 0 }}>
                3
              </span>
              <div style={{ flex: "999 1 420px", minWidth: 0 }}>
                <h3 style={{ margin: "0 0 6px", fontSize: "22px", lineHeight: "31px", fontWeight: 600, color: "#2A2118" }}>Đặt lịch khám</h3>
                <p style={{ margin: 0, maxWidth: "66ch", fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                  Bấm
                  {" "}
                  <strong style={{ color: "#2A2118" }}>Đặt lịch khám</strong>
                  , chọn ngày giờ mong muốn (từ hôm nay trở đi) và kể ngắn gọn cô chú đang bị gì. Xong thì bấm
                  {" "}
                  <strong style={{ color: "#2A2118" }}>Đặt lịch khám</strong>
                  {" "}
                  ở bước cuối.
                </p>
              </div>
              <Link to={bookTo} className="btn-primary" style={{ display: "inline-flex", alignItems: "center", minHeight: "48px", padding: "0 18px", background: "#3B7145", borderRadius: "12px", fontSize: "17px", fontWeight: 600, color: "#FFFFFF", textDecoration: "none", whiteSpace: "nowrap" }}>
                {user ? 'Mở trang đặt lịch' : 'Đăng nhập để đặt lịch'}
              </Link>
            </li>
            <li style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "26px 28px", display: "flex", flexWrap: "wrap", gap: "22px", alignItems: "flex-start" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "52px", height: "52px", background: "#3B7145", color: "#FFFFFF", borderRadius: "999px", fontSize: "22px", fontWeight: 700, flexShrink: 0 }}>
                4
              </span>
              <div style={{ flex: "999 1 420px", minWidth: 0 }}>
                <h3 style={{ margin: "0 0 10px", fontSize: "22px", lineHeight: "31px", fontWeight: 600, color: "#2A2118" }}>Nhận phiếu khám ngay</h3>
                <p style={{ margin: "0 0 14px", maxWidth: "66ch", fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                  Bấm đặt lịch xong, trang chuyển sang
                  {" "}
                  <strong style={{ color: "#2A2118" }}>phiếu khám</strong>
                  {" "}
                  có mã phiếu khám và giờ khám dự kiến. Không cần chờ phòng khám xác nhận:
                </p>
                <div style={{ display: "flex", flexWrap: "wrap", gap: "12px" }}>
                  <span style={{ display: "inline-flex", alignItems: "center", gap: "8px", background: "#E4EFE2", color: "#2E5B37", fontSize: "17px", fontWeight: 600, padding: "8px 16px", borderRadius: "999px" }}>
                    <Icon name="check" size={18} strokeWidth={2.25} />
                    Đã đặt lịch — cô chú có phiếu khám
                  </span>
                </div>
              </div>
            </li>
            <li style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "26px 28px", display: "flex", flexWrap: "wrap", gap: "22px", alignItems: "flex-start" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "52px", height: "52px", background: "#3B7145", color: "#FFFFFF", borderRadius: "999px", fontSize: "22px", fontWeight: 700, flexShrink: 0 }}>
                5
              </span>
              <div style={{ flex: "999 1 420px", minWidth: 0 }}>
                <h3 style={{ margin: "0 0 6px", fontSize: "22px", lineHeight: "31px", fontWeight: 600, color: "#2A2118" }}>Đến phòng khám đưa phiếu khám</h3>
                <p style={{ margin: 0, maxWidth: "66ch", fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                  Cô chú xem lại phiếu khám trong mục
                  {" "}
                  <strong style={{ color: "#2A2118" }}>Phiếu khám bệnh</strong>
                  . Hôm đi khám, đưa phiếu khám cho tiếp tân check-in là xong.
                </p>
              </div>
              <Link to={user ? '/phieu-kham' : '/dang-nhap'} style={{ display: "inline-flex", alignItems: "center", minHeight: "48px", padding: "0 18px", border: "2px solid #8C7B5E", borderRadius: "12px", fontSize: "17px", fontWeight: 600, color: "#2E5B37", textDecoration: "none", whiteSpace: "nowrap" }}>
                {user ? 'Xem phiếu khám bệnh' : 'Đăng nhập để xem phiếu khám bệnh'}
              </Link>
            </li>
          </ol>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "20px" }}>
            <div style={{ flex: "1 1 420px", minWidth: 0, background: "#F7EBD2", borderRadius: "20px", padding: "24px", display: "flex", gap: "16px", alignItems: "flex-start" }}>
              <Icon name="clock" size={28} stroke="#8A6012" style={{ flexShrink: 0 }} />
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                <strong style={{ color: "#2A2118" }}>Muốn huỷ lịch?</strong>
                {" "}
                Cô chú mở phiếu khám trong mục
                {" "}
                <strong style={{ color: "#2A2118" }}>Phiếu khám bệnh</strong>
                {" "}
                rồi bấm
                {" "}
                <strong style={{ color: "#2A2118" }}>Huỷ khám</strong>
                .
              </p>
            </div>
            <div style={{ flex: "1 1 420px", minWidth: 0, background: "#E4EFE2", borderRadius: "20px", padding: "24px", display: "flex", gap: "16px", alignItems: "flex-start" }}>
              <Icon name="phone" size={28} stroke="#2E5B37" style={{ flexShrink: 0 }} />
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                <strong style={{ color: "#2A2118" }}>Không rành điện thoại?</strong>
                {" "}
                Cô chú nhờ con cháu đăng ký giúp, hoặc gọi
                {" "}
                <a href="tel:0397116666" style={{ color: "#2E5B37", fontWeight: 700, textDecoration: "underline" }}>0397 116 666</a>
                {" "}
                để nhân viên ghi hộ yêu cầu.
              </p>
            </div>
          </div>
        </div>
      </section>
      <section id="di-kham-truc-tiep" style={{ width: "100%", padding: "72px 24px", background: "#F4EADB" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexDirection: "column", gap: "32px" }}>
          <div>
            <p style={{ margin: "0 0 8px", fontSize: "16px", fontWeight: 700, letterSpacing: "0.06em", textTransform: "uppercase", color: "#8A6012" }}>
              Cách thứ hai
            </p>
            <h2 style={{ margin: "0 0 12px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "34px", lineHeight: "44px", fontWeight: 700, color: "#2A2118" }}>
              Hướng dẫn quy trình đi khám trực tiếp
            </h2>
            <div style={{ width: "72px", height: "4px", background: "#C9963F", borderRadius: "999px", marginBottom: "16px" }} />
            <p style={{ margin: 0, maxWidth: "68ch", fontSize: "19px", lineHeight: "31px", color: "#4A3F33" }}>
              Không đăng ký trước cô chú vẫn được khám. Cứ tới phòng khám trong giờ làm việc
              {" "}
              <strong style={{ color: "#2A2118" }}>8:00 – 16:30</strong>
              , tất cả các ngày kể cả Chủ Nhật.
            </p>
          </div>
          <ol style={{ listStyle: "none", margin: 0, padding: 0, display: "grid", gridTemplateColumns: "repeat(3, minmax(0, 1fr))", gap: "24px" }}>
            <li style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "28px", display: "flex", flexDirection: "column", gap: "12px" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "48px", height: "48px", background: "#3B7145", color: "#FFFFFF", borderRadius: "999px", fontSize: "21px", fontWeight: 700 }}>
                1
              </span>
              <h3 style={{ margin: 0, fontSize: "21px", lineHeight: "30px", fontWeight: 600, color: "#2A2118" }}>Tới phòng khám</h3>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Vào cổng, gửi xe trong sân rồi đi tới bàn tiếp đón. Buổi sáng đông, cô chú nên đi sớm.
              </p>
            </li>
            <li style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "28px", display: "flex", flexDirection: "column", gap: "12px" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "48px", height: "48px", background: "#3B7145", color: "#FFFFFF", borderRadius: "999px", fontSize: "21px", fontWeight: 700 }}>
                2
              </span>
              <h3 style={{ margin: 0, fontSize: "21px", lineHeight: "30px", fontWeight: 600, color: "#2A2118" }}>Khai thông tin, lập hồ sơ</h3>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Nhân viên ghi họ tên, ngày sinh, số điện thoại và tình trạng bệnh. Chưa có tài khoản trên web vẫn được lập hồ sơ bình thường.
              </p>
            </li>
            <li style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "28px", display: "flex", flexDirection: "column", gap: "12px" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "48px", height: "48px", background: "#3B7145", color: "#FFFFFF", borderRadius: "999px", fontSize: "21px", fontWeight: 700 }}>
                3
              </span>
              <h3 style={{ margin: 0, fontSize: "21px", lineHeight: "30px", fontWeight: 600, color: "#2A2118" }}>Ngồi chờ đến lượt</h3>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Có đặt lịch trước thì đưa phiếu khám cho tiếp tân, cô chú được gọi theo giờ khám dự kiến trên phiếu khám, đỡ phải chờ lâu.
              </p>
            </li>
            <li style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "28px", display: "flex", flexDirection: "column", gap: "12px" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "48px", height: "48px", background: "#3B7145", color: "#FFFFFF", borderRadius: "999px", fontSize: "21px", fontWeight: 700 }}>
                4
              </span>
              <h3 style={{ margin: 0, fontSize: "21px", lineHeight: "30px", fontWeight: 600, color: "#2A2118" }}>Bác sĩ xem mạch và chẩn đoán</h3>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Cô chú kể rõ chỗ đau, đau từ bao giờ và đang uống thuốc gì. Bác sĩ bắt mạch rồi giải thích hướng điều trị.
              </p>
            </li>
            <li style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "28px", display: "flex", flexDirection: "column", gap: "12px" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "48px", height: "48px", background: "#3B7145", color: "#FFFFFF", borderRadius: "999px", fontSize: "21px", fontWeight: 700 }}>
                5
              </span>
              <h3 style={{ margin: 0, fontSize: "21px", lineHeight: "30px", fontWeight: 600, color: "#2A2118" }}>Trị liệu nếu có chỉ định</h3>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Qua khu trị liệu để châm cứu, bấm huyệt, xoa bóp hoặc tác động cột sống. Mỗi buổi thường kéo dài khoảng nửa tiếng.
              </p>
            </li>
            <li style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "28px", display: "flex", flexDirection: "column", gap: "12px" }}>
              <span style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", width: "48px", height: "48px", background: "#3B7145", color: "#FFFFFF", borderRadius: "999px", fontSize: "21px", fontWeight: 700 }}>
                6
              </span>
              <h3 style={{ margin: 0, fontSize: "21px", lineHeight: "30px", fontWeight: 600, color: "#2A2118" }}>Nhận thuốc và hẹn tái khám</h3>
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                Lấy thang thuốc tại quầy, nghe dặn cách sắc và cách uống. Cần theo đợt thì phòng khám hẹn ngày quay lại.
              </p>
            </li>
          </ol>
          <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderLeft: "6px solid #C9963F", borderRadius: "20px", padding: "24px", display: "flex", gap: "16px", alignItems: "flex-start" }}>
            <Icon name="heart" size={28} stroke="#8A6012" style={{ flexShrink: 0 }} />
            <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
              Cả quá trình — thăm khám, châm cứu, trị liệu và thuốc mang về — đều
              {" "}
              <strong style={{ color: "#2E5B37" }}>miễn phí hoàn toàn</strong>
              . Cô chú không phải chuẩn bị tiền, cũng không ai thu khoản nào.
            </p>
          </div>
        </div>
      </section>
      <section id="luu-y" style={{ width: "100%", padding: "72px 24px" }}>
        <div style={{ maxWidth: "1160px", margin: "0 auto", display: "flex", flexDirection: "column", gap: "32px" }}>
          <div>
            <p style={{ margin: "0 0 8px", fontSize: "16px", fontWeight: 700, letterSpacing: "0.06em", textTransform: "uppercase", color: "#8A6012" }}>
              Trước khi đi
            </p>
            <h2 style={{ margin: "0 0 12px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "34px", lineHeight: "44px", fontWeight: 700, color: "#2A2118" }}>
              Lưu ý khi đi khám
            </h2>
            <div style={{ width: "72px", height: "4px", background: "#C9963F", borderRadius: "999px" }} />
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(2, minmax(0, 1fr))", gap: "20px" }}>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "24px", display: "flex", gap: "14px", alignItems: "flex-start" }}>
              <Icon name="check" size={28} stroke="#2E5B37" strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                <strong style={{ color: "#2A2118" }}>Gọi điện hỏi trước</strong>
                {" "}
                — lịch của các bác sĩ có thể thay đổi, có ngày bệnh nhân quá đông. Gọi
                {" "}
                <a href="tel:0397116666" style={{ color: "#2E5B37", fontWeight: 600, textDecoration: "underline" }}>0397 116 666</a>
                {" "}
                để hỏi khung giờ còn nhận bệnh.
              </p>
            </div>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "24px", display: "flex", gap: "14px", alignItems: "flex-start" }}>
              <Icon name="check" size={28} stroke="#2E5B37" strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                <strong style={{ color: "#2A2118" }}>Đi sớm đầu giờ sáng</strong>
                {" "}
                — buổi sáng rất đông, đi sớm vừa đỡ phải chờ vừa tránh hết số nhận bệnh trong ngày.
              </p>
            </div>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "24px", display: "flex", gap: "14px", alignItems: "flex-start" }}>
              <Icon name="check" size={28} stroke="#2E5B37" strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                <strong style={{ color: "#2A2118" }}>Mang theo hồ sơ bệnh cũ</strong>
                {" "}
                — phim X-quang, MRI, đơn thuốc cũ từ bệnh viện Tây y. Giúp bác sĩ nắm rõ tổn thương để châm cứu, bấm huyệt chính xác và an toàn hơn.
              </p>
            </div>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "24px", display: "flex", gap: "14px", alignItems: "flex-start" }}>
              <Icon name="check" size={28} stroke="#2E5B37" strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                <strong style={{ color: "#2A2118" }}>Nói rõ thuốc đang uống</strong>
                {" "}
                — kể cả thuốc huyết áp, tiểu đường hay thuốc Nam mua ngoài, để bác sĩ cân nhắc khi kê thang thuốc.
              </p>
            </div>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "24px", display: "flex", gap: "14px", alignItems: "flex-start" }}>
              <Icon name="check" size={28} stroke="#2E5B37" strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                <strong style={{ color: "#2A2118" }}>Ăn nhẹ trước khi đi</strong>
                {" "}
                — đừng để bụng quá đói khi châm cứu, dễ bị “vựng châm” (chóng mặt, cồn cào, vã mồ hôi). Cũng đừng ăn quá no ngay trước giờ khám.
              </p>
            </div>
            <div style={{ background: "#FFFCF6", border: "1px solid #E0D3BC", borderRadius: "20px", padding: "24px", display: "flex", gap: "14px", alignItems: "flex-start" }}>
              <Icon name="check" size={28} stroke="#2E5B37" strokeWidth={2} style={{ flexShrink: 0, marginTop: "2px" }} />
              <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
                <strong style={{ color: "#2A2118" }}>Mặc quần áo rộng rãi</strong>
                {" "}
                — áo quần ống rộng hoặc cài nút, để bác sĩ dễ bộc lộ huyệt vị khi châm cứu, xoa bóp.
              </p>
            </div>
          </div>
          <div style={{ background: "#F8E3DD", borderRadius: "20px", padding: "24px", display: "flex", gap: "16px", alignItems: "flex-start" }}>
            <Icon name="alert-triangle" size={28} stroke="#90372A" style={{ flexShrink: 0 }} />
            <p style={{ margin: 0, fontSize: "18px", lineHeight: "29px", color: "#4A3F33" }}>
              <strong style={{ color: "#90372A" }}>Người cao tuổi, người đi lại khó khăn nên có người nhà đi cùng.</strong>
              {" "}
              Nếu cô chú đang sốt cao, đau dữ dội hoặc có dấu hiệu cấp cứu, xin tới bệnh viện trước — phòng khám là nơi điều trị bằng y học cổ truyền, không nhận cấp cứu.
            </p>
          </div>
          <div style={{ background: "#3B7145", borderRadius: "28px", padding: "36px", display: "flex", flexWrap: "wrap", gap: "24px", alignItems: "center", justifyContent: "space-between" }}>
            <div style={{ flex: "1 1 420px", minWidth: 0 }}>
              <h3 style={{ margin: "0 0 8px", fontFamily: "'Noto Serif', Georgia, serif", fontSize: "26px", lineHeight: "36px", fontWeight: 700, color: "#FFFFFF" }}>
                Còn điều gì chưa rõ?
              </h3>
              <p style={{ margin: 0, maxWidth: "56ch", fontSize: "18px", lineHeight: "29px", color: "#FFFFFF" }}>
                Cô chú gọi cho phòng khám, có người trực nghe máy trong giờ làm việc.
              </p>
            </div>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "14px" }}>
              <a href="tel:0397116666" style={{ display: "inline-flex", alignItems: "center", gap: "10px", minHeight: "56px", padding: "0 26px", background: "#FFFCF6", borderRadius: "12px", fontSize: "19px", fontWeight: 700, color: "#2E5B37", textDecoration: "none" }}>
                <Icon name="phone" size={22} />
                0397 116 666
              </a>
              <Link to="/lien-he" style={{ display: "inline-flex", alignItems: "center", minHeight: "56px", padding: "0 24px", border: "2px solid #FFFFFF", borderRadius: "12px", fontSize: "19px", fontWeight: 600, color: "#FFFFFF", textDecoration: "underline" }}>
                Trang Liên hệ
              </Link>
            </div>
          </div>
        </div>
      </section>
    </PublicLayout>
  )
}
