import { useEffect, useState } from 'react';
import { Icon } from '../../../shared/ui/Icon.jsx';
import { ErrorAlert, Loading } from '../../../shared/ui/ui.jsx';
import { api } from '../../../shared/lib/api.js';
import { TDialog } from '../dialogs.jsx';

/** Lấy số điện thoại thô để tạo đường dẫn sms: và zalo.me/ */
function cleanPhone(raw) {
  if (!raw) return '';
  const digits = String(raw).replace(/\D/g, '');
  if (digits.startsWith('84') && digits.length === 11) {
    return '0' + digits.substring(2);
  }
  return digits;
}

/** Hộp thoại nhắc lịch đơn cho 1 phiếu khám (Ticket 4). */
export function ReminderDialog({ appt, onClose, onDone }) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [preview, setPreview] = useState(null);
  const [copied, setCopied] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    api
      .get(`/staff/appointments/${appt.id}/reminder-preview`)
      .then((res) => {
        if (!active) return;
        setPreview(res);
        setLoading(false);
      })
      .catch((err) => {
        if (!active) return;
        setError(err);
        setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [appt.id]);

  const copyContent = async () => {
    if (!preview?.content) return;
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(preview.content);
      } else {
        // Fallback cho trình duyệt cũ
        const el = document.getElementById('reminder-text');
        if (el) {
          el.select();
          document.execCommand('copy');
        }
      }
      setCopied(true);
      setTimeout(() => setCopied(false), 4000);
    } catch {
      setCopied(false);
    }
  };

  const markSent = async () => {
    setSubmitting(true);
    setError(null);
    try {
      const res = await api.post(`/staff/appointments/${appt.id}/reminder-mark-sent`, {});
      onDone(
        `Đã ghi nhận gửi nhắc lịch cho ${appt.patient?.fullName || appt.ticketCode}.`,
        res.reminderSentAt,
      );
    } catch (err) {
      setError(err);
      setSubmitting(false);
    }
  };

  const phone = cleanPhone(preview?.rawPhone || appt.patient?.phone || appt.bookerPhone);
  const smsHref = preview?.content ? `sms:${phone}?body=${encodeURIComponent(preview.content)}` : `sms:${phone}`;
  const zaloHref = phone ? `https://zalo.me/${phone}` : null;

  return (
    <TDialog
      title="Nhắc lịch khám"
      subtitle={`Phiếu ${appt.ticketCode} — ${appt.patient?.fullName || 'Bệnh nhân'}`}
      onClose={onClose}
      width={680}
    >
      <div className="t-dialog-body" style={{ gap: 16 }}>
        <ErrorAlert error={error} />
        {loading && <Loading message="Đang soạn nội dung nhắc lịch..." />}

        {!loading && preview && (
          <>
            <div className="t-reminder-info">
              <div className="t-reminder-info-row">
                <span className="t-label">Người nhận:</span>
                <strong className="t-reminder-val">{appt.patient?.fullName || 'Bệnh nhân'}</strong>
              </div>
              <div className="t-reminder-info-row">
                <span className="t-label">Số điện thoại:</span>
                <span className="t-reminder-phone">{preview.phone || appt.patient?.phone || 'Chưa có SĐT'}</span>
              </div>
              <div className="t-reminder-info-row">
                <span className="t-label">Giờ khám:</span>
                <span className="t-reminder-val">{appt.slotStart} ({appt.slotDate})</span>
              </div>
            </div>

            <div className="t-field" style={{ gap: 8 }}>
              <label htmlFor="reminder-text" className="t-label" style={{ fontSize: 17, fontWeight: 700 }}>
                Nội dung tin nhắn (tiếng Việt có dấu, không chứa chẩn đoán/đơn thuốc):
              </label>
              <textarea
                id="reminder-text"
                className="t-input t-reminder-textarea"
                readOnly
                rows={5}
                value={preview.content || ''}
                aria-label="Nội dung tin nhắn nhắc lịch"
              />
            </div>

            {copied && (
              <div className="t-copy-feedback" role="status" aria-live="polite">
                <Icon name="checkCircle" size={24} />
                <span>Đã sao chép nội dung tin nhắn vào bộ nhớ tạm!</span>
              </div>
            )}

            <div className="t-reminder-actions">
              <button
                type="button"
                className="t-btn t-reminder-copy-btn t-btn-bold"
                onClick={copyContent}
                aria-label="Sao chép nội dung tin nhắn"
              >
                <Icon name={copied ? 'check' : 'clipboard'} size={22} />
                <span>{copied ? 'Đã sao chép' : 'Sao chép nội dung'}</span>
              </button>

              <div className="t-reminder-links">
                {phone && (
                  <a
                    href={smsHref}
                    className="t-btn t-reminder-link-btn"
                    title="Mở ứng dụng tin nhắn SMS của điện thoại"
                  >
                    <Icon name="chat" size={20} />
                    <span>Mở SMS</span>
                  </a>
                )}
                {zaloHref && (
                  <a
                    href={zaloHref}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="t-btn t-reminder-link-btn"
                    title="Mở ứng dụng Zalo để nhắn tin"
                  >
                    <Icon name="external" size={20} />
                    <span>Mở Zalo</span>
                  </a>
                )}
              </div>
            </div>
          </>
        )}

        <div className="t-dialog-actions t-reminder-footer">
          <button
            type="button"
            className="t-btn t-btn-ghost"
            style={{ minHeight: 48, minWidth: 120 }}
            onClick={onClose}
          >
            Đóng
          </button>
          <button
            type="button"
            className="t-btn t-btn-primary t-btn-bold"
            style={{ minHeight: 48, minWidth: 170 }}
            onClick={markSent}
            disabled={loading || submitting}
          >
            <Icon name="check" size={22} />
            <span>{submitting ? 'Đang lưu...' : '✓ Đã gửi tin nhắc'}</span>
          </button>
        </div>
      </div>
    </TDialog>
  );
}

/** Hộp thoại nhắc lịch hàng loạt cho nhiều phiếu (Ticket 5). */
export function BulkReminderDialog({ ids, onClose, onDone }) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [data, setData] = useState(null);
  const [copiedId, setCopiedId] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    api
      .post('/staff/appointments/reminder-bulk-preview', { ids })
      .then((res) => {
        if (!active) return;
        setData(res);
        setLoading(false);
      })
      .catch((err) => {
        if (!active) return;
        setError(err);
        setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [ids]);

  const copyItem = async (item) => {
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(item.content);
      }
      setCopiedId(item.appointmentId);
      setTimeout(() => setCopiedId((curr) => (curr === item.appointmentId ? null : curr)), 3000);
    } catch {
      setCopiedId(null);
    }
  };

  const markAllSent = async () => {
    setSubmitting(true);
    setError(null);
    try {
      const validIds = (data?.items || []).map((i) => i.appointmentId);
      await api.post('/staff/appointments/reminder-bulk-mark-sent', { ids: validIds });
      onDone(`Đã ghi nhận nhắc lịch thành công cho ${validIds.length} bệnh nhân.`);
    } catch (err) {
      setError(err);
      setSubmitting(false);
    }
  };

  const items = data?.items || [];
  const warnings = data?.warnings || [];

  return (
    <TDialog
      title={`Nhắc lịch hàng loạt (${ids.length} phiếu đã chọn)`}
      subtitle="Sao chép nội dung tin nhắn lần lượt cho từng người rồi bấm Đánh dấu tất cả đã gửi."
      onClose={onClose}
      width={780}
    >
      <div className="t-dialog-body" style={{ gap: 18 }}>
        <ErrorAlert error={error} />
        {loading && <Loading message="Đang soạn danh sách tin nhắn..." />}

        {!loading && (
          <>
            {warnings.length > 0 && (
              <div className="t-bulk-warn" role="status">
                <strong>Lưu ý:</strong>
                <ul style={{ margin: '4px 0 0 18px', padding: 0 }}>
                  {warnings.map((w, idx) => (
                    <li key={idx}>{w}</li>
                  ))}
                </ul>
              </div>
            )}

            <div className="t-bulk-list" style={{ display: 'flex', flexDirection: 'column', gap: 16, maxHeight: '55vh', overflowY: 'auto', paddingRight: 4 }}>
              {items.map((item, index) => {
                const phone = cleanPhone(item.rawPhone || item.phone);
                const smsHref = item.content ? `sms:${phone}?body=${encodeURIComponent(item.content)}` : `sms:${phone}`;
                const zaloHref = phone ? `https://zalo.me/${phone}` : null;
                const isCopied = copiedId === item.appointmentId;

                return (
                  <div key={item.appointmentId} className="t-bulk-card">
                    <div className="t-bulk-card-head">
                      <span className="t-bulk-badge">#{index + 1}</span>
                      <strong>{item.patientName || 'Bệnh nhân'}</strong>
                      <span className="t-td-code">{item.ticketCode}</span>
                      <span className="t-reminder-phone">{item.phone}</span>
                    </div>

                    <textarea
                      className="t-input t-reminder-textarea"
                      readOnly
                      rows={3}
                      value={item.content || ''}
                      aria-label={`Tin nhắn cho ${item.patientName || item.ticketCode}`}
                    />

                    <div className="t-bulk-card-foot">
                      <button
                        type="button"
                        className={`t-btn ${isCopied ? 't-btn-green' : 't-btn-primary'} t-btn-bold`}
                        style={{ minHeight: 46, padding: '0 18px', fontSize: 16 }}
                        onClick={() => copyItem(item)}
                      >
                        <Icon name={isCopied ? 'check' : 'clipboard'} size={20} />
                        {isCopied ? 'Đã sao chép ✓' : 'Sao chép tin này'}
                      </button>

                      <div style={{ display: 'flex', gap: 8 }}>
                        {phone && (
                          <a href={smsHref} className="t-btn t-btn-ghost" style={{ minHeight: 46, padding: '0 14px', fontSize: 16 }}>
                            <Icon name="chat" size={18} />
                            SMS
                          </a>
                        )}
                        {zaloHref && (
                          <a href={zaloHref} target="_blank" rel="noopener noreferrer" className="t-btn t-btn-ghost" style={{ minHeight: 46, padding: '0 14px', fontSize: 16 }}>
                            <Icon name="external" size={18} />
                            Zalo
                          </a>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </>
        )}

        <div className="t-dialog-actions t-reminder-footer" style={{ marginTop: 10 }}>
          <button type="button" className="t-btn t-btn-ghost" style={{ minHeight: 48, minWidth: 120 }} onClick={onClose}>
            Đóng
          </button>
          <button
            type="button"
            className="t-btn t-btn-primary t-btn-bold"
            style={{ minHeight: 48, minWidth: 220 }}
            onClick={markAllSent}
            disabled={loading || submitting || items.length === 0}
          >
            <Icon name="checkCircle" size={22} />
            <span>{submitting ? 'Đang lưu...' : '✓ Đánh dấu tất cả đã gửi'}</span>
          </button>
        </div>
      </div>
    </TDialog>
  );
}
