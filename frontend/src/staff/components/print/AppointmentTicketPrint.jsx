import { fmtDate, shortTime, weekday } from '../../../shared/lib/format.js';
import { PrintSheet } from './PrintSheet.jsx';
import { formatSlotRange, maskPhone } from './printUtils.js';

/**
 * Mẫu in Phiếu khám bệnh tại quầy (Sprint 8 - Ticket 2).
 * Khổ A5 dọc, chữ to rõ ràng, che 3 số giữa SĐT, mã phiếu nổi bật.
 */
export function AppointmentTicketPrint({ appt, variant = 'a5' }) {
  if (!appt) return null;

  const p = appt.patient || {};
  const birthYear = p.birthYear || (typeof p.dob === 'string' ? p.dob.slice(0, 4) : p.dob instanceof Date ? p.dob.getFullYear() : '—');
  const dateVal = appt.slotDate || appt.appointmentDate;
  const dateStr = dateVal ? `${weekday(dateVal)}, ngày ${fmtDate(dateVal)}` : '—';
  const start = appt.slotStart || (appt.timeSlot ? appt.timeSlot.startTime : appt.expectedTime);
  const end = appt.slotEnd || (appt.timeSlot ? appt.timeSlot.endTime : null);
  const timeStr = formatSlotRange(start, end);
  const service = appt.serviceLabel || appt.serviceType || 'Khám chữa bệnh YHCT';
  const doctor = appt.doctor || 'Bác sĩ phòng khám';

  return (
    <PrintSheet title="PHIẾU HẸN KHÁM BỆNH" variant={variant}>
      {/* Khối Mã phiếu khám to, dễ đọc */}
      <div className="print-ticket-badge print-block">
        <span className="print-ticket-badge-label">MÃ PHIẾU KHÁM</span>
        <span className="print-ticket-badge-code">{appt.ticketCode}</span>
      </div>

      {/* Thông tin lịch khám */}
      <dl className="print-dl print-block">
        <div className="print-dl-row">
          <dt className="print-dt">Ngày khám:</dt>
          <dd className="print-dd print-dd-bold">{dateStr}</dd>
        </div>
        <div className="print-dl-row">
          <dt className="print-dt">Khung giờ:</dt>
          <dd className="print-dd print-dd-bold">{timeStr}</dd>
        </div>
        <div className="print-dl-row">
          <dt className="print-dt">Dịch vụ:</dt>
          <dd className="print-dd">{service}</dd>
        </div>
        <div className="print-dl-row">
          <dt className="print-dt">Bác sĩ phụ trách:</dt>
          <dd className="print-dd">{doctor}</dd>
        </div>
      </dl>

      <div style={{ height: '2mm' }} />

      {/* Thông tin bệnh nhân */}
      <dl className="print-dl print-block">
        <div className="print-dl-row">
          <dt className="print-dt">Họ tên bệnh nhân:</dt>
          <dd className="print-dd print-dd-bold">{p.fullName || '—'}</dd>
        </div>
        <div className="print-dl-row">
          <dt className="print-dt">Năm sinh:</dt>
          <dd className="print-dd">{birthYear}</dd>
        </div>
        <div className="print-dl-row">
          <dt className="print-dt">Số điện thoại:</dt>
          <dd className="print-dd">{maskPhone(p.phone)}</dd>
        </div>
        {p.address && (
          <div className="print-dl-row">
            <dt className="print-dt">Địa chỉ:</dt>
            <dd className="print-dd">{p.address}</dd>
          </div>
        )}
      </dl>

      {/* Dòng nhắc nhở */}
      <div className="print-reminder-box print-block">
        Vui lòng đến trước 15 phút và mang theo giấy tờ tuỳ thân.
      </div>
    </PrintSheet>
  );
}
