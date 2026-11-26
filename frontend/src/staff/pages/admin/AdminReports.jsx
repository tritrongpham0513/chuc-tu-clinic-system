import { useCallback, useEffect, useState } from 'react';
import { api, qs } from '../../../shared/lib/api.js';
import { addDays, fmtDate, todayVN } from '../../../shared/lib/format.js';
import { useDocumentTitle } from '../../../shared/lib/hooks.js';
import { Icon } from '../../../shared/ui/Icon.jsx';
import './admin.css';

const PRESETS = [
  { key: 'this_week', label: 'Tuần này', comparePrefix: 'Tuần trước' },
  { key: 'last_week', label: 'Tuần trước', comparePrefix: 'Tuần trước đó' },
  { key: 'this_month', label: 'Tháng này', comparePrefix: 'Tháng trước' },
  { key: 'last_month', label: 'Tháng trước', comparePrefix: 'Tháng trước đó' },
  { key: 'custom', label: 'Tùy chọn', comparePrefix: 'Kỳ trước' },
];

const pad2 = (n) => String(n).padStart(2, '0');

function parseIsoUtc(isoDate) {
  return new Date(`${isoDate}T00:00:00Z`);
}

function diffDaysInclusive(from, to) {
  const ms = parseIsoUtc(to).getTime() - parseIsoUtc(from).getTime();
  return Math.round(ms / 86400_000) + 1;
}

/** Thứ Hai đầu tuần theo lịch Việt Nam. */
function startOfWeekVN(isoDate) {
  const d = parseIsoUtc(isoDate);
  const dow = d.getUTCDay(); // 0 = CN, 1 = T2 ... 6 = T7
  const diffToMonday = dow === 0 ? -6 : 1 - dow;
  return addDays(isoDate, diffToMonday);
}

function monthRange(year, month1to12) {
  const first = `${year}-${pad2(month1to12)}-01`;
  const lastDay = new Date(Date.UTC(year, month1to12, 0)).getUTCDate();
  const last = `${year}-${pad2(month1to12)}-${pad2(lastDay)}`;
  return { from: first, to: last };
}

function computePresetRange(presetKey) {
  const today = todayVN();
  const [y, m] = today.split('-').map(Number);

  if (presetKey === 'this_week') {
    const mon = startOfWeekVN(today);
    return { from: mon, to: addDays(mon, 6) };
  }
  if (presetKey === 'last_week') {
    const thisMon = startOfWeekVN(today);
    const lastMon = addDays(thisMon, -7);
    return { from: lastMon, to: addDays(lastMon, 6) };
  }
  if (presetKey === 'this_month') {
    return monthRange(y, m);
  }
  if (presetKey === 'last_month') {
    const prevYear = m === 1 ? y - 1 : y;
    const prevMonth = m === 1 ? 12 : m - 1;
    return monthRange(prevYear, prevMonth);
  }
  return { from: addDays(today, -6), to: today };
}

function computePreviousRange(presetKey, from, to) {
  if (presetKey === 'this_month') {
    const [y, m] = from.split('-').map(Number);
    const prevYear = m === 1 ? y - 1 : y;
    const prevMonth = m === 1 ? 12 : m - 1;
    return monthRange(prevYear, prevMonth);
  }
  if (presetKey === 'last_month') {
    const [y, m] = from.split('-').map(Number);
    const prevYear = m === 1 ? y - 1 : y;
    const prevMonth = m === 1 ? 12 : m - 1;
    return monthRange(prevYear, prevMonth);
  }
  const span = diffDaysInclusive(from, to);
  const prevTo = addDays(from, -1);
  const prevFrom = addDays(prevTo, -(span - 1));
  return { from: prevFrom, to: prevTo };
}

function formatComparison(prefix, currentVal, prevTotals, field) {
  if (!prevTotals || typeof prevTotals[field] !== 'number') {
    return `${prefix}: —`;
  }
  const prevVal = prevTotals[field];
  const delta = currentVal - prevVal;
  const sign = delta > 0 ? `+${delta}` : String(delta);
  return `${prefix}: ${prevVal} (${sign})`;
}

function FillRateBar({ pct }) {
  const safePct = Number.isFinite(pct) ? pct : 0;
  const widthPct = Math.min(100, Math.max(0, safePct));
  return (
    <div className="rpt-fill">
      <div className="rpt-fill-track" aria-hidden="true">
        <div className="rpt-fill-bar" style={{ width: `${widthPct}%`, backgroundColor: '#5C3A21' }} />
      </div>
      <span className="rpt-fill-pct">{safePct.toFixed(1)}%</span>
    </div>
  );
}

function CsvButton({ type, downloadingType, onDownload }) {
  const isDownloading = downloadingType === type;
  return (
    <button
      type="button"
      className="t-btn t-btn-ghost rpt-csv-btn"
      disabled={Boolean(downloadingType)}
      onClick={() => onDownload(type)}
    >
      <Icon name="file" size={20} strokeWidth={1.75} />
      <span>{isDownloading ? 'Đang tạo file…' : 'Tải CSV cho Excel'}</span>
    </button>
  );
}

export default function AdminReports() {
  useDocumentTitle('Báo cáo hoạt động');

  const initial = computePresetRange('this_week');
  const [preset, setPreset] = useState('this_week');
  const [appliedRange, setAppliedRange] = useState(initial);
  const [customFrom, setCustomFrom] = useState(initial.from);
  const [customTo, setCustomTo] = useState(initial.to);
  const [customValidationError, setCustomValidationError] = useState('');

  const [report, setReport] = useState(null);
  const [prevTotals, setPrevTotals] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [downloadingType, setDownloadingType] = useState('');
  const [csvError, setCsvError] = useState('');

  const loadReports = useCallback(async (presetKey, from, to) => {
    setLoading(true);
    setError(null);
    setCsvError('');

    const prevRange = computePreviousRange(presetKey, from, to);

    try {
      const [currentData, prevData] = await Promise.all([
        api.get(`/admin/reports/summary${qs({ from, to })}`),
        api.get(`/admin/reports/summary${qs({ from: prevRange.from, to: prevRange.to })}`).catch(() => null),
      ]);
      setReport(currentData);
      setPrevTotals(prevData?.totals || null);
    } catch (err) {
      setReport(null);
      setPrevTotals(null);
      setError(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadReports(preset, appliedRange.from, appliedRange.to);
  }, [loadReports, preset, appliedRange.from, appliedRange.to]);

  const handleSelectPreset = (key) => {
    setPreset(key);
    setCustomValidationError('');
    if (key === 'custom') {
      setCustomFrom(appliedRange.from);
      setCustomTo(appliedRange.to);
      return;
    }
    const next = computePresetRange(key);
    setCustomFrom(next.from);
    setCustomTo(next.to);
    setAppliedRange(next);
  };

  const handleApplyCustom = (e) => {
    e.preventDefault();
    if (!customFrom || !customTo) {
      setCustomValidationError('Vui lòng chọn đủ Ngày bắt đầu và Ngày kết thúc.');
      return;
    }
    if (customFrom > customTo) {
      setCustomValidationError('Ngày bắt đầu phải trước ngày kết thúc.');
      return;
    }
    const span = diffDaysInclusive(customFrom, customTo) - 1;
    if (span > 366) {
      setCustomValidationError('Khoảng báo cáo tối đa 366 ngày.');
      return;
    }
    setCustomValidationError('');
    setAppliedRange({ from: customFrom, to: customTo });
  };

  const handleDownloadCsv = async (type) => {
    if (downloadingType) return;
    setDownloadingType(type);
    setCsvError('');

    try {
      const query = qs({ from: appliedRange.from, to: appliedRange.to, type });
      const { blob, disposition } = await api.download(`/admin/reports/export${query}`);
      const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
      const plainMatch = disposition.match(/filename="?([^";]+)"?/i);
      const fallbackPrefix = type === 'byDoctor' ? 'bao-cao-bac-si' : type === 'bySlot' ? 'bao-cao-khung-gio' : 'bao-cao';
      const filename = utf8Match
        ? decodeURIComponent(utf8Match[1])
        : plainMatch?.[1] || `${fallbackPrefix}-${appliedRange.from}_${appliedRange.to}.csv`;

      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      setCsvError(err?.message || 'Không tạo được file CSV. Vui lòng thử lại.');
    } finally {
      setDownloadingType('');
    }
  };

  const activePresetMeta = PRESETS.find((p) => p.key === preset) || PRESETS[0];
  const comparePrefix = activePresetMeta.comparePrefix;
  const isNetworkOrServerError = !error || error.status === 0 || error.status >= 500;
  const errorText = isNetworkOrServerError
    ? 'Không tải được số liệu. Vui lòng thử lại.'
    : error.message || 'Không tải được số liệu. Vui lòng thử lại.';

  return (
    <div className="rpt-page">
      <div className="rpt-head">
        <div>
          <h1 className="t-h1" style={{ fontSize: '28px', lineHeight: '36px', marginBottom: 6 }}>
            Báo cáo hoạt động
          </h1>
          <p className="t-text" style={{ fontSize: '18px' }}>
            Kỳ báo cáo: <strong>{fmtDate(appliedRange.from)}</strong> đến <strong>{fmtDate(appliedRange.to)}</strong>
          </p>
        </div>
      </div>

      {/* 1. Chọn kỳ */}
      <section className="db-panel" aria-labelledby="rpt-chon-ky">
        <h2 id="rpt-chon-ky" className="t-h2">
          Chọn kỳ báo cáo
        </h2>
        <div className="rpt-presets" role="group" aria-label="Chọn khoảng thời gian báo cáo">
          {PRESETS.map((p) => {
            const active = preset === p.key;
            return (
              <button
                key={p.key}
                type="button"
                className={`rpt-preset-btn ${active ? 'is-active' : ''}`}
                aria-pressed={active}
                onClick={() => handleSelectPreset(p.key)}
              >
                {p.label}
              </button>
            );
          })}
        </div>

        {preset === 'custom' && (
          <form className="rpt-custom-form" onSubmit={handleApplyCustom} noValidate>
            <div className="t-field">
              <label htmlFor="rpt-from" className="t-label" style={{ fontSize: '17px' }}>
                Từ ngày
              </label>
              <input
                id="rpt-from"
                type="date"
                className="t-input rpt-date-input"
                value={customFrom}
                onChange={(e) => setCustomFrom(e.target.value)}
              />
            </div>
            <div className="t-field">
              <label htmlFor="rpt-to" className="t-label" style={{ fontSize: '17px' }}>
                Đến ngày
              </label>
              <input
                id="rpt-to"
                type="date"
                className="t-input rpt-date-input"
                value={customTo}
                onChange={(e) => setCustomTo(e.target.value)}
              />
            </div>
            <button type="submit" className="t-btn t-btn-primary rpt-apply-btn">
              Xem báo cáo
            </button>
          </form>
        )}

        {customValidationError && (
          <div className="t-error" role="alert" style={{ fontSize: '17px' }}>
            <Icon name="alert" size={20} />
            <span>{customValidationError}</span>
          </div>
        )}
      </section>

      {csvError && (
        <div className="t-tint-red rpt-alert-box" role="alert">
          <Icon name="alert" size={22} />
          <span>{csvError}</span>
        </div>
      )}

      {/* 4 trạng thái bắt buộc */}
      {loading ? (
        <section className="db-panel rpt-state-box" role="status" aria-live="polite">
          <p className="rpt-state-text">Đang tải số liệu…</p>
        </section>
      ) : error ? (
        <section className="db-panel rpt-state-box" role="alert">
          <p className="rpt-state-title" style={{ color: 'var(--alert-700)' }}>
            {errorText}
          </p>
          <button
            type="button"
            className="t-btn t-btn-primary rpt-retry-btn"
            onClick={() => loadReports(preset, appliedRange.from, appliedRange.to)}
          >
            <Icon name="refresh" size={20} />
            <span>Thử lại</span>
          </button>
        </section>
      ) : !report || report.totals.booked === 0 ? (
        <section className="db-panel rpt-state-box" role="status">
          <p className="rpt-state-title">Kỳ này chưa có phiếu khám nào.</p>
          <p className="t-text" style={{ fontSize: '17px' }}>
            Cô chú có thể chọn khoảng thời gian khác ở phía trên để xem số liệu.
          </p>
        </section>
      ) : (
        <>
          {/* 2. 4 ô số lớn */}
          <section aria-labelledby="rpt-tong-quan">
            <div className="rpt-summary-bar">
              <h2 id="rpt-tong-quan" className="t-h2">
                Tổng hợp trong kỳ
              </h2>
              <p className="t-text" style={{ fontSize: '17px' }}>
                Tổng lượt đặt: <strong>{report.totals.booked}</strong> phiếu · Bệnh nhân quay lại:{' '}
                <strong>{report.totals.returningPatients}</strong> người
              </p>
            </div>
            <div className="rpt-kpis">
              <div className="rpt-kpi rpt-kpi-green">
                <span className="rpt-kpi-label">Lượt khám</span>
                <span className="rpt-kpi-value">{report.totals.completed}</span>
                <span className="rpt-kpi-compare">
                  {formatComparison(comparePrefix, report.totals.completed, prevTotals, 'completed')}
                </span>
              </div>
              <div className="rpt-kpi rpt-kpi-gold">
                <span className="rpt-kpi-label">Đã huỷ</span>
                <span className="rpt-kpi-value">{report.totals.cancelled}</span>
                <span className="rpt-kpi-compare">
                  {formatComparison(comparePrefix, report.totals.cancelled, prevTotals, 'cancelled')}
                </span>
              </div>
              <div className="rpt-kpi rpt-kpi-alert">
                <span className="rpt-kpi-label">Không đến</span>
                <span className="rpt-kpi-value">{report.totals.noShow}</span>
                <span className="rpt-kpi-compare">
                  {formatComparison(comparePrefix, report.totals.noShow, prevTotals, 'noShow')}
                </span>
              </div>
              <div className="rpt-kpi">
                <span className="rpt-kpi-label">Bệnh nhân mới</span>
                <span className="rpt-kpi-value">{report.totals.newPatients}</span>
                <span className="rpt-kpi-compare">
                  {formatComparison(comparePrefix, report.totals.newPatients, prevTotals, 'newPatients')}
                </span>
              </div>
            </div>
          </section>

          {/* 3. Bảng Theo ngày */}
          <section className="db-panel" aria-labelledby="rpt-theo-ngay">
            <div className="db-panel-head" style={{ alignItems: 'center' }}>
              <div>
                <h2 id="rpt-theo-ngay" className="t-h2">
                  Theo ngày
                </h2>
                <p className="t-muted" style={{ fontSize: '16px', marginTop: 4 }}>
                  Tỷ lệ lấp đầy là ước tính theo khung giờ hiện tại.
                </p>
              </div>
              <CsvButton type="summary" downloadingType={downloadingType} onDownload={handleDownloadCsv} />
            </div>
            <div className="t-scroll">
              <table className="rpt-table rpt-table-day">
                <thead>
                  <tr>
                    <th>Ngày</th>
                    <th className="num">Lượt đặt</th>
                    <th className="num">Lượt khám</th>
                    <th className="num">Huỷ</th>
                    <th className="num">Không đến</th>
                    <th>Tỷ lệ lấp đầy (ước tính theo khung giờ hiện tại)</th>
                  </tr>
                </thead>
                <tbody>
                  {report.byDay.map((row) => (
                    <tr key={row.date}>
                      <td className="strong">{fmtDate(row.date)}</td>
                      <td className="num">{row.booked}</td>
                      <td className="num">{row.completed}</td>
                      <td className="num">{row.cancelled}</td>
                      <td className="num">{row.noShow}</td>
                      <td>
                        <FillRateBar pct={row.fillRatePct} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          {/* 4. Bảng Theo bác sĩ */}
          <section className="db-panel" aria-labelledby="rpt-theo-bac-si">
            <div className="db-panel-head" style={{ alignItems: 'center' }}>
              <h2 id="rpt-theo-bac-si" className="t-h2">
                Theo bác sĩ
              </h2>
              <CsvButton type="byDoctor" downloadingType={downloadingType} onDownload={handleDownloadCsv} />
            </div>
            <div className="t-scroll">
              <table className="rpt-table rpt-table-doctor">
                <thead>
                  <tr>
                    <th>Bác sĩ</th>
                    <th className="num">Lượt đặt</th>
                    <th className="num">Lượt khám</th>
                    <th className="num">Đã huỷ</th>
                    <th className="num">Không đến</th>
                  </tr>
                </thead>
                <tbody>
                  {report.byDoctor.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="t-muted">
                        Chưa có dữ liệu phân công bác sĩ trong kỳ này.
                      </td>
                    </tr>
                  ) : (
                    report.byDoctor.map((doc) => (
                      <tr key={doc.doctorId ?? 'unassigned'}>
                        <td className="strong">{doc.doctorName}</td>
                        <td className="num">{doc.booked}</td>
                        <td className="num">{doc.completed}</td>
                        <td className="num">{doc.cancelled}</td>
                        <td className="num">{doc.noShow}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </section>

          {/* 5. Bảng Theo khung giờ */}
          <section className="db-panel" aria-labelledby="rpt-theo-khung-gio">
            <div className="db-panel-head" style={{ alignItems: 'center' }}>
              <div>
                <h2 id="rpt-theo-khung-gio" className="t-h2">
                  Theo khung giờ
                </h2>
                <p className="t-muted" style={{ fontSize: '16px', marginTop: 4 }}>
                  Tỷ lệ lấp đầy là ước tính theo khung giờ hiện tại.
                </p>
              </div>
              <CsvButton type="bySlot" downloadingType={downloadingType} onDownload={handleDownloadCsv} />
            </div>
            <div className="t-scroll">
              <table className="rpt-table rpt-table-slot">
                <thead>
                  <tr>
                    <th>Khung giờ</th>
                    <th className="num">Lượt đặt</th>
                    <th className="num">Lượt khám</th>
                    <th>Tỷ lệ lấp đầy (ước tính theo khung giờ hiện tại)</th>
                  </tr>
                </thead>
                <tbody>
                  {report.bySlot.map((slot) => (
                    <tr key={slot.slotId}>
                      <td className="strong">{slot.slotLabel}</td>
                      <td className="num">{slot.booked}</td>
                      <td className="num">{slot.completed}</td>
                      <td>
                        <FillRateBar pct={slot.fillRatePct} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}

      {/* 6. Chân trang: định nghĩa số liệu */}
      <footer className="rpt-footer" aria-label="Giải thích định nghĩa số liệu">
        <h2 className="t-h2" style={{ marginBottom: 10 }}>
          Chú thích định nghĩa số liệu
        </h2>
        <ul className="rpt-defs">
          <li>
            <strong>Lượt đặt:</strong> Số phiếu có ngày khám nằm trong kỳ (mọi trạng thái).
          </li>
          <li>
            <strong>Lượt khám:</strong> Trong số lượt đặt, các phiếu đã hoàn tất khám.
          </li>
          <li>
            <strong>Đã huỷ:</strong> Phiếu bị huỷ (do người bệnh, tiếp tân hoặc đổi giờ).
          </li>
          <li>
            <strong>Không đến:</strong> Phiếu tự động chuyển sang không đến vào cuối ngày do người bệnh không có mặt.
          </li>
          <li>
            <strong>Bệnh nhân mới:</strong> Bệnh nhân có lượt khám hoàn tất đầu tiên nằm trong kỳ báo cáo.
          </li>
          <li>
            <strong>Bệnh nhân quay lại:</strong> Bệnh nhân có lượt khám hoàn tất trong kỳ và đã từng có ít nhất 1 lượt khám hoàn tất trước kỳ.
          </li>
          <li>
            <strong>Tỷ lệ lấp đầy:</strong> Tỷ lệ lấp đầy là ước tính theo khung giờ hiện tại (tính trên sức chứa các khung giờ đang mở hiện nay).
          </li>
        </ul>
      </footer>
    </div>
  );
}
