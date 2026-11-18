import { useAsync, useDocumentTitle } from '../../../shared/lib/hooks.js';
import { api } from '../../../shared/lib/api.js';
import { fmtDate, todayVN, weekday } from '../../../shared/lib/format.js';
import { Icon } from '../../../shared/ui/Icon.jsx';
import { Empty, ErrorAlert, Loading } from '../../../shared/ui/ui.jsx';
import './mySchedule.css';

const WEEKDAY_NAMES = {
  MON: 'Thứ Hai',
  TUE: 'Thứ Ba',
  WED: 'Thứ Tư',
  THU: 'Thứ Năm',
  FRI: 'Thứ Sáu',
  SAT: 'Thứ Bảy',
  SUN: 'Chủ Nhật',
};

const DOW_BY_INDEX = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];

const PERIOD_LABELS = {
  FULL_DAY: 'Cả ngày (Sáng + Chiều)',
  MORNING: 'Buổi sáng',
  AFTERNOON: 'Buổi chiều',
};

function getDayOffBadge(offDate, todayDateStr) {
  if (!offDate || !todayDateStr) return null;
  if (offDate === todayDateStr) {
    return { label: 'Hôm nay', cls: 'ms-badge ms-badge-alert', icon: 'alert' };
  }
  const diffDays = Math.round(
    (new Date(offDate).getTime() - new Date(todayDateStr).getTime()) / 86400000
  );
  if (diffDays > 0 && diffDays <= 3) {
    return { label: 'Sắp tới', cls: 'ms-badge ms-badge-gold', icon: 'clock' };
  }
  return null;
}

export default function MySchedule() {
  useDocumentTitle('Lịch trực của tôi');

  const { data: scheduleData, loading, error, reload } = useAsync(() => api.get('/staff/my-schedule'), []);

  const todayDateStr = todayVN();
  const dayIndex = new Date(`${todayDateStr}T00:00:00Z`).getUTCDay();
  const todayKey = DOW_BY_INDEX[dayIndex];

  const days = scheduleData?.days || ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];
  const shifts = scheduleData?.shifts || {};
  const dayOffs = scheduleData?.dayOffs || [];

  return (
    <div className="t-page">
      <header className="t-pagehead ms-header">
        <div>
          <h1 className="t-h1">Lịch trực của tôi</h1>
          <p className="ms-lead">
            Lịch cố định hằng tuần do ban quản lý phân công. Cần đổi ca hoặc báo nghỉ đột xuất, vui lòng liên hệ quản trị viên.
          </p>
        </div>
      </header>

      {loading && <Loading label="Đang tải lịch trực của bác sĩ…" />}
      {error && <ErrorAlert error={error} onRetry={reload} />}

      {!loading && !error && (
        <>
          {/* Khối 1: Ma trận tuần 7 ngày */}
          <section className="t-card" style={{ marginBottom: 28 }}>
            <div className="t-card-pad">
              <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
                <h2 className="t-h2" style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <Icon name="calendar" size={24} /> Lịch trực cố định trong tuần
                </h2>
                <div className="ms-legend">
                  <span className="ms-legend-item">
                    <span className="ms-legend-dot full" /> Sáng: 08:00 – 12:00
                  </span>
                  <span className="ms-legend-item">
                    <span className="ms-legend-dot part" /> Chiều: 13:00 – 16:30
                  </span>
                </div>
              </div>

              {/* Desktop: 7 thẻ Thứ Hai..Chủ Nhật (Tách 2 khối Sáng & Chiều, chống tràn) */}
              <div className="ms-matrix-desktop-wrap">
                <div className="ms-matrix-desktop">
                  {days.map((dayKey) => {
                    const dayShifts = shifts[dayKey] || [];
                    const hasMorning = dayShifts.includes('MORNING');
                    const hasAfternoon = dayShifts.includes('AFTERNOON');
                    const isToday = dayKey === todayKey;
                    return (
                      <div key={dayKey} className={`ms-day-card ${isToday ? 'is-today' : ''}`}>
                        <div className="ms-day-header">
                          {isToday ? (
                            <span className="ms-today-badge">
                              <Icon name="checkCircle" size={11} strokeWidth={2.5} /> Hôm nay
                            </span>
                          ) : (
                            <span className="ms-today-placeholder" aria-hidden="true" />
                          )}
                          <span className="ms-day-name">{WEEKDAY_NAMES[dayKey] || dayKey}</span>
                        </div>

                        <div className="ms-shifts-stack">
                          {/* Khối Ca Sáng */}
                          <div className={`ms-slot-box ${hasMorning ? 'is-active' : 'is-off'}`}>
                            <div className="ms-slot-title">
                              <Icon name="sun" size={13} />
                              <span>Sáng</span>
                            </div>
                            <span className="ms-slot-status">{hasMorning ? 'Trực' : 'Nghỉ'}</span>
                          </div>

                          {/* Khối Ca Chiều */}
                          <div className={`ms-slot-box ${hasAfternoon ? 'is-active' : 'is-off'}`}>
                            <div className="ms-slot-title">
                              <Icon name="clock" size={13} />
                              <span>Chiều</span>
                            </div>
                            <span className="ms-slot-status">{hasAfternoon ? 'Trực' : 'Nghỉ'}</span>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Mobile (< 820px): Danh sách dọc 7 dòng, tách 2 khối Sáng & Chiều */}
              <div className="ms-matrix-mobile">
                {days.map((dayKey) => {
                  const dayShifts = shifts[dayKey] || [];
                  const hasMorning = dayShifts.includes('MORNING');
                  const hasAfternoon = dayShifts.includes('AFTERNOON');
                  const isToday = dayKey === todayKey;
                  return (
                    <div key={dayKey} className={`ms-mobile-row ${isToday ? 'is-today' : ''}`}>
                      <div className="ms-mobile-day">
                        <span className="ms-mobile-day-name">{WEEKDAY_NAMES[dayKey] || dayKey}</span>
                        {isToday && (
                          <span className="ms-today-badge">
                            <Icon name="checkCircle" size={12} strokeWidth={2.5} /> Hôm nay
                          </span>
                        )}
                      </div>
                      <div className="ms-mobile-slots">
                        <div className={`ms-slot-box ${hasMorning ? 'is-active' : 'is-off'}`}>
                          <div className="ms-slot-title">
                            <Icon name="sun" size={13} />
                            <span>Sáng:</span>
                          </div>
                          <span className="ms-slot-status">{hasMorning ? 'Trực' : 'Nghỉ'}</span>
                        </div>
                        <div className={`ms-slot-box ${hasAfternoon ? 'is-active' : 'is-off'}`}>
                          <div className="ms-slot-title">
                            <Icon name="clock" size={13} />
                            <span>Chiều:</span>
                          </div>
                          <span className="ms-slot-status">{hasAfternoon ? 'Trực' : 'Nghỉ'}</span>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </section>

          {/* Khối 2: Ngày nghỉ sắp tới */}
          <section className="t-card">
            <div className="t-card-pad">
              <h2 className="t-h2" style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
                <Icon name="clock" size={24} /> Danh sách ngày nghỉ sắp tới
              </h2>

              {dayOffs.length === 0 ? (
                <Empty icon="calendar" title="Chưa có ngày nghỉ nào">
                  Bác sĩ hiện không có ngày nghỉ đột xuất nào được ghi nhận trong thời gian tới.
                </Empty>
              ) : (
                <div className="ms-table-wrap">
                  <table className="t-table">
                    <thead>
                      <tr>
                        <th style={{ width: '38%' }}>Ngày nghỉ</th>
                        <th style={{ width: '28%' }}>Ca nghỉ</th>
                        <th>Lý do</th>
                      </tr>
                    </thead>
                    <tbody>
                      {dayOffs.map((d) => {
                        const badge = getDayOffBadge(d.offDate, todayDateStr);
                        const weekdayLabel = weekday(d.offDate);
                        const formattedDate = fmtDate(d.offDate);
                        return (
                          <tr key={d.id}>
                            <td>
                              <div className="ms-date-cell">
                                <strong>
                                  {weekdayLabel ? `${weekdayLabel}, ` : ''}{formattedDate}
                                </strong>
                                {badge && (
                                  <span className={badge.cls}>
                                    <Icon name={badge.icon} size={14} /> {badge.label}
                                  </span>
                                )}
                              </div>
                            </td>
                            <td>
                              <span className="ms-period-pill">
                                {PERIOD_LABELS[d.period] || d.period}
                              </span>
                            </td>
                            <td style={{ color: d.reason ? 'var(--ink-900)' : 'var(--ink-500)' }}>
                              {d.reason || '—'}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </section>
        </>
      )}
    </div>
  );
}
