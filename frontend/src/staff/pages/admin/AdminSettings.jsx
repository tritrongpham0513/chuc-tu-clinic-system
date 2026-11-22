import { useEffect, useState } from 'react';
import { Icon } from '../../../shared/ui/Icon.jsx';
import { Alert, Button, Empty, ErrorAlert, Loading } from '../../../shared/ui/ui.jsx';
import { api } from '../../../shared/lib/api.js';
import { useDocumentTitle } from '../../../shared/lib/hooks.js';
import './admin.css';

/**
 * Quản trị Cấu hình Phòng khám (Sprint 5 — Ticket 5)
 * Đường dẫn: /noi-bo/quan-tri/cau-hinh, chỉ dành cho ADMIN.
 * Tuân thủ tiêu chuẩn Elderly-First UX: Chữ >= 18px, Touch Target >= 48px, phân 3 nhóm rõ ràng.
 */
export default function AdminSettings() {
  useDocumentTitle('Cấu hình phòng khám');

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [warnings, setWarnings] = useState([]);
  const [fieldErrors, setFieldErrors] = useState({});
  const [generalError, setGeneralError] = useState(null);

  const [settings, setSettings] = useState({});
  const [originalSettings, setOriginalSettings] = useState({});
  const [metadata, setMetadata] = useState({});
  const [maxFutureBooked, setMaxFutureBooked] = useState(0);

  const loadSettings = async () => {
    setLoading(true);
    setError(null);
    setFieldErrors({});
    setGeneralError(null);
    setSaveSuccess(false);

    try {
      const res = await api.get('/admin/settings');
      const data = res?.settings ? res : (res?.data || {});
      const loadedSettings = data.settings || {};
      const loadedMeta = data.metadata || {};
      const maxBooked = data.maxFutureBookedPerSlot || 0;

      setSettings(loadedSettings);
      setOriginalSettings(loadedSettings);
      setMetadata(loadedMeta);
      setMaxFutureBooked(maxBooked);
      setWarnings(data.warnings || []);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSettings();
  }, []);

  const handleChange = (key, val) => {
    setSettings((prev) => ({ ...prev, [key]: val }));
    // Clear field-specific error when user modifies value
    if (fieldErrors[key]) {
      setFieldErrors((prev) => {
        const next = { ...prev };
        delete next[key];
        return next;
      });
    }
    setSaveSuccess(false);
  };

  const handleReset = () => {
    setSettings(originalSettings);
    setFieldErrors({});
    setGeneralError(null);
    setSaveSuccess(false);
  };

  const handleSubmit = async (e) => {
    if (e) e.preventDefault();
    setSaving(true);
    setFieldErrors({});
    setGeneralError(null);
    setSaveSuccess(false);

    try {
      const res = await api.put('/admin/settings', settings);
      const data = res?.settings ? res : (res?.data || {});
      const updated = data.settings || settings;
      setSettings(updated);
      setOriginalSettings(updated);
      setMaxFutureBooked(data.maxFutureBookedPerSlot || maxFutureBooked);
      setWarnings(data.warnings || []);
      setSaveSuccess(true);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (err) {
      const msg = err.message || 'Không thể lưu cấu hình, vui lòng thử lại';
      // Phát hiện lỗi theo từng trường nếu message chứa tên trường
      let matchedField = null;
      Object.keys(settings).forEach((k) => {
        if (msg.includes(k) || msg.includes(`'${k}'`)) {
          matchedField = k;
        }
      });

      if (matchedField) {
        setFieldErrors((prev) => ({ ...prev, [matchedField]: msg }));
      } else {
        setGeneralError(msg);
      }
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } finally {
      setSaving(false);
    }
  };

  // Kiểm tra cảnh báo hạ slot_capacity thấp hơn số đã đặt
  const currentSlotCap = parseInt(settings.slot_capacity, 10) || 0;
  const isSlotCapLowerThanBooked = maxFutureBooked > 0 && currentSlotCap > 0 && currentSlotCap < maxFutureBooked;

  const isDirty = JSON.stringify(settings) !== JSON.stringify(originalSettings);

  // Nhóm theo mục
  const GROUPS = [
    {
      id: 'BOOKING',
      title: '1. Đặt lịch khám bệnh',
      description: 'Quy tắc tiếp nhận, giới hạn số lượng và thời gian đặt hẹn của bệnh nhân',
      keys: [
        'slot_capacity',
        'patients_per_doctor',
        'booking_advance_days',
        'user_max_days',
        'counter_max_days',
        'booking_min_hours',
        'max_profiles',
      ],
    },
    {
      id: 'RECEPTION',
      title: '2. Tiếp nhận tại phòng khám',
      description: 'Khung giờ tiếp đón thực tế, ưu tiên bệnh nhân đến sớm và giờ tự động hủy',
      keys: ['reception_open', 'reception_close', 'priority_early_minutes', 'no_show_cancel_at'],
    },
    {
      id: 'QUEUE',
      title: '3. Hàng đợi & Ghế khám',
      description: 'Cơ sở vật chất ghế khám và cơ chế gọi bệnh nhân tự động vào phòng',
      keys: ['seats', 'auto_invite'],
    },
  ];

  if (loading) {
    return (
      <div style={{ padding: '40px 0' }}>
        <Loading label="Đang tải danh sách cấu hình phòng khám..." />
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: '32px 16px', maxWidth: 800, margin: '0 auto' }}>
        <ErrorAlert error={error} />
        <div style={{ marginTop: 20 }}>
          <Button variant="primary" onClick={loadSettings} style={{ minHeight: 48, fontSize: 18 }}>
            Thử tải lại cấu hình
          </Button>
        </div>
      </div>
    );
  }

  if (!settings || Object.keys(settings).length === 0) {
    return (
      <div style={{ padding: '40px 16px' }}>
        <Empty title="Chưa có dữ liệu cấu hình" action={<Button onClick={loadSettings}>Tải lại</Button>} />
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 960, margin: '0 auto', paddingBottom: 64 }}>
      {/* Header trang */}
      <div style={{ marginBottom: 28 }}>
        <h1 style={{ fontFamily: 'var(--font-serif)', fontSize: 32, fontWeight: 700, color: 'var(--ink-900)', margin: '0 0 8px 0' }}>
          Cấu hình phòng khám
        </h1>
        <p style={{ fontSize: 18, color: 'var(--ink-500)', margin: 0 }}>
          Điều chỉnh các tham số vận hành thực tế của phòng khám. Thay đổi có hiệu lực ngay lập tức sau khi lưu.
        </p>
      </div>

      {/* Thông báo thành công */}
      {saveSuccess && (
        <div style={{ marginBottom: 24 }}>
          <Alert type="success" title="Cập nhật thành công!">
            Toàn bộ cấu hình mới đã được lưu và đồng bộ trên toàn hệ thống phòng khám.
          </Alert>
        </div>
      )}

      {/* Thông báo lỗi chung */}
      {generalError && (
        <div style={{ marginBottom: 24 }}>
          <Alert type="error" title="Không thể lưu cấu hình">
            {generalError}
          </Alert>
        </div>
      )}

      {/* Cảnh báo hạ sức chứa thấp hơn số lượng đã đặt */}
      {(isSlotCapLowerThanBooked || (warnings && warnings.length > 0)) && (
        <div
          style={{
            marginBottom: 24,
            background: 'var(--gold-100)',
            border: '2px solid var(--gold-500)',
            borderRadius: 16,
            padding: '16px 20px',
            color: 'var(--gold-700)',
            display: 'flex',
            alignItems: 'flex-start',
            gap: 14,
          }}
          role="status"
        >
          <div style={{ marginTop: 2 }}>
            <Icon name="alert" size={26} />
          </div>
          <div style={{ fontSize: 18, lineHeight: 1.5 }}>
            <strong style={{ display: 'block', fontSize: 19, marginBottom: 4 }}>
              Cảnh báo: Sức chứa mới nhỏ hơn lượng bệnh nhân đã đặt
            </strong>
            {warnings && warnings.length > 0 ? (
              warnings.map((w, idx) => <p key={idx} style={{ margin: '4px 0' }}>{w}</p>)
            ) : (
              <p style={{ margin: 0 }}>
                Hiện có khung giờ trong tương lai đã có <strong>{maxFutureBooked}</strong> bệnh nhân đặt chỗ, lớn hơn sức chứa bạn vừa nhập ({currentSlotCap}). Hệ thống vẫn cho phép lưu nhưng cô chú lưu ý để tránh quá tải ca khám.
              </p>
            )}
          </div>
        </div>
      )}

      {/* Form cấu hình 3 nhóm */}
      <form onSubmit={handleSubmit} noValidate>
        {GROUPS.map((group) => (
          <section
            key={group.id}
            style={{
              background: 'var(--surface-card)',
              border: '1px solid var(--line-soft)',
              borderRadius: 24,
              padding: '28px 32px',
              marginBottom: 28,
              boxShadow: '0 2px 8px rgba(0,0,0,0.03)',
            }}
          >
            <div style={{ borderBottom: '1.5px solid var(--line-soft)', paddingBottom: 16, marginBottom: 24 }}>
              <h2 style={{ fontFamily: 'var(--font-serif)', fontSize: 24, fontWeight: 700, color: 'var(--ink-900)', margin: '0 0 6px 0' }}>
                {group.title}
              </h2>
              <p style={{ fontSize: 16, color: 'var(--ink-500)', margin: 0 }}>{group.description}</p>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '24px 32px' }}>
              {group.keys.map((key) => {
                const meta = metadata[key] || {};
                const label = meta.label || key;
                const desc = meta.description || '';
                const unit = meta.unit || '';
                const type = meta.type || 'text';
                const fieldErr = fieldErrors[key];
                const value = settings[key] ?? '';

                return (
                  <div key={key} style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                    <label
                      htmlFor={`input-${key}`}
                      style={{ fontSize: 18, fontWeight: 600, color: 'var(--ink-900)', display: 'block' }}
                    >
                      {label}
                    </label>

                    {desc && (
                      <span style={{ fontSize: 15, color: 'var(--ink-500)', lineHeight: 1.4 }}>
                        {desc}
                      </span>
                    )}

                    <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginTop: 4 }}>
                      {type === 'boolean' || key === 'auto_invite' ? (
                        <select
                          id={`input-${key}`}
                          value={value === '1' || value === 'true' ? '1' : '0'}
                          onChange={(e) => handleChange(key, e.target.value)}
                          style={{
                            height: 48,
                            fontSize: 18,
                            padding: '0 16px',
                            borderRadius: 12,
                            border: fieldErr ? '2px solid var(--alert-700)' : '1.5px solid var(--line-soft)',
                            background: 'var(--surface-card)',
                            color: 'var(--ink-900)',
                            minWidth: 180,
                            cursor: 'pointer',
                          }}
                        >
                          <option value="1">1 — Bật tự động</option>
                          <option value="0">0 — Tắt</option>
                        </select>
                      ) : (
                        <input
                          id={`input-${key}`}
                          type={type === 'time' ? 'time' : 'number'}
                          value={value}
                          min={meta.min !== undefined ? meta.min : undefined}
                          max={meta.max !== undefined ? meta.max : undefined}
                          onChange={(e) => handleChange(key, e.target.value)}
                          style={{
                            height: 48,
                            fontSize: 18,
                            padding: '0 16px',
                            borderRadius: 12,
                            border: fieldErr ? '2px solid var(--alert-700)' : '1.5px solid var(--line-soft)',
                            background: 'var(--surface-card)',
                            color: 'var(--ink-900)',
                            width: type === 'time' ? 180 : 160,
                            outline: 'none',
                          }}
                        />
                      )}

                      {unit && (
                        <span style={{ fontSize: 16, color: 'var(--ink-500)', fontWeight: 500, whiteSpace: 'nowrap' }}>
                          {unit}
                        </span>
                      )}
                    </div>

                    {fieldErr && (
                      <span style={{ color: 'var(--alert-700)', fontSize: 15, fontWeight: 500, display: 'flex', alignItems: 'center', gap: 6, marginTop: 4 }}>
                        <Icon name="alert" size={18} />
                        {fieldErr}
                      </span>
                    )}
                  </div>
                );
              })}
            </div>
          </section>
        ))}

        {/* Thanh tác vụ cố định hoặc nút lưu to ở cuối */}
        <div
          style={{
            display: 'flex',
            flexWrap: 'wrap',
            gap: 16,
            alignItems: 'center',
            justifyContent: 'flex-start',
            marginTop: 36,
            padding: '24px 0',
            borderTop: '1.5px solid var(--line-soft)',
          }}
        >
          <button
            type="submit"
            disabled={saving || !isDirty}
            style={{
              minHeight: 52,
              padding: '0 36px',
              fontSize: 19,
              fontWeight: 600,
              background: isDirty ? 'var(--green-700)' : 'var(--surface-300)',
              color: isDirty ? '#ffffff' : 'var(--ink-500)',
              border: 'none',
              borderRadius: 14,
              cursor: isDirty && !saving ? 'pointer' : 'not-allowed',
              display: 'inline-flex',
              alignItems: 'center',
              gap: 10,
              boxShadow: isDirty ? '0 4px 12px rgba(45, 90, 39, 0.25)' : 'none',
              transition: 'all 0.15s ease-in-out',
            }}
          >
            {saving ? (
              <>
                <span className="spinner" aria-hidden="true" />
                Đang lưu cấu hình...
              </>
            ) : (
              <>
                <Icon name="check" size={22} />
                Lưu cấu hình phòng khám
              </>
            )}
          </button>

          {isDirty && (
            <button
              type="button"
              onClick={handleReset}
              disabled={saving}
              style={{
                minHeight: 52,
                padding: '0 24px',
                fontSize: 18,
                background: 'transparent',
                color: 'var(--ink-700)',
                border: '1.5px solid var(--line-strong)',
                borderRadius: 14,
                cursor: 'pointer',
              }}
            >
              Huỷ thay đổi
            </button>
          )}

          {!isDirty && (
            <span style={{ fontSize: 16, color: 'var(--ink-500)', marginLeft: 8 }}>
              (Chưa có thay đổi nào cần lưu)
            </span>
          )}
        </div>
      </form>
    </div>
  );
}
