import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Icon } from '../../../shared/ui/Icon.jsx';
import { ErrorAlert, Loading } from '../../../shared/ui/ui.jsx';
import { api } from '../../../shared/lib/api.js';
import { GENDERS, ageFrom, fmtDate, shortTime, todayVN } from '../../../shared/lib/format.js';
import { isAdmin, isDoctor, useSession } from '../../../shared/lib/session.jsx';
import { useAsync, useDocumentTitle } from '../../../shared/lib/hooks.js';
import { PatientFields, focusFirstError, patientBody, patientForm, validatePatient } from '../../../site/components/PatientFields.jsx';
import { NoticeDialog, TDialog } from '../../components/dialogs.jsx';
import { EditRecordDialog } from '../../components/records/EditRecordDialog.jsx';
import { HistoryTable } from '../../components/records/HistoryTable.jsx';
import { ResultDialog } from '../../components/records/ResultSheet.jsx';
import { usePrint } from '../../components/print/PrintProvider.jsx';
import { AppointmentHistoryDialog } from '../../components/appointments/AppointmentHistoryDialog.jsx';

/** Hồ sơ chưa có ngày sinh đủ (tạo tại quầy chỉ có năm sinh): điền sẵn năm để tiếp tân chọn thêm ngày, tháng. */
function editForm(patient) {
  const form = patientForm(patient);
  if (!patient.dob && patient.birthYear) form.dob = { day: '', month: '', year: String(patient.birthYear) };
  return form;
}

/** Cùng quy tắc như lúc người dùng tạo hồ sơ, trừ quan hệ với người khám (chỉ có nghĩa với tài khoản người dùng). */
function editErrors(form) {
  const { relationship, ...rest } = validatePatient({ ...form, relationship: form.relationship || 'KHAC' });
  return rest;
}

/** Sửa thông tin hồ sơ: đủ các ô như lúc người dùng tạo hồ sơ (UC.9.5). Mã bệnh nhân không đổi. */
function BreakGlassDialog({ patientId, onClose, onSuccess }) {
  const [reason, setReason] = useState('');
  const [pending, setPending] = useState(false);
  const [error, setError] = useState(null);

  const submit = async (e) => {
    e.preventDefault();
    if (!reason || reason.trim().length < 10) {
      setError({ message: 'Vui lòng ghi rõ lý do (ít nhất 10 ký tự)' });
      return;
    }
    setPending(true);
    setError(null);
    try {
      const res = await api.post(`/staff/patients/${patientId}/break-glass`, { reason: reason.trim() });
      onSuccess(res);
    } catch (err) {
      setError(err);
      setPending(false);
    }
  };

  return (
    <TDialog title="Xem khẩn cấp (Break-glass)" width={600} onClose={onClose}>
      <form className="t-dialog-body a-vars" style={{ padding: '24px 28px 28px', gap: 20 }} onSubmit={submit}>
        <p className="t-text" style={{ fontSize: '18px', lineHeight: '28px' }}>
          Hành động mở quyền xem khẩn cấp sẽ được ghi nhận vào nhật ký kiểm toán hệ thống. Vui lòng ghi rõ lý do y tế:
        </p>
        <div className="t-form-group">
          <label htmlFor="bg-reason" className="t-label" style={{ fontSize: '18px' }}>
            Lý do xem khẩn cấp <span className="t-req">*</span>
          </label>
          <textarea
            id="bg-reason"
            rows={4}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Ví dụ: Bệnh nhân đau đầu dữ dội tại quầy, cần xem tiền sử để cấp cứu..."
            style={{ fontSize: '18px', width: '100%', padding: '12px', borderRadius: 8, border: '1px solid var(--line-strong)' }}
            disabled={pending}
            autoFocus
          />
          <span className="t-muted" style={{ fontSize: '16px' }}>Tối thiểu 10 ký tự ({reason.trim().length}/10)</span>
        </div>
        {error && <ErrorAlert error={error} />}
        <div className="t-actions" style={{ paddingTop: 12, borderTop: '1px solid var(--line-soft)' }}>
          <button type="submit" className="t-btn t-btn-primary" style={{ fontSize: '18px', padding: '0 24px', minHeight: '48px' }} disabled={pending}>
            {pending ? 'Đang kích hoạt...' : 'Xác nhận xem khẩn cấp'}
          </button>
          <button type="button" className="t-btn t-btn-ghost" style={{ fontSize: '18px', minHeight: '48px' }} onClick={onClose} disabled={pending}>
            Huỷ bỏ
          </button>
        </div>
      </form>
    </TDialog>
  );
}

function EditPatientDialog({ patient, onClose, onSaved }) {
  const [form, setForm] = useState(() => editForm(patient));
  const [version, setVersion] = useState(patient.updatedAt);
  const [tried, setTried] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState(null);
  const server = error?.fields || {};
  // Lỗi địa chỉ của Backend gắn vào ô phường / xã như bên người dùng
  const fe = { ...server, ward: server.address, ...(tried ? editErrors(form) : {}) };

  const submit = async (e) => {
    e.preventDefault();
    setTried(true);
    if (Object.keys(editErrors(form)).length) {
      focusFirstError('.t-dialog');
      return;
    }
    setPending(true);
    setError(null);
    try {
      const { relationship, ...body } = patientBody(form);
      const res = await api.patch(`/staff/patients/${patient.id}`, { ...body, expectedUpdatedAt: version });
      onSaved(res.patient);
    } catch (err) {
      // Vừa có người khác đổi hồ sơ: nạp lại để xem thông tin mới rồi mới lưu
      if (err.code === 'STALE') {
        const fresh = (await api.get(`/staff/patients/${patient.id}`)).patient;
        setForm(editForm(fresh));
        setVersion(fresh.updatedAt);
        setTried(false);
      }
      setError(err);
      setPending(false);
      focusFirstError('.t-dialog');
    }
  };

  return (
    <TDialog title="Sửa thông tin hồ sơ" width={760} onClose={onClose}>
      <form className="t-dialog-body a-vars" style={{ padding: '24px 28px 28px', gap: 22 }} onSubmit={submit} noValidate>
        <p className="t-text">
          Mã bệnh nhân <strong className="t-strong">{patient.patientCode}</strong> được cấp một lần và không đổi. Ô có dấu <span className="t-req">*</span> là bắt buộc.
        </p>
        {/* Hồ sơ chính chủ: tài khoản đổi theo; số đăng nhập chỉ đổi ở Quản lý tài khoản */}
        <PatientFields
          id="sua"
          form={form}
          setForm={setForm}
          errors={fe}
          phoneReadOnly={patient.relationship === 'BAN_THAN'}
          phoneNote={patient.relationship === 'BAN_THAN' ? 'Số đăng nhập của tài khoản, đổi ở Quản lý tài khoản.' : undefined}
        />
        {error && !Object.keys(server).length && <ErrorAlert error={error} />}
        <div className="t-actions" style={{ paddingTop: 8, borderTop: '1px solid var(--line-soft)' }}>
          <button type="submit" className="t-btn t-btn-primary" style={{ padding: '0 26px' }} disabled={pending}>
            Lưu thay đổi
          </button>
          <button type="button" className="t-btn t-btn-ghost" onClick={onClose}>
            Huỷ
          </button>
        </div>
      </form>
    </TDialog>
  );
}

/** Hồ sơ chi tiết, phiếu khám đang chờ, lịch sử khám, khoá/mở khoá (UC.9.3, UC.9.5, UC.9.6, UC.9.8). */
export default function PatientDetail() {
  const { id } = useParams();
  const { user } = useSession();
  const { printTicket, printRecord } = usePrint();
  const canEditRecord = isAdmin(user) || isDoctor(user);
  const isReceptionist = !isAdmin(user) && !isDoctor(user);
  const { data, loading, error, setData } = useAsync(() => api.get(`/staff/patients/${id}`), [id]);
  const [dialog, setDialog] = useState(null);
  const [notice, setNotice] = useState(null);
  useDocumentTitle(data?.patient?.fullName || 'Hồ sơ bệnh nhân');

  if (loading) return <Loading />;
  if (error) {
    if (error.status === 403 && isDoctor(user)) {
      return (
        <>
          <Link to="/noi-bo/benh-nhan" className="t-back">
            <Icon name="arrowLeft" size={22} />
            Quay lại danh sách hồ sơ
          </Link>
          <div className="t-panel" style={{ padding: '36px 32px', gap: 20, alignItems: 'flex-start' }}>
            <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
              <span className="t-avatar" style={{ background: 'var(--amber-100)', color: 'var(--amber-800)' }}>
                <Icon name="lock" size={32} />
              </span>
              <div>
                <h1 className="t-h2" style={{ marginBottom: 4 }}>
                  Chưa từng phụ trách bệnh nhân này
                </h1>
                <p className="t-text" style={{ fontSize: '18px', color: 'var(--ink-700)', lineHeight: '28px' }}>
                  Theo quy định phân quyền, bác sĩ chỉ xem hồ sơ bệnh nhân trong phạm vi phiếu khám của mình. Trong trường hợp cấp cứu hoặc khám đột xuất, bác sĩ có thể kích hoạt quyền xem khẩn cấp (Break-glass).
                </p>
              </div>
            </div>
            <button
              type="button"
              className="t-btn t-btn-primary"
              style={{ fontSize: '18px', padding: '12px 28px', minHeight: '48px', marginTop: 12 }}
              onClick={() => setDialog({ type: 'breakGlass' })}
            >
              <Icon name="alertTriangle" size={22} />
              Xem khẩn cấp
            </button>
          </div>
          {dialog?.type === 'breakGlass' && (
            <BreakGlassDialog
              patientId={id}
              onClose={() => setDialog(null)}
              onSuccess={(fullData) => {
                setData(fullData);
                setDialog(null);
                setNotice('Đã kích hoạt quyền xem khẩn cấp thành công. Lượt truy cập này đã được ghi nhận vào nhật ký kiểm toán.');
              }}
            />
          )}
          {notice && <NoticeDialog onClose={() => setNotice(null)}>{notice}</NoticeDialog>}
        </>
      );
    }
    return <ErrorAlert error={error} />;
  }

  const p = data.patient;
  const today = todayVN();
  const upcoming = data.appointments
    .filter((a) => ['BOOKED', 'CHECKED_IN', 'IN_PROGRESS'].includes(a.status) && a.slotDate >= today)
    .sort((a, b) => (a.slotDate + a.slotStart).localeCompare(b.slotDate + b.slotStart));
  const last = data.records[0]?.visitDate;
  const age = ageFrom(p.birthYear);
  const info = [
    ['Mã bệnh nhân', <strong key="m">{p.patientCode}</strong>],
    ['Họ và tên', p.fullName],
    [
      'Năm sinh',
      <>
        {p.birthYear || '—'} {age > 0 && <span style={{ color: 'var(--ink-500)' }}>({age} tuổi)</span>}
      </>,
    ],
    ['Giới tính', GENDERS[p.gender] || 'Chưa cập nhật'],
    ['Số điện thoại', p.phone || '—'],
    ['Địa chỉ', p.address || 'Chưa cập nhật'],
    ['Hồ sơ tạo ngày', fmtDate(p.createdAt)],
    ['Lần khám gần nhất', last ? fmtDate(last) : 'Chưa khám'],
  ];

  const replaceRecord = (rec) => setData((d) => ({ ...d, records: d.records.map((r) => (r.id === rec.id ? { ...r, ...rec } : r)) }));

  return (
    <>
      <Link to="/noi-bo/benh-nhan" className="t-back">
        <Icon name="arrowLeft" size={22} />
        Quay lại danh sách hồ sơ
      </Link>
      {notice && <NoticeDialog onClose={() => setNotice(null)}>{notice}</NoticeDialog>}

      <div className="t-panel" style={{ flexDirection: 'row', flexWrap: 'wrap', gap: '20px 24px', alignItems: 'center' }}>
        <span className="t-avatar">
          <Icon name="user" size={36} strokeWidth={1.75} />
        </span>
        <div style={{ flex: '999 1 280px', minWidth: 0 }}>
          <p className="t-muted" style={{ marginBottom: 2, fontWeight: 600 }}>
            Hồ sơ bệnh nhân {p.patientCode}
          </p>
          <h1 className="t-h1" style={{ marginBottom: 10 }}>
            {p.fullName}
          </h1>
          {p.isLocked ? (
            <span className="t-chip t-chip-done t-chip-sm" style={{ padding: '2px 12px' }}>
              <Icon name="lock" size={18} strokeWidth={2.25} />
              Hồ sơ đã khoá
            </span>
          ) : (
            <span className="t-chip t-chip-green" style={{ padding: '4px 12px' }}>
              <Icon name="check" size={18} strokeWidth={2.25} />
              Hồ sơ đang hoạt động
            </span>
          )}
        </div>
        <div className="t-actions" style={{ flex: '0 1 auto', gap: 12, alignItems: 'center' }}>
          {!p.isLocked && (
            <button type="button" className="t-btn t-btn-ghost" style={{ padding: '0 18px' }} onClick={() => setDialog({ type: 'edit' })}>
              <Icon name="edit" size={20} />
              Sửa thông tin
            </button>
          )}
        </div>
      </div>

      {p.isLocked && (
        <div role="note" className="t-iconrow" style={{ background: 'var(--surface-200)', border: '2px solid var(--line-strong)', borderRadius: 20, padding: '18px 22px' }}>
          <Icon name="lock" size={28} strokeWidth={1.75} />
          <div>
            <p className="t-legend" style={{ marginBottom: 4 }}>
              Hồ sơ này đã khoá
            </p>
            <p style={{ lineHeight: '26px', color: 'var(--ink-900)' }}>
              Không chọn được khi đặt lịch khám, không ghi nhận lượt khám mới, không sửa thông tin và không sửa phiếu kết quả. Lịch sử khám vẫn xem được.{' '}
              {isAdmin(user) ? (
                'Muốn dùng lại thì mở khoá ở danh sách Hồ sơ bệnh nhân (nút ⋮ ở cột Thao tác).'
              ) : (
                <>
                  Hồ sơ do Admin khoá, muốn dùng lại thì liên hệ <strong>Admin</strong> để mở khoá.
                </>
              )}
            </p>
          </div>
        </div>
      )}

      <section aria-labelledby="tt-tieude" className="t-panel" style={{ gap: 14 }}>
        <h2 id="tt-tieude" className="t-h2">
          Thông tin hồ sơ
        </h2>
        <dl className="t-dl">
          {info.map(([label, value]) => (
            <div key={label} className="t-dl-row">
              <dt>{label}</dt>
              <dd>{value}</dd>
            </div>
          ))}
        </dl>
      </section>

      {!p.isLocked && (
        <section aria-labelledby="ve-tieude" className="t-panel">
          <h2 id="ve-tieude" className="t-h2">
            Phiếu khám hôm nay và sắp tới
          </h2>
          {upcoming.length > 0 ? (
            <div className="t-boxed t-scroll">
              <div role="table" aria-label="Phiếu khám chưa hoàn thành" className="t-table">
                <div role="row" className="t-tr t-thead" style={{ gridTemplateColumns: 'minmax(0, 1fr) 150px 280px' }}>
                  <div role="columnheader" className="t-th">
                    Mã phiếu khám
                  </div>
                  <div role="columnheader" className="t-th">
                    Trạng thái
                  </div>
                  <div role="columnheader" className="t-th">
                    Thao tác
                  </div>
                </div>
                <div role="rowgroup" className="t-tbody">
                  {upcoming.map((a) => (
                    <div key={a.id} role="row" className="t-tr" style={{ gridTemplateColumns: 'minmax(0, 1fr) 150px 280px' }}>
                      <div role="cell" className="t-td">
                        <span className="t-td-code">{a.ticketCode}</span>
                        <span className="t-td-sub">
                          {a.slotDate === today ? 'Hôm nay ' : ''}
                          {fmtDate(a.slotDate)}, {shortTime(a.slotStart)}
                        </span>
                      </div>
                      <div role="cell" className="t-td">
                        {a.status === 'BOOKED' ? (
                          <span className="t-chip t-chip-done t-chip-sm">Chưa đến</span>
                        ) : a.status === 'CHECKED_IN' ? (
                          <span className="t-chip t-chip-green" style={{ padding: '4px 12px' }}>
                            Đã check-in
                          </span>
                        ) : (
                          <span className="t-chip t-chip-solid" style={{ padding: '4px 12px' }}>
                            Đã vào khám
                          </span>
                        )}
                      </div>
                      <div role="cell" className="t-td" style={{ padding: '8px 16px', display: 'flex', alignItems: 'center', gap: 10, justifyContent: 'flex-end' }}>
                        <button
                          type="button"
                          className="t-btn-print"
                          aria-label={`In phiếu khám cho ${p.fullName} ngày ${fmtDate(a.slotDate)}`}
                          onClick={() => printTicket({ ...a, patient: p })}
                          title="In phiếu khám bệnh"
                        >
                          <Icon name="printer" size={18} strokeWidth={2} />
                          <span>In phiếu</span>
                        </button>
                        <button
                          type="button"
                          className="t-btn t-btn-ghost"
                          style={{ minHeight: '40px', padding: '0 14px', fontSize: '18px' }}
                          onClick={() => setDialog({ type: 'history', appt: a })}
                          title="Xem lịch sử phiếu khám"
                        >
                          <span>Lịch sử</span>
                        </button>
                        {a.status === 'IN_PROGRESS' ? (
                          <Link to={`/noi-bo/phieu-kham/${a.id}/ket-qua`} state={{ back: `/noi-bo/benh-nhan/${p.id}` }} className="t-btn t-btn-primary" style={{ padding: '0 14px' }}>
                            Ghi nhận lượt khám
                          </Link>
                        ) : (
                          <span className="t-muted" style={{ fontSize: '15px' }}>{a.status === 'CHECKED_IN' ? 'Chờ vào khám' : 'Chờ đến'}</span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            <p className="t-text">Hồ sơ này chưa có phiếu khám nào đang chờ.</p>
          )}
          <Link to={`/noi-bo/lich-trong-ngay?dat=${p.id}`} className="t-btn t-btn-ghost" style={{ alignSelf: 'flex-start' }}>
            <Icon name="calendarPlus" strokeWidth={1.75} />
            Đặt lịch khám cho bệnh nhân này
          </Link>
        </section>
      )}

      <section aria-labelledby="ls-tieude" className="t-panel">
        <h2 id="ls-tieude" className="t-h2">
          Lịch sử khám
        </h2>
        {data.records.length > 0 ? (
          <HistoryTable
            records={data.records}
            locked={p.isLocked}
            isReceptionist={isReceptionist}
            onView={isReceptionist ? null : (r) => setDialog({ type: 'view', record: r })}
            onPrint={isReceptionist ? null : (r) => printRecord(r, p)}
            onEdit={isReceptionist || p.isLocked || !canEditRecord ? null : (r) => setDialog({ type: 'editRecord', record: r })}
          />
        ) : (
          <p className="t-text">Hồ sơ này chưa có lượt khám nào.</p>
        )}
        <p className="t-iconrow t-text" style={{ gap: 10 }}>
          <Icon name="lock" size={22} />
          Dữ liệu bệnh nhân, lượt khám và kết quả không bao giờ hiển thị công khai trên website.
        </p>
      </section>

      {dialog?.type === 'breakGlass' && (
        <BreakGlassDialog
          patientId={id}
          onClose={() => setDialog(null)}
          onSuccess={(fullData) => {
            setData(fullData);
            setDialog(null);
            setNotice('Đã kích hoạt quyền xem khẩn cấp thành công. Lượt truy cập này đã được ghi nhận vào nhật ký kiểm toán.');
          }}
        />
      )}
      {dialog?.type === 'edit' && (
        <EditPatientDialog
          patient={p}
          onClose={() => setDialog(null)}
          onSaved={(patient) => {
            setData((d) => ({ ...d, patient }));
            setDialog(null);
            setNotice('Đã lưu thông tin hồ sơ.');
          }}
        />
      )}
      {dialog?.type === 'view' && !isReceptionist && (
        <ResultDialog
          record={dialog.record}
          patient={p}
          locked={p.isLocked}
          onClose={() => setDialog(null)}
          onEdit={canEditRecord ? () => setDialog({ type: 'editRecord', record: dialog.record }) : undefined}
        />
      )}
      {dialog?.type === 'editRecord' && (
        <EditRecordDialog
          record={dialog.record}
          patient={p}
          onClose={() => setDialog(null)}
          onSaved={(rec) => {
            replaceRecord(rec);
            setDialog(null);
            setNotice(`Đã cập nhật phiếu kết quả khám cho phiếu khám ${rec.appointment?.ticketCode}. Bệnh nhân sẽ thấy nội dung mới trong tài khoản website.`);
          }}
        />
      )}
      {dialog?.type === 'history' && (
        <AppointmentHistoryDialog
          appt={dialog.appt}
          onClose={() => setDialog(null)}
        />
      )}
    </>
  );
}
