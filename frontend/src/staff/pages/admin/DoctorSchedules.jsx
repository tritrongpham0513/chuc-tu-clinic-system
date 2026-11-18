import { useEffect, useId, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Icon } from '../../../shared/ui/Icon.jsx';
import { Empty, ErrorAlert, Loading } from '../../../shared/ui/ui.jsx';
import { api } from '../../../shared/lib/api.js';
import { addDays, fmtDate, fmtTime, todayVN, weekday } from '../../../shared/lib/format.js';
import { useAsync, useDocumentTitle } from '../../../shared/lib/hooks.js';
import { NoticeDialog, PlainDialog } from '../../components/dialogs.jsx';
import './admin.css';

const DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];

const DAY_LABELS = {
  MON: 'Thứ Hai',
  TUE: 'Thứ Ba',
  WED: 'Thứ Tư',
  THU: 'Thứ Năm',
  FRI: 'Thứ Sáu',
  SAT: 'Thứ Bảy',
  SUN: 'Chủ Nhật',
};

const PERIOD_LABELS = {
  FULL_DAY: 'Cả ngày',
  MORNING: 'Buổi sáng',
  AFTERNOON: 'Buổi chiều',
};

function normalizeScheduleMap(schedules) {
  const res = {};
  for (const day of DAYS) {
    const list = schedules?.[day] || [];
    res[day] = [...list].sort((a, b) => (a === 'MORNING' ? -1 : 1));
  }
  return res;
}

function areSchedulesEqual(mapA, mapB) {
  if (!mapA || !mapB) return false;
  for (const day of DAYS) {
    const a = mapA[day] || [];
    const b = mapB[day] || [];
    if (a.length !== b.length) return false;
    for (let i = 0; i < a.length; i++) {
      if (a[i] !== b[i]) return false;
    }
  }
  return true;
}

export default function DoctorSchedules() {
  useDocumentTitle('Lịch trực bác sĩ');
  const [params, setParams] = useSearchParams();
  const currentTab = params.get('tab') === 'nghi-phep' ? 'dayOff' : 'weekly';

  const [localSchedules, setLocalSchedules] = useState({});
  const [saveTarget, setSaveTarget] = useState(null); // doctor or 'all'
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState(null);
  const [actionError, setActionError] = useState(null);

  // Dialogs
  const [showAddDayOff, setShowAddDayOff] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  // APIs
  const matrix = useAsync(() => api.get('/admin/doctor-schedules/matrix'), []);
  const dayOffs = useAsync(() => api.get('/admin/doctor-day-offs'), []);

  const doctors = useMemo(() => matrix.data?.doctors || [], [matrix.data]);

  // Đồng bộ local schedules khi server data trả về
  useEffect(() => {
    if (matrix.data?.doctors) {
      const initial = {};
      for (const d of matrix.data.doctors) {
        initial[d.id] = normalizeScheduleMap(d.schedules);
      }
      setLocalSchedules(initial);
    }
  }, [matrix.data]);

  // Kiểm tra những bác sĩ có thay đổi chưa lưu
  const changedDoctorIds = useMemo(() => {
    if (!doctors.length) return [];
    const changed = [];
    for (const d of doctors) {
      const current = localSchedules[d.id];
      const original = normalizeScheduleMap(d.schedules);
      if (!areSchedulesEqual(current, original)) {
        changed.push(d.id);
      }
    }
    return changed;
  }, [doctors, localSchedules]);

  const hasUnsavedChanges = changedDoctorIds.length > 0;

  const handleTabChange = (targetTab) => {
    if (targetTab === currentTab) return;
    if (currentTab === 'weekly' && hasUnsavedChanges) {
      const ok = window.confirm('Cô chú có thay đổi lịch trực tuần chưa lưu. Rời tab sẽ mất các thay đổi này, cô chú có muốn tiếp tục?');
      if (!ok) return;
    }
    setParams({ tab: targetTab === 'dayOff' ? 'nghi-phep' : 'lich-truc' });
    setActionError(null);
  };

  const handleToggleShift = (doctorId, day, period) => {
    setLocalSchedules((prev) => {
      const docSched = prev[doctorId] ? { ...prev[doctorId] } : {};
      const currentPeriods = docSched[day] ? [...docSched[day]] : [];
      const idx = currentPeriods.indexOf(period);
      if (idx >= 0) {
        currentPeriods.splice(idx, 1);
      } else {
        currentPeriods.push(period);
      }
      currentPeriods.sort((a, b) => (a === 'MORNING' ? -1 : 1));
      docSched[day] = currentPeriods;
      return { ...prev, [doctorId]: docSched };
    });
  };

  const handleResetDoctor = (doctorId) => {
    const originalDoc = doctors.find((d) => d.id === doctorId);
    if (!originalDoc) return;
    setLocalSchedules((prev) => ({
      ...prev,
      [doctorId]: normalizeScheduleMap(originalDoc.schedules),
    }));
  };

  const handleResetAll = () => {
    const initial = {};
    for (const d of doctors) {
      initial[d.id] = normalizeScheduleMap(d.schedules);
    }
    setLocalSchedules(initial);
  };

  // Xác nhận lưu
  const handleConfirmSave = async () => {
    if (!saveTarget) return;
    setSaving(true);
    setActionError(null);
    try {
      const targetIds = saveTarget === 'all' ? changedDoctorIds : [saveTarget.id];
      for (const dId of targetIds) {
        const scheduleMap = localSchedules[dId] || {};
        const items = [];
        for (const day of DAYS) {
          const periods = scheduleMap[day] || [];
          items.push({
            dayOfWeek: day,
            period: 'MORNING',
            isActive: periods.includes('MORNING'),
          });
          items.push({
            dayOfWeek: day,
            period: 'AFTERNOON',
            isActive: periods.includes('AFTERNOON'),
          });
        }
        await api.put(`/admin/doctor-schedules/${dId}`, { schedules: items });
      }
      setSaveTarget(null);
      await matrix.reload();
      setNotice('Cập nhật lịch trực tuần thành công.');
    } catch (err) {
      setActionError(err);
    } finally {
      setSaving(false);
    }
  };

  // Xoá ngày nghỉ
  const handleConfirmDeleteDayOff = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    setActionError(null);
    try {
      await api.del(`/admin/doctor-day-offs/${deleteTarget.id}`);
      setDeleteTarget(null);
      await dayOffs.reload();
      await matrix.reload();
      setNotice('Đã huỷ ngày nghỉ phép của bác sĩ.');
    } catch (err) {
      setActionError(err);
    } finally {
      setDeleting(false);
    }
  };

  const today = todayVN();

  return (
    <div className="t-main a-accounts" style={{ paddingBottom: 60 }}>
      <div className="t-head" style={{ marginBottom: 16 }}>
        <div>
          <h1 className="t-h1">Quản lý lịch trực bác sĩ</h1>
          <p className="t-text" style={{ marginTop: 4 }}>
            Phân công ca trực cố định hàng tuần và theo dõi ngày nghỉ đột xuất của đội ngũ bác sĩ.
          </p>
        </div>
      </div>

      {/* Tabs */}
      <div className="ds-tabs" role="tablist" aria-label="Phân hệ lịch trực">
        <button
          type="button"
          role="tab"
          aria-selected={currentTab === 'weekly'}
          className="ds-tab-btn"
          onClick={() => handleTabChange('weekly')}
        >
          Lịch trực tuần cố định
          {hasUnsavedChanges && (
            <span style={{ marginLeft: 8, padding: '2px 8px', borderRadius: 999, background: 'var(--alert-100)', color: 'var(--alert-700)', fontSize: 13, fontWeight: 700 }}>
              {changedDoctorIds.length}
            </span>
          )}
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={currentTab === 'dayOff'}
          className="ds-tab-btn"
          onClick={() => handleTabChange('dayOff')}
        >
          Ngày nghỉ đột xuất
          {dayOffs.data && dayOffs.data.length > 0 && (
            <span style={{ marginLeft: 8, padding: '2px 8px', borderRadius: 999, background: 'var(--surface-200)', color: 'var(--ink-700)', fontSize: 13 }}>
              {dayOffs.data.length}
            </span>
          )}
        </button>
      </div>

      <ErrorAlert error={actionError} />

      {/* ========================================================================= */}
      {/* TAB 1: LỊCH TRỰC TUẦN CỐ ĐỊNH */}
      {/* ========================================================================= */}
      {currentTab === 'weekly' && (
        <>
          {matrix.loading && <Loading label="Đang tải ma trận lịch trực bác sĩ…" />}
          {matrix.error && <ErrorAlert error={matrix.error} />}

          {!matrix.loading && !matrix.error && doctors.length === 0 && (
            <Empty title="Chưa có bác sĩ nào">Chưa có thông tin bác sĩ đang hoạt động trong hệ thống.</Empty>
          )}

          {!matrix.loading && !matrix.error && doctors.length > 0 && (
            <>
              {hasUnsavedChanges && (
                <div className="ds-bar" role="alert">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <Icon name="alert" size={24} style={{ color: 'var(--gold-700)' }} />
                    <span style={{ fontWeight: 600, color: 'var(--ink-900)' }}>
                      Cô chú có thay đổi chưa lưu cho <strong>{changedDoctorIds.length}</strong> bác sĩ.
                    </span>
                  </div>
                  <div style={{ display: 'flex', gap: 10 }}>
                    <button type="button" className="t-btn t-btn-ghost" onClick={handleResetAll}>
                      Hoàn tác thay đổi
                    </button>
                    <button type="button" className="t-btn t-btn-primary" onClick={() => setSaveTarget('all')}>
                      Lưu tất cả thay đổi
                    </button>
                  </div>
                </div>
              )}

              <div className="ds-matrix-wrap">
                <table className="ds-table">
                  <thead>
                    <tr>
                      <th className="ds-th ds-th-doctor ds-sticky-col">Bác sĩ phụ trách</th>
                      {DAYS.map((day) => (
                        <th key={day} className="ds-th">
                          <div>{DAY_LABELS[day]}</div>
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {doctors.map((doc) => {
                      const docSchedules = localSchedules[doc.id] || {};
                      const isChanged = changedDoctorIds.includes(doc.id);
                      return (
                        <tr key={doc.id} style={{ background: isChanged ? 'var(--gold-50, #fffcf4)' : undefined }}>
                          <td className="ds-td ds-sticky-col" style={{ background: isChanged ? 'var(--gold-50, #fffcf4)' : 'var(--surface-card)' }}>
                            <div className="ds-doc-info">
                              <span className="ds-doc-name">{doc.fullName}</span>
                              <span className="ds-doc-meta">{[doc.title, doc.specialty].filter(Boolean).join(' · ')}</span>
                            </div>
                            {isChanged && (
                              <div className="ds-row-actions">
                                <button
                                  type="button"
                                  className="t-btn t-btn-primary"
                                  style={{ padding: '0 12px', minHeight: 36, fontSize: 14 }}
                                  onClick={() => setSaveTarget(doc)}
                                >
                                  Lưu
                                </button>
                                <button
                                  type="button"
                                  className="t-btn t-btn-ghost"
                                  style={{ padding: '0 10px', minHeight: 36, fontSize: 14 }}
                                  onClick={() => handleResetDoctor(doc.id)}
                                >
                                  Hoàn tác
                                </button>
                              </div>
                            )}
                          </td>
                          {DAYS.map((day) => {
                            const periods = docSchedules[day] || [];
                            const hasMorning = periods.includes('MORNING');
                            const hasAfternoon = periods.includes('AFTERNOON');
                            let statusText = 'Nghỉ';
                            let statusClass = 'ds-chip-off';
                            if (hasMorning && hasAfternoon) {
                              statusText = 'Cả ngày';
                              statusClass = 'ds-chip-full';
                            } else if (hasMorning) {
                              statusText = 'Sáng';
                              statusClass = 'ds-chip-part';
                            } else if (hasAfternoon) {
                              statusText = 'Chiều';
                              statusClass = 'ds-chip-part';
                            }

                            return (
                              <td key={day} className="ds-td" style={{ textAlign: 'center' }}>
                                <div className="ds-cell">
                                  <div className="ds-shift-btns">
                                    <button
                                      type="button"
                                      className="ds-shift-btn"
                                      aria-pressed={hasMorning}
                                      aria-label={`Bác sĩ ${doc.fullName} trực ${DAY_LABELS[day]} ca sáng`}
                                      onClick={() => handleToggleShift(doc.id, day, 'MORNING')}
                                    >
                                      Sáng
                                    </button>
                                    <button
                                      type="button"
                                      className="ds-shift-btn"
                                      aria-pressed={hasAfternoon}
                                      aria-label={`Bác sĩ ${doc.fullName} trực ${DAY_LABELS[day]} ca chiều`}
                                      onClick={() => handleToggleShift(doc.id, day, 'AFTERNOON')}
                                    >
                                      Chiều
                                    </button>
                                  </div>
                                  <span className={`ds-chip ${statusClass}`}>{statusText}</span>
                                </div>
                              </td>
                            );
                          })}
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              <div style={{ marginTop: 16, display: 'flex', gap: 20, flexWrap: 'wrap', color: 'var(--ink-500)', fontSize: 15 }}>
                <span>• Bấm nút <strong>Sáng</strong> hoặc <strong>Chiều</strong> để bật/tắt ca trực cho từng ngày.</span>
                <span>• Khi bật cả 2 ca, hệ thống tính là trực <strong>Cả ngày</strong>.</span>
                <span>• Bấm <strong>Lưu thay đổi</strong> để áp dụng lịch vào hệ thống đặt lịch.</span>
              </div>
            </>
          )}
        </>
      )}

      {/* ========================================================================= */}
      {/* TAB 2: NGÀY NGHỈ ĐỘT XUẤT */}
      {/* ========================================================================= */}
      {currentTab === 'dayOff' && (
        <>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20, flexWrap: 'wrap', gap: 12 }}>
            <p className="t-text" style={{ margin: 0 }}>
              Danh sách các ngày nghỉ phép hoặc nghỉ đột xuất từ hôm nay trở đi.
            </p>
            <button
              type="button"
              className="t-btn t-btn-primary"
              style={{ padding: '0 24px', minHeight: 48, fontSize: 16 }}
              onClick={() => setShowAddDayOff(true)}
            >
              <Icon name="calendarPlus" size={20} />
              + Thêm ngày nghỉ phép
            </button>
          </div>

          {dayOffs.loading && <Loading label="Đang tải danh sách ngày nghỉ…" />}
          {dayOffs.error && <ErrorAlert error={dayOffs.error} />}

          {!dayOffs.loading && !dayOffs.error && (!dayOffs.data || dayOffs.data.length === 0) && (
            <Empty title="Chưa có ngày nghỉ nào">Hiện tại không có bác sĩ nào đăng ký nghỉ phép trong các ngày sắp tới.</Empty>
          )}

          {!dayOffs.loading && !dayOffs.error && dayOffs.data && dayOffs.data.length > 0 && (
            <div className="ds-matrix-wrap">
              <table className="ds-table" style={{ minWidth: 800 }}>
                <thead>
                  <tr>
                    <th className="ds-th" style={{ textAlign: 'left', minWidth: 180 }}>Bác sĩ</th>
                    <th className="ds-th" style={{ textAlign: 'left', minWidth: 160 }}>Ngày nghỉ</th>
                    <th className="ds-th" style={{ minWidth: 110 }}>Ca nghỉ</th>
                    <th className="ds-th" style={{ textAlign: 'left' }}>Lý do</th>
                    <th className="ds-th" style={{ minWidth: 140 }}>Ngày đăng ký</th>
                    <th className="ds-th" style={{ minWidth: 100 }}>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {dayOffs.data.map((item) => {
                    const isToday = item.offDate === today;
                    const isSoon = item.offDate > today && item.offDate <= addDays(today, 3);
                    const badgeClass = isToday ? 'ds-badge-today' : isSoon ? 'ds-badge-soon' : 'ds-badge-normal';
                    const badgeLabel = isToday ? 'Hôm nay' : isSoon ? 'Sắp tới' : null;

                    return (
                      <tr key={item.id}>
                        <td className="ds-td">
                          <span style={{ fontWeight: 700, color: 'var(--ink-900)' }}>{item.doctorName}</span>
                        </td>
                        <td className="ds-td">
                          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                              <span style={{ fontWeight: 600 }}>{fmtDate(item.offDate)}</span>
                              {badgeLabel && <span className={badgeClass}>{badgeLabel}</span>}
                            </div>
                            <span style={{ fontSize: 14, color: 'var(--ink-500)' }}>{weekday(item.offDate)}</span>
                          </div>
                        </td>
                        <td className="ds-td" style={{ textAlign: 'center' }}>
                          <span className={`ds-chip ${item.period === 'FULL_DAY' ? 'ds-chip-full' : 'ds-chip-part'}`}>
                            {PERIOD_LABELS[item.period] || item.period}
                          </span>
                        </td>
                        <td className="ds-td">
                          <span style={{ color: item.reason ? 'var(--ink-900)' : 'var(--ink-500)' }}>
                            {item.reason || '—'}
                          </span>
                        </td>
                        <td className="ds-td" style={{ fontSize: 14, color: 'var(--ink-500)', whiteSpace: 'nowrap' }}>
                          {fmtDate(item.createdAt)} {fmtTime(item.createdAt)}
                        </td>
                        <td className="ds-td" style={{ textAlign: 'center' }}>
                          <button
                            type="button"
                            className="t-btn t-btn-ghost"
                            style={{ padding: '0 12px', minHeight: 36, color: 'var(--alert-700)', fontSize: 14 }}
                            onClick={() => setDeleteTarget(item)}
                          >
                            Huỷ nghỉ
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {/* ========================================================================= */}
      {/* DIALOG XÁC NHẬN LƯU LỊCH TRỰC */}
      {/* ========================================================================= */}
      {saveTarget && (
        <ConfirmSaveScheduleDialog
          target={saveTarget}
          doctors={doctors}
          localSchedules={localSchedules}
          saving={saving}
          onClose={() => setSaveTarget(null)}
          onConfirm={handleConfirmSave}
        />
      )}

      {/* ========================================================================= */}
      {/* DIALOG THÊM NGÀY NGHỈ PHÉP */}
      {/* ========================================================================= */}
      {showAddDayOff && (
        <AddDayOffDialog
          doctors={doctors}
          today={today}
          onClose={() => setShowAddDayOff(false)}
          onSuccess={async (res) => {
            setShowAddDayOff(false);
            await dayOffs.reload();
            await matrix.reload();
            if (res.affectedAppointments > 0) {
              setNotice(
                `Đã ghi nhận ngày nghỉ phép thành công. Lưu ý: Có ${res.affectedAppointments} phiếu khám đã đặt trước trong ngày này. Vui lòng kiểm tra lại để điều chuyển hoặc thông báo cho bệnh nhân.`
              );
            } else {
              setNotice('Đã ghi nhận ngày nghỉ phép thành công.');
            }
          }}
        />
      )}

      {/* ========================================================================= */}
      {/* DIALOG XÁC NHẬN HUỶ NGÀY NGHỈ */}
      {/* ========================================================================= */}
      {deleteTarget && (
        <ConfirmDeleteDayOffDialog
          item={deleteTarget}
          deleting={deleting}
          onClose={() => setDeleteTarget(null)}
          onConfirm={handleConfirmDeleteDayOff}
        />
      )}

      {/* ========================================================================= */}
      {/* THÔNG BÁO HOÀN THÀNH */}
      {/* ========================================================================= */}
      {notice && (
        <NoticeDialog onClose={() => setNotice(null)}>
          <p>{notice}</p>
        </NoticeDialog>
      )}
    </div>
  );
}

// -----------------------------------------------------------------------------
// Component: Dialog xác nhận lưu lịch trực tuần
// -----------------------------------------------------------------------------
function ConfirmSaveScheduleDialog({ target, doctors, localSchedules, saving, onClose, onConfirm }) {
  const titleId = useId();
  const targetDoctors = target === 'all'
    ? doctors.filter((d) => localSchedules[d.id])
    : doctors.filter((d) => d.id === target.id);

  return (
    <PlainDialog labelledBy={titleId} width={620} onClose={onClose}>
      <div className="a-modal-head">
        <span className="a-modal-icon">
          <Icon name="calendar" size={30} strokeWidth={1.75} />
        </span>
        <div style={{ minWidth: 0 }}>
          <h2 id={titleId} className="t-h2" style={{ marginBottom: 8 }}>
            Xác nhận lưu lịch trực tuần
          </h2>
          <p className="t-text">
            Cô chú vui lòng kiểm tra lại phân công lịch trực trước khi áp dụng vào hệ thống:
          </p>
        </div>
      </div>

      <div style={{ maxHeight: 360, overflowY: 'auto', padding: '16px 24px', background: 'var(--surface-100)', borderRadius: 12, margin: '16px 24px' }}>
        {targetDoctors.map((doc) => {
          const docSched = localSchedules[doc.id] || {};
          return (
            <div key={doc.id} style={{ marginBottom: 18, borderBottom: '1px solid var(--line-soft)', paddingBottom: 12 }}>
              <div style={{ fontWeight: 700, fontSize: 16, color: 'var(--ink-900)', marginBottom: 6 }}>
                BS. {doc.fullName}
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: '6px 12px', fontSize: 14 }}>
                {DAYS.map((day) => {
                  const periods = docSched[day] || [];
                  const label = periods.length === 2 ? 'Cả ngày' : periods.length === 1 ? (periods[0] === 'MORNING' ? 'Sáng' : 'Chiều') : 'Nghỉ';
                  return (
                    <div key={day} style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <span style={{ color: 'var(--ink-700)' }}>{DAY_LABELS[day]}:</span>
                      <strong style={{ color: label === 'Nghỉ' ? 'var(--ink-500)' : 'var(--green-700)' }}>{label}</strong>
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>

      <div className="a-modal-actions" style={{ padding: '0 24px 20px', display: 'flex', justifyContent: 'flex-end', gap: 12 }}>
        <button type="button" className="t-btn t-btn-ghost" onClick={onClose} disabled={saving}>
          Quay lại
        </button>
        <button type="button" className="t-btn t-btn-primary" onClick={onConfirm} disabled={saving}>
          {saving ? 'Đang lưu…' : 'Xác nhận lưu'}
        </button>
      </div>
    </PlainDialog>
  );
}

// -----------------------------------------------------------------------------
// Component: Dialog thêm ngày nghỉ phép
// -----------------------------------------------------------------------------
function AddDayOffDialog({ doctors, today, onClose, onSuccess }) {
  const titleId = useId();
  const [doctorId, setDoctorId] = useState(doctors[0]?.id || '');
  const [offDate, setOffDate] = useState(today);
  const [period, setPeriod] = useState('FULL_DAY');
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!doctorId) {
      setError(new Error('Vui lòng chọn bác sĩ'));
      return;
    }
    if (!offDate) {
      setError(new Error('Vui lòng chọn ngày nghỉ'));
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const res = await api.post('/admin/doctor-day-offs', {
        doctorId: Number(doctorId),
        offDate,
        period,
        reason: reason.trim() || undefined,
      });
      onSuccess(res);
    } catch (err) {
      setError(err);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <PlainDialog labelledBy={titleId} width={540} onClose={onClose}>
      <form onSubmit={handleSubmit} noValidate>
        <div className="a-modal-head">
          <span className="a-modal-icon">
            <Icon name="calendarPlus" size={30} strokeWidth={1.75} />
          </span>
          <div style={{ minWidth: 0 }}>
            <h2 id={titleId} className="t-h2" style={{ marginBottom: 4 }}>
              Đăng ký ngày nghỉ phép
            </h2>
            <p className="t-text">
              Ghi nhận bác sĩ nghỉ phép đột xuất để hệ thống tự động khoá lịch khám.
            </p>
          </div>
        </div>

        <div style={{ padding: '0 24px 20px', display: 'flex', flexDirection: 'column', gap: 16 }}>
          <ErrorAlert error={error} />

          {/* Bác sĩ */}
          <div className="a-field">
            <label className="a-label" htmlFor="do-doc">
              Bác sĩ nghỉ phép <span style={{ color: 'var(--alert-700)' }}>*</span>
            </label>
            <select
              id="do-doc"
              className="a-input"
              value={doctorId}
              onChange={(e) => setDoctorId(e.target.value)}
              required
            >
              {doctors.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.fullName} {d.specialty ? `(${d.specialty})` : ''}
                </option>
              ))}
            </select>
          </div>

          {/* Ngày nghỉ */}
          <div className="a-field">
            <label className="a-label" htmlFor="do-date">
              Ngày nghỉ <span style={{ color: 'var(--alert-700)' }}>*</span>
            </label>
            <input
              id="do-date"
              type="date"
              className="a-input"
              min={today}
              value={offDate}
              onChange={(e) => setOffDate(e.target.value)}
              required
            />
          </div>

          {/* Ca nghỉ */}
          <div className="a-field">
            <label className="a-label" htmlFor="do-period">
              Ca nghỉ <span style={{ color: 'var(--alert-700)' }}>*</span>
            </label>
            <select
              id="do-period"
              className="a-input"
              value={period}
              onChange={(e) => setPeriod(e.target.value)}
            >
              <option value="FULL_DAY">Cả ngày (Sáng và Chiều)</option>
              <option value="MORNING">Chỉ nghỉ buổi Sáng (08:00 – 12:00)</option>
              <option value="AFTERNOON">Chỉ nghỉ buổi Chiều (13:00 – 16:30)</option>
            </select>
          </div>

          {/* Lý do */}
          <div className="a-field">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <label className="a-label" htmlFor="do-reason">
                Lý do nghỉ (không bắt buộc)
              </label>
              <span style={{ fontSize: 13, color: 'var(--ink-500)' }}>{reason.length}/255</span>
            </div>
            <textarea
              id="do-reason"
              className="a-input"
              rows={3}
              maxLength={255}
              placeholder="Ví dụ: Bác sĩ bận việc gia đình, công tác…"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          </div>
        </div>

        <div className="a-modal-actions" style={{ padding: '0 24px 20px', display: 'flex', justifyContent: 'flex-end', gap: 12 }}>
          <button type="button" className="t-btn t-btn-ghost" onClick={onClose} disabled={submitting}>
            Huỷ
          </button>
          <button type="submit" className="t-btn t-btn-primary" disabled={submitting}>
            {submitting ? 'Đang ghi nhận…' : 'Ghi nhận ngày nghỉ'}
          </button>
        </div>
      </form>
    </PlainDialog>
  );
}

// -----------------------------------------------------------------------------
// Component: Dialog xác nhận huỷ ngày nghỉ
// -----------------------------------------------------------------------------
function ConfirmDeleteDayOffDialog({ item, deleting, onClose, onConfirm }) {
  const titleId = useId();
  return (
    <PlainDialog labelledBy={titleId} width={500} alert onClose={onClose}>
      <div className="a-modal-head">
        <span className="a-modal-icon" style={{ background: 'var(--alert-100)', color: 'var(--alert-700)' }}>
          <Icon name="alert" size={30} strokeWidth={1.75} />
        </span>
        <div style={{ minWidth: 0 }}>
          <h2 id={titleId} className="t-h2" style={{ marginBottom: 8 }}>
            Huỷ ngày nghỉ phép?
          </h2>
          <p className="t-text">
            Cô chú có chắc chắn muốn huỷ ngày nghỉ của bác sĩ{' '}
            <strong>{item.doctorName}</strong> vào ngày <strong>{fmtDate(item.offDate)}</strong> (
            {PERIOD_LABELS[item.period] || item.period}) không?
          </p>
          <p className="t-text" style={{ marginTop: 8, fontSize: 14, color: 'var(--ink-500)' }}>
            Sau khi huỷ, bác sĩ sẽ được tính là có lịch trực lại theo phân công tuần.
          </p>
        </div>
      </div>
      <div className="a-modal-actions" style={{ padding: '16px 24px 20px', display: 'flex', justifyContent: 'flex-end', gap: 12 }}>
        <button type="button" className="t-btn t-btn-ghost" onClick={onClose} disabled={deleting}>
          Quay lại
        </button>
        <button
          type="button"
          className="t-btn t-btn-primary"
          style={{ background: 'var(--alert-700)', borderColor: 'var(--alert-700)' }}
          onClick={onConfirm}
          disabled={deleting}
        >
          {deleting ? 'Đang huỷ…' : 'Xác nhận huỷ'}
        </button>
      </div>
    </PlainDialog>
  );
}
