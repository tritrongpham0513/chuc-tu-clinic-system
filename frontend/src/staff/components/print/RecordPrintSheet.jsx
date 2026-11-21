import { fmtDate, weekday } from '../../../shared/lib/format.js';
import { PrintSheet } from './PrintSheet.jsx';

/**
 * Mẫu in Phiếu kết quả khám + đơn thuốc tại quầy (Sprint 8 - Ticket 3).
 * Khổ A5 dọc / A4, không cắt trang giữa đơn thuốc, chữ ký chuẩn.
 */
export function RecordPrintSheet({ record, patient, variant = 'a5' }) {
  if (!record) return null;

  const p = patient || record.patient || {};
  const birthYear = p.birthYear || (typeof p.dob === 'string' ? p.dob.slice(0, 4) : p.dob instanceof Date ? p.dob.getFullYear() : '—');
  const dateStr = record.visitDate ? `${weekday(record.visitDate)}, ngày ${fmtDate(record.visitDate)}` : '—';
  const doctor = record.doctorName || 'Bác sĩ phòng khám';

  const notes = record.treatmentNotes || {};
  const acup = (notes.acupuncture || record.treatmentAcupuncture || '').trim();
  const press = (notes.acupressure || record.treatmentAcupressure || '').trim();
  const oth = (notes.other || record.treatmentOther || '').trim();
  const hasTreatment = Boolean(acup || press || oth);

  const pulse = record.pulse ? `${String(record.pulse).replace(/\D/g, '')} lần/phút` : null;
  const bp = record.bloodPressure ? `${String(record.bloodPressure).replace(/[^\d/]/g, '')} mmHg` : null;
  const vitals = [pulse, bp].filter(Boolean).join(' · ');

  // Tách ngày tháng năm ký tên (xử lý chuỗi an toàn kể cả khi có kèm giờ)
  let signDateText = '';
  if (record.visitDate && String(record.visitDate).includes('-')) {
    const [y, m, d] = String(record.visitDate).slice(0, 10).split('-');
    if (y && m && d) {
      signDateText = `Ngày ${Number(d)} tháng ${Number(m)} năm ${y}`;
    }
  }

  return (
    <PrintSheet title="PHIẾU KẾT QUẢ KHÁM BỆNH" variant={variant}>
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
          <dt className="print-dt">Ngày khám:</dt>
          <dd className="print-dd">{dateStr}</dd>
        </div>
        <div className="print-dl-row">
          <dt className="print-dt">Bác sĩ điều trị:</dt>
          <dd className="print-dd">{doctor}</dd>
        </div>
        {vitals && (
          <div className="print-dl-row">
            <dt className="print-dt">Mạch & Huyết áp:</dt>
            <dd className="print-dd">{vitals}</dd>
          </div>
        )}
      </dl>

      <div style={{ height: '2mm' }} />

      {/* 2. Chẩn đoán (chữ đậm) */}
      <div className="print-block">
        <div className="print-dl-row" style={{ alignItems: 'flex-start' }}>
          <dt className="print-dt" style={{ fontWeight: 700 }}>Chẩn đoán:</dt>
          <dd className="print-dd print-dd-bold" style={{ fontSize: '12.5pt' }}>
            {record.diagnosis || '—'}
          </dd>
        </div>
      </div>

      {/* 3. Điều trị (chỉ hiện mục có nội dung) */}
      {hasTreatment && (
        <div className="print-section print-block">
          <h2 className="print-section-heading">Phương pháp điều trị</h2>
          <dl className="print-dl">
            {acup && (
              <div className="print-dl-row">
                <dt className="print-dt">Châm cứu:</dt>
                <dd className="print-dd">{acup}</dd>
              </div>
            )}
            {press && (
              <div className="print-dl-row">
                <dt className="print-dt">Bấm huyệt:</dt>
                <dd className="print-dd">{press}</dd>
              </div>
            )}
            {oth && (
              <div className="print-dl-row">
                <dt className="print-dt">Khác:</dt>
                <dd className="print-dd">{oth}</dd>
              </div>
            )}
          </dl>
        </div>
      )}

      {/* 4. Đơn thuốc (giữ xuống dòng gốc, không bị cắt) */}
      {record.prescription && (
        <div className="print-section print-block">
          <h2 className="print-section-heading">Đơn thuốc / Toa thuốc thảo mộc</h2>
          <p className="print-pre-wrap">{record.prescription}</p>
        </div>
      )}

      {/* 5. Lời dặn & Hẹn tái khám */}
      <div className="print-section print-block">
        <h2 className="print-section-heading">Lời dặn & Tái khám</h2>
        {record.advice && (
          <p className="print-pre-wrap" style={{ marginBottom: '2mm' }}>
            {record.advice}
          </p>
        )}
        <p className="print-dd-bold" style={{ margin: 0 }}>
          {record.followUpDate
            ? `Hẹn tái khám ngày ${fmtDate(record.followUpDate)}.`
            : 'Chưa hẹn ngày tái khám.'}
        </p>
      </div>

      {/* 6. Chỗ ký bác sĩ */}
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
