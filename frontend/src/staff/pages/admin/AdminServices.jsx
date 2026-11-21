import { useCallback, useEffect, useId, useState } from 'react';
import { api } from '../../../shared/lib/api.js';
import { Icon } from '../../../shared/ui/Icon.jsx';
import { Empty, Loading } from '../../../shared/ui/ui.jsx';
import { TDialog } from '../../components/dialogs.jsx';

/**
 * Chuẩn hoá chuỗi tiếng Việt thành slug URL an toàn
 */
function toSlug(text) {
  if (!text) return '';
  let str = text.trim().toLowerCase();
  str = str.replace(/đ/g, 'd').replace(/Đ/g, 'd');
  str = str.normalize('NFD').replace(/[\u0300-\u036f]/g, '');
  str = str.replace(/[^a-z0-9\s-]/g, '');
  str = str.replace(/[\s-]+/g, '-');
  return str.replace(/^-+|-+$/g, '');
}

/**
 * Định dạng tiền tệ tham khảo VND
 */
function fmtVND(val) {
  if (val == null || val === 0 || val === '0') return '0 đ (Miễn phí)';
  return new Intl.NumberFormat('vi-VN').format(val) + ' đ';
}

export default function AdminServices() {
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);

  // Dialog Thêm/Sửa
  const [formDialog, setFormDialog] = useState(null); // null | { mode: 'create' | 'edit', service: ... }

  // Dialog Xoá
  const [deleteDialog, setDeleteDialog] = useState(null); // null | service
  const [deletePending, setDeletePending] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get('/admin/services');
      setServices(Array.isArray(res) ? res : res?.data || []);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  useEffect(() => {
    if (notice) {
      const timer = setTimeout(() => setNotice(null), 4000);
      return () => clearTimeout(timer);
    }
  }, [notice]);

  // Bật/tắt nhanh trạng thái isActive
  const handleToggleActive = async (service) => {
    const nextActive = !service.isActive;
    try {
      await api.patch(`/admin/services/${service.id}`, {
        isActive: nextActive,
        updatedAt: service.updatedAt,
      });
      setNotice(`Đã ${nextActive ? 'kích hoạt' : 'ẩn'} dịch vụ "${service.name}".`);
      loadData();
    } catch (err) {
      if (err?.status === 409 || err?.data?.message?.includes('thay đổi bởi người khác')) {
        setError(new Error('Dữ liệu dịch vụ đã được cập nhật bởi quản trị viên khác. Vui lòng làm mới trang.'));
      } else {
        setError(err);
      }
    }
  };

  // Xác nhận xoá dịch vụ
  const handleDelete = async () => {
    if (!deleteDialog) return;
    setDeletePending(true);
    setDeleteError(null);
    try {
      await api.del(`/admin/services/${deleteDialog.id}`);
      setNotice(`Đã xoá dịch vụ "${deleteDialog.name}" thành công.`);
      setDeleteDialog(null);
      loadData();
    } catch (err) {
      const msg = err?.data?.message || err?.message || '';
      if (err?.status === 422 || msg.includes('dùng') || msg.includes('ẩn')) {
        setDeleteError('Dịch vụ này đã được dùng trong các phiếu khám. Hệ thống không cho phép xoá cứng để bảo toàn hồ sơ bệnh nhân — quý vị vui lòng chọn "Ẩn" dịch vụ thay vì xoá.');
      } else {
        setDeleteError(msg || 'Không thể xoá dịch vụ này. Vui lòng thử lại.');
      }
    } finally {
      setDeletePending(false);
    }
  };

  return (
    <div className="db-panel" style={{ minHeight: 600 }}>
      {/* Tiêu đề & Nút Thêm mới */}
      <div className="db-panel-head" style={{ alignItems: 'center', marginBottom: 20 }}>
        <div>
          <h1 className="t-h1" style={{ fontSize: '26px', color: '#1F1610', margin: 0 }}>
            Quản trị dịch vụ khám chữa bệnh
          </h1>
          <p className="t-muted" style={{ fontSize: '18px', marginTop: 6, color: '#5C4838' }}>
            Quản lý danh mục dịch vụ đông y, phục hồi chức năng và bắt mạch bốc thuốc tại Phòng Khám Chúc Từ.
          </p>
        </div>
        <div>
          <button
            type="button"
            className="t-btn t-btn-primary"
            style={{ minHeight: 48, fontSize: '18px', padding: '10px 20px', display: 'flex', alignItems: 'center', gap: 8 }}
            onClick={() => setFormDialog({ mode: 'create', service: null })}
          >
            <Icon name="plus" size={20} strokeWidth={2.25} />
            Thêm dịch vụ khám
          </button>
        </div>
      </div>

      {/* Thông báo thông tin thành công */}
      {notice && (
        <div
          role="status"
          style={{
            backgroundColor: '#DCFCE7',
            color: '#166534',
            border: '1px solid #86EFAC',
            borderRadius: 8,
            padding: '14px 18px',
            fontSize: '18px',
            marginBottom: 20,
            display: 'flex',
            alignItems: 'center',
            gap: 12,
          }}
        >
          <Icon name="checkCircle" size={24} strokeWidth={2} />
          <span>{notice}</span>
        </div>
      )}

      {/* 4 Trạng thái: 1. Loading */}
      {loading && <Loading />}

      {/* 4 Trạng thái: 2. Lỗi mạng / API */}
      {!loading && error && (
        <div
          role="alert"
          style={{
            backgroundColor: '#FEF2F2',
            color: '#991B1B',
            border: '1px solid #FECACA',
            borderRadius: 8,
            padding: '20px',
            marginBottom: 20,
            fontSize: '18px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, fontWeight: 700, marginBottom: 8 }}>
            <Icon name="alert" size={24} />
            <span>Đã xảy ra lỗi khi tải danh mục dịch vụ</span>
          </div>
          <p style={{ margin: '0 0 16px 0', color: '#7F1D1D' }}>{error.message || 'Không thể kết nối đến máy chủ.'}</p>
          <button
            type="button"
            className="t-btn t-btn-primary"
            style={{ minHeight: 48, fontSize: '18px', padding: '8px 20px' }}
            onClick={loadData}
          >
            Thử lại
          </button>
        </div>
      )}

      {/* 4 Trạng thái: 3. Rỗng */}
      {!loading && !error && services.length === 0 && (
        <Empty title="Chưa có dịch vụ nào trong danh mục">
          <p style={{ fontSize: '18px', color: '#5C4838', marginBottom: 20 }}>
            Quý vị có thể bắt đầu thêm các dịch vụ y học cổ truyền (Châm cứu, Xoa bóp, Bốc thuốc...) vào hệ thống.
          </p>
          <button
            type="button"
            className="t-btn t-btn-primary"
            style={{ minHeight: 48, fontSize: '18px', padding: '10px 24px' }}
            onClick={() => setFormDialog({ mode: 'create', service: null })}
          >
            <Icon name="plus" size={20} />
            Thêm dịch vụ đầu tiên
          </button>
        </Empty>
      )}

      {/* 4 Trạng thái: 4. Thành công - Hiển thị Bảng danh sách */}
      {!loading && !error && services.length > 0 && (
        <div className="t-table-wrap" style={{ overflowX: 'auto', backgroundColor: '#FFFFFF', borderRadius: 8, border: '1px solid #E5E0D8' }}>
          <table className="t-table" style={{ width: '100%', fontSize: '18px', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ backgroundColor: '#F5EFE6', borderBottom: '2px solid #D8CFC4', textAlign: 'left' }}>
                <th style={{ padding: '16px 14px', color: '#1F1610', fontWeight: 700 }}>Tên dịch vụ</th>
                <th style={{ padding: '16px 14px', color: '#1F1610', fontWeight: 700 }}>Slug (URL)</th>
                <th style={{ padding: '16px 14px', color: '#1F1610', fontWeight: 700 }}>Thời lượng</th>
                <th style={{ padding: '16px 14px', color: '#1F1610', fontWeight: 700 }}>Giá tham khảo</th>
                <th style={{ padding: '16px 14px', color: '#1F1610', fontWeight: 700, textAlign: 'center' }}>Thứ tự</th>
                <th style={{ padding: '16px 14px', color: '#1F1610', fontWeight: 700, textAlign: 'center' }}>Đang dùng</th>
                <th style={{ padding: '16px 14px', color: '#1F1610', fontWeight: 700, textAlign: 'right' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {services.map((s) => (
                <tr
                  key={s.id}
                  style={{
                    borderBottom: '1px solid #EDE8E1',
                    backgroundColor: s.isActive ? '#FFFFFF' : '#F9F8F6',
                    opacity: s.isActive ? 1 : 0.8,
                  }}
                >
                  <td style={{ padding: '16px 14px', fontWeight: 600, color: '#1F1610' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <span
                        style={{
                          width: 36,
                          height: 36,
                          borderRadius: '50%',
                          backgroundColor: '#F5EFE6',
                          color: '#5C3A21',
                          display: 'inline-flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          flexShrink: 0,
                        }}
                      >
                        <Icon name="clipboard" size={20} strokeWidth={1.75} />
                      </span>
                      <div>
                        <div>{s.name}</div>
                        {s.description && (
                          <div style={{ fontSize: '15px', color: '#6B7280', marginTop: 2, maxWidth: 320, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {s.description}
                          </div>
                        )}
                      </div>
                    </div>
                  </td>
                  <td style={{ padding: '16px 14px', color: '#5C4838', fontFamily: 'monospace', fontSize: '16px' }}>
                    <code>{s.slug}</code>
                  </td>
                  <td style={{ padding: '16px 14px', color: '#1F1610' }}>
                    {s.durationMin} phút
                  </td>
                  <td style={{ padding: '16px 14px', color: '#1F1610' }}>
                    <span>{fmtVND(s.price)}</span>
                  </td>
                  <td style={{ padding: '16px 14px', textAlign: 'center', color: '#5C4838' }}>
                    {s.sortOrder ?? 0}
                  </td>
                  <td style={{ padding: '16px 14px', textAlign: 'center' }}>
                    <button
                      type="button"
                      role="switch"
                      aria-checked={s.isActive}
                      aria-label={`Trạng thái hiển thị của ${s.name}`}
                      onClick={() => handleToggleActive(s)}
                      style={{
                        minHeight: 44,
                        minWidth: 44,
                        cursor: 'pointer',
                        padding: '6px 14px',
                        borderRadius: 20,
                        border: 'none',
                        fontSize: '16px',
                        fontWeight: 600,
                        backgroundColor: s.isActive ? '#DCFCE7' : '#F3F4F6',
                        color: s.isActive ? '#166534' : '#6B7280',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 6,
                        transition: 'background-color 150ms ease',
                      }}
                    >
                      <span
                        style={{
                          width: 10,
                          height: 10,
                          borderRadius: '50%',
                          backgroundColor: s.isActive ? '#16A34A' : '#9CA3AF',
                        }}
                      />
                      {s.isActive ? 'Đang bật' : 'Đã ẩn'}
                    </button>
                  </td>
                  <td style={{ padding: '16px 14px', textAlign: 'right' }}>
                    <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
                      <button
                        type="button"
                        className="t-btn t-btn-subtle"
                        style={{ minHeight: 48, minWidth: 48, padding: '8px 14px', fontSize: '17px', display: 'inline-flex', alignItems: 'center', gap: 6 }}
                        onClick={() => setFormDialog({ mode: 'edit', service: s })}
                      >
                        <Icon name="edit" size={18} />
                        Sửa
                      </button>
                      <button
                        type="button"
                        className="t-btn t-btn-danger"
                        style={{ minHeight: 48, minWidth: 48, padding: '8px 14px', fontSize: '17px', display: 'inline-flex', alignItems: 'center', gap: 6 }}
                        onClick={() => {
                          setDeleteError(null);
                          setDeleteDialog(s);
                        }}
                      >
                        <Icon name="trash" size={18} />
                        Xoá
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Hộp thoại Thêm / Chỉnh sửa dịch vụ */}
      {formDialog && (
        <ServiceFormDialog
          mode={formDialog.mode}
          initialService={formDialog.service}
          onClose={() => setFormDialog(null)}
          onSuccess={(saved) => {
            setNotice(
              formDialog.mode === 'create'
                ? `Đã thêm dịch vụ "${saved.name}" thành công.`
                : `Đã cập nhật dịch vụ "${saved.name}".`
            );
            setFormDialog(null);
            loadData();
          }}
        />
      )}

      {/* Hộp thoại Xác nhận xoá */}
      {deleteDialog && (
        <TDialog
          title="Xác nhận xoá dịch vụ"
          subtitle="Dịch vụ sẽ bị xoá khỏi danh mục nếu chưa từng được đặt lịch"
          tone="red"
          width={580}
          onClose={() => {
            if (!deletePending) {
              setDeleteDialog(null);
              setDeleteError(null);
            }
          }}
          alert
        >
          <div className="t-dialog-body" style={{ padding: '20px 24px', fontSize: '18px', color: '#1F1610' }}>
            {deleteError ? (
              <div
                style={{
                  backgroundColor: '#FEF2F2',
                  border: '1px solid #FECACA',
                  borderRadius: 8,
                  padding: '16px',
                  color: '#991B1B',
                  marginBottom: 16,
                  lineHeight: 1.5,
                }}
              >
                <div style={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
                  <Icon name="alert" size={22} />
                  <span>Không thể xoá dịch vụ</span>
                </div>
                <div>{deleteError}</div>
              </div>
            ) : (
              <div>
                <p style={{ marginTop: 0, marginBottom: 12 }}>
                  Quý vị có chắc chắn muốn xoá dịch vụ <strong>"{deleteDialog.name}"</strong> (mã slug: <code>{deleteDialog.slug}</code>)?
                </p>
                <p style={{ color: '#5C4838', fontSize: '17px', margin: 0 }}>
                  Lưu ý: Nếu dịch vụ đã phát sinh phiếu khám bệnh trong quá khứ, hệ thống sẽ ngăn chặn xoá để không làm sai lệch bệnh án. Trong trường hợp đó, quý vị hãy tắt trạng thái "Đang dùng" để ẩn dịch vụ khỏi người bệnh.
                </p>
              </div>
            )}
          </div>
          <div
            className="t-dialog-foot"
            style={{
              padding: '16px 24px',
              display: 'flex',
              justifyContent: 'flex-end',
              gap: 12,
              borderTop: '1px solid #EDE8E1',
            }}
          >
            <button
              type="button"
              className="t-btn t-btn-subtle"
              style={{ minHeight: 48, fontSize: '18px', padding: '10px 20px' }}
              onClick={() => {
                setDeleteDialog(null);
                setDeleteError(null);
              }}
              disabled={deletePending}
            >
              {deleteError ? 'Đóng lại' : 'Huỷ bỏ'}
            </button>
            {!deleteError && (
              <button
                type="button"
                className="t-btn t-btn-danger"
                style={{ minHeight: 48, fontSize: '18px', padding: '10px 24px' }}
                onClick={handleDelete}
                disabled={deletePending}
              >
                {deletePending ? 'Đang xoá...' : 'Xác nhận xoá'}
              </button>
            )}
          </div>
        </TDialog>
      )}
    </div>
  );
}

/**
 * Hộp thoại Form Thêm / Chỉnh sửa dịch vụ
 */
function ServiceFormDialog({ mode, initialService, onClose, onSuccess }) {
  const isCreate = mode === 'create';
  const [name, setName] = useState(initialService?.name || '');
  const [slug, setSlug] = useState(initialService?.slug || '');
  const [slugManuallyEdited, setSlugManuallyEdited] = useState(!isCreate);
  const [durationMin, setDurationMin] = useState(initialService?.durationMin ?? 30);
  const [price, setPrice] = useState(initialService?.price ?? 0);
  const [sortOrder, setSortOrder] = useState(initialService?.sortOrder ?? 0);
  const [isActive, setIsActive] = useState(initialService?.isActive ?? true);
  const [description, setDescription] = useState(initialService?.description || '');

  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState(null);
  const [conflictStale, setConflictStale] = useState(false);

  const nameId = useId();
  const slugId = useId();
  const durationId = useId();
  const priceId = useId();
  const sortId = useId();
  const descId = useId();

  // Tự sinh slug khi gõ tên (nếu người dùng chưa sửa tay slug)
  const handleNameChange = (e) => {
    const nextName = e.target.value;
    setName(nextName);
    if (!slugManuallyEdited) {
      setSlug(toSlug(nextName));
    }
  };

  const handleSlugChange = (e) => {
    setSlugManuallyEdited(true);
    setSlug(e.target.value);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError(null);
    setConflictStale(false);

    if (!name.trim()) {
      setFormError('Vui lòng nhập tên dịch vụ khám.');
      return;
    }

    const dur = parseInt(durationMin, 10);
    if (isNaN(dur) || dur < 5 || dur > 240) {
      setFormError('Thời lượng khám phải từ 5 đến 240 phút.');
      return;
    }

    const p = parseFloat(price);
    if (isNaN(p) || p < 0) {
      setFormError('Giá tham khảo phải lớn hơn hoặc bằng 0.');
      return;
    }

    setSaving(true);
    try {
      const payload = {
        name: name.trim(),
        slug: slug.trim() ? toSlug(slug) : toSlug(name),
        durationMin: dur,
        price: p,
        sortOrder: parseInt(sortOrder, 10) || 0,
        isActive: Boolean(isActive),
        description: description.trim() || null,
        iconUrl: initialService?.iconUrl || null,
      };

      let res;
      if (isCreate) {
        res = await api.post('/admin/services', payload);
      } else {
        // Gửi kèm updatedAt để khoá lạc quan
        res = await api.patch(`/admin/services/${initialService.id}`, {
          ...payload,
          updatedAt: initialService.updatedAt,
        });
      }

      const savedData = res?.data || res;
      onSuccess(savedData);
    } catch (err) {
      const msg = err?.data?.message || err?.message || '';
      if (err?.status === 409 || msg.includes('thay đổi bởi người khác')) {
        setConflictStale(true);
        setFormError('Dữ liệu dịch vụ này đã bị sửa đổi bởi người khác kể từ khi bạn mở trang. Để tránh ghi đè dữ liệu, vui lòng đóng hộp thoại và tải lại trang.');
      } else if (err?.status === 422 || msg.includes('slug')) {
        setFormError(msg || 'Mã định danh (slug) này đã tồn tại trong hệ thống. Vui lòng chọn slug khác.');
      } else {
        setFormError(msg || 'Đã có lỗi xảy ra khi lưu dịch vụ. Vui lòng kiểm tra lại.');
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <TDialog
      title={isCreate ? 'Thêm dịch vụ khám mới' : 'Chỉnh sửa thông tin dịch vụ'}
      subtitle={isCreate ? 'Khai báo dịch vụ đông y hoặc phục hồi chức năng' : `Dịch vụ: ${initialService.name}`}
      tone={isCreate ? 'green' : 'blue'}
      width={680}
      onClose={() => !saving && onClose()}
    >
      <form onSubmit={handleSubmit}>
        <div className="t-dialog-body" style={{ padding: '24px', fontSize: '18px' }}>
          {formError && (
            <div
              role="alert"
              style={{
                backgroundColor: '#FEF2F2',
                border: '1px solid #FECACA',
                borderRadius: 8,
                padding: '14px 18px',
                color: '#991B1B',
                marginBottom: 20,
                fontSize: '18px',
                display: 'flex',
                alignItems: 'flex-start',
                gap: 10,
              }}
            >
              <Icon name="alert" size={24} style={{ flexShrink: 0, marginTop: 2 }} />
              <div>
                <div style={{ fontWeight: 700, marginBottom: 4 }}>
                  {conflictStale ? 'Xung đột phiên bản dữ liệu (STALE)' : 'Không thể lưu dịch vụ'}
                </div>
                <div>{formError}</div>
              </div>
            </div>
          )}

          {/* Tên dịch vụ */}
          <div style={{ marginBottom: 18 }}>
            <label htmlFor={nameId} style={{ display: 'block', fontWeight: 700, marginBottom: 6, color: '#1F1610' }}>
              Tên dịch vụ <span style={{ color: '#DC2626' }}>*</span>
            </label>
            <input
              id={nameId}
              type="text"
              className="t-input"
              style={{ width: '100%', minHeight: 48, fontSize: '18px', padding: '10px 14px' }}
              value={name}
              onChange={handleNameChange}
              placeholder="Ví dụ: Châm cứu, Vật lý trị liệu..."
              required
              autoFocus
            />
          </div>

          {/* Slug URL */}
          <div style={{ marginBottom: 18 }}>
            <label htmlFor={slugId} style={{ display: 'block', fontWeight: 700, marginBottom: 6, color: '#1F1610' }}>
              Mã định danh (Slug) <span style={{ color: '#DC2626' }}>*</span>
            </label>
            <input
              id={slugId}
              type="text"
              className="t-input"
              style={{ width: '100%', minHeight: 48, fontSize: '18px', padding: '10px 14px', fontFamily: 'monospace' }}
              value={slug}
              onChange={handleSlugChange}
              placeholder="ví-dụ: cham-cuu"
            />
            <p className="t-fine" style={{ color: '#5C4838', fontSize: '15px', marginTop: 4, marginBottom: 0 }}>
              Đường dẫn hiển thị: <code>chuctupk.vn/dich-vu#{toSlug(slug || name) || '...'}</code>
            </p>
          </div>

          {/* Thời lượng & Giá tham khảo */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 18 }}>
            <div>
              <label htmlFor={durationId} style={{ display: 'block', fontWeight: 700, marginBottom: 6, color: '#1F1610' }}>
                Thời lượng (phút) <span style={{ color: '#DC2626' }}>*</span>
              </label>
              <input
                id={durationId}
                type="number"
                min="5"
                max="240"
                className="t-input"
                style={{ width: '100%', minHeight: 48, fontSize: '18px', padding: '10px 14px' }}
                value={durationMin}
                onChange={(e) => setDurationMin(e.target.value)}
                required
              />
              <p className="t-fine" style={{ color: '#6B7280', fontSize: '15px', marginTop: 4, marginBottom: 0 }}>
                Từ 5 đến 240 phút
              </p>
            </div>

            <div>
              <label htmlFor={priceId} style={{ display: 'block', fontWeight: 700, marginBottom: 6, color: '#1F1610' }}>
                Giá tham khảo (VND)
              </label>
              <input
                id={priceId}
                type="number"
                min="0"
                step="1000"
                className="t-input"
                style={{ width: '100%', minHeight: 48, fontSize: '18px', padding: '10px 14px' }}
                value={price}
                onChange={(e) => setPrice(e.target.value)}
              />
              <p className="t-fine" style={{ color: '#D97706', fontSize: '15px', marginTop: 4, marginBottom: 0, fontWeight: 600 }}>
                Phòng khám không thu tiền — giá chỉ để tham khảo
              </p>
            </div>
          </div>

          {/* Thứ tự & Trạng thái bật/tắt */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 18, alignItems: 'center' }}>
            <div>
              <label htmlFor={sortId} style={{ display: 'block', fontWeight: 700, marginBottom: 6, color: '#1F1610' }}>
                Thứ tự hiển thị
              </label>
              <input
                id={sortId}
                type="number"
                className="t-input"
                style={{ width: '100%', minHeight: 48, fontSize: '18px', padding: '10px 14px' }}
                value={sortOrder}
                onChange={(e) => setSortOrder(e.target.value)}
              />
              <p className="t-fine" style={{ color: '#6B7280', fontSize: '15px', marginTop: 4, marginBottom: 0 }}>
                Số nhỏ hơn xếp trước
              </p>
            </div>

            <div style={{ paddingTop: 20 }}>
              <label
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 12,
                  cursor: 'pointer',
                  fontWeight: 600,
                  color: '#1F1610',
                  minHeight: 48,
                }}
              >
                <input
                  type="checkbox"
                  checked={isActive}
                  onChange={(e) => setIsActive(e.target.checked)}
                  style={{ width: 24, height: 24, cursor: 'pointer', accentColor: '#15803D' }}
                />
                <span>Đang dùng (hiển thị cho bệnh nhân)</span>
              </label>
            </div>
          </div>

          {/* Mô tả dịch vụ */}
          <div>
            <label htmlFor={descId} style={{ display: 'block', fontWeight: 700, marginBottom: 6, color: '#1F1610' }}>
              Mô tả tóm tắt dịch vụ
            </label>
            <textarea
              id={descId}
              rows={3}
              className="t-input"
              style={{ width: '100%', fontSize: '18px', padding: '10px 14px', resize: 'vertical' }}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Nội dung điều trị, chỉ định chính, lời dặn cho cô chú..."
            />
          </div>
        </div>

        {/* Nút hành động */}
        <div
          className="t-dialog-foot"
          style={{
            padding: '16px 24px',
            display: 'flex',
            justifyContent: 'flex-end',
            gap: 12,
            borderTop: '1px solid #EDE8E1',
          }}
        >
          <button
            type="button"
            className="t-btn t-btn-subtle"
            style={{ minHeight: 48, fontSize: '18px', padding: '10px 20px' }}
            onClick={onClose}
            disabled={saving}
          >
            Đóng
          </button>
          <button
            type="submit"
            className="t-btn t-btn-primary"
            style={{ minHeight: 48, fontSize: '18px', padding: '10px 24px' }}
            disabled={saving || conflictStale}
          >
            {saving ? 'Đang lưu...' : isCreate ? 'Thêm dịch vụ' : 'Lưu thay đổi'}
          </button>
        </div>
      </form>
    </TDialog>
  );
}
