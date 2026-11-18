import { useEffect, useState } from 'react';
import { Icon } from '../../../shared/ui/Icon.jsx';
import { ErrorAlert, Loading } from '../../../shared/ui/ui.jsx';
import { api } from '../../../shared/lib/api.js';
import { shortTime } from '../../../shared/lib/format.js';
import { useAsync } from '../../../shared/lib/hooks.js';
import { Summary, TDialog } from '../dialogs.jsx';
import { longDay } from './time.js';

export function ChangeDoctorDialog({ appt, onClose, onDone }) {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState(null);

  const { data, loading, error: loadError } = useAsync(() => api.get('/doctors'), []);
  const doctors = Array.isArray(data) ? data : data?.data || data?.items || [];

  const currentDoctorId = appt.doctorId != null
    ? Number(appt.doctorId)
    : (doctors.find((d) => d.fullName === appt.doctor)?.id ?? null);

  const [selectedId, setSelectedId] = useState(null);

  useEffect(() => {
    if (doctors.length && selectedId === null && currentDoctorId != null) {
      setSelectedId(currentDoctorId);
    }
  }, [doctors, currentDoctorId, selectedId]);

  const isDifferent = selectedId != null && selectedId !== currentDoctorId;
  const canSubmit = isDifferent && !pending;

  const summaryItems = [
    ['Bệnh nhân', appt.patient?.fullName, [appt.patient?.patientCode, appt.patient?.phone].filter(Boolean).join(' · ')],
    ['Mã phiếu khám', appt.ticketCode],
    ['Giờ khám dự kiến', shortTime(appt.slotStart), longDay(appt.slotDate), false],
    ['Bác sĩ hiện tại', appt.doctor || 'Chưa phân công'],
  ];

  const submit = async () => {
    if (!canSubmit) return;
    setPending(true);
    setError(null);
    try {
      await api.patch(`/staff/appointments/${appt.id}/doctor?doctorId=${selectedId}`);
      onDone(`Đã đổi bác sĩ cho phiếu ${appt.ticketCode}.`);
    } catch (err) {
      setError(err);
      setPending(false);
    }
  };

  return (
    <TDialog title="Đổi bác sĩ khám" tone="green" width={600} onClose={onClose}>
      <div className="t-dialog-body" style={{ gap: 20 }}>
        <Summary items={summaryItems} />

        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <span className="t-label">Chọn bác sĩ khám mới</span>
          {loading && !data && <Loading />}
          {doctors.length > 0 && (
            <div role="radiogroup" aria-label="Chọn bác sĩ khám mới" className="t-doctor-list">
              {doctors.map((d) => {
                const isSelected = selectedId === d.id;
                const isCurrent = d.id === currentDoctorId;
                const meta = [d.title, d.specialty].filter(Boolean).join(' · ');
                return (
                  <button
                    key={d.id}
                    type="button"
                    role="radio"
                    aria-checked={isSelected}
                    className={`t-doctor-item ${isSelected ? 'selected' : ''}`}
                    onClick={() => setSelectedId(d.id)}
                  >
                    <span className="t-doctor-radio-mark" aria-hidden="true">
                      <span className="t-doctor-radio-dot" />
                    </span>
                    <span className="t-doctor-info">
                      <span className="t-doctor-name">
                        {d.fullName}
                        {isCurrent ? ' (hiện tại)' : ''}
                      </span>
                      {meta && <span className="t-doctor-meta">{meta}</span>}
                    </span>
                  </button>
                );
              })}
            </div>
          )}
        </div>

        <ErrorAlert error={error || loadError} />

        <div className="t-actions">
          <button
            type="button"
            className="t-btn t-btn-primary t-btn-bold t-btn-grow"
            disabled={!canSubmit}
            onClick={submit}
          >
            <Icon name="check" strokeWidth={2.25} />
            {pending ? 'Đang lưu...' : 'Xác nhận đổi'}
          </button>
          <button
            type="button"
            className="t-btn t-btn-ghost"
            style={{ padding: '0 28px' }}
            onClick={onClose}
          >
            Huỷ
          </button>
        </div>
      </div>
    </TDialog>
  );
}
