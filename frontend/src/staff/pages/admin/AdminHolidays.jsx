import { useCallback, useEffect, useId, useState } from 'react';
import { api } from '../../../shared/lib/api.js';
import { fmtDate, todayVN } from '../../../shared/lib/format.js';
import { Icon } from '../../../shared/ui/Icon.jsx';
import { Empty, Loading } from '../../../shared/ui/ui.jsx';
import { TDialog } from '../../components/dialogs.jsx';

export default function AdminHolidays() {
  const [holidays, setHolidays] = useState([]);
  const [yearFilter, setYearFilter] = useState(new Date().getFullYear());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);

  // Form dialog: null | { mode: 'create' | 'edit', holiday: ... }
  const [formDialog, setFormDialog] = useState(null);
  // Delete confirm dialog: null | holiday
  const [deleteDialog, setDeleteDialog] = useState(null);
  const [deletePending, setDeletePending] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const url = yearFilter ? `/admin/holidays?year=${yearFilter}` : '/admin/holidays';
      const res = await api.get(url);
      setHolidays(Array.isArray(res) ? res : res?.data || []);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [yearFilter]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  useEffect(() => {
    if (notice) {
      const timer = setTimeout(() => setNotice(null), 4000);
      return () => clearTimeout(timer);
    }
  }, [notice]);

  // Toggle nhanh trạng thái "Nghỉ cả ngày" (isClosed)
  const handleToggleClosed = async (holiday) => {
    try {
      const newStatus = !holiday.isClosed;
      await api.patch(`/admin/holidays/${holiday.id}`, { isClosed: newStatus });
      setNotice(`Đã cập nhật trạng thái ngày nghỉ ${fmtDate(holiday.holidayDate)}.`);
      loadData();
    } catch (err) {
      setError(err);
    }
  };

  // Xác nhận xoá ngày nghỉ
  const handleDelete = async () => {
    if (!deleteDialog) return;
    setDeletePending(true);
    try {
      await api.del(`/admin/holidays/${deleteDialog.id}`);
      setNotice(`Đã xoá ngày nghỉ ${deleteDialog.name} (${fmtDate(deleteDialog.holidayDate)}) thành công.`);
      setDeleteDialog(null);
      loadData();
    } catch (err) {
      setError(err);
    } finally {
      setDeletePending(false);
    }
  };

  // Gom nhóm danh sách ngày nghỉ theo tháng
  const groupedByMonth = holidays.reduce((acc, h) => {
    const monthKey = h.holidayDate ? h.holidayDate.substring(0, 7) : 'other'; // "2026-09"
    if (!acc[monthKey]) acc[monthKey] = [];
    acc[monthKey].push(h);
    return acc;
  }, {});

  const monthKeys = Object.keys(groupedByMonth).sort();

  return (
    <div className="db-panel" style={{ minHeight: 600 }}>
      {/* Tiêu đề & Nút Thêm mới */}
      <div className="db-panel-head" style={{ alignItems: 'center', marginBottom: 16 }}>
        <div>
          <h1 className="t-h1" style={{ fontSize: '26px', color: 'var(--ink-900)', margin: 0 }}>
            Ngày nghỉ phòng khám
          </h1>
          <p className="t-muted" style={{ fontSize: '18px', marginTop: 6, color: 'var(--ink-600)' }}>
            Khai báo lịch nghỉ lễ, Tết và ngày nghỉ định kỳ. Hệ thống sẽ chặn người bệnh đặt lịch vào các ngày này.
          </p>
        </div>
        <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          {/* Bộ lọc năm */}
          <select
            className="t-input"
            value={yearFilter}
            onChange={(e) => setYearFilter(Number(e.target.value))}
            style={{ minHeight: 48, fontSize: 18, padding: '0 16px', background: 'var(--surface-card)', color: 'var(--ink-900)', border: '2px solid var(--line-strong)', fontWeight: 600 }}
            aria-label="Chọn năm"
          >
            {[2025, 2026, 2027, 2028].map((y) => (
              <option key={y} value={y}>
                Năm {y}
              </option>
            ))}
          </select>

          <button
            type="button"
            className="t-btn t-btn-primary"
            style={{ minHeight: '48px', padding: '0 24px', fontSize: '18px', fontWeight: 600 }}
            onClick={() => setFormDialog({ mode: 'create' })}
          >
            <Icon name="plus" size={24} />
            <span>Thêm ngày nghỉ</span>
          </button>
        </div>
      </div>

      {/* Thông báo thành công */}
      {notice && (
        <div
          role="status"
          style={{
            background: 'var(--green-100)',
            border: '2px solid var(--green-600)',
            borderRadius: '8px',
            padding: '16px 20px',
            color: 'var(--green-700)',
            fontSize: '18px',
            fontWeight: 600,
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            marginBottom: 20,
          }}
        >
          <Icon name="checkCircle" size={24} />
          <span>{notice}</span>
        </div>
      )}

      {/* 4 Trạng thái bắt buộc */}
      {loading ? (
        <div style={{ padding: '60px 0' }}>
          <Loading label="Đang tải danh sách ngày nghỉ..." />
        </div>
      ) : error ? (
        <div className="empty stack-sm" style={{ alignItems: 'center', padding: '48px 16px' }} role="alert">
          <Icon name="alert" size={48} />
          <h2 className="t-h2" style={{ color: '#991B1B', margin: '8px 0' }}>
            Không thể tải dữ liệu ngày nghỉ
          </h2>
          <p className="t-text" style={{ fontSize: '18px', color: '#5C4838', marginBottom: '20px' }}>
            {error?.message || 'Có lỗi khi kết nối máy chủ. Cô chú vui lòng thử lại.'}
          </p>
          <button
            type="button"
            className="t-btn t-btn-primary"
            style={{ minHeight: '48px', padding: '0 28px', fontSize: '18px' }}
            onClick={loadData}
          >
            <Icon name="refresh" size={22} />
            <span>Thử lại</span>
          </button>
        </div>
      ) : holidays.length === 0 ? (
        <div style={{ padding: '40px 0' }}>
          <Empty icon="calendar" title="Chưa có ngày nghỉ nào">
            <p style={{ fontSize: '18px', color: '#5C4838', marginBottom: '20px' }}>
              Hiện phòng khám chưa có ngày nghỉ nào được khai báo cho năm {yearFilter}.
            </p>
            <button
              type="button"
              className="t-btn t-btn-primary"
              style={{ minHeight: '48px', padding: '0 24px', fontSize: '18px' }}
              onClick={() => setFormDialog({ mode: 'create' })}
            >
              <Icon name="plus" size={24} />
              <span>Thêm ngày nghỉ đầu tiên</span>
            </button>
          </Empty>
        </div>
      ) : (
        /* Trạng thái 4: Thành công - Danh sách theo tháng */
        <div style={{ display: 'flex', flexDirection: 'column', gap: 24, marginTop: 12 }}>
          {monthKeys.map((mKey) => {
            const [y, m] = mKey.split('-');
            const monthHolidays = groupedByMonth[mKey];
            return (
              <div
                key={mKey}
                style={{
                  background: 'var(--surface-card)',
                  borderRadius: '12px',
                  border: '1px solid var(--line-soft)',
                  overflow: 'hidden',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
                }}
              >
                <div
                  style={{
                    background: 'var(--surface-200)',
                    padding: '14px 20px',
                    borderBottom: '2px solid var(--line-soft)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 10,
                  }}
                >
                  <Icon name="calendar" size={22} />
                  <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--ink-900)', margin: 0 }}>
                    Tháng {m}/{y} ({monthHolidays.length} ngày nghỉ)
                  </h2>
                </div>

                <div style={{ overflowX: 'auto' }}>
                  <table
                    className="t-table"
                    style={{ width: '100%', borderCollapse: 'collapse', fontSize: '18px', textAlign: 'left' }}
                  >
                    <thead>
                      <tr style={{ background: 'var(--surface-100)', borderBottom: '1px solid var(--line-soft)' }}>
                        <th style={{ padding: '14px 18px', fontWeight: 700, color: 'var(--ink-800)', width: '180px' }}>
                          Ngày nghỉ
                        </th>
                        <th style={{ padding: '14px 18px', fontWeight: 700, color: 'var(--ink-800)' }}>Tên ngày nghỉ</th>
                        <th style={{ padding: '14px 18px', fontWeight: 700, color: 'var(--ink-800)', textAlign: 'center', width: '180px' }}>
                          Nghỉ cả ngày
                        </th>
                        <th style={{ padding: '14px 18px', fontWeight: 700, color: 'var(--ink-800)', textAlign: 'center', width: '160px' }}>
                          Phiếu khám
                        </th>
                        <th style={{ padding: '14px 18px', fontWeight: 700, color: 'var(--ink-800)', textAlign: 'center', width: '200px' }}>
                          Thao tác
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {monthHolidays.map((h) => {
                        const isPast = h.holidayDate < todayVN();
                        return (
                          <tr
                            key={h.id}
                            style={{
                              borderBottom: '1px solid var(--line-soft)',
                              background: 'var(--surface-card)',
                            }}
                          >
                            <td style={{ padding: '16px 18px', fontWeight: 600, color: 'var(--ink-900)' }}>
                              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                                <span>{fmtDate(h.holidayDate)}</span>
                                {isPast && (
                                  <span
                                    style={{
                                      fontSize: '13px',
                                      padding: '2px 8px',
                                      background: 'var(--surface-200)',
                                      color: 'var(--ink-500)',
                                      borderRadius: '4px',
                                    }}
                                  >
                                    Đã qua
                                  </span>
                                )}
                              </div>
                            </td>
                            <td style={{ padding: '16px 18px', color: 'var(--ink-900)', fontWeight: 600 }}>
                              {h.name}
                            </td>
                            <td style={{ padding: '16px 18px', textAlign: 'center' }}>
                              <button
                                type="button"
                                onClick={() => handleToggleClosed(h)}
                                style={{
                                  display: 'inline-flex',
                                  alignItems: 'center',
                                  gap: 8,
                                  minHeight: '44px',
                                  padding: '6px 14px',
                                  borderRadius: '20px',
                                  border: h.isClosed ? '2px solid var(--alert-700)' : '2px solid var(--line-strong)',
                                  background: h.isClosed ? 'var(--alert-100)' : 'var(--surface-200)',
                                  color: h.isClosed ? 'var(--alert-700)' : 'var(--ink-700)',
                                  fontSize: '16px',
                                  fontWeight: 600,
                                  cursor: 'pointer',
                                }}
                                aria-label={`Đổi trạng thái nghỉ cả ngày cho ${h.name}`}
                              >
                                <Icon name={h.isClosed ? 'check' : 'x'} size={18} />
                                <span>{h.isClosed ? 'Nghỉ cả ngày' : 'Mở cửa'}</span>
                              </button>
                            </td>
                            <td style={{ padding: '16px 18px', textAlign: 'center' }}>
                              {h.appointmentCount > 0 ? (
                                <span
                                  style={{
                                    display: 'inline-block',
                                    padding: '4px 12px',
                                    borderRadius: '12px',
                                    background: 'var(--gold-100)',
                                    color: 'var(--gold-700)',
                                    fontWeight: 700,
                                    fontSize: '16px',
                                  }}
                                >
                                  {h.appointmentCount} phiếu
                                </span>
                              ) : (
                                <span style={{ color: 'var(--ink-500)', fontSize: '16px' }}>0</span>
                              )}
                            </td>
                            <td style={{ padding: '16px 18px', textAlign: 'center' }}>
                              <div style={{ display: 'flex', gap: 8, justifyContent: 'center' }}>
                                <button
                                  type="button"
                                  className="t-btn t-btn-outline"
                                  style={{ minHeight: '44px', padding: '0 14px', fontSize: '16px' }}
                                  onClick={() => setFormDialog({ mode: 'edit', holiday: h })}
                                >
                                  <Icon name="edit" size={18} />
                                  <span>Sửa</span>
                                </button>
                                <button
                                  type="button"
                                  className="t-btn t-btn-outline"
                                  style={{ minHeight: '44px', padding: '0 14px', fontSize: '16px', color: 'var(--alert-700)' }}
                                  onClick={() => setDeleteDialog(h)}
                                >
                                  <Icon name="trash" size={18} />
                                  <span>Xoá</span>
                                </button>
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Dialog Thêm / Sửa ngày nghỉ */}
      {formDialog && (
        <HolidayFormDialog
          mode={formDialog.mode}
          holiday={formDialog.holiday}
          onClose={() => setFormDialog(null)}
          onSuccess={(msg) => {
            setFormDialog(null);
            setNotice(msg);
            loadData();
          }}
        />
      )}

      {/* Dialog Xác nhận xoá */}
      {deleteDialog && (
        <TDialog
          title="Xác nhận xoá ngày nghỉ"
          subtitle={`Ngày ${fmtDate(deleteDialog.holidayDate)} — ${deleteDialog.name}`}
          tone="red"
          width={560}
          onClose={() => !deletePending && setDeleteDialog(null)}
          alert
        >
          <div style={{ padding: '24px', fontSize: '18px', color: 'var(--ink-900)' }}>
            <p style={{ margin: '0 0 16px', lineHeight: 1.6 }}>
              Bạn có chắc chắn muốn xoá ngày nghỉ <strong>{deleteDialog.name}</strong> ({fmtDate(deleteDialog.holidayDate)}) không?
            </p>
            <p style={{ margin: 0, color: 'var(--ink-600)', fontSize: '16px' }}>
              Sau khi xoá, khung giờ của ngày này sẽ mở lại cho người bệnh đặt lịch bình thường.
            </p>
          </div>
          <div className="t-dialog-actions" style={{ padding: '16px 24px', background: 'var(--surface-200)' }}>
            <button
              type="button"
              className="t-btn t-btn-outline"
              style={{ minHeight: 48, fontSize: 18 }}
              onClick={() => setDeleteDialog(null)}
              disabled={deletePending}
            >
              Huỷ bỏ
            </button>
            <button
              type="button"
              className="t-btn t-btn-danger"
              style={{ minHeight: 48, fontSize: 18 }}
              onClick={handleDelete}
              disabled={deletePending}
            >
              {deletePending ? 'Đang xoá...' : 'Xác nhận xoá'}
            </button>
          </div>
        </TDialog>
      )}
    </div>
  );
}

/** Form Hộp thoại Thêm/Sửa ngày nghỉ với Cảnh báo khi có phiếu khám & ngày quá khứ */
function HolidayFormDialog({ mode, holiday, onClose, onSuccess }) {
  const isEdit = mode === 'edit';
  const title = isEdit ? 'Chỉnh sửa ngày nghỉ' : 'Thêm ngày nghỉ mới';

  const [holidayDate, setHolidayDate] = useState(holiday?.holidayDate || '');
  const [name, setName] = useState(holiday?.name || '');
  const [isClosed, setIsClosed] = useState(holiday?.isClosed !== false);

  const [serverError, setServerError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  // State cảnh báo phiếu khám tồn tại trên ngày được chọn
  const [appointmentWarning, setAppointmentWarning] = useState(null); // null | { count, confirmed: boolean }
  const [pastDateWarning, setPastDateWarning] = useState(false);

  const nameId = useId();
  const dateId = useId();

  // Kiểm tra số phiếu khám khi đổi ngày ở chế độ Thêm mới
  const checkDateAppointments = async (dateVal) => {
    if (!dateVal || isEdit) return;
    try {
      const res = await api.get(`/admin/holidays/appointment-count?date=${dateVal}`);
      const count = res?.count || 0;
      if (count > 0) {
        setAppointmentWarning({ count, confirmed: false });
      } else {
        setAppointmentWarning(null);
      }
    } catch {
      // bỏ qua lỗi kiểm tra phụ
    }
  };

  const handleDateChange = (e) => {
    const val = e.target.value;
    setHolidayDate(val);
    setFieldErrors((prev) => ({ ...prev, holidayDate: null }));
    if (val && val < todayVN()) {
      setPastDateWarning(true);
    } else {
      setPastDateWarning(false);
    }
    checkDateAppointments(val);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setServerError(null);

    const errors = {};
    if (!holidayDate && !isEdit) errors.holidayDate = 'Vui lòng chọn ngày nghỉ.';
    if (!name.trim()) errors.name = 'Vui lòng nhập tên ngày nghỉ.';

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    // Nếu ngày này có phiếu khám và admin chưa bấm xác nhận bỏ qua cảnh báo
    if (appointmentWarning && !appointmentWarning.confirmed) {
      return;
    }

    setSubmitting(true);
    try {
      if (isEdit) {
        await api.patch(`/admin/holidays/${holiday.id}`, {
          name: name.trim(),
          isClosed,
        });
        onSuccess(`Đã cập nhật ngày nghỉ ${name.trim()} thành công.`);
      } else {
        await api.post('/admin/holidays', {
          holidayDate,
          name: name.trim(),
          isClosed,
        });
        onSuccess(`Đã thêm ngày nghỉ ${name.trim()} (${fmtDate(holidayDate)}) thành công.`);
      }
    } catch (err) {
      setServerError(err?.message || 'Có lỗi xảy ra khi lưu ngày nghỉ.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <TDialog title={title} tone="green" width={640} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <div style={{ padding: '24px', fontSize: '18px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {/* Lỗi máy chủ */}
          {serverError && (
            <div
              role="alert"
              style={{
                background: '#FEF2F2',
                border: '2px solid #EF4444',
                color: '#991B1B',
                borderRadius: '8px',
                padding: '14px 18px',
                fontSize: '18px',
                fontWeight: 600,
              }}
            >
              {serverError}
            </div>
          )}

          {/* Ô chọn ngày */}
          <div>
            <label htmlFor={dateId} style={{ display: 'block', fontWeight: 700, color: 'var(--ink-900)', marginBottom: 8 }}>
              Ngày nghỉ <span style={{ color: 'var(--alert-700)' }}>*</span>
            </label>
            <input
              id={dateId}
              type="date"
              className="t-input"
              value={holidayDate}
              onChange={handleDateChange}
              disabled={isEdit || submitting}
              style={{
                width: '100%',
                minHeight: '48px',
                fontSize: '18px',
                padding: '0 16px',
                background: isEdit ? 'var(--surface-200)' : 'var(--surface-card)',
                color: 'var(--ink-900)',
                borderColor: fieldErrors.holidayDate ? 'var(--alert-700)' : undefined,
              }}
            />
            {fieldErrors.holidayDate && (
              <p style={{ color: 'var(--alert-700)', fontSize: '16px', marginTop: 4, margin: 0 }}>
                {fieldErrors.holidayDate}
              </p>
            )}
          </div>

          {/* Cảnh báo ngày trong quá khứ */}
          {pastDateWarning && !isEdit && (
            <div
              style={{
                background: 'var(--gold-100)',
                border: '2px solid var(--gold-500)',
                borderRadius: '8px',
                padding: '12px 16px',
                color: 'var(--gold-700)',
                fontSize: '17px',
                display: 'flex',
                alignItems: 'center',
                gap: 10,
              }}
            >
              <Icon name="info" size={22} />
              <span>Ngày này đã qua trong quá khứ. Hệ thống vẫn lưu để ghi nhận lịch sử.</span>
            </div>
          )}

          {/* Cảnh báo khi ngày này đã có phiếu khám */}
          {appointmentWarning && !isEdit && (
            <div
              style={{
                background: 'var(--gold-100)',
                border: '2px solid var(--gold-700)',
                borderRadius: '8px',
                padding: '16px',
                color: 'var(--gold-700)',
                fontSize: '18px',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, fontWeight: 700, marginBottom: 8 }}>
                <Icon name="alert" size={24} />
                <span>Cảnh báo trùng phiếu khám</span>
              </div>
              <p style={{ margin: '0 0 12px', lineHeight: 1.5 }}>
                Ngày này đang có <strong>{appointmentWarning.count} phiếu</strong> khám đã đặt. Bạn vẫn muốn khai báo nghỉ?
              </p>
              <label
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                  cursor: 'pointer',
                  fontWeight: 600,
                  fontSize: '17px',
                  color: 'var(--ink-900)',
                }}
              >
                <input
                  type="checkbox"
                  style={{ width: 22, height: 22 }}
                  checked={appointmentWarning.confirmed}
                  onChange={(e) => setAppointmentWarning({ ...appointmentWarning, confirmed: e.target.checked })}
                />
                <span>Tôi hiểu và vẫn muốn khai báo ngày nghỉ này</span>
              </label>
            </div>
          )}

          {/* Ô nhập tên ngày nghỉ */}
          <div>
            <label htmlFor={nameId} style={{ display: 'block', fontWeight: 700, color: 'var(--ink-900)', marginBottom: 8 }}>
              Tên ngày nghỉ <span style={{ color: 'var(--alert-700)' }}>*</span>
            </label>
            <input
              id={nameId}
              type="text"
              className="t-input"
              placeholder="Ví dụ: Giỗ Tổ Hùng Vương, Quốc khánh 2/9, Nghỉ dọn vệ sinh..."
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                setFieldErrors((prev) => ({ ...prev, name: null }));
              }}
              disabled={submitting}
              style={{
                width: '100%',
                minHeight: '48px',
                fontSize: '18px',
                padding: '0 16px',
                borderColor: fieldErrors.name ? 'var(--alert-700)' : undefined,
              }}
            />
            {fieldErrors.name && (
              <p style={{ color: 'var(--alert-700)', fontSize: '16px', marginTop: 4, margin: 0 }}>
                {fieldErrors.name}
              </p>
            )}
          </div>

          {/* Công tắc Nghỉ cả ngày */}
          <div>
            <label
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                cursor: 'pointer',
                fontWeight: 600,
                color: 'var(--ink-900)',
                fontSize: '18px',
              }}
            >
              <input
                type="checkbox"
                checked={isClosed}
                onChange={(e) => setIsClosed(e.target.checked)}
                style={{ width: 22, height: 22, cursor: 'pointer' }}
                disabled={submitting}
              />
              <span>Nghỉ cả ngày (đóng cửa toàn bộ các khung giờ)</span>
            </label>
            <p style={{ margin: '6px 0 0 34px', color: '#5C4838', fontSize: '16px' }}>
              Khi bật, toàn bộ các khung giờ trong ngày sẽ bị chặn không cho người bệnh đặt lịch.
            </p>
          </div>
        </div>

        {/* Nút hành động */}
        <div className="t-dialog-actions" style={{ padding: '16px 24px', background: '#FAF7F2' }}>
          <button
            type="button"
            className="t-btn t-btn-outline"
            style={{ minHeight: 48, fontSize: 18 }}
            onClick={onClose}
            disabled={submitting}
          >
            Huỷ bỏ
          </button>
          <button
            type="submit"
            className="t-btn t-btn-primary"
            style={{ minHeight: 48, fontSize: 18 }}
            disabled={submitting || (appointmentWarning && !appointmentWarning.confirmed)}
          >
            {submitting ? 'Đang lưu...' : isEdit ? 'Lưu thay đổi' : 'Tạo ngày nghỉ'}
          </button>
        </div>
      </form>
    </TDialog>
  );
}
