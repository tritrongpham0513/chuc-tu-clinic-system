// Số điện thoại: người dùng nhập 9–10 chữ số sau tiền tố +84 cố định.
export const cleanPhone = (value) => String(value || '').replace(/\D/g, '').slice(0, 10)

/**
 * Chuẩn hóa số điện thoại về định dạng 10 số (bắt đầu bằng 0)
 * Ví dụ: 912345678 -> 0912345678; 84912345678 -> 0912345678
 */
export const normalizePhone = (value) => {
  const digits = String(value || '').replace(/\D/g, '')
  if (digits.startsWith('84') && digits.length === 11) {
    return '0' + digits.slice(2)
  }
  if (digits.length === 9 && !digits.startsWith('0')) {
    return '0' + digits
  }
  return digits
}

export const isValidPhone = (value) => {
  const normalized = normalizePhone(value)
  return /^(03|05|07|08|09)\d{8}$/.test(normalized)
}

export const phoneMessage = (value) => {
  const normalized = normalizePhone(value)
  if (!normalized) return 'Vui lòng nhập số điện thoại.'
  return /^(03|05|07|08|09)\d{8}$/.test(normalized)
    ? ''
    : 'Số điện thoại gồm 10 chữ số và bắt đầu bằng 03, 05, 07, 08 hoặc 09.'
}

// 0912 *** 678 — dùng trong hộp OTP để nhắc số vừa gửi mã tới
export const maskPhone = (value) => {
  const normalized = normalizePhone(value)
  return normalized.length >= 8
    ? `${normalized.slice(0, 4)} *** ${normalized.slice(-3)}`
    : 'điện thoại của cô chú'
}
