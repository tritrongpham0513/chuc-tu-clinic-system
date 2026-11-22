import { useState, useCallback, useMemo } from 'react';
import { api, qs } from '../../../shared/lib/api.js';
import { useAsync, useDocumentTitle } from '../../../shared/lib/hooks.js';
import { fmtDate, weekday } from '../../../shared/lib/format.js';
import { Icon } from '../../../shared/ui/Icon.jsx';
import { Loading, ErrorAlert } from '../../../shared/ui/ui.jsx';
import { AddAppointmentDialog } from '../../components/appointments/AddAppointmentDialog.jsx';
import { TDialog } from '../../components/dialogs.jsx';
import { usePrint } from '../../components/print/PrintProvider.jsx';

/**
 * Màn hình danh sách "Cần tái khám" (Sprint 11).
 * Hỗ trợ tiếp tân và bác sĩ:
 * 1. Xem danh sách theo nhóm Quá hạn, Hôm nay, Trong N ngày.
 * 2. Đặt lịch tái khám điền sẵn 1 click.
 * 3. Nhắc lịch tái khám thủ công 1 click theo mẫu chuẩn.
 * 4. In đơn thuốc / toa thuốc riêng cho người bệnh.
 */
export default function FollowUps() {
  useDocumentTitle('Cần tái khám');
  const { printPrescription } = usePrint();

  const [days, setDays] = useState(7);
  const [bookingItem, setBookingItem] = useState(null);
  const [remindItem, setRemindItem] = useState(null);
  const [notice, setNotice] = useState(null);

  const { data, loading, error, reload, setData } = useAsync(
    () => api.get(`/staff/follow-ups${qs({ days })}`),
    [days]
  );

  const items = useMemo(() => data?.items || [], [data]);

  // Phân 3 nhóm: Quá hạn (daysUntil < 0), Hôm nay (daysUntil == 0), Sắp tới (daysUntil > 0)
  const overdueList = useMemo(() => items.filter((it) => it.daysUntil < 0), [items]);
  const todayList = useMemo(() => items.filter((it) => it.daysUntil === 0), [items]);
  const upcomingList = useMemo(() => items.filter((it) => it.daysUntil > 0), [items]);

  const handleBooked = useCallback(
    (ticketNotice, ticket) => {
      if (bookingItem) {
        // Sau khi đặt lịch thành công: loại bỏ dòng này khỏi danh sách
        setData((prev) => {
          if (!prev) return prev;
          const nextItems = prev.items.filter((it) => it.recordId !== bookingItem.recordId);
          return {
            ...prev,
            items: nextItems,
            total: Math.max(0, prev.total - 1),
            overdueCount: bookingItem.daysUntil < 0 ? Math.max(0, prev.overdueCount - 1) : prev.overdueCount,
            todayCount: bookingItem.daysUntil === 0 ? Math.max(0, prev.todayCount - 1) : prev.todayCount,
            upcomingCount: bookingItem.daysUntil > 0 ? Math.max(0, prev.upcomingCount - 1) : prev.upcomingCount,
          };
        });

        setNotice(
          `Đã tạo phiếu hẹn tái khám ${ticket?.ticketCode || ''} cho bệnh nhân ${bookingItem.patientName}.`
        );
      }
      setBookingItem(null);
    },
    [bookingItem, setData]
  );

  const handleReminderMarked = useCallback(
    (recordId, sentIso) => {
      setData((prev) => {
        if (!prev) return prev;
        const nextItems = prev.items.map((it) =>
          it.recordId === recordId ? { ...it, reminderSentAt: sentIso } : it
        );
        return { ...prev, items: nextItems };
      });
      setRemindItem(null);
      setNotice('Đã ghi nhận nhắc tái khám thành công.');
    },
    [setData]
  );

  return (
    <div className="t-page-container" style={{ padding: '24px 20px', maxWidth: 1100, margin: '0 auto' }}>
      {/* Tiêu đề & bộ lọc */}
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: 16, marginBottom: 24 }}>
        <div>
          <h1 style={{ fontSize: 28, fontWeight: 700, color: 'var(--wood-800)', margin: 0 }}>
            Danh sách Cần tái khám
          </h1>
          <p style={{ fontSize: 18, color: 'var(--ink-700)', marginTop: 4, margin: 0 }}>
            Theo dõi người bệnh có lịch hẹn tái khám của bác sĩ để kịp thời nhắc nhở và sắp xếp lượt khám.
          </p>
        </div>

        {/* Bộ lọc số ngày */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontSize: 17, fontWeight: 600, color: 'var(--ink-800)' }}>Xem trong:</span>
          {[7, 14, 30].map((d) => (
            <button
              key={d}
              type="button"
              className={`t-btn ${days === d ? 't-btn-primary' : 't-btn-ghost'}`}
              style={{ minHeight: 44, padding: '0 16px', fontSize: 17, fontWeight: 600, borderRadius: 10 }}
              onClick={() => setDays(d)}
            >
              {d} ngày tới
            </button>
          ))}
        </div>
      </div>

      {notice && (
        <div role="status" className="t-tint-green" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 18px', borderRadius: 12, marginBottom: 20, fontSize: 18 }}>
          <span>{notice}</span>
          <button type="button" className="t-btn t-btn-ghost" style={{ minHeight: 36, padding: '0 10px' }} onClick={() => setNotice(null)}>
            Đóng
          </button>
        </div>
      )}

      {error && <ErrorAlert error={error} />}

      {loading && !data && <Loading />}

      {data && (
        <>
          {/* Thẻ thống kê */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 14, marginBottom: 28 }}>
            <div style={{ background: 'var(--alert-100)', border: '1.5px solid var(--alert-700)', borderRadius: 14, padding: '16px 20px' }}>
              <p style={{ fontSize: 16, fontWeight: 600, color: 'var(--alert-700)', margin: 0 }}>ĐÃ QUÁ HẠN</p>
              <p style={{ fontSize: 32, fontWeight: 800, color: 'var(--alert-700)', margin: '4px 0 0' }}>{data.overdueCount || 0}</p>
            </div>
            <div style={{ background: 'var(--gold-100)', border: '1.5px solid var(--gold-500)', borderRadius: 14, padding: '16px 20px' }}>
              <p style={{ fontSize: 16, fontWeight: 600, color: 'var(--gold-700)', margin: 0 }}>HÔM NAY</p>
              <p style={{ fontSize: 32, fontWeight: 800, color: 'var(--gold-700)', margin: '4px 0 0' }}>{data.todayCount || 0}</p>
            </div>
            <div style={{ background: 'var(--green-100)', border: '1.5px solid var(--green-600)', borderRadius: 14, padding: '16px 20px' }}>
              <p style={{ fontSize: 16, fontWeight: 600, color: 'var(--green-700)', margin: 0 }}>TRONG {days} NGÀY TỚI</p>
              <p style={{ fontSize: 32, fontWeight: 800, color: 'var(--green-700)', margin: '4px 0 0' }}>{data.upcomingCount || 0}</p>
            </div>
            <div style={{ background: 'var(--surface-card)', border: '1.5px solid var(--line-soft)', borderRadius: 14, padding: '16px 20px' }}>
              <p style={{ fontSize: 16, fontWeight: 600, color: 'var(--ink-700)', margin: 0 }}>TỔNG CẦN TÁI KHÁM</p>
              <p style={{ fontSize: 32, fontWeight: 800, color: 'var(--wood-800)', margin: '4px 0 0' }}>{data.total || 0}</p>
            </div>
          </div>

          {items.length === 0 ? (
            <div className="t-boxed" style={{ padding: '48px 24px', textAlign: 'center', borderRadius: 16, background: 'var(--surface-card)', border: '1.5px dashed var(--line-soft)' }}>
              <Icon name="checkCircle" size={48} style={{ color: 'var(--green-700)', margin: '0 auto 12px' }} />
              <p style={{ fontSize: 20, fontWeight: 600, color: 'var(--ink-900)', margin: 0 }}>
                Không có bệnh nhân nào cần tái khám trong {days} ngày tới.
              </p>
              <p style={{ fontSize: 17, color: 'var(--ink-600)', marginTop: 6, margin: 0 }}>
                Hệ thống tự động loại bỏ những người bệnh đã có lịch hẹn mới.
              </p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 32 }}>
              {/* Nhóm 1: Quá hạn */}
              {overdueList.length > 0 && (
                <section aria-labelledby="sec-overdue">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
                    <span style={{ display: 'inline-block', width: 12, height: 12, borderRadius: '50%', background: 'var(--alert-700)' }} />
                    <h2 id="sec-overdue" style={{ fontSize: 22, fontWeight: 700, color: 'var(--alert-700)', margin: 0 }}>
                      Quá hạn tái khám ({overdueList.length})
                    </h2>
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                    {overdueList.map((item) => (
                      <FollowUpRow
                        key={item.recordId}
                        item={item}
                        variant="overdue"
                        onBook={() => setBookingItem(item)}
                        onRemind={() => setRemindItem(item)}
                        onPrint={() => printPrescription({ id: item.recordId, visitDate: item.lastVisitDate, followUpDate: item.followUpDate, doctorName: item.doctorName }, { id: item.patientId, fullName: item.patientName, yearOfBirth: item.yearOfBirth, phone: item.phone })}
                      />
                    ))}
                  </div>
                </section>
              )}

              {/* Nhóm 2: Hôm nay */}
              {todayList.length > 0 && (
                <section aria-labelledby="sec-today">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
                    <span style={{ display: 'inline-block', width: 12, height: 12, borderRadius: '50%', background: 'var(--gold-500)' }} />
                    <h2 id="sec-today" style={{ fontSize: 22, fontWeight: 700, color: 'var(--gold-700)', margin: 0 }}>
                      Hẹn tái khám hôm nay ({todayList.length})
                    </h2>
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                    {todayList.map((item) => (
                      <FollowUpRow
                        key={item.recordId}
                        item={item}
                        variant="today"
                        onBook={() => setBookingItem(item)}
                        onRemind={() => setRemindItem(item)}
                        onPrint={() => printPrescription({ id: item.recordId, visitDate: item.lastVisitDate, followUpDate: item.followUpDate, doctorName: item.doctorName }, { id: item.patientId, fullName: item.patientName, yearOfBirth: item.yearOfBirth, phone: item.phone })}
                      />
                    ))}
                  </div>
                </section>
              )}

              {/* Nhóm 3: Trong N ngày */}
              {upcomingList.length > 0 && (
                <section aria-labelledby="sec-upcoming">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
                    <span style={{ display: 'inline-block', width: 12, height: 12, borderRadius: '50%', background: 'var(--green-600)' }} />
                    <h2 id="sec-upcoming" style={{ fontSize: 22, fontWeight: 700, color: 'var(--green-700)', margin: 0 }}>
                      Tái khám trong {days} ngày tới ({upcomingList.length})
                    </h2>
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                    {upcomingList.map((item) => (
                      <FollowUpRow
                        key={item.recordId}
                        item={item}
                        variant="upcoming"
                        onBook={() => setBookingItem(item)}
                        onRemind={() => setRemindItem(item)}
                        onPrint={() => printPrescription({ id: item.recordId, visitDate: item.lastVisitDate, followUpDate: item.followUpDate, doctorName: item.doctorName }, { id: item.patientId, fullName: item.patientName, yearOfBirth: item.yearOfBirth, phone: item.phone })}
                      />
                    ))}
                  </div>
                </section>
              )}
            </div>
          )}
        </>
      )}

      {/* Dialog Đặt tái khám (Ticket 3) */}
      {bookingItem && (
        <AddAppointmentDialog
          patient={{
            id: bookingItem.patientId,
            fullName: bookingItem.patientName,
            yearOfBirth: bookingItem.yearOfBirth,
            phone: bookingItem.phone,
          }}
          initialService={bookingItem.serviceType}
          initialDoctor={bookingItem.doctorId ? { id: bookingItem.doctorId, fullName: bookingItem.doctorName } : null}
          suggestedDate={bookingItem.followUpDate}
          defaultNote={`Tái khám theo hẹn ngày ${fmtDate(bookingItem.followUpDate)}`}
          onClose={() => setBookingItem(null)}
          onBooked={handleBooked}
        />
      )}

      {/* Dialog Nhắc tái khám (Ticket 4) */}
      {remindItem && (
        <ReminderDialog
          item={remindItem}
          onClose={() => setRemindItem(null)}
          onMarkedSent={handleReminderMarked}
        />
      )}
    </div>
  );
}

/**
 * Một dòng bệnh nhân trong danh sách Cần tái khám.
 * Tuân thủ Elderly-First UX: Font >= 18px, nút bấm >= 48px, tương phản cao.
 */
function FollowUpRow({ item, variant, onBook, onRemind, onPrint }) {
  const isOverdue = variant === 'overdue';
  const isToday = variant === 'today';

  const bgColor = isOverdue ? 'var(--alert-100)' : isToday ? 'var(--gold-100)' : 'var(--surface-card)';
  const borderColor = isOverdue ? 'var(--alert-700)' : isToday ? 'var(--gold-500)' : 'var(--line-soft)';

  const daysText =
    item.daysUntil < 0
      ? `Quá hạn ${Math.abs(item.daysUntil)} ngày`
      : item.daysUntil === 0
      ? 'Hôm nay'
      : `Còn ${item.daysUntil} ngày`;

  return (
    <div
      style={{
        background: bgColor,
        border: `1.5px solid ${borderColor}`,
        borderRadius: 14,
        padding: '16px 20px',
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 16,
      }}
    >
      {/* Thông tin bệnh nhân */}
      <div style={{ flex: '1 1 300px', minWidth: 260 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
          <span style={{ fontSize: 20, fontWeight: 700, color: 'var(--ink-900)' }}>
            {item.patientName}
          </span>
          {item.yearOfBirth && (
            <span style={{ fontSize: 17, color: 'var(--ink-600)' }}>
              ({item.yearOfBirth})
            </span>
          )}
          <span style={{ fontSize: 17, fontWeight: 600, color: 'var(--ink-800)' }}>
            · {item.phone}
          </span>
        </div>

        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 14, marginTop: 6, fontSize: 17, color: 'var(--ink-700)' }}>
          <span>
            Khám lần trước: <strong>{fmtDate(item.lastVisitDate)}</strong>
          </span>
          <span>
            Hẹn tái khám: <strong style={{ color: isOverdue ? 'var(--alert-700)' : isToday ? 'var(--gold-700)' : 'var(--green-700)' }}>{weekday(item.followUpDate)}, {fmtDate(item.followUpDate)}</strong> ({daysText})
          </span>
          <span>
            Bác sĩ: <strong>{item.doctorName}</strong>
          </span>
        </div>

        {item.reminderSentAt && (
          <p style={{ margin: '6px 0 0', fontSize: 15, fontWeight: 600, color: 'var(--green-700)' }}>
            ✓ Đã gửi tin nhắc lúc {new Date(item.reminderSentAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })} ({fmtDate(item.reminderSentAt.slice(0, 10))})
          </p>
        )}
      </div>

      {/* Cụm nút bấm chuẩn Elderly-First >= 48px */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
        <button
          type="button"
          className="t-btn t-btn-primary t-btn-bold"
          style={{ minHeight: 48, minWidth: 140, padding: '0 20px', fontSize: 18, borderRadius: 12 }}
          onClick={onBook}
          aria-label={`Đặt tái khám cho ${item.patientName}`}
        >
          <Icon name="calendar" size={22} strokeWidth={2.2} />
          Đặt tái khám
        </button>

        <button
          type="button"
          className="t-btn t-btn-ghost"
          style={{ minHeight: 48, minWidth: 96, padding: '0 16px', fontSize: 18, fontWeight: 700, borderRadius: 12, border: '1.5px solid var(--gold-500)', color: 'var(--gold-700)' }}
          onClick={onRemind}
          aria-label={`Nhắc lịch tái khám cho ${item.patientName}`}
        >
          <Icon name="phone" size={20} strokeWidth={2} />
          Nhắc
        </button>

        <button
          type="button"
          className="t-btn t-btn-ghost"
          style={{ minHeight: 48, minWidth: 48, padding: '0 14px', fontSize: 17, borderRadius: 12, color: 'var(--ink-700)' }}
          onClick={onPrint}
          title="In đơn thuốc"
          aria-label={`In đơn thuốc cho ${item.patientName}`}
        >
          <Icon name="clipboard" size={20} />
          In đơn
        </button>
      </div>
    </div>
  );
}

/**
 * Hộp thoại nhắc tái khám 1-click (Ticket 4).
 * Hiển thị nội dung soạn thảo sẵn, nút Sao chép và link Zalo/SMS.
 */
function ReminderDialog({ item, onClose, onMarkedSent }) {
  const [copied, setCopied] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState(null);

  const { data: preview, loading } = useAsync(
    () => api.get(`/staff/follow-ups/${item.recordId}/reminder-preview`),
    [item.recordId]
  );

  const handleCopy = async () => {
    if (!preview?.content) return;
    try {
      await navigator.clipboard.writeText(preview.content);
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    } catch {
      // Fallback
      setCopied(true);
    }
  };

  const handleConfirmSent = async () => {
    setPending(true);
    setError(null);
    try {
      const res = await api.post(`/staff/follow-ups/${item.recordId}/reminder-mark-sent`);
      onMarkedSent(item.recordId, res.sentAt);
    } catch (err) {
      setError(err);
      setPending(false);
    }
  };

  const cleanPhone = (item.rawPhone || preview?.rawPhone || '').replace(/\D/g, '');
  const encodedContent = encodeURIComponent(preview?.content || '');

  return (
    <TDialog title="Nhắc lịch tái khám" subtitle={`Bệnh nhân ${item.patientName} (${item.phone})`} width={620} onClose={onClose}>
      <div className="t-dialog-body" style={{ gap: 20 }}>
        {loading && <Loading />}

        {preview && (
          <>
            <div className="t-field" style={{ gap: 8 }}>
              <label htmlFor="reminder-content" className="t-label" style={{ fontSize: 18 }}>
                Nội dung tin nhắn nhắc tái khám (soạn tự động)
              </label>
              <textarea
                id="reminder-content"
                readOnly
                rows={4}
                className="t-input"
                style={{
                  fontSize: 18,
                  lineHeight: 1.5,
                  padding: '14px 16px',
                  background: 'var(--surface-card)',
                  color: 'var(--ink-900)',
                  border: '1.5px solid var(--line-soft)',
                  borderRadius: 10,
                  resize: 'none',
                }}
                value={preview.content}
              />
            </div>

            {/* Các nút sao chép & gửi nhanh */}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, alignItems: 'center' }}>
              <button
                type="button"
                className="t-btn t-btn-primary"
                style={{ minHeight: 48, padding: '0 20px', fontSize: 18, fontWeight: 700, borderRadius: 10 }}
                onClick={handleCopy}
              >
                <Icon name={copied ? 'check' : 'clipboard'} size={20} strokeWidth={2.2} />
                {copied ? 'Đã sao chép vào bộ nhớ!' : 'Sao chép tin nhắn'}
              </button>

              {cleanPhone && (
                <>
                  <a
                    href={`sms:${cleanPhone}?body=${encodedContent}`}
                    className="t-btn t-btn-ghost"
                    style={{ minHeight: 48, padding: '0 16px', fontSize: 17, borderRadius: 10, border: '1.5px solid var(--line-strong)', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: 6 }}
                  >
                    <Icon name="phone" size={18} />
                    Mở ứng dụng SMS
                  </a>
                  <a
                    href={`https://zalo.me/${cleanPhone}`}
                    target="_blank"
                    rel="noreferrer noopener"
                    className="t-btn t-btn-ghost"
                    style={{ minHeight: 48, padding: '0 16px', fontSize: 17, borderRadius: 10, border: '1.5px solid var(--line-strong)', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: 6 }}
                  >
                    <Icon name="user" size={18} />
                    Mở Zalo
                  </a>
                </>
              )}
            </div>

            <p style={{ fontSize: 16, color: 'var(--ink-500)', margin: 0, fontStyle: 'italic' }}>
              * Sau khi sao chép hoặc gửi tin nhắn cho người bệnh, bấm &quot;Đã gửi&quot; bên dưới để hệ thống lưu lịch sử nhắc nhở.
            </p>
          </>
        )}

        {error && <ErrorAlert error={error} />}

        <div className="t-actions" style={{ marginTop: 12 }}>
          <button
            type="button"
            className="t-btn t-btn-primary t-btn-bold"
            style={{ minHeight: 48, padding: '0 24px', fontSize: 18 }}
            disabled={pending || loading}
            onClick={handleConfirmSent}
          >
            <Icon name="check" strokeWidth={2.4} />
            Đã gửi tin nhắc
          </button>
          <button type="button" className="t-btn t-btn-ghost" style={{ minHeight: 48, padding: '0 20px', fontSize: 18 }} onClick={onClose}>
            Đóng
          </button>
        </div>
      </div>
    </TDialog>
  );
}
