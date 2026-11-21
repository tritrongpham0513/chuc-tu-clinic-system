import { useCallback, useEffect, useState } from 'react';
import { api } from '../../../shared/lib/api.js';
import { Icon } from '../../../shared/ui/Icon.jsx';
import { Empty, Loading } from '../../../shared/ui/ui.jsx';
import { TDialog } from '../../components/dialogs.jsx';

const PERIOD_LABELS = {
  MORNING: 'Buổi sáng',
  AFTERNOON: 'Buổi chiều',
  FULL_DAY: 'Cả ngày',
};

export default function AdminTimeSlots() {
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);

  // Dialogs state
  const [formDialog, setFormDialog] = useState(null); // null | { mode: 'create' | 'edit', slot: ... }
  const [toggleConfirm, setToggleConfirm] = useState(null); // null | { slot, targetActive }
  const [togglePending, setTogglePending] = useState(false);
  const [toggleError, setToggleError] = useState(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get('/admin/time-slots');
      // res là ApiResponse.data (mảng các AdminTimeSlotResponse)
      setSlots(Array.isArray(res) ? res : res?.data || []);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Tự ẩn thông báo sau 4 giây
  useEffect(() => {
    if (notice) {
      const timer = setTimeout(() => setNotice(null), 4000);
      return () => clearTimeout(timer);
    }
  }, [notice]);

  const handleToggleActive = async () => {
    if (!toggleConfirm?.slot) return;
    const { slot, targetActive } = toggleConfirm;
    setTogglePending(true);
    setToggleError(null);

    try {
      if (targetActive) {
        await api.post(`/admin/time-slots/${slot.id}/activate`);
        setNotice(`Đã kích hoạt khung giờ ${slot.slotLabel} thành công.`);
      } else {
        await api.post(`/admin/time-slots/${slot.id}/deactivate`);
        setNotice(`Đã ngưng hoạt động khung giờ ${slot.slotLabel} thành công.`);
      }
      setToggleConfirm(null);
      loadData();
    } catch (err) {
      // Hiển thị nguyên văn lỗi từ server (422)
      setToggleError(err?.message || 'Không thể thay đổi trạng thái khung giờ.');
    } finally {
      setTogglePending(false);
    }
  };

  return (
    <div className="db-panel" style={{ minHeight: 600 }}>
      {/* Tiêu đề & Nút Thêm mới */}
      <div className="db-panel-head" style={{ alignItems: 'center', marginBottom: 8 }}>
        <div>
          <h1 className="t-h1" style={{ fontSize: '26px', color: '#1F1610', margin: 0 }}>
            Khung giờ khám
          </h1>
          <p className="t-muted" style={{ fontSize: '18px', marginTop: 6, color: '#5C4838' }}>
            Quản lý các khung giờ tiếp nhận bệnh nhân, ca khám, sức chứa và kiểm soát an toàn phiếu hẹn.
          </p>
        </div>
        <button
          type="button"
          className="t-btn t-btn-primary"
          style={{ minHeight: '48px', padding: '0 24px', fontSize: '18px', fontWeight: 600 }}
          onClick={() => setFormDialog({ mode: 'create' })}
        >
          <Icon name="plus" size={24} />
          <span>Thêm khung giờ</span>
        </button>
      </div>

      {/* Thông báo thao tác thành công */}
      {notice && (
        <div
          role="status"
          style={{
            background: '#ECFDF5',
            border: '2px solid #10B981',
            borderRadius: '8px',
            padding: '16px 20px',
            color: '#065F46',
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
          <Loading label="Đang tải danh sách khung giờ..." />
        </div>
      ) : error ? (
        <div className="empty stack-sm" style={{ alignItems: 'center', padding: '48px 16px' }} role="alert">
          <Icon name="alert" size={48} />
          <h2 className="t-h2" style={{ color: '#991B1B', margin: '8px 0' }}>
            Không thể tải dữ liệu khung giờ
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
      ) : slots.length === 0 ? (
        <div style={{ padding: '40px 0' }}>
          <Empty icon="clock" title="Chưa có khung giờ nào">
            <p style={{ fontSize: '18px', color: '#5C4838', marginBottom: '20px' }}>
              Hiện phòng khám chưa có khung giờ khám nào trong hệ thống.
            </p>
            <button
              type="button"
              className="t-btn t-btn-primary"
              style={{ minHeight: '48px', padding: '0 24px', fontSize: '18px' }}
              onClick={() => setFormDialog({ mode: 'create' })}
            >
              <Icon name="plus" size={24} />
              <span>Thêm khung giờ đầu tiên</span>
            </button>
          </Empty>
        </div>
      ) : (
        /* Trạng thái 4: Thành công - Bảng danh sách */
        <div className="t-table-wrap" style={{ overflowX: 'auto', marginTop: 12 }}>
          <table
            className="t-table"
            style={{ width: '100%', borderCollapse: 'collapse', fontSize: '18px', textAlign: 'left' }}
          >
            <thead>
              <tr style={{ background: '#F5EFE6', borderBottom: '2px solid #D5C7B5' }}>
                <th style={{ padding: '16px 14px', fontWeight: 700, color: '#1F1610' }}>Khung giờ</th>
                <th style={{ padding: '16px 14px', fontWeight: 700, color: '#1F1610' }}>Ca khám</th>
                <th style={{ padding: '16px 14px', fontWeight: 700, color: '#1F1610', textAlign: 'center' }}>Sức chứa</th>
                <th style={{ padding: '16px 14px', fontWeight: 700, color: '#1F1610', textAlign: 'center' }}>Phiếu sắp tới</th>
                <th style={{ padding: '16px 14px', fontWeight: 700, color: '#1F1610', textAlign: 'center' }}>Thứ tự</th>
                <th style={{ padding: '16px 14px', fontWeight: 700, color: '#1F1610', textAlign: 'center' }}>Trạng thái</th>
                <th style={{ padding: '16px 14px', fontWeight: 700, color: '#1F1610', textAlign: 'center' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {slots.map((s) => (
                <tr
                  key={s.id}
                  style={{
                    borderBottom: '1px solid #E8DFD5',
                    background: s.isActive ? '#FFFFFF' : '#FAF8F5',
                    opacity: s.isActive ? 1 : 0.85,
                  }}
                >
                  {/* Cột 1: Khung giờ */}
                  <td style={{ padding: '16px 14px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <span
                        style={{
                          width: 10,
                          height: 10,
                          borderRadius: '50%',
                          background: s.isActive ? '#15803D' : '#9CA3AF',
                          flexShrink: 0,
                        }}
                      />
                      <span style={{ fontWeight: 700, color: '#1F1610', fontSize: '19px' }}>
                        {s.slotLabel || `${s.startTime} - ${s.endTime}`}
                      </span>
                    </div>
                  </td>

                  {/* Cột 2: Ca khám */}
                  <td style={{ padding: '16px 14px' }}>
                    <span
                      style={{
                        display: 'inline-block',
                        padding: '4px 12px',
                        borderRadius: '6px',
                        fontSize: '16px',
                        fontWeight: 600,
                        background: s.period === 'MORNING' ? '#FEF3C7' : '#E0E7FF',
                        color: s.period === 'MORNING' ? '#92400E' : '#3730A3',
                      }}
                    >
                      {PERIOD_LABELS[s.period] || s.period}
                    </span>
                  </td>

                  {/* Cột 3: Sức chứa */}
                  <td style={{ padding: '16px 14px', textAlign: 'center' }}>
                    <span style={{ fontWeight: 600, color: '#1F1610' }}>
                      {s.maxPatients} người
                    </span>
                  </td>

                  {/* Cột 4: Phiếu sắp tới */}
                  <td style={{ padding: '16px 14px', textAlign: 'center' }}>
                    <span
                      style={{
                        display: 'inline-block',
                        padding: '4px 12px',
                        borderRadius: '16px',
                        fontSize: '16px',
                        fontWeight: 700,
                        background: s.upcomingAppointmentCount > 0 ? '#FEE2E2' : '#F3F4F6',
                        color: s.upcomingAppointmentCount > 0 ? '#B91C1C' : '#6B7280',
                      }}
                    >
                      {s.upcomingAppointmentCount} phiếu
                    </span>
                  </td>

                  {/* Cột 5: Thứ tự */}
                  <td style={{ padding: '16px 14px', textAlign: 'center', color: '#5C4838' }}>
                    {s.sortOrder}
                  </td>

                  {/* Cột 6: Trạng thái */}
                  <td style={{ padding: '16px 14px', textAlign: 'center' }}>
                    <span
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 6,
                        padding: '6px 12px',
                        borderRadius: '20px',
                        fontSize: '16px',
                        fontWeight: 600,
                        background: s.isActive ? '#DCFCE7' : '#F3F4F6',
                        color: s.isActive ? '#15803D' : '#4B5563',
                      }}
                    >
                      <span
                        style={{
                          width: 8,
                          height: 8,
                          borderRadius: '50%',
                          background: s.isActive ? '#16A34A' : '#9CA3AF',
                        }}
                      />
                      {s.isActive ? 'Đang hoạt động' : 'Đã ngưng'}
                    </span>
                  </td>

                  {/* Cột 7: Thao tác */}
                  <td style={{ padding: '16px 14px', textAlign: 'center' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10 }}>
                      <button
                        type="button"
                        className="t-btn t-btn-ghost"
                        style={{ minHeight: '48px', padding: '0 16px', fontSize: '17px', color: '#5C3A21' }}
                        onClick={() => setFormDialog({ mode: 'edit', slot: s })}
                        aria-label={`Chỉnh sửa khung giờ ${s.slotLabel}`}
                      >
                        <Icon name="edit" size={20} />
                        <span>Sửa</span>
                      </button>

                      {s.isActive ? (
                        <button
                          type="button"
                          className="t-btn t-btn-ghost"
                          style={{ minHeight: '48px', padding: '0 16px', fontSize: '17px', color: '#991B1B' }}
                          onClick={() => {
                            setToggleError(null);
                            setToggleConfirm({ slot: s, targetActive: false });
                          }}
                          aria-label={`Ngưng khung giờ ${s.slotLabel}`}
                        >
                          <Icon name="xCircle" size={20} />
                          <span>Ngưng</span>
                        </button>
                      ) : (
                        <button
                          type="button"
                          className="t-btn t-btn-ghost"
                          style={{ minHeight: '48px', padding: '0 16px', fontSize: '17px', color: '#15803D' }}
                          onClick={() => {
                            setToggleError(null);
                            setToggleConfirm({ slot: s, targetActive: true });
                          }}
                          aria-label={`Kích hoạt khung giờ ${s.slotLabel}`}
                        >
                          <Icon name="checkCircle" size={20} />
                          <span>Kích hoạt</span>
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Modal Thêm / Sửa khung giờ */}
      {formDialog && (
        <TimeSlotFormDialog
          mode={formDialog.mode}
          slot={formDialog.slot}
          onClose={() => setFormDialog(null)}
          onSuccess={(savedSlot, isEdit) => {
            setFormDialog(null);
            setNotice(isEdit ? `Đã cập nhật khung giờ ${savedSlot.slotLabel}.` : `Đã thêm khung giờ mới ${savedSlot.slotLabel}.`);
            loadData();
          }}
        />
      )}

      {/* Modal Xác nhận Ngưng / Kích hoạt */}
      {toggleConfirm && (
        <TDialog
          title={toggleConfirm.targetActive ? 'Kích hoạt khung giờ' : 'Ngưng hoạt động khung giờ'}
          subtitle={toggleConfirm.slot.slotLabel}
          width={580}
          onClose={() => !togglePending && setToggleConfirm(null)}
        >
          <div className="t-dialog-body" style={{ gap: 20 }}>
            {toggleConfirm.targetActive ? (
              <p className="t-text" style={{ fontSize: '18px', color: '#1F1610', margin: 0 }}>
                Bạn có chắc chắn muốn kích hoạt lại khung giờ{' '}
                <strong style={{ color: '#15803D' }}>{toggleConfirm.slot.slotLabel}</strong>? Khung giờ sẽ hiển thị cho bệnh nhân đặt lịch trở lại.
              </p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                <p className="t-text" style={{ fontSize: '18px', color: '#1F1610', margin: 0 }}>
                  Bạn có chắc chắn muốn ngưng hoạt động khung giờ{' '}
                  <strong style={{ color: '#991B1B' }}>{toggleConfirm.slot.slotLabel}</strong>?
                </p>
                {toggleConfirm.slot.upcomingAppointmentCount > 0 ? (
                  <div
                    style={{
                      background: '#FEF2F2',
                      border: '1px solid #F87171',
                      borderRadius: '8px',
                      padding: '14px',
                      color: '#991B1B',
                      fontSize: '17px',
                    }}
                  >
                    <strong>Cảnh báo:</strong> Khung giờ này hiện đang có{' '}
                    <strong>{toggleConfirm.slot.upcomingAppointmentCount} phiếu khám sắp tới</strong>.
                    Hệ thống sẽ từ chối ngưng hoạt động cho đến khi các phiếu này được xử lý hoặc dời sang khung khác.
                  </div>
                ) : (
                  <p className="t-muted" style={{ fontSize: '17px', margin: 0 }}>
                    Khung giờ hiện không có phiếu khám nào sắp tới. Ngưng hoạt động sẽ ẩn khung này khỏi trang đặt lịch.
                  </p>
                )}
              </div>
            )}

            {/* Hiển thị lỗi 422 nguyên văn từ server */}
            {toggleError && (
              <div
                role="alert"
                style={{
                  background: '#FEF2F2',
                  border: '1px solid #DC2626',
                  borderRadius: '8px',
                  padding: '14px 18px',
                  color: '#B91C1C',
                  fontSize: '17px',
                  fontWeight: 600,
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                }}
              >
                <Icon name="alert" size={24} />
                <span>{toggleError}</span>
              </div>
            )}

            <div className="t-actions" style={{ justifyContent: 'flex-end', gap: 14, marginTop: 10 }}>
              <button
                type="button"
                className="t-btn t-btn-ghost"
                style={{ minHeight: '48px', padding: '0 24px', fontSize: '18px' }}
                disabled={togglePending}
                onClick={() => setToggleConfirm(null)}
              >
                Huỷ bỏ
              </button>
              <button
                type="button"
                className={`t-btn ${toggleConfirm.targetActive ? 't-btn-primary' : 't-btn-danger'}`}
                style={{ minHeight: '48px', padding: '0 28px', fontSize: '18px', fontWeight: 600 }}
                disabled={togglePending}
                onClick={handleToggleActive}
              >
                {togglePending ? 'Đang xử lý...' : toggleConfirm.targetActive ? 'Kích hoạt ngay' : 'Xác nhận ngưng'}
              </button>
            </div>
          </div>
        </TDialog>
      )}
    </div>
  );
}

/** Form Thêm mới / Cập nhật khung giờ với Client-side Validation khớp Server */
function TimeSlotFormDialog({ mode, slot, onClose, onSuccess }) {
  const isEdit = mode === 'edit';
  const [startTime, setStartTime] = useState(slot?.startTime ? slot.startTime.substring(0, 5) : '08:00');
  const [endTime, setEndTime] = useState(slot?.endTime ? slot.endTime.substring(0, 5) : '09:00');
  const [period, setPeriod] = useState(slot?.period || 'MORNING');
  const [maxPatients, setMaxPatients] = useState(slot?.maxPatients ?? 5);
  const [sortOrder, setSortOrder] = useState(slot?.sortOrder ?? 0);
  const [slotLabel, setSlotLabel] = useState(slot?.slotLabel || '');
  const [isActive, setIsActive] = useState(slot?.isActive ?? true);

  const [fieldErrors, setFieldErrors] = useState({});
  const [serverError, setServerError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Client validation
  const validate = () => {
    const errors = {};
    if (!startTime) {
      errors.startTime = 'Giờ bắt đầu không được để trống';
    }
    if (!endTime) {
      errors.endTime = 'Giờ kết thúc không được để trống';
    }

    if (startTime && endTime) {
      if (endTime <= startTime) {
        errors.endTime = 'Giờ kết thúc phải sau giờ bắt đầu';
      }
    }

    if (startTime && period) {
      if (period === 'MORNING' && startTime >= '12:00') {
        errors.period = 'Khung giờ sáng phải bắt đầu trước 12:00';
      } else if (period === 'AFTERNOON' && startTime < '12:00') {
        errors.period = 'Khung giờ chiều phải bắt đầu từ 12:00 trở đi';
      }
    }

    const num = Number(maxPatients);
    if (!maxPatients || isNaN(num) || num < 1 || num > 50) {
      errors.maxPatients = 'Sức chứa mỗi khung giờ phải từ 1 đến 50 bệnh nhân';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setSubmitting(true);
    setServerError(null);

    const payload = {
      startTime,
      endTime,
      period,
      maxPatients: Number(maxPatients),
      sortOrder: Number(sortOrder) || 0,
      isActive,
      slotLabel: slotLabel.trim() || undefined,
    };

    try {
      let saved;
      if (isEdit) {
        saved = await api.patch(`/admin/time-slots/${slot.id}`, payload);
      } else {
        saved = await api.post('/admin/time-slots', payload);
      }
      onSuccess(saved?.data || saved, isEdit);
    } catch (err) {
      // Hiển thị nguyên văn lỗi từ backend (422)
      setServerError(err?.message || 'Có lỗi khi lưu khung giờ. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  const hasUpcoming = isEdit && (slot?.upcomingAppointmentCount > 0);

  return (
    <TDialog
      title={isEdit ? 'Chỉnh sửa khung giờ' : 'Thêm khung giờ khám mới'}
      subtitle={isEdit ? `ID: ${slot.id} · ${slot.slotLabel}` : 'Tạo mới khung tiếp nhận bệnh nhân'}
      width={680}
      onClose={() => !submitting && onClose()}
    >
      <form onSubmit={handleSubmit} className="t-dialog-body" style={{ gap: 20 }}>
        {/* Cảnh báo khi sửa khung giờ có phiếu tương lai */}
        {hasUpcoming && (
          <div
            style={{
              background: '#FFFBEB',
              border: '1px solid #F59E0B',
              borderRadius: '8px',
              padding: '14px 18px',
              color: '#92400E',
              fontSize: '17px',
            }}
          >
            <strong>Lưu ý an toàn:</strong> Khung này đang có{' '}
            <strong>{slot.upcomingAppointmentCount} phiếu khám sắp tới</strong>. Sửa giờ bắt đầu hoặc giờ kết thúc sẽ bị từ chối để bảo vệ lịch hẹn của người bệnh. Nếu muốn đổi giờ khám, hãy tạo khung mới và ngưng khung cũ.
          </div>
        )}

        {/* Lỗi chung từ Server (422 / 409 ...) */}
        {serverError && (
          <div
            role="alert"
            style={{
              background: '#FEF2F2',
              border: '1px solid #DC2626',
              borderRadius: '8px',
              padding: '14px 18px',
              color: '#B91C1C',
              fontSize: '17px',
              fontWeight: 600,
              display: 'flex',
              alignItems: 'center',
              gap: 10,
            }}
          >
            <Icon name="alert" size={24} />
            <span>{serverError}</span>
          </div>
        )}

        {/* Grid 2 cột: Giờ bắt đầu & Giờ kết thúc */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
          <div className="t-field">
            <label htmlFor="slot-start" className="t-label" style={{ fontSize: '18px' }}>
              Giờ bắt đầu <span className="t-req">*</span>
            </label>
            <input
              id="slot-start"
              type="time"
              className="t-input"
              style={{ minHeight: '48px', fontSize: '18px' }}
              value={startTime}
              onChange={(e) => {
                setStartTime(e.target.value);
                if (fieldErrors.startTime || fieldErrors.endTime || fieldErrors.period) {
                  setFieldErrors((prev) => ({ ...prev, startTime: null, endTime: null, period: null }));
                }
              }}
            />
            {fieldErrors.startTime && (
              <span className="t-field-error" style={{ color: '#DC2626', fontSize: '15px', marginTop: 4 }}>
                {fieldErrors.startTime}
              </span>
            )}
          </div>

          <div className="t-field">
            <label htmlFor="slot-end" className="t-label" style={{ fontSize: '18px' }}>
              Giờ kết thúc <span className="t-req">*</span>
            </label>
            <input
              id="slot-end"
              type="time"
              className="t-input"
              style={{ minHeight: '48px', fontSize: '18px' }}
              value={endTime}
              onChange={(e) => {
                setEndTime(e.target.value);
                if (fieldErrors.endTime) {
                  setFieldErrors((prev) => ({ ...prev, endTime: null }));
                }
              }}
            />
            {fieldErrors.endTime && (
              <span className="t-field-error" style={{ color: '#DC2626', fontSize: '15px', marginTop: 4 }}>
                {fieldErrors.endTime}
              </span>
            )}
          </div>
        </div>

        {/* Grid 2 cột: Ca khám & Sức chứa */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
          <div className="t-field">
            <label htmlFor="slot-period" className="t-label" style={{ fontSize: '18px' }}>
              Ca khám <span className="t-req">*</span>
            </label>
            <select
              id="slot-period"
              className="t-input"
              style={{ minHeight: '48px', fontSize: '18px' }}
              value={period}
              onChange={(e) => {
                setPeriod(e.target.value);
                if (fieldErrors.period) {
                  setFieldErrors((prev) => ({ ...prev, period: null }));
                }
              }}
            >
              <option value="MORNING">Buổi sáng (trước 12:00)</option>
              <option value="AFTERNOON">Buổi chiều (từ 12:00 trở đi)</option>
            </select>
            {fieldErrors.period && (
              <span className="t-field-error" style={{ color: '#DC2626', fontSize: '15px', marginTop: 4 }}>
                {fieldErrors.period}
              </span>
            )}
          </div>

          <div className="t-field">
            <label htmlFor="slot-max" className="t-label" style={{ fontSize: '18px' }}>
              Sức chứa tối đa (bệnh nhân) <span className="t-req">*</span>
            </label>
            <input
              id="slot-max"
              type="number"
              min={1}
              max={50}
              className="t-input"
              style={{ minHeight: '48px', fontSize: '18px' }}
              value={maxPatients}
              onChange={(e) => {
                setMaxPatients(e.target.value);
                if (fieldErrors.maxPatients) {
                  setFieldErrors((prev) => ({ ...prev, maxPatients: null }));
                }
              }}
            />
            {fieldErrors.maxPatients && (
              <span className="t-field-error" style={{ color: '#DC2626', fontSize: '15px', marginTop: 4 }}>
                {fieldErrors.maxPatients}
              </span>
            )}
          </div>
        </div>

        {/* Grid 2 cột: Thứ tự hiển thị & Nhãn hiển thị */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
          <div className="t-field">
            <label htmlFor="slot-order" className="t-label" style={{ fontSize: '18px' }}>
              Thứ tự hiển thị
            </label>
            <input
              id="slot-order"
              type="number"
              min={0}
              className="t-input"
              style={{ minHeight: '48px', fontSize: '18px' }}
              placeholder="0 (tự động điền tiếp theo)"
              value={sortOrder}
              onChange={(e) => setSortOrder(e.target.value)}
            />
            <span className="t-muted" style={{ fontSize: '15px', marginTop: 4 }}>
              Số nhỏ hơn sẽ đứng trước trong danh sách chọn.
            </span>
          </div>

          <div className="t-field">
            <label htmlFor="slot-label" className="t-label" style={{ fontSize: '18px' }}>
              Tên hiển thị (tuỳ chọn)
            </label>
            <input
              id="slot-label"
              type="text"
              className="t-input"
              style={{ minHeight: '48px', fontSize: '18px' }}
              placeholder={`${startTime || '08:00'} - ${endTime || '09:00'}`}
              value={slotLabel}
              onChange={(e) => setSlotLabel(e.target.value)}
            />
            <span className="t-muted" style={{ fontSize: '15px', marginTop: 4 }}>
              Để trống sẽ tự động lấy theo giờ bắt đầu - kết thúc.
            </span>
          </div>
        </div>

        {/* Trạng thái hoạt động */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginTop: 4 }}>
          <input
            id="slot-active"
            type="checkbox"
            style={{ width: 24, height: 24, cursor: 'pointer' }}
            checked={isActive}
            onChange={(e) => setIsActive(e.target.checked)}
          />
          <label htmlFor="slot-active" style={{ fontSize: '18px', fontWeight: 600, color: '#1F1610', cursor: 'pointer' }}>
            Đang hoạt động (cho phép tiếp nhận đặt lịch)
          </label>
        </div>

        {/* Nút hành động */}
        <div className="t-actions" style={{ justifyContent: 'flex-end', gap: 14, marginTop: 16 }}>
          <button
            type="button"
            className="t-btn t-btn-ghost"
            style={{ minHeight: '48px', padding: '0 24px', fontSize: '18px' }}
            disabled={submitting}
            onClick={onClose}
          >
            Huỷ bỏ
          </button>
          <button
            type="submit"
            className="t-btn t-btn-primary"
            style={{ minHeight: '48px', padding: '0 32px', fontSize: '18px', fontWeight: 600 }}
            disabled={submitting}
          >
            {submitting ? 'Đang lưu...' : isEdit ? 'Lưu thay đổi' : 'Tạo khung giờ'}
          </button>
        </div>
      </form>
    </TDialog>
  );
}
