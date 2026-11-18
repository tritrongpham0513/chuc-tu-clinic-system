// Nhãn dùng chung với server nằm ở shared/constants.js (bản chép của backend)
export { GENDERS, RELATIONSHIPS, ROLE_LABELS } from '../domain/constants.js';

export const CLINIC = {
  name: 'Phòng chẩn trị Y học cổ truyền từ thiện Chúc Từ',
  short: 'Chúc Từ',
  address: '57/8-10 Điện Biên Phủ, Phường 25, Quận Bình Thạnh, TP. Hồ Chí Minh',
  phone: '0397 116 666',
  phoneHref: 'tel:0397116666',
  // Zalo của phòng khám dùng cùng số điện thoại chính
  zaloUrl: 'https://zalo.me/0397116666',
  phone2: '0835 555 115',
  hours: '8:00 – 16:30',
  mapUrl: 'https://www.google.com/maps/search/?api=1&query=57%2F8-10+%C4%90i%E1%BB%87n+Bi%C3%AAn+Ph%E1%BB%A7%2C+B%C3%ACnh+Th%E1%BA%A1nh%2C+TP.HCM',
};

const WEEKDAYS = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];

const pad = (n) => String(n).padStart(2, '0');

/** "2026-10-20" → Date ở nửa đêm UTC, chỉ dùng để tính thứ và cộng ngày. */
const parseDay = (s) => new Date(`${s}T00:00:00Z`);

export function addDays(dateStr, n) {
  return new Date(parseDay(dateStr).getTime() + n * 86400_000).toISOString().slice(0, 10);
}

export function weekday(dateStr) {
  return WEEKDAYS[parseDay(dateStr).getUTCDay()];
}

const VN_TZ = 'Asia/Ho_Chi_Minh';

/**
 * Tách ngày/giờ theo giờ Việt Nam.
 * - Chuỗi KHÔNG có timezone ("2026-11-17 10:00:00"): backend đã gửi giờ tường VN
 *   -> đọc thẳng các con số, KHÔNG cộng trừ gì.
 * - Date hoặc chuỗi có Z/offset: quy đổi sang Asia/Ho_Chi_Minh.
 * Trả null nếu không parse được.
 */
function vnParts(value) {
  if (value == null || value === '') return null;

  if (typeof value === 'string') {
    const m = value.match(/^(\d{4})-(\d{2})-(\d{2})(?:[ T](\d{2}):(\d{2})(?::\d{2})?)?$/);
    if (m) {
      return {
        y: +m[1],
        m: +m[2],
        d: +m[3],
        h: m[4] != null ? +m[4] : null,
        mi: m[5] != null ? +m[5] : null,
      };
    }
  }

  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return null;

  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: VN_TZ,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  })
    .formatToParts(date)
    .reduce((acc, p) => {
      acc[p.type] = p.value;
      return acc;
    }, {});

  return { y: +parts.year, m: +parts.month, d: +parts.day, h: +parts.hour, mi: +parts.minute };
}

/** 20/10/2026 */
export function fmtDate(value) {
  const p = vnParts(value);
  return p ? `${pad(p.d)}/${pad(p.m)}/${p.y}` : '—';
}

/** 09:05 theo giờ Việt Nam */
export function fmtTime(value) {
  const p = vnParts(value);
  return p && p.h != null ? `${pad(p.h)}:${pad(p.mi)}` : '—';
}

/** "08:00" → "8:00 – 9:00" */
export function slotLabel(start) {
  const [h, m] = start.split(':').map(Number);
  return `${h}:${pad(m)} – ${h + 1}:${pad(m)}`;
}

export function shortTime(start) {
  const [h, m] = start.split(':').map(Number);
  return `${h}:${pad(m)}`;
}

/** Ngày hôm nay theo giờ Việt Nam */
export function todayVN() {
  return new Date(Date.now() + 7 * 3600_000).toISOString().slice(0, 10);
}

export function ageFrom(birthYear) {
  if (!birthYear) return null;
  return Number(todayVN().slice(0, 4)) - birthYear;
}
