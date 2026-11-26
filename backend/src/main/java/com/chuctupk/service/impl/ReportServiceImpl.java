package com.chuctupk.service.impl;

import com.chuctupk.dto.response.ReportSummaryResponse;
import com.chuctupk.dto.response.ReportSummaryResponse.DayItem;
import com.chuctupk.dto.response.ReportSummaryResponse.DoctorItem;
import com.chuctupk.dto.response.ReportSummaryResponse.SlotItem;
import com.chuctupk.dto.response.ReportSummaryResponse.Totals;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.TimeSlotRepository;
import com.chuctupk.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final int MAX_RANGE_DAYS = 366;
    private static final String UNASSIGNED_DOCTOR_NAME = "Chưa gán";
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter VN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_HM = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public ReportSummaryResponse getSummary(LocalDate from, LocalDate to) {
        DateRange range = resolveAndValidateRange(from, to);
        LocalDate start = range.from();
        LocalDate end = range.to();
        long daysInPeriod = ChronoUnit.DAYS.between(start, end) + 1;

        // 1. Gom theo ngày + trạng thái (1 query)
        List<Object[]> dayRows = appointmentRepository.countByDayAndStatus(start, end);
        // 2. Gom theo bác sĩ + trạng thái (1 query, LEFT JOIN để giữ phiếu chưa gán bác sĩ)
        List<Object[]> doctorRows = appointmentRepository.countByDoctorAndStatus(start, end);
        // 3. Gom theo khung giờ + trạng thái (1 query)
        List<Object[]> slotRows = appointmentRepository.countBySlotAndStatus(start, end);
        // 4. Tổng số bệnh nhân có lượt khám hoàn tất (COMPLETED) trong kỳ (1 query)
        long distinctCompletedPatients = appointmentRepository.countDistinctCompletedPatientsInPeriod(start, end);
        // 5. Danh sách bệnh nhân có lần khám hoàn tất đầu tiên (MIN(appointment_date)) trong kỳ (1 query)
        int newPatients = appointmentRepository.findNewPatientIdsInPeriod(start, end).size();
        int returningPatients = (int) Math.max(0L, distinctCompletedPatients - newPatients);
        // 6. Danh sách khung giờ để tính tỷ lệ lấp đầy ước tính (1 query)
        List<TimeSlot> allSlots = timeSlotRepository.findAllByOrderBySortOrderAsc();

        long dailyCapacity = allSlots.stream()
                .filter(ts -> Boolean.TRUE.equals(ts.getIsActive()))
                .mapToLong(ts -> ts.getMaxPatients() != null ? ts.getMaxPatients() : 0)
                .sum();

        Map<LocalDate, StatusCounts> dayMap = new HashMap<>();
        int totalBooked = 0;
        int totalCompleted = 0;
        int totalCancelled = 0;
        int totalNoShow = 0;

        for (Object[] row : dayRows) {
            LocalDate date = (LocalDate) row[0];
            AppointmentStatus status = (AppointmentStatus) row[1];
            int count = ((Number) row[2]).intValue();

            StatusCounts counts = dayMap.computeIfAbsent(date, k -> new StatusCounts());
            counts.add(status, count);

            totalBooked += count;
            if (status == AppointmentStatus.COMPLETED) {
                totalCompleted += count;
            } else if (status == AppointmentStatus.CANCELLED) {
                totalCancelled += count;
            } else if (status == AppointmentStatus.NO_SHOW) {
                totalNoShow += count;
            }
        }

        List<DayItem> byDay = new ArrayList<>((int) daysInPeriod);
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            StatusCounts counts = dayMap.get(d);
            int booked = counts != null ? counts.booked : 0;
            int completed = counts != null ? counts.completed : 0;
            int cancelled = counts != null ? counts.cancelled : 0;
            int noShow = counts != null ? counts.noShow : 0;
            double fillRatePct = dailyCapacity > 0
                    ? roundOneDecimal(booked * 100.0 / dailyCapacity)
                    : 0.0;
            byDay.add(new DayItem(
                    d.format(ISO_DATE),
                    booked,
                    completed,
                    cancelled,
                    noShow,
                    fillRatePct
            ));
        }

        Map<Long, DoctorAccumulator> doctorMap = new LinkedHashMap<>();
        for (Object[] row : doctorRows) {
            Long doctorId = row[0] != null ? ((Number) row[0]).longValue() : null;
            String doctorName = doctorId != null && row[1] != null
                    ? String.valueOf(row[1])
                    : UNASSIGNED_DOCTOR_NAME;
            AppointmentStatus status = (AppointmentStatus) row[2];
            int count = ((Number) row[3]).intValue();

            DoctorAccumulator acc = doctorMap.computeIfAbsent(
                    doctorId,
                    k -> new DoctorAccumulator(doctorId, doctorName)
            );
            acc.counts.add(status, count);
        }

        List<DoctorItem> byDoctor = doctorMap.values().stream()
                .sorted(Comparator
                        .comparing((DoctorAccumulator a) -> a.doctorId == null)
                        .thenComparing(Comparator.comparingInt((DoctorAccumulator a) -> a.counts.completed).reversed())
                        .thenComparing(Comparator.comparingInt((DoctorAccumulator a) -> a.counts.booked).reversed())
                        .thenComparing(a -> a.doctorName, String.CASE_INSENSITIVE_ORDER))
                .map(a -> new DoctorItem(
                        a.doctorId,
                        a.doctorName,
                        a.counts.booked,
                        a.counts.completed,
                        a.counts.cancelled,
                        a.counts.noShow
                ))
                .toList();

        Map<Long, SlotAccumulator> slotMap = new LinkedHashMap<>();
        for (TimeSlot ts : allSlots) {
            if (Boolean.TRUE.equals(ts.getIsActive())) {
                slotMap.put(ts.getId(), new SlotAccumulator(
                        ts.getId(),
                        resolveSlotLabel(ts.getSlotLabel(), ts.getStartTime(), ts.getEndTime()),
                        ts.getMaxPatients() != null ? ts.getMaxPatients() : 0
                ));
            }
        }

        for (Object[] row : slotRows) {
            Long slotId = ((Number) row[0]).longValue();
            String label = resolveSlotLabel(
                    row[1] != null ? String.valueOf(row[1]) : null,
                    (LocalTime) row[2],
                    (LocalTime) row[3]
            );
            int maxPatients = row[4] != null ? ((Number) row[4]).intValue() : 0;
            AppointmentStatus status = (AppointmentStatus) row[6];
            int count = ((Number) row[7]).intValue();

            SlotAccumulator acc = slotMap.computeIfAbsent(
                    slotId,
                    k -> new SlotAccumulator(slotId, label, maxPatients)
            );
            acc.counts.add(status, count);
        }

        List<SlotItem> bySlot = slotMap.values().stream()
                .map(a -> {
                    long slotCapacity = (long) a.maxPatients * daysInPeriod;
                    double fillRatePct = slotCapacity > 0
                            ? roundOneDecimal(a.counts.booked * 100.0 / slotCapacity)
                            : 0.0;
                    return new SlotItem(
                            a.slotId,
                            a.slotLabel,
                            a.counts.booked,
                            a.counts.completed,
                            fillRatePct
                    );
                })
                .toList();

        Totals totals = new Totals(
                totalBooked,
                totalCompleted,
                totalCancelled,
                totalNoShow,
                newPatients,
                returningPatients
        );

        String generatedAt = LocalDateTime.now(clock).format(ISO_DATE_TIME);
        return new ReportSummaryResponse(
                start.format(ISO_DATE),
                end.format(ISO_DATE),
                generatedAt,
                totals,
                byDay,
                byDoctor,
                bySlot
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CsvExportResult exportCsv(LocalDate from, LocalDate to, String type) {
        String normalizedType = (type == null || type.isBlank()) ? "summary" : type.trim();
        if (!"summary".equals(normalizedType) && !"byDoctor".equals(normalizedType) && !"bySlot".equals(normalizedType)) {
            throw AppException.unprocessable("Loại báo cáo CSV không hợp lệ");
        }

        ReportSummaryResponse summary = getSummary(from, to);
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');

        switch (normalizedType) {
            case "summary" -> {
                sb.append("Ngày;Lượt đặt;Lượt khám;Đã huỷ;Không đến\r\n");
                for (DayItem day : summary.byDay()) {
                    LocalDate parsed = LocalDate.parse(day.date(), ISO_DATE);
                    appendCsvRow(sb,
                            parsed.format(VN_DATE),
                            String.valueOf(day.booked()),
                            String.valueOf(day.completed()),
                            String.valueOf(day.cancelled()),
                            String.valueOf(day.noShow())
                    );
                }
            }
            case "byDoctor" -> {
                sb.append("Bác sĩ;Lượt đặt;Lượt khám;Đã huỷ;Không đến\r\n");
                for (DoctorItem doc : summary.byDoctor()) {
                    appendCsvRow(sb,
                            doc.doctorName(),
                            String.valueOf(doc.booked()),
                            String.valueOf(doc.completed()),
                            String.valueOf(doc.cancelled()),
                            String.valueOf(doc.noShow())
                    );
                }
            }
            case "bySlot" -> {
                sb.append("Khung giờ;Lượt đặt;Lượt khám;Tỷ lệ lấp đầy (%)\r\n");
                for (SlotItem slot : summary.bySlot()) {
                    appendCsvRow(sb,
                            slot.slotLabel(),
                            String.valueOf(slot.booked()),
                            String.valueOf(slot.completed()),
                            String.format(Locale.US, "%.1f", slot.fillRatePct())
                    );
                }
            }
            default -> throw AppException.unprocessable("Loại báo cáo CSV không hợp lệ");
        }

        String prefix = switch (normalizedType) {
            case "byDoctor" -> "bao-cao-bac-si";
            case "bySlot" -> "bao-cao-khung-gio";
            default -> "bao-cao";
        };
        String filename = String.format("%s-%s_%s.csv", prefix, summary.from(), summary.to());
        return new CsvExportResult(filename, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private DateRange resolveAndValidateRange(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock);
        LocalDate resolvedTo = to != null ? to : today;
        LocalDate resolvedFrom = from != null ? from : resolvedTo.minusDays(6);

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw AppException.unprocessable("Ngày bắt đầu phải trước ngày kết thúc");
        }
        long diffDays = ChronoUnit.DAYS.between(resolvedFrom, resolvedTo);
        if (diffDays > MAX_RANGE_DAYS) {
            throw AppException.unprocessable("Khoảng báo cáo tối đa 366 ngày");
        }
        return new DateRange(resolvedFrom, resolvedTo);
    }

    private static String resolveSlotLabel(String slotLabel, LocalTime startTime, LocalTime endTime) {
        if (slotLabel != null && !slotLabel.isBlank()) {
            return slotLabel.trim();
        }
        String start = startTime != null ? startTime.format(TIME_HM) : "";
        String end = endTime != null ? endTime.format(TIME_HM) : "";
        return start + " - " + end;
    }

    private static double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static void appendCsvRow(StringBuilder sb, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(escapeCsvCell(cells[i]));
        }
        sb.append("\r\n");
    }

    private static String escapeCsvCell(String raw) {
        if (raw == null) {
            return "";
        }
        boolean needsQuotes = raw.indexOf(';') >= 0
                || raw.indexOf('"') >= 0
                || raw.indexOf('\n') >= 0
                || raw.indexOf('\r') >= 0;
        if (!needsQuotes) {
            return raw;
        }
        return "\"" + raw.replace("\"", "\"\"") + "\"";
    }

    private record DateRange(LocalDate from, LocalDate to) {}

    private static final class StatusCounts {
        int booked;
        int completed;
        int cancelled;
        int noShow;

        void add(AppointmentStatus status, int count) {
            booked += count;
            if (status == AppointmentStatus.COMPLETED) {
                completed += count;
            } else if (status == AppointmentStatus.CANCELLED) {
                cancelled += count;
            } else if (status == AppointmentStatus.NO_SHOW) {
                noShow += count;
            }
        }
    }

    private static final class DoctorAccumulator {
        final Long doctorId;
        final String doctorName;
        final StatusCounts counts = new StatusCounts();

        DoctorAccumulator(Long doctorId, String doctorName) {
            this.doctorId = doctorId;
            this.doctorName = doctorName;
        }
    }

    private static final class SlotAccumulator {
        final Long slotId;
        final String slotLabel;
        final int maxPatients;
        final StatusCounts counts = new StatusCounts();

        SlotAccumulator(Long slotId, String slotLabel, int maxPatients) {
            this.slotId = slotId;
            this.slotLabel = slotLabel;
            this.maxPatients = maxPatients;
        }
    }
}
