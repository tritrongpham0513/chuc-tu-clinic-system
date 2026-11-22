import { useEffect, useState } from 'react';
import { api } from '../../../shared/lib/api.js';
import { fmtDate, fmtTime } from '../../../shared/lib/format.js';
import { TDialog } from '../dialogs.jsx';

export function AppointmentHistoryDialog({ appt, onClose }) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [items, setItems] = useState([]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    api.get(`/staff/appointments/${appt.id}/history`)
      .then((res) => {
        if (!cancelled) {
          setItems(res?.items || []);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err?.message || 'Có lỗi khi tải lịch sử.');
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [appt.id]);

  const formatTimestamp = (isoStr) => {
    if (!isoStr) return '';
    // fmtTime/fmtDate tự hiểu chuỗi ISO "YYYY-MM-DDTHH:mm:ss" theo giờ Việt Nam,
    // không đổi sang giờ máy người dùng (tránh lệch múi giờ).
    return `${fmtTime(isoStr)} ${fmtDate(isoStr)}`;
  };

  return (
    <TDialog title={`Lịch sử phiếu khám: ${appt.ticketCode || appt.bookingCode || ''}`} width={700} onClose={onClose}>
      <div className="t-dialog-body" style={{ padding: '24px 28px', fontSize: '18px', color: '#1F1610' }}>
        <h3 className="t-h3" style={{ fontSize: '20px', marginBottom: '16px', color: '#5C3A21' }}>
          Lịch sử phiếu
        </h3>

        {loading ? (
          <p style={{ fontSize: '18px', color: '#4B5563' }}>Đang tải lịch sử...</p>
        ) : error ? (
          <p style={{ fontSize: '18px', color: '#B91C1C' }}>{error}</p>
        ) : items.length === 0 ? (
          <p style={{ fontSize: '18px', color: '#6B7280' }}>Chưa có thay đổi nào.</p>
        ) : (
          <div
            className="appointment-history-timeline"
            style={{
              display: 'flex',
              flexDirection: 'column',
              gap: '16px',
              borderLeft: '3px solid #78350F',
              paddingLeft: '18px',
              marginLeft: '6px',
            }}
          >
            {items.map((item, idx) => (
              <div
                key={idx}
                style={{
                  backgroundColor: '#FDFBF7',
                  border: '1px solid #E5E7EB',
                  borderRadius: '8px',
                  padding: '16px 20px',
                  fontSize: '18px',
                  lineHeight: 1.5,
                }}
              >
                <div style={{ fontWeight: 600, color: '#1F1610', marginBottom: '6px' }}>
                  <span>{formatTimestamp(item.at)}</span>
                  <span style={{ margin: '0 8px' }}>—</span>
                  <span style={{ color: '#78350F' }}>
                    {item.oldStatusLabel ? `${item.oldStatusLabel} → ` : 'Khởi tạo → '}
                    {item.newStatusLabel}
                  </span>
                </div>
                <div style={{ fontSize: '18px', color: '#374151' }}>
                  Người thực hiện: <strong>{item.actorName || 'Hệ thống'}</strong>
                </div>
                {item.reason && (
                  <div style={{ fontSize: '18px', color: '#4B5563', marginTop: '6px' }}>
                    Lý do: <em>{item.reason}</em>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

        <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '24px' }}>
          <button
            type="button"
            className="t-btn t-btn-ghost"
            style={{ fontSize: '18px', minHeight: '48px', padding: '0 24px' }}
            onClick={onClose}
          >
            Đóng
          </button>
        </div>
      </div>
    </TDialog>
  );
}
