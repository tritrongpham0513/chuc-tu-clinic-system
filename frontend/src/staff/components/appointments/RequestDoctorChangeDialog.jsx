import { useState } from 'react';
import { Icon } from '../../../shared/ui/Icon.jsx';
import { ErrorAlert } from '../../../shared/ui/ui.jsx';
import { api } from '../../../shared/lib/api.js';
import { shortTime } from '../../../shared/lib/format.js';
import { Summary, TDialog } from '../dialogs.jsx';
import { longDay } from './time.js';

export function RequestDoctorChangeDialog({ appt, onClose, onDone }) {
  const [note, setNote] = useState(appt.doctorChangeNote || '');
  const [pending, setPending] = useState(false);
  const [error, setError] = useState(null);

  const summaryItems = [
    ['Bệnh nhân', appt.patient?.fullName, [appt.patient?.patientCode, appt.patient?.phone].filter(Boolean).join(' · ')],
    ['Mã phiếu khám', appt.ticketCode],
    ['Giờ khám dự kiến', shortTime(appt.slotStart), longDay(appt.slotDate), false],
    ['Bác sĩ hiện tại', appt.doctor || 'Chưa phân công'],
  ];

  const submit = async (e) => {
    if (e) e.preventDefault();
    if (pending) return;
    setPending(true);
    setError(null);
    try {
      await api.post(`/staff/appointments/${appt.id}/doctor-change-request`, {
        note: note.trim() || undefined,
      });
      onDone(`Đã gửi yêu cầu đổi bác sĩ cho phiếu ${appt.ticketCode} tới tiếp tân.`);
    } catch (err) {
      setError(err);
      setPending(false);
    }
  };

  return (
    <TDialog title="Báo lễ tân đổi bác sĩ" tone="gold" width={600} onClose={onClose}>
      <form onSubmit={submit} className="t-dialog-body" style={{ gap: 20 }}>
        <Summary items={summaryItems} />

        <div className="t-field" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <label htmlFor="change-doctor-note" className="t-label" style={{ fontWeight: 600, fontSize: 18 }}>
            Lý do / Ghi chú cho lễ tân (tuỳ chọn, tối đa 255 ký tự)
          </label>
          <textarea
            id="change-doctor-note"
            className="t-input"
            rows={3}
            maxLength={255}
            placeholder="Ví dụ: Bệnh nhân cần khám chuyên khoa Châm cứu / Bác sĩ bận việc đột xuất..."
            value={note}
            onChange={(e) => setNote(e.target.value)}
            style={{ resize: 'vertical', minHeight: 90, fontSize: 18 }}
          />
          <span style={{ fontSize: 15, color: '#78350F', alignSelf: 'flex-end' }}>
            {note.length}/255 ký tự
          </span>
        </div>

        <ErrorAlert error={error} />

        <div className="t-actions" style={{ justifyContent: 'flex-end', gap: 12, marginTop: 10 }}>
          <button
            type="button"
            className="t-btn t-btn-ghost"
            style={{ minHeight: 48, padding: '0 20px', fontSize: 18 }}
            onClick={onClose}
            disabled={pending}
          >
            Đóng
          </button>
          <button
            type="submit"
            className="t-btn t-btn-primary"
            style={{ minHeight: 48, padding: '0 24px', fontSize: 18, fontWeight: 600 }}
            disabled={pending}
          >
            <Icon name="send" size={20} strokeWidth={2} />
            {pending ? 'Đang gửi...' : 'Gửi yêu cầu tới tiếp tân'}
          </button>
        </div>
      </form>
    </TDialog>
  );
}
