import { useCallback, useEffect, useState } from 'react';
import { api } from '../../../shared/lib/api.js';
import { Icon } from '../../../shared/ui/Icon.jsx';
import { Empty, Loading } from '../../../shared/ui/ui.jsx';

export default function AdminNotifications() {
  const [notifications, setNotifications] = useState([]);
  const [statusFilter, setStatusFilter] = useState(''); // '' (Tất cả), 'FAILED', 'SENT', 'SKIPPED'
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);
  const [resendingId, setResendingId] = useState(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const url = statusFilter ? `/admin/notifications?status=${statusFilter}` : '/admin/notifications';
      const res = await api.get(url);
      setNotifications(Array.isArray(res) ? res : res?.data || []);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  useEffect(() => {
    if (notice) {
      const timer = setTimeout(() => setNotice(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [notice]);

  const handleResend = async (id, bookingCode) => {
    if (resendingId) return;
    setResendingId(id);
    setError(null);
    try {
      const res = await api.post(`/admin/notifications/${id}/resend`);
      const newItem = res?.data || res;
      setNotice(
        `Đã gửi lại thông báo thành công cho phiếu ${bookingCode || '#' + id}! (Trạng thái: ${newItem?.status || 'SENT'})`
      );
      loadData();
    } catch (err) {
      setError(err);
    } finally {
      setResendingId(null);
    }
  };

  const formatDateTime = (dtStr) => {
    if (!dtStr) return '-';
    try {
      const d = new Date(dtStr);
      if (isNaN(d.getTime())) return dtStr;
      const pad = (n) => String(n).padStart(2, '0');
      return `${pad(d.getHours())}:${pad(d.getMinutes())} ${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
    } catch {
      return dtStr;
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'SENT':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 4,
              padding: '4px 10px',
              borderRadius: 6,
              fontSize: '15px',
              fontWeight: 600,
              backgroundColor: '#DCFCE7',
              color: '#15803D',
            }}
          >
            <Icon name="check" size={16} /> Đã gửi
          </span>
        );
      case 'FAILED':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 4,
              padding: '4px 10px',
              borderRadius: 6,
              fontSize: '15px',
              fontWeight: 600,
              backgroundColor: '#FEE2E2',
              color: '#B91C1C',
            }}
          >
            <Icon name="x" size={16} /> Thất bại
          </span>
        );
      case 'SKIPPED':
      default:
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 4,
              padding: '4px 10px',
              borderRadius: 6,
              fontSize: '15px',
              fontWeight: 600,
              backgroundColor: '#F3F4F6',
              color: '#4B5563',
            }}
          >
            <Icon name="clock" size={16} /> Bỏ qua
          </span>
        );
    }
  };

  return (
    <div className="db-panel" style={{ minHeight: 650 }}>
      {/* Tiêu đề & Bộ lọc */}
      <div className="db-panel-head" style={{ alignItems: 'flex-start', marginBottom: 20 }}>
        <div>
          <h1 className="t-h1" style={{ fontSize: '26px', color: 'var(--ink-900)', margin: 0 }}>
            Nhật ký tin nhắn & Thông báo SMS
          </h1>
          <p className="t-muted" style={{ fontSize: '18px', marginTop: 6, color: 'var(--ink-600)' }}>
            Giám sát trạng thái gửi tin SMS qua nhà cung cấp (eSMS / SpeedSMS), xem chi tiết mã lỗi và gửi lại tin nhắn thất bại.
          </p>
        </div>
        <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          {/* Bộ lọc trạng thái */}
          <select
            className="t-input"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            style={{ minWidth: 180, height: 48, fontSize: '16px' }}
            aria-label="Lọc theo trạng thái"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="FAILED">Chỉ tin lỗi (FAILED)</option>
            <option value="SENT">Đã gửi thành công (SENT)</option>
            <option value="SKIPPED">Bỏ qua / Giả lập (SKIPPED)</option>
          </select>

          <button
            type="button"
            className="t-btn t-btn-secondary"
            onClick={loadData}
            disabled={loading}
            style={{ minHeight: 48, padding: '0 16px', display: 'flex', alignItems: 'center', gap: 8, fontSize: '16px' }}
          >
            <Icon name="refresh" size={18} />
            Làm mới
          </button>
        </div>
      </div>

      {/* Thông báo thành công */}
      {notice && (
        <div
          role="status"
          style={{
            marginBottom: 16,
            padding: '14px 18px',
            backgroundColor: '#ECFDF5',
            color: '#065F46',
            border: '1px solid #A7F3D0',
            borderRadius: 8,
            fontSize: '17px',
            display: 'flex',
            alignItems: 'center',
            gap: 10,
          }}
        >
          <Icon name="check" size={20} />
          <span>{notice}</span>
        </div>
      )}

      {/* Thông báo lỗi */}
      {error && (
        <div
          role="alert"
          style={{
            marginBottom: 16,
            padding: '14px 18px',
            backgroundColor: '#FEF2F2',
            color: '#991B1B',
            border: '1px solid #FECACA',
            borderRadius: 8,
            fontSize: '17px',
            display: 'flex',
            alignItems: 'center',
            gap: 10,
          }}
        >
          <Icon name="x" size={20} />
          <span>{error.message || 'Có lỗi xảy ra khi xử lý thông báo'}</span>
        </div>
      )}

      {/* Danh sách nhật ký */}
      {loading ? (
        <Loading />
      ) : notifications.length === 0 ? (
        <Empty title="Chưa có nhật ký thông báo">
          {statusFilter === 'FAILED'
            ? 'Không có tin nhắn nào bị lỗi. Toàn bộ hệ thống SMS đang hoạt động bình thường.'
            : 'Hiện chưa có bản ghi nhật ký thông báo nào phù hợp với bộ lọc.'}
        </Empty>
      ) : (
        <div style={{ overflowX: 'auto', border: '1px solid var(--border-light, #E5E7EB)', borderRadius: 8 }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', minWidth: 900 }}>
            <thead>
              <tr style={{ backgroundColor: '#F9FAFB', borderBottom: '2px solid #E5E7EB', color: '#374151', fontSize: '16px' }}>
                <th style={{ padding: '14px 16px', width: '140px' }}>Ngày giờ</th>
                <th style={{ padding: '14px 16px', width: '180px' }}>Phiếu hẹn</th>
                <th style={{ padding: '14px 16px', width: '160px' }}>Loại thông báo</th>
                <th style={{ padding: '14px 16px', width: '130px' }}>Người nhận</th>
                <th style={{ padding: '14px 16px', width: '90px' }}>Kênh</th>
                <th style={{ padding: '14px 16px', width: '120px' }}>Trạng thái</th>
                <th style={{ padding: '14px 16px' }}>Lỗi / Chi tiết nhà mạng</th>
                <th style={{ padding: '14px 16px', width: '120px', textAlign: 'center' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {notifications.map((item) => {
                const isFailed = item.status === 'FAILED';
                const canResend = item.status === 'FAILED' || item.status === 'SKIPPED';
                const isResending = resendingId === item.id;
                return (
                  <tr
                    key={item.id}
                    style={{
                      borderBottom: '1px solid #E5E7EB',
                      backgroundColor: isFailed ? '#FFFBFB' : '#FFFFFF',
                      fontSize: '16px',
                    }}
                  >
                    <td style={{ padding: '14px 16px', whiteSpace: 'nowrap', color: '#4B5563' }}>
                      {formatDateTime(item.createdAt)}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <div style={{ fontWeight: 600, color: 'var(--ink-900, #1F2937)' }}>
                        {item.bookingCode || '-'}
                      </div>
                      <div style={{ fontSize: '14px', color: '#6B7280', marginTop: 2 }}>
                        {item.patientName || '-'}
                      </div>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <div style={{ fontWeight: 500 }}>{item.templateLabel || item.template}</div>
                      <div style={{ fontSize: '13px', color: '#6B7280' }}>
                        Cổng: {item.provider || 'esms'}
                      </div>
                    </td>
                    <td style={{ padding: '14px 16px', fontFamily: 'monospace', fontWeight: 600, color: '#1F2937' }}>
                      {item.recipientMasked || '-'}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <span style={{ fontWeight: 600, color: '#374151' }}>{item.channel || 'SMS'}</span>
                    </td>
                    <td style={{ padding: '14px 16px' }}>{getStatusBadge(item.status)}</td>
                    <td style={{ padding: '14px 16px' }}>
                      {item.errorMessage ? (
                        <div>
                          <span style={{ color: '#DC2626', fontWeight: 500 }}>{item.errorMessage}</span>
                          {item.errorCode && (
                            <span style={{ fontSize: '13px', color: '#6B7280', marginLeft: 6 }}>
                              (Mã: {item.errorCode})
                            </span>
                          )}
                        </div>
                      ) : item.errorCode ? (
                        <span style={{ color: '#DC2626' }}>Mã lỗi: {item.errorCode}</span>
                      ) : (
                        <span style={{ color: '#9CA3AF' }}>-</span>
                      )}
                    </td>
                    <td style={{ padding: '14px 16px', textAlign: 'center' }}>
                      {canResend && (
                        <button
                          type="button"
                          className={isFailed ? 't-btn t-btn-primary' : 't-btn t-btn-secondary'}
                          onClick={() => handleResend(item.id, item.bookingCode)}
                          disabled={isResending}
                          style={{
                            minHeight: 48,
                            padding: '0 16px',
                            display: 'inline-flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            gap: 6,
                            fontSize: '15px',
                            fontWeight: 600,
                            borderRadius: 6,
                            cursor: isResending ? 'not-allowed' : 'pointer',
                          }}
                          aria-label={`Gửi lại thông báo cho phiếu ${item.bookingCode}`}
                        >
                          <Icon name="refresh" size={16} />
                          {isResending ? 'Đang gửi...' : 'Gửi lại'}
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
