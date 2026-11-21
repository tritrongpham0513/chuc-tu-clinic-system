/**
 * Tiện ích hỗ trợ in ấn tại quầy khu nội bộ (Sprint 8).
 */

/**
 * Che 3 số giữa của số điện thoại để bảo mật thông tin bệnh nhân trên bản in quầy.
 * Ví dụ: "0901234567" -> "090***4567", "090 123 4567" -> "090***4567"
 */
export function maskPhone(phone) {
  if (!phone) return '—';
  const digits = String(phone).replace(/\D/g, '');
  if (digits.length === 10) {
    // 090 123 4567 -> 090***4567 (che đúng 3 số giữa)
    return `${digits.slice(0, 3)}***${digits.slice(6)}`;
  }
  if (digits.length === 11) {
    // 84 901 234 567 -> 8490***4567
    return `${digits.slice(0, 4)}***${digits.slice(7)}`;
  }
  if (digits.length >= 7) {
    const mid = Math.floor(digits.length / 2);
    return `${digits.slice(0, mid - 1)}***${digits.slice(mid + 2)}`;
  }
  return String(phone);
}

/**
 * Định dạng khung giờ hiển thị: "08:00 – 09:00" hoặc "08:00"
 */
export function formatSlotRange(slotStart, slotEnd) {
  if (!slotStart) return '—';
  const start = String(slotStart).slice(0, 5);
  if (!slotEnd) return start;
  const end = String(slotEnd).slice(0, 5);
  return `${start} – ${end}`;
}
