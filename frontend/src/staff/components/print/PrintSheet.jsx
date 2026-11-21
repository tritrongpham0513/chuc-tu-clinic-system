import { CLINIC } from '../../../shared/lib/format.js';
import './print.css';

/**
 * Component PrintSheet chuẩn A5 dọc / A4 cho khu nội bộ (Sprint 8).
 *
 * @param {object} props
 * @param {React.ReactNode} props.children Nội dung cần in
 * @param {string} [props.title] Tiêu đề phiếu in (14-16pt, in hoa, đậm)
 * @param {'a5'|'a4'} [props.variant='a5'] Khổ in: 'a5' (mặc định) hoặc 'a4'
 * @param {string} [props.footer] Nội dung chân trang tuỳ biến (mặc định theo quy chuẩn phòng khám)
 * @param {string} [props.className] Class bổ sung nếu cần
 */
export function PrintSheet({
  children,
  title,
  variant = 'a5',
  footer,
  className = '',
}) {
  const isA4 = variant === 'a4';
  const defaultFooter = `Phòng khám Chúc Từ · ${CLINIC.address} · Hotline ${CLINIC.phone}`;

  return (
    <article className={`print-sheet ${isA4 ? 'print-sheet--a4' : ''} ${className}`}>
      {/* Đầu trang: Tên phòng khám + địa chỉ + hotline */}
      <header className="print-sheet-head">
        <p className="print-clinic-name">{CLINIC.name}</p>
        <p className="print-clinic-contact">
          Địa chỉ: {CLINIC.address}
          <br />
          Hotline: {CLINIC.phone} · {CLINIC.phone2} · Sư cô Chúc Từ
        </p>
      </header>

      {/* Tiêu đề phiếu */}
      {title && <h1 className="print-sheet-title">{title}</h1>}

      {/* Thân phiếu in */}
      <div className="print-sheet-body">
        {children}
      </div>

      {/* Chân trang cố định chuẩn */}
      <footer className="print-sheet-foot">
        {footer || defaultFooter}
      </footer>
    </article>
  );
}
