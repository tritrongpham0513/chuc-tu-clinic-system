import { useCallback, useEffect, useState } from 'react';
import { Link, NavLink, Outlet, matchPath, useLocation, useNavigate } from 'react-router-dom';
import { useDismiss } from '../../shared/lib/hooks.js';
import { isAdmin, isDoctor, useSession } from '../../shared/lib/session.jsx';
import { ROLE_LABELS } from '../../shared/lib/format.js';
import { Icon } from '../../shared/ui/Icon.jsx';
import { api } from '../../shared/lib/api.js';
import { PrintProvider } from './print/PrintProvider.jsx';
import { useStaffNotifications } from './useStaffNotifications.js';
import './print/print.css';
import './staff.css';

const MY_SCHEDULE_NAV = { to: '/noi-bo/lich-cua-toi', label: 'Lịch trực của tôi', icon: 'calendar' };

const STAFF_NAV = [
  { to: '/noi-bo/lich-trong-ngay', label: 'Lịch trong ngày', icon: 'calendar' },
  { to: '/noi-bo/hang-doi', label: 'Hàng đợi', icon: 'queue' },
  { to: '/noi-bo/benh-nhan', label: 'Hồ sơ bệnh nhân', icon: 'users' },
  { to: '/noi-bo/tai-kham', label: 'Cần tái khám', icon: 'refresh' },
];
const ADMIN_NAV = [
  { to: '/noi-bo/quan-tri', label: 'Tổng quan', icon: 'dashboard', end: true },
  { to: '/noi-bo/quan-tri/bao-cao', label: 'Báo cáo', icon: 'file' },
  ...STAFF_NAV,
  { to: '/noi-bo/quan-tri/lich-truc', label: 'Lịch trực bác sĩ', icon: 'calendar' },
  { to: '/noi-bo/quan-tri/bac-si', label: 'Hồ sơ bác sĩ', icon: 'users' },
  { to: '/noi-bo/quan-tri/khung-gio', label: 'Khung giờ khám', icon: 'clock' },
  { to: '/noi-bo/quan-tri/dich-vu', label: 'Dịch vụ khám', icon: 'clipboard' },
  { to: '/noi-bo/quan-tri/ngay-nghi', label: 'Ngày nghỉ', icon: 'calendarPlus' },
  { to: '/noi-bo/quan-tri/thong-bao', label: 'Nhật ký tin nhắn', icon: 'message' },
  { to: '/noi-bo/quan-tri/cau-hinh', label: 'Cấu hình phòng khám', icon: 'settings' },
  { to: '/noi-bo/quan-tri/bai-viet', label: 'Quản lý bài viết', icon: 'news' },
  { to: '/noi-bo/quan-tri/tai-khoan', label: 'Quản lý tài khoản', icon: 'userPlus' },
];

// Trang lập phiếu kết quả và trang soạn bài rộng hết khung, không có cột menu (theo prototype)
const WIDE = ['/noi-bo/phieu-kham/:appointmentId/ket-qua', '/noi-bo/quan-tri/bai-viet/moi', '/noi-bo/quan-tri/bai-viet/:id'];
const EXTRA_WIDE = ['/noi-bo/quan-tri/bai-viet/moi', '/noi-bo/quan-tri/bai-viet/:id'];

export function StaffLayout() {
  const { user, logout } = useSession();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);
  const [badges, setBadges] = useState(null);
  const notifications = useStaffNotifications(user);
  const admin = isAdmin(user);
  const nav = admin
    ? ADMIN_NAV
    : isDoctor(user)
    ? [...STAFF_NAV, MY_SCHEDULE_NAV]
    : STAFF_NAV;
  const wide = WIDE.some((p) => matchPath(p, pathname));
  const extraWide = EXTRA_WIDE.some((p) => matchPath(p, pathname));

  useEffect(() => setMenuOpen(false), [pathname]);

  useEffect(() => {
    api
      .get('/staff/dashboard/badges')
      .then(setBadges)
      .catch(() => {});
  }, [pathname]);

  const handleLogout = async () => {
    await logout();
    navigate('/noi-bo/dang-nhap');
  };

  return (
    <PrintProvider>
      <div className="t-app t-vars">
        <a className="skip-link" href="#noi-dung">
          Bỏ qua, tới nội dung chính
        </a>
        <header className="t-header">
          <div className="t-header-row">
            <Link to={admin ? '/noi-bo/quan-tri' : '/noi-bo/lich-trong-ngay'} className="t-logo" aria-label="Khu nội bộ Phòng khám Chúc Từ">
              <img src={`${import.meta.env.BASE_URL}logo-192.png`} alt="" />
              <span>Chúc Từ</span>
            </Link>
            <span className={`t-role ${admin ? 't-role-admin' : ''}`}>
              <Icon name={admin ? 'shield' : 'user'} size={20} strokeWidth={1.75} />
              {ROLE_LABELS[user?.role] || (admin ? 'Quản trị viên' : 'Tiếp tân')}
            </span>
            {!wide && (
              <button type="button" className="t-burger" aria-expanded={menuOpen} aria-controls="t-side" onClick={() => setMenuOpen((v) => !v)}>
                <Icon name={menuOpen ? 'x' : 'menu'} />
                <span className="t-burger-label">Menu</span>
              </button>
            )}
            <div className="t-user">
              <NotificationBell notifications={notifications} onNavigate={navigate} />
              <UserMenu user={user} onLogout={handleLogout} />
            </div>
          </div>
        </header>
        <section className="t-page">
          {wide ? (
            <main id="noi-dung" className="t-shell-narrow" style={extraWide ? { maxWidth: 1160 } : undefined}>
              <Outlet />
            </main>
          ) : (
            <div className="t-shell">
              <div className="t-aside">
                <nav id="t-side" className={`t-side ${menuOpen ? 'open' : ''}`} aria-label={admin ? 'Chức năng quản trị' : 'Chức năng tiếp tân'}>
                  {nav.map((n) => (
                    <NavLink key={n.to} to={n.to} end={n.end}>
                      <Icon name={n.icon} strokeWidth={1.75} />
                      <span>{n.label}</span>
                      {n.to === '/noi-bo/lich-trong-ngay' && (badges?.doctorChangeRequests || 0) > 0 && (
                        <span
                          style={{
                            marginLeft: 'auto',
                            background: '#DC2626',
                            color: '#FFFFFF',
                            fontSize: '13px',
                            fontWeight: 700,
                            padding: '2px 8px',
                            borderRadius: '999px',
                            minWidth: '22px',
                            textAlign: 'center',
                            lineHeight: '18px',
                          }}
                          aria-label={`${badges.doctorChangeRequests} yêu cầu đổi bác sĩ`}
                        >
                          {badges.doctorChangeRequests}
                        </span>
                      )}
                    </NavLink>
                  ))}
                </nav>
              </div>
              <main id="noi-dung" className="t-main">
                <Outlet />
              </main>
            </div>
          )}
        </section>
      </div>
    </PrintProvider>
  );
}

/** Nút tên người dùng trên thanh đầu, mở menu Thông tin tài khoản và Đăng xuất. */
function UserMenu({ user, onLogout }) {
  const [open, setOpen] = useState(false);
  const close = useCallback(() => setOpen(false), []);
  const ref = useDismiss(open, close);
  const { pathname } = useLocation();
  useEffect(close, [pathname, close]);
  return (
    <div className="t-menu-wrap t-user-menu" ref={ref}>
      <button type="button" className="t-user-btn" aria-haspopup="menu" aria-expanded={open} aria-label={`Tài khoản của ${user.fullName}`} onClick={() => setOpen((v) => !v)}>
        <span className="t-user-avatar">
          <Icon name="user" size={20} strokeWidth={1.75} />
        </span>
        <span className="t-user-name">{user.fullName}</span>
        <Icon name="chevronDown" size={18} strokeWidth={2.25} />
      </button>
      {open && (
        <div role="menu" aria-label={`Tài khoản của ${user.fullName}`} className="t-menu down">
          <NavLink to="/noi-bo/tai-khoan" role="menuitem" className="t-menu-item">
            <Icon name="user" size={22} strokeWidth={1.75} />
            Thông tin tài khoản
          </NavLink>
          <button type="button" role="menuitem" className="t-menu-item danger" onClick={onLogout}>
            <Icon name="logout" size={22} strokeWidth={1.75} />
            Đăng xuất
          </button>
        </div>
      )}
    </div>
  );
}

function formatRelativeTime(at) {
  if (!at) return '';
  const normalized = /[Z+\-]\d{2}:?\d{2}$|Z$/.test(at) ? at : `${at}+07:00`;
  const date = new Date(normalized);
  if (Number.isNaN(date.getTime())) return at;

  const diffSec = Math.max(0, Math.floor((Date.now() - date.getTime()) / 1000));
  if (diffSec < 60) return 'Vừa xong';
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin} phút trước`;
  const diffHour = Math.floor(diffMin / 60);
  if (diffHour < 24) return `${diffHour} giờ trước`;

  const dd = String(date.getDate()).padStart(2, '0');
  const mm = String(date.getMonth() + 1).padStart(2, '0');
  const hh = String(date.getHours()).padStart(2, '0');
  const mi = String(date.getMinutes()).padStart(2, '0');
  return `${dd}/${mm} ${hh}:${mi}`;
}

/** Chuông thông báo lịch khám realtime + hộp thư trên thanh đầu trang. */
function NotificationBell({ notifications, onNavigate }) {
  const { items, unread, loading, error, reload, markRead, markAllRead } = notifications;
  const [open, setOpen] = useState(false);
  const close = useCallback(() => setOpen(false), []);
  const ref = useDismiss(open, close);
  const { pathname } = useLocation();
  useEffect(close, [pathname, close]);

  const handleItemClick = async (item) => {
    setOpen(false);
    await markRead(item.id);
    if (item.link) {
      onNavigate(item.link);
    }
  };

  return (
    <div className="t-notif-wrap" ref={ref}>
      <button
        type="button"
        className="t-notif-btn"
        aria-haspopup="true"
        aria-expanded={open}
        aria-label={`Thông báo${unread > 0 ? `, ${unread} chưa đọc` : ''}`}
        onClick={() => setOpen((v) => !v)}
      >
        <Icon name="bell" size={22} strokeWidth={1.9} />
        {unread > 0 && (
          <span className="t-notif-badge" aria-hidden="true">
            {unread > 99 ? '99+' : unread}
          </span>
        )}
      </button>

      {open && (
        <section className="t-notif-panel" aria-label="Danh sách thông báo">
          <div className="t-notif-head">
            <h2 className="t-notif-heading">Thông báo</h2>
            <div className="t-notif-actions">
              <button
                type="button"
                className="t-notif-action-btn"
                onClick={reload}
                disabled={loading}
              >
                Tải lại
              </button>
              <button
                type="button"
                className="t-notif-action-btn"
                onClick={markAllRead}
                disabled={unread === 0 || loading}
              >
                Đã đọc tất cả
              </button>
            </div>
          </div>

          {error && (
            <div className="t-notif-error" role="alert">
              {error}
            </div>
          )}

          <div className="t-notif-list">
            {loading && items.length === 0 ? (
              <p className="t-notif-empty">Đang tải thông báo...</p>
            ) : items.length === 0 ? (
              <p className="t-notif-empty">Chưa có thông báo nào.</p>
            ) : (
              items.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className={`t-notif-item${item.read ? '' : ' unread'}`}
                  onClick={() => handleItemClick(item)}
                >
                  <div className="t-notif-item-top">
                    <span className="t-notif-item-title">{item.title}</span>
                    {!item.read && <span className="t-notif-new">Mới</span>}
                  </div>
                  <p className="t-notif-item-body">{item.body}</p>
                  <span className="t-notif-item-time">{formatRelativeTime(item.at)}</span>
                </button>
              ))
            )}
          </div>
        </section>
      )}
    </div>
  );
}
