import { fmtDate, weekday } from '../../../shared/lib/format.js';
import { PrintSheet } from './PrintSheet.jsx';

/**
 * Mẫu in Đơn thuốc / Toa thuốc thảo mộc riêng biệt (Sprint 11 - Ticket 5).
 * Khổ A5 dọc / A4, không cắt trang giữa đơn thuốc, chữ ký bác sĩ điều trị,
 * phần dòng kẻ để ghi tay thêm nếu cần.
 */
export function PrescriptionPrintSheet({ record, patient, variant = 'a5' }) {
  if (!record) return null;

  const p = patient || record.patient || {};
  const birthYear = p.birthYear || p.yearOfBirth || (typeof p.dob === 'string' ? p.dob.slice(0, 4) : p.dob instanceof Date ? p.dob.getFullYear() : '—');
  const dateStr = record.visitDate ? `${weekday(record.visitDate)}, ngày ${fmtDate(record.visitDate)}` : '—';
  const doctor = record.doctorName || (record.doctor ? record.doctor.fullName : 'Bác sĩ điều trị');

  // Tách ngày tháng năm ký tên
  let signDateText = '';
  if (record.visitDate && String(record.visitDate).includes('-')) {
    const [y, m, d] = String(record.visitDate).slice(0, 10).split('-');
    if (y && m && d) {
      signDateText = `Ngày ${Number(d)} tháng ${Number(m)} năm ${y}`;
    }
  }

  return (
    <PrintSheet title="ĐƠN THUỐC THẢO MỘC" variant={variant}>
      {/* 1. Thông tin chung */}
      <dl className="print-dl print-block">
        <div className="print-dl-row">
          <dt className="print-dt">Họ tên bệnh nhân:</dt>
          <dd className="print-dd print-dd-bold">
            {p.fullName || '—'} {p.patientCode ? `· Mã BN: ${p.patientCode}` : ''}
          </dd>
        </div>
        <div className="print-dl-row">
          <dt className="print-dt">Năm sinh:</dt>
          <dd className="print-dd">{birthYear}</dd>
        </div>
        <div className="print-dl-row">
          <dt className="print-dt">Ngày kê:</dt>
          <dd className="print-dd">{dateStr}</dd>
        </div>
        <div className="print-dl-row">
          <dt className="print-dt">Bác sĩ kê đơn:</dt>
          <dd className="print-dd">{doctor}</dd>
        </div>
        {record.diagnosis && (
          <div className="print-dl-row">
            <dt className="print-dt">Chẩn đoán:</dt>
            <dd className="print-dd">{record.diagnosis}</dd>
          </div>
        )}
      </dl>

      <div style={{ height: '3mm' }} />

      {/* 2. Đơn thuốc (giữ nguyên xuống dòng, không bị cắt) */}
      <div className="print-section print-block" style={{ pageBreakInside: 'avoid', breakInside: 'avoid' }}>
        <h2 className="print-section-heading">Chi tiết thang thuốc / đơn thuốc</h2>
        {record.prescription ? (
          <p className="print-pre-wrap">{record.prescription}</p>
        ) : (
          <p className="print-pre-wrap" style={{ fontStyle: 'italic', color: '#666' }}>
            (Chưa ghi đơn thuốc trên hệ thống)
          </p>
        )}
      </div>

      {/* 3. Phần trống có dòng kẻ để bác sĩ ghi tay thêm (nếu cần) */}
      <div className="print-section print-block" style={{ marginTop: '3mm', pageBreakInside: 'avoid', breakInside: 'avoid' }}>
        <p style={{ fontSize: '11pt', fontWeight: 600, fontStyle: 'italic', marginBottom: '2mm', color: '#333' }}>
          Dặn dò / Ghi chú thêm của thầy thuốc:
        </p>
        <div className="print-hw-line" />
        <div className="print-hw-line" />
        <div className="print-hw-line" />
      </div>

      {/* 4. Hẹn tái khám (nếu có) */}
      {record.followUpDate && (
        <div className="print-block" style={{ marginTop: '2mm' }}>
          <p className="print-dd-bold" style={{ margin: 0, fontSize: '11pt' }}>
            * Hẹn tái khám: Ngày {fmtDate(record.followUpDate)}.
          </p>
        </div>
      )}

      {/* 5. Chỗ ký bác sĩ */}
      <div className="print-sign-area print-block">
        <div className="print-sign-box">
          {signDateText && <p className="print-sign-date">{signDateText}</p>}
          <p className="print-sign-role">Bác sĩ điều trị</p>
          <div className="print-sign-line">
            {doctor}
          </div>
        </div>
      </div>
    </PrintSheet>
  );
}
