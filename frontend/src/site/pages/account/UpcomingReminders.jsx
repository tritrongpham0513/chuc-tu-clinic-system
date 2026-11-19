import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Icon } from '../../../shared/ui/Icon.jsx';
import { Loading } from '../../../shared/ui/ui.jsx';
import { api } from '../../../shared/lib/api.js';

export default function UpcomingReminders() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchReminders = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get('/me/notifications');
      setItems(res?.items || []);
    } catch (err) {
      setError(err?.message || 'Không thể tải danh sách nhắc lịch. Vui lòng thử lại sau.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReminders();
  }, []);

  return (
    <section className="k-card k-reminders-card" aria-labelledby="reminders-heading">
      <div className="k-card-head">
        <h2 id="reminders-heading" className="k-reminders-title">
          <Icon name="clock" size={26} strokeWidth={2} />
          Nhắc lịch sắp tới
        </h2>
        <p>Thông báo các ca khám bệnh trong 7 ngày tới của cô chú và người thân.</p>
      </div>

      {loading && <Loading message="Đang kiểm tra lịch khám..." />}

      {error && !loading && (
        <div className="k-reminders-error" role="alert">
          <div className="k-reminders-error-text">
            <Icon name="alert" size={24} />
            <span>{typeof error === 'string' ? error : 'Không thể tải danh sách nhắc lịch. Vui lòng thử lại sau.'}</span>
          </div>
          <button
            type="button"
            className="a-btn-outline k-btn-retry"
            onClick={fetchReminders}
          >
            <Icon name="refresh" size={20} />
            Thử lại
          </button>
        </div>
      )}

      {!loading && !error && items.length === 0 && (
        <div className="k-reminders-empty" role="status">
          <Icon name="calendar" size={32} />
          <p>Chưa có lịch nào trong 7 ngày tới.</p>
        </div>
      )}

      {!loading && !error && items.length > 0 && (
        <ul className="k-reminders-list" aria-label="Danh sách nhắc lịch khám">
          {items.map((item, idx) => (
            <li key={item.appointmentId || idx} className="k-reminder-item">
              <div className="k-reminder-info">
                <span className="k-reminder-badge">
                  <Icon name="calendar" size={18} />
                  {item.title || 'Nhắc lịch khám'}
                </span>
                <p className="k-reminder-body">{item.body}</p>
              </div>
              <div className="k-reminder-action">
                <Link
                  to={`/phieu-kham/${item.appointmentId}`}
                  className="a-btn-primary k-btn-view"
                >
                  Xem phiếu
                </Link>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
