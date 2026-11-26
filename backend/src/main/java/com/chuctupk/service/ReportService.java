package com.chuctupk.service;

import com.chuctupk.dto.response.ReportSummaryResponse;

import java.time.LocalDate;

/**
 * Dịch vụ tổng hợp báo cáo hoạt động khám bệnh và xuất file CSV cho quản trị viên.
 *
 * <h2>Định nghĩa số liệu chuẩn (chốt nghiệp vụ Sprint 13)</h2>
 * <ul>
 *   <li><b>Lượt đặt</b>: Số phiếu có {@code appointment_date} trong kỳ (mọi trạng thái trừ phiếu test/xoá mềm nếu có).</li>
 *   <li><b>Lượt khám</b>: Trong số đó, {@code status = COMPLETED}.</li>
 *   <li><b>Đã huỷ</b>: {@code status = CANCELLED} (do người hoặc đổi giờ).</li>
 *   <li><b>Không đến</b>: {@code status = NO_SHOW} (task tự chuyển lúc {@code no_show_cancel_at}).</li>
 *   <li><b>Bệnh nhân mới</b>: Bệnh nhân có lần khám hoàn tất đầu tiên ({@code MIN(appointment_date)} với {@code status = COMPLETED}) nằm trong kỳ.</li>
 *   <li><b>Bệnh nhân quay lại</b>: Có lượt khám hoàn tất ({@code status = COMPLETED}) trong kỳ và có ít nhất 1 lượt khám hoàn tất trước kỳ.</li>
 *   <li><b>Tỷ lệ lấp đầy</b>: {@code lượt đặt / (số khung active trong ngày × time_slots.max_patients)},
 *       làm tròn 1 số lẻ — bắt buộc nhãn UI: <i>"ước tính theo khung giờ hiện tại"</i>.
 *       Không được gọi là "công suất thật" vì lịch trực quá khứ có thể đã bị sửa.</li>
 * </ul>
 */
public interface ReportService {

    /**
     * Tổng hợp báo cáo theo khoảng ngày {@code [from, to]}.
     * Nếu thiếu tham số, mặc định {@code to = hôm nay}, {@code from = hôm nay - 6 ngày} theo bean {@code Clock}.
     */
    ReportSummaryResponse getSummary(LocalDate from, LocalDate to);

    /**
     * Xuất file CSV UTF-8 (có BOM {@code \uFEFF}, dấu phân cách {@code ;}, xuống dòng {@code \r\n})
     * tương thích Excel bản tiếng Việt trên Windows.
     */
    CsvExportResult exportCsv(LocalDate from, LocalDate to, String type);

    record CsvExportResult(String filename, byte[] content) {}
}
