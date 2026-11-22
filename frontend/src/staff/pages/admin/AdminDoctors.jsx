import { useEffect, useId, useState } from 'react';
import { Icon } from '../../../shared/ui/Icon.jsx';
import { Empty, ErrorAlert, Loading } from '../../../shared/ui/ui.jsx';
import { api } from '../../../shared/lib/api.js';
import { useAsync, useDocumentTitle } from '../../../shared/lib/hooks.js';
import { NoticeDialog, PlainDialog } from '../../components/dialogs.jsx';
import { FieldError } from '../../components/fields.jsx';
import './admin.css';

/**
 * Quản lý hồ sơ bác sĩ (Sprint 4 — Ticket 4)
 * Đường dẫn: /noi-bo/quan-tri/bac-si, chỉ dành cho ADMIN.
 * Tuân thủ tiêu chuẩn Elderly-First UX: Chữ >= 18px, Touch Target >= 48px, nhãn rõ ràng, 4 trạng thái bắt buộc.
 */
export default function AdminDoctors() {
  useDocumentTitle('Hồ sơ bác sĩ');

  const [doctors, setDoctors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Dialogs state
  const [formDialog, setFormDialog] = useState(null); // null | { mode: 'create' | 'edit', doctor?: any }
  const [toggleConfirm, setToggleConfirm] = useState(null); // null | { doctor: any, targetActive: boolean }
  const [togglePending, setTogglePending] = useState(false);
  const [toggleError, setToggleError] = useState(null);
  const [notice, setNotice] = useState(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get('/admin/doctors');
      setDoctors(Array.isArray(res) ? res : (res?.data || []));
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleSaved = (savedDoctor, isCreate) => {
    setFormDialog(null);
    setNotice(isCreate ? 'Đã thêm hồ sơ bác sĩ thành công.' : 'Đã cập nhật hồ sơ bác sĩ thành công.');
    loadData();
  };

  const handleToggleActive = async () => {
    if (!toggleConfirm?.doctor) return;
    const { doctor, targetActive } = toggleConfirm;
    setTogglePending(true);
    setToggleError(null);

    try {
      await api.patch(`/admin/doctors/${doctor.id}`, {
        isActive: targetActive,
        updatedAt: doctor.updatedAt,
      });
      setToggleConfirm(null);
      setNotice(targetActive ? `Đã hiện bác sĩ ${doctor.fullName} trên hệ thống.` : `Đã ẩn bác sĩ ${doctor.fullName} thành công.`);
      loadData();
    } catch (err) {
      // Hiển thị nguyên văn lỗi (kể cả 422: "Bác sĩ còn {n} phiếu khám sắp tới, không thể ẩn")
      setToggleError(err?.message || 'Không thể thay đổi trạng thái bác sĩ.');
    } finally {
      setTogglePending(false);
    }
  };

  return (
    <div className="db-panel" style={{ minHeight: 600 }}>
      {/* Tiêu đề trang & Nút Thêm mới */}
      <div className="db-panel-head" style={{ alignItems: 'center', marginBottom: 8 }}>
        <div>
          <h1 className="t-h1" style={{ fontSize: '26px', color: 'var(--ink-900)', margin: 0 }}>
            Hồ sơ bác sĩ
          </h1>
          <p className="t-muted" style={{ fontSize: '18px', marginTop: 6, color: 'var(--ink-600)' }}>
            Quản lý thông tin khám bệnh, chuyên khoa, phân quyền tài khoản và thứ tự hiển thị.
          </p>
        </div>
        <button
          type="button"
          className="t-btn t-btn-primary"
          style={{ minHeight: '48px', padding: '0 24px', fontSize: '18px', fontWeight: 600 }}
          onClick={() => setFormDialog({ mode: 'create' })}
        >
          <Icon name="userPlus" size={24} />
          <span>Thêm bác sĩ</span>
        </button>
      </div>

      {/* 4 Trạng thái bắt buộc: 1. Loading, 2. Lỗi mạng/Server, 3. Rỗng, 4. Thành công */}
      {loading ? (
        <div style={{ padding: '60px 0' }}>
          <Loading label="Đang tải danh sách bác sĩ..." />
        </div>
      ) : error ? (
        <div className="empty stack-sm" style={{ alignItems: 'center', padding: '48px 16px' }} role="alert">
          <Icon name="alert" size={48} />
          <h2 className="t-h2" style={{ color: 'var(--alert-700)', margin: '8px 0' }}>
            Không thể tải dữ liệu bác sĩ
          </h2>
          <p className="t-text" style={{ fontSize: '18px', color: 'var(--ink-600)', marginBottom: '20px' }}>
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
      ) : doctors.length === 0 ? (
        <div style={{ padding: '40px 0' }}>
          <Empty icon="users" title="Chưa có bác sĩ nào">
            <p style={{ fontSize: '18px', color: 'var(--ink-600)', marginBottom: '20px' }}>
              Hiện phòng khám chưa có hồ sơ bác sĩ nào trong hệ thống.
            </p>
            <button
              type="button"
              className="t-btn t-btn-primary"
              style={{ minHeight: '48px', padding: '0 24px', fontSize: '18px' }}
              onClick={() => setFormDialog({ mode: 'create' })}
            >
              <Icon name="userPlus" size={24} />
              <span>Thêm bác sĩ đầu tiên</span>
            </button>
          </Empty>
        </div>
      ) : (
        /* Trạng thái 4: Thành công - Bảng danh sách bác sĩ */
        <div className="t-table-wrap" style={{ overflowX: 'auto', marginTop: 12 }}>
          <table
            className="t-table"
            style={{ width: '100%', borderCollapse: 'collapse', fontSize: '18px', textAlign: 'left' }}
          >
            <thead>
              <tr style={{ background: 'var(--surface-200)', borderBottom: '2px solid var(--line-soft)' }}>
                <th style={{ padding: '16px 14px', fontWeight: 700, color: 'var(--ink-800)' }}>Bác sĩ</th>
                <th style={{ padding: '16px 14px', fontWeight: 700, color: 'var(--ink-800)' }}>Chuyên khoa</th>
                <th style={{ padding: '16px 14px', fontWeight: 700, color: 'var(--ink-800)' }}>Tài khoản</th>
                <th style={{ padding: '16px 14px', fontWeight: 700, color: 'var(--ink-800)', textAlign: 'center' }}>Thứ tự</th>
                <th style={{ padding: '16px 14px', fontWeight: 700, color: 'var(--ink-800)', textAlign: 'center' }}>Đang hoạt động</th>
                <th style={{ padding: '16px 14px', fontWeight: 700, color: 'var(--ink-800)', textAlign: 'center' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {doctors.map((d) => (
                <tr
                  key={d.id}
                  style={{
                    borderBottom: '1px solid var(--line-soft)',
                    background: d.isActive ? 'var(--surface-card)' : 'var(--surface-100)',
                    opacity: d.isActive ? 1 : 0.85,
                  }}
                >
                  {/* Cột 1: Tên, chức danh, SĐT masked */}
                  <td style={{ padding: '16px 14px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                      {d.avatarUrl ? (
                        <img
                          src={d.avatarUrl}
                          alt=""
                          style={{ width: 48, height: 48, borderRadius: '50%', objectFit: 'cover', flexShrink: 0 }}
                        />
                      ) : (
                        <div
                          style={{
                            width: 48,
                            height: 48,
                            borderRadius: '50%',
                            background: 'var(--surface-200)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: 'var(--wood-800)',
                            fontWeight: 700,
                            flexShrink: 0,
                          }}
                        >
                          {d.fullName ? d.fullName.charAt(0) : 'B'}
                        </div>
                      )}
                      <div>
                        <div style={{ fontWeight: 700, color: 'var(--ink-900)', fontSize: '18px' }}>
                          {d.title ? `${d.title} ` : ''}
                          {d.fullName}
                        </div>
                        <div style={{ fontSize: '16px', color: 'var(--ink-500)', marginTop: 2 }}>
                          📞 {d.phone || 'Chưa có SĐT'}
                        </div>
                      </div>
                    </div>
                  </td>

                  {/* Cột 2: Chuyên khoa */}
                  <td style={{ padding: '16px 14px', verticalAlign: 'middle' }}>
                    <div style={{ fontWeight: 600, color: 'var(--green-700)' }}>{d.specialty}</div>
                    {d.experienceYears != null && (
                      <div style={{ fontSize: '16px', color: 'var(--ink-500)', marginTop: 2 }}>
                        {d.experienceYears} năm kinh nghiệm
                      </div>
                    )}
                  </td>

                  {/* Cột 3: Tài khoản liên kết */}
                  <td style={{ padding: '16px 14px', verticalAlign: 'middle' }}>
                    {d.accountId ? (
                      <span
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: 6,
                          padding: '6px 12px',
                          borderRadius: 8,
                          background: 'var(--green-100)',
                          color: 'var(--green-700)',
                          fontWeight: 600,
                          fontSize: '16px',
                        }}
                      >
                        <Icon name="user" size={18} />
                        <span>{d.accountUsername || `ID: ${d.accountId}`}</span>
                      </span>
                    ) : (
                      <span
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: 6,
                          padding: '6px 12px',
                          borderRadius: 8,
                          background: 'var(--surface-200)',
                          color: 'var(--ink-500)',
                          fontSize: '16px',
                        }}
                      >
                        <span>Chưa gán</span>
                      </span>
                    )}
                  </td>

                  {/* Cột 4: Thứ tự hiển thị */}
                  <td style={{ padding: '16px 14px', textAlign: 'center', verticalAlign: 'middle', fontWeight: 600, color: 'var(--ink-900)' }}>
                    {d.sortOrder}
                  </td>

                  {/* Cột 5: Trạng thái Đang hoạt động */}
                  <td style={{ padding: '16px 14px', textAlign: 'center', verticalAlign: 'middle' }}>
                    {d.isActive ? (
                      <span
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: 6,
                          padding: '6px 14px',
                          borderRadius: 20,
                          background: 'var(--green-100)',
                          color: 'var(--green-700)',
                          fontWeight: 700,
                          fontSize: '16px',
                        }}
                      >
                        <Icon name="checkCircle" size={18} />
                        <span>Hoạt động</span>
                      </span>
                    ) : (
                      <span
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: 6,
                          padding: '6px 14px',
                          borderRadius: 20,
                          background: 'var(--alert-100)',
                          color: 'var(--alert-700)',
                          fontWeight: 700,
                          fontSize: '16px',
                        }}
                      >
                        <Icon name="alert" size={18} />
                        <span>Đang ẩn</span>
                      </span>
                    )}
                  </td>

                  {/* Cột 6: Thao tác (Sửa, Ẩn/Hiện) */}
                  <td style={{ padding: '16px 14px', textAlign: 'center', verticalAlign: 'middle' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10 }}>
                      <button
                        type="button"
                        className="t-btn t-btn-ghost"
                        style={{ minHeight: '44px', padding: '0 14px', fontSize: '16px', fontWeight: 600 }}
                        onClick={() => setFormDialog({ mode: 'edit', doctor: d })}
                      >
                        <Icon name="edit" size={20} />
                        <span>Sửa</span>
                      </button>

                      {d.isActive ? (
                        <button
                          type="button"
                          className="t-btn t-btn-ghost"
                          style={{
                            minHeight: '44px',
                            padding: '0 14px',
                            fontSize: '16px',
                            fontWeight: 600,
                            color: 'var(--alert-700)',
                            borderColor: 'var(--alert-700)',
                          }}
                          onClick={() => {
                            setToggleError(null);
                            setToggleConfirm({ doctor: d, targetActive: false });
                          }}
                        >
                          <Icon name="alert" size={20} />
                          <span>Ẩn</span>
                        </button>
                      ) : (
                        <button
                          type="button"
                          className="t-btn t-btn-ghost"
                          style={{
                            minHeight: '44px',
                            padding: '0 14px',
                            fontSize: '16px',
                            fontWeight: 600,
                            color: 'var(--green-700)',
                            borderColor: 'var(--green-700)',
                          }}
                          onClick={() => {
                            setToggleError(null);
                            setToggleConfirm({ doctor: d, targetActive: true });
                          }}
                        >
                          <Icon name="checkCircle" size={20} />
                          <span>Hiện</span>
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

      {/* Hộp thoại Thêm/Sửa Bác sĩ */}
      {formDialog && (
        <DoctorFormDialog
          mode={formDialog.mode}
          doctor={formDialog.doctor}
          onClose={() => setFormDialog(null)}
          onSaved={handleSaved}
        />
      )}

      {/* Hộp thoại Xác nhận Ẩn/Hiện Bác sĩ */}
      {toggleConfirm && (
        <PlainDialog
          labelledBy="toggle-doctor-title"
          width={540}
          alert
          onClose={() => {
            if (!togglePending) setToggleConfirm(null);
          }}
        >
          <div style={{ padding: '24px 28px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 16 }}>
              <span
                style={{
                  width: 44,
                  height: 44,
                  borderRadius: '50%',
                  background: toggleConfirm.targetActive ? 'var(--green-100)' : 'var(--alert-100)',
                  color: toggleConfirm.targetActive ? 'var(--green-700)' : 'var(--alert-700)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                }}
              >
                <Icon name={toggleConfirm.targetActive ? 'checkCircle' : 'alert'} size={28} />
              </span>
              <h2 id="toggle-doctor-title" className="t-h2" style={{ margin: 0, fontSize: '22px', color: 'var(--ink-900)' }}>
                {toggleConfirm.targetActive ? 'Hiện bác sĩ trên hệ thống?' : 'Ẩn hồ sơ bác sĩ?'}
              </h2>
            </div>

            <p style={{ fontSize: '18px', lineHeight: '26px', color: 'var(--ink-700)', marginBottom: 18 }}>
              {toggleConfirm.targetActive ? (
                <>
                  Bác sĩ <strong>{toggleConfirm.doctor.fullName}</strong> sẽ được kích hoạt lại và xuất hiện trên danh
                  sách khám bệnh công khai.
                </>
              ) : (
                <>
                  Bác sĩ <strong>{toggleConfirm.doctor.fullName}</strong> sẽ không còn xuất hiện trên trang đặt lịch công
                  khai và không nhận lượt khám mới.
                </>
              )}
            </p>

            {/* Hiển thị lỗi 422 nguyên văn từ server nếu có vi phạm (ví dụ còn phiếu tương lai) */}
            {toggleError && (
              <div
                style={{
                  padding: '14px 18px',
                  borderRadius: 8,
                  background: 'var(--alert-100)',
                  border: '1px solid var(--alert-700)',
                  color: 'var(--alert-700)',
                  fontSize: '18px',
                  lineHeight: '24px',
                  marginBottom: 18,
                }}
                role="alert"
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontWeight: 700 }}>
                  <Icon name="alert" size={22} />
                  <span>Không thể ẩn bác sĩ</span>
                </div>
                <div style={{ marginTop: 6 }}>{toggleError}</div>
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 24 }}>
              <button
                type="button"
                className="t-btn t-btn-ghost"
                style={{ minHeight: '48px', padding: '0 20px', fontSize: '18px' }}
                disabled={togglePending}
                onClick={() => setToggleConfirm(null)}
              >
                {toggleError ? 'Đóng' : 'Huỷ bỏ'}
              </button>
              <button
                type="button"
                className={`t-btn ${toggleConfirm.targetActive ? 't-btn-primary' : 't-btn-danger'}`}
                style={{
                  minHeight: '48px',
                  padding: '0 24px',
                  fontSize: '18px',
                  fontWeight: 600,
                  background: toggleConfirm.targetActive ? 'var(--green-600)' : 'var(--alert-700)',
                  color: '#FFFFFF',
                }}
                disabled={togglePending}
                onClick={handleToggleActive}
              >
                {togglePending ? 'Đang xử lý...' : toggleConfirm.targetActive ? 'Xác nhận hiện' : 'Xác nhận ẩn'}
              </button>
            </div>
          </div>
        </PlainDialog>
      )}

      {/* Thông báo thao tác thành công */}
      {notice && (
        <NoticeDialog onClose={() => setNotice(null)}>
          <p style={{ fontSize: '18px' }}>{notice}</p>
        </NoticeDialog>
      )}
    </div>
  );
}

/**
 * Hộp thoại biểu mẫu Thêm / Sửa bác sĩ
 */
function DoctorFormDialog({ mode, doctor, onClose, onSaved }) {
  const isCreate = mode === 'create';
  const titleId = useId();

  const [form, setForm] = useState({
    fullName: doctor?.fullName || '',
    title: doctor?.title || '',
    specialty: doctor?.specialty || '',
    experienceYears: doctor?.experienceYears != null ? String(doctor.experienceYears) : '',
    phone: doctor?.phone || '',
    avatarUrl: doctor?.avatarUrl || '',
    sortOrder: doctor?.sortOrder != null ? String(doctor.sortOrder) : '',
    accountId: doctor?.accountId ? String(doctor.accountId) : '',
    bio: doctor?.bio || '',
  });

  const [accounts, setAccounts] = useState([]);
  const [loadingAccounts, setLoadingAccounts] = useState(false);
  const [loadingDetail, setLoadingDetail] = useState(!isCreate);
  const [tried, setTried] = useState(false);
  const [pending, setPending] = useState(false);
  const [serverError, setServerError] = useState(null);
  const [updatedAt, setUpdatedAt] = useState(doctor?.updatedAt);

  // Nếu là sửa, tải chi tiết bác sĩ để lấy bio và phone nguyên bản
  useEffect(() => {
    if (!isCreate && doctor?.id) {
      setLoadingDetail(true);
      api
        .get(`/admin/doctors/${doctor.id}`)
        .then((res) => {
          const d = res?.data || res;
          if (d) {
            setForm({
              fullName: d.fullName || '',
              title: d.title || '',
              specialty: d.specialty || '',
              experienceYears: d.experienceYears != null ? String(d.experienceYears) : '',
              phone: d.phone || '',
              avatarUrl: d.avatarUrl || '',
              sortOrder: d.sortOrder != null ? String(d.sortOrder) : '',
              accountId: d.accountId ? String(d.accountId) : '',
              bio: d.bio || '',
            });
            setUpdatedAt(d.updatedAt);
          }
        })
        .catch((err) => {
          setServerError(err?.message || 'Không tải được chi tiết bác sĩ.');
        })
        .finally(() => setLoadingDetail(false));
    }
  }, [isCreate, doctor]);

  // Tải danh sách tài khoản DOCTOR chưa được gán
  useEffect(() => {
    setLoadingAccounts(true);
    const param = doctor?.id ? `?currentDoctorId=${doctor.id}` : '';
    api
      .get(`/admin/doctors/unassigned-accounts${param}`)
      .then((res) => {
        setAccounts(Array.isArray(res) ? res : (res?.data || []));
      })
      .catch(() => {
        // Fallback: nếu endpoint chưa nạp thì lấy từ danh sách tài khoản
        api.get('/admin/accounts?role=DOCTOR&limit=100').then((res) => {
          const items = res?.items || res?.data?.items || [];
          const list = items.map((u) => ({
            id: u.id,
            username: u.username,
            fullName: u.fullName,
            phone: u.phone,
          }));
          setAccounts(list);
        }).catch(() => {});
      })
      .finally(() => setLoadingAccounts(false));
  }, [doctor]);

  // Kiểm tra lỗi client-side
  const errors = {
    fullName: form.fullName.trim() ? null : 'Vui lòng nhập họ và tên bác sĩ.',
    specialty: form.specialty.trim() ? null : 'Vui lòng nhập chuyên khoa của bác sĩ.',
    phone:
      isCreate || form.phone.trim()
        ? /^(03|05|07|08|09)\d{8}$/.test(form.phone.trim().replace(/\D/g, ''))
          ? null
          : 'Số điện thoại không đúng định dạng Việt Nam (10 chữ số, ví dụ: 0909123456).'
        : null,
  };

  const hasClientError = Object.values(errors).some(Boolean);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setTried(true);
    setServerError(null);

    if (hasClientError) return;

    setPending(true);
    try {
      const payload = {
        fullName: form.fullName.trim(),
        title: form.title.trim() || null,
        specialty: form.specialty.trim(),
        experienceYears: form.experienceYears ? parseInt(form.experienceYears, 10) : null,
        phone: form.phone.trim().replace(/\D/g, '') || null,
        avatarUrl: form.avatarUrl.trim() || null,
        sortOrder: form.sortOrder ? parseInt(form.sortOrder, 10) : null,
        bio: form.bio.trim() || null,
      };

      if (form.accountId) {
        payload.accountId = parseInt(form.accountId, 10);
      } else {
        payload.unassignAccount = true;
      }

      let res;
      if (isCreate) {
        res = await api.post('/admin/doctors', payload);
      } else {
        payload.updatedAt = updatedAt;
        res = await api.patch(`/admin/doctors/${doctor.id}`, payload);
      }

      onSaved(res?.data || res, isCreate);
    } catch (err) {
      if (err.code === 'STALE') {
        setServerError('Dữ liệu bác sĩ đã được người khác cập nhật trước đó. Vui lòng đóng hộp thoại và tải lại.');
      } else {
        setServerError(err?.message || 'Có lỗi khi lưu hồ sơ bác sĩ.');
      }
    } finally {
      setPending(false);
    }
  };

  return (
    <PlainDialog labelledBy={titleId} width={640} onClose={onClose}>
      <form onSubmit={handleSubmit} noValidate style={{ padding: '28px 32px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
          <h2 id={titleId} className="t-h2" style={{ margin: 0, fontSize: '24px', color: 'var(--ink-900)' }}>
            {isCreate ? 'Thêm mới hồ sơ bác sĩ' : `Chỉnh sửa hồ sơ bác sĩ: ${doctor?.fullName}`}
          </h2>
          <button
            type="button"
            className="t-dialog-close"
            style={{ width: 44, height: 44, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}
            onClick={onClose}
            aria-label="Đóng"
          >
            <Icon name="x" size={24} />
          </button>
        </div>

        {serverError && (
          <div
            style={{
              padding: '14px 18px',
              borderRadius: 8,
              background: 'var(--alert-100)',
              border: '1px solid var(--alert-700)',
              color: 'var(--alert-700)',
              fontSize: '18px',
              lineHeight: '24px',
              marginBottom: 18,
            }}
            role="alert"
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontWeight: 700 }}>
              <Icon name="alert" size={22} />
              <span>Thông báo từ hệ thống</span>
            </div>
            <div style={{ marginTop: 6 }}>{serverError}</div>
          </div>
        )}

        {loadingDetail ? (
          <div style={{ padding: '40px 0' }}>
            <Loading label="Đang nạp chi tiết bác sĩ..." />
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
            {/* Họ và tên & Chức danh */}
            <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 16 }}>
              <div className="t-field">
                <label className="t-label" htmlFor="doc-fullname" style={{ fontSize: '18px', fontWeight: 600 }}>
                  Họ và tên bác sĩ <span style={{ color: 'var(--alert-700)' }}>*</span>
                </label>
                <input
                  id="doc-fullname"
                  type="text"
                  className="t-input"
                  style={{ minHeight: '48px', fontSize: '18px' }}
                  placeholder="Ví dụ: Nguyễn Văn A"
                  value={form.fullName}
                  onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                />
                {tried && errors.fullName && <FieldError>{errors.fullName}</FieldError>}
              </div>

              <div className="t-field">
                <label className="t-label" htmlFor="doc-title" style={{ fontSize: '18px', fontWeight: 600 }}>
                  Chức danh
                </label>
                <input
                  id="doc-title"
                  type="text"
                  className="t-input"
                  style={{ minHeight: '48px', fontSize: '18px' }}
                  placeholder="Bác sĩ, Lương y..."
                  value={form.title}
                  onChange={(e) => setForm({ ...form, title: e.target.value })}
                />
              </div>
            </div>

            {/* Chuyên khoa & Số năm kinh nghiệm */}
            <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 16 }}>
              <div className="t-field">
                <label className="t-label" htmlFor="doc-specialty" style={{ fontSize: '18px', fontWeight: 600 }}>
                  Chuyên khoa <span style={{ color: 'var(--alert-700)' }}>*</span>
                </label>
                <input
                  id="doc-specialty"
                  type="text"
                  className="t-input"
                  style={{ minHeight: '48px', fontSize: '18px' }}
                  placeholder="Y học cổ truyền, Châm cứu..."
                  value={form.specialty}
                  onChange={(e) => setForm({ ...form, specialty: e.target.value })}
                />
                {tried && errors.specialty && <FieldError>{errors.specialty}</FieldError>}
              </div>

              <div className="t-field">
                <label className="t-label" htmlFor="doc-exp" style={{ fontSize: '18px', fontWeight: 600 }}>
                  Kinh nghiệm (năm)
                </label>
                <input
                  id="doc-exp"
                  type="number"
                  min="0"
                  max="80"
                  className="t-input"
                  style={{ minHeight: '48px', fontSize: '18px' }}
                  placeholder="10"
                  value={form.experienceYears}
                  onChange={(e) => setForm({ ...form, experienceYears: e.target.value })}
                />
              </div>
            </div>

            {/* Số điện thoại & Thứ tự hiển thị */}
            <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 16 }}>
              <div className="t-field">
                <label className="t-label" htmlFor="doc-phone" style={{ fontSize: '18px', fontWeight: 600 }}>
                  Số điện thoại <span style={{ color: 'var(--alert-700)' }}>*</span>
                </label>
                <input
                  id="doc-phone"
                  type="tel"
                  className="t-input"
                  style={{ minHeight: '48px', fontSize: '18px' }}
                  placeholder="0909123456"
                  value={form.phone}
                  onChange={(e) => setForm({ ...form, phone: e.target.value })}
                />
                {tried && errors.phone && <FieldError>{errors.phone}</FieldError>}
              </div>

              <div className="t-field">
                <label className="t-label" htmlFor="doc-sort" style={{ fontSize: '18px', fontWeight: 600 }}>
                  Thứ tự hiển thị
                </label>
                <input
                  id="doc-sort"
                  type="number"
                  min="0"
                  className="t-input"
                  style={{ minHeight: '48px', fontSize: '18px' }}
                  placeholder="Tự động"
                  value={form.sortOrder}
                  onChange={(e) => setForm({ ...form, sortOrder: e.target.value })}
                />
              </div>
            </div>

            {/* Tài khoản DOCTOR liên kết */}
            <div className="t-field">
              <label className="t-label" htmlFor="doc-account" style={{ fontSize: '18px', fontWeight: 600 }}>
                Tài khoản đăng nhập (chỉ vai trò Bác sĩ)
              </label>
              <select
                id="doc-account"
                className="t-input"
                style={{ minHeight: '48px', fontSize: '18px', background: 'var(--surface-card)', color: 'var(--ink-900)' }}
                value={form.accountId}
                onChange={(e) => setForm({ ...form, accountId: e.target.value })}
                disabled={loadingAccounts}
              >
                <option value="">-- Chưa gán tài khoản --</option>
                {accounts.map((acc) => (
                  <option key={acc.id} value={acc.id}>
                    {acc.username} — {acc.fullName} ({acc.phone || 'SĐT: N/A'})
                  </option>
                ))}
              </select>
              <p className="t-muted" style={{ fontSize: '16px', marginTop: 4, color: 'var(--ink-500)' }}>
                Tuỳ chọn: Cho phép bác sĩ đăng nhập xem danh sách và lập phiếu kết quả khám.
              </p>
            </div>

            {/* Tiểu sử bác sĩ */}
            <div className="t-field">
              <label className="t-label" htmlFor="doc-bio" style={{ fontSize: '18px', fontWeight: 600 }}>
                Tiểu sử / Quá trình công tác
              </label>
              <textarea
                id="doc-bio"
                className="t-input"
                rows={3}
                style={{ fontSize: '18px', padding: '12px', minHeight: 90 }}
                placeholder="Giới thiệu kinh nghiệm, chứng chỉ, chuyên trị..."
                value={form.bio}
                onChange={(e) => setForm({ ...form, bio: e.target.value })}
              />
            </div>

            {/* Ảnh đại diện URL */}
            <div className="t-field">
              <label className="t-label" htmlFor="doc-avatar" style={{ fontSize: '18px', fontWeight: 600 }}>
                Đường dẫn ảnh đại diện (URL)
              </label>
              <input
                id="doc-avatar"
                type="url"
                className="t-input"
                style={{ minHeight: '48px', fontSize: '18px' }}
                placeholder="https://... hoặc /uploads/..."
                value={form.avatarUrl}
                onChange={(e) => setForm({ ...form, avatarUrl: e.target.value })}
              />
            </div>
          </div>
        )}

        {/* Nút hành động */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 14, marginTop: 28 }}>
          <button
            type="button"
            className="t-btn t-btn-ghost"
            style={{ minHeight: '48px', padding: '0 24px', fontSize: '18px' }}
            disabled={pending}
            onClick={onClose}
          >
            Đóng
          </button>
          <button
            type="submit"
            className="t-btn t-btn-primary"
            style={{ minHeight: '48px', padding: '0 32px', fontSize: '18px', fontWeight: 600 }}
            disabled={pending || loadingDetail}
          >
            {pending ? 'Đang lưu...' : isCreate ? 'Thêm bác sĩ' : 'Lưu thay đổi'}
          </button>
        </div>
      </form>
    </PlainDialog>
  );
}
