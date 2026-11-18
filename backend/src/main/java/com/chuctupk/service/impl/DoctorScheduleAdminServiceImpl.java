package com.chuctupk.service.impl;

import com.chuctupk.dto.request.CreateDoctorDayOffRequest;
import com.chuctupk.dto.request.ScheduleItemRequest;
import com.chuctupk.dto.request.UpdateDoctorScheduleRequest;
import com.chuctupk.dto.response.CreateDoctorDayOffResult;
import com.chuctupk.dto.response.DoctorDayOffDto;
import com.chuctupk.dto.response.DoctorScheduleMatrixResponse;
import com.chuctupk.entity.Doctor;
import com.chuctupk.entity.DoctorDayOff;
import com.chuctupk.entity.DoctorSchedule;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.DayOfWeek;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.DoctorDayOffRepository;
import com.chuctupk.repository.DoctorRepository;
import com.chuctupk.repository.DoctorScheduleRepository;
import com.chuctupk.service.DoctorScheduleAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorScheduleAdminServiceImpl implements DoctorScheduleAdminService {

    private static final List<String> ORDERED_DAYS = List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");

    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorDayOffRepository doctorDayOffRepository;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public DoctorScheduleMatrixResponse getWeeklyScheduleMatrix() {
        List<Doctor> doctors = doctorRepository.findByIsActiveTrueOrderBySortOrderAsc();
        List<DoctorScheduleMatrixResponse.DoctorScheduleRow> rows = new ArrayList<>();

        for (Doctor doctor : doctors) {
            Map<String, List<String>> scheduleMap = new LinkedHashMap<>();
            for (String day : ORDERED_DAYS) {
                scheduleMap.put(day, new ArrayList<>());
            }

            List<DoctorSchedule> activeSchedules = doctorScheduleRepository.findByDoctorIdAndIsActiveTrue(doctor.getId());
            for (DoctorSchedule ds : activeSchedules) {
                if (ds.getDayOfWeek() == null || ds.getPeriod() == null) {
                    continue;
                }
                String dayKey = ds.getDayOfWeek().name();
                List<String> periodList = scheduleMap.get(dayKey);
                if (periodList == null) {
                    continue;
                }

                if (ds.getPeriod() == Period.FULL_DAY) {
                    if (!periodList.contains("MORNING")) periodList.add("MORNING");
                    if (!periodList.contains("AFTERNOON")) periodList.add("AFTERNOON");
                } else {
                    String pName = ds.getPeriod().name();
                    if (!periodList.contains(pName)) {
                        periodList.add(pName);
                    }
                }
            }

            // Sắp xếp MORNING trước AFTERNOON trong từng ngày
            for (List<String> periodList : scheduleMap.values()) {
                periodList.sort((a, b) -> {
                    if (a.equals(b)) return 0;
                    return "MORNING".equals(a) ? -1 : 1;
                });
            }

            rows.add(DoctorScheduleMatrixResponse.DoctorScheduleRow.builder()
                    .id(doctor.getId())
                    .fullName(doctor.getFullName())
                    .title(doctor.getTitle())
                    .specialty(doctor.getSpecialty())
                    .isActive(doctor.getIsActive())
                    .schedules(scheduleMap)
                    .build());
        }

        return DoctorScheduleMatrixResponse.builder()
                .days(ORDERED_DAYS)
                .doctors(rows)
                .build();
    }

    @Override
    @Transactional
    public DoctorScheduleMatrixResponse updateDoctorSchedule(Long doctorId, UpdateDoctorScheduleRequest request) {
        if (doctorId == null) {
            throw AppException.badRequest("Mã bác sĩ không được để trống");
        }
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy bác sĩ"));
        if (!Boolean.TRUE.equals(doctor.getIsActive())) {
            throw AppException.unprocessable("Bác sĩ hiện không còn hoạt động");
        }

        if (request.schedules() == null || request.schedules().isEmpty()) {
            throw AppException.badRequest("Lịch trực không được để trống");
        }

        record TargetSlot(DayOfWeek dayOfWeek, Period period, boolean isActive) {}
        List<TargetSlot> normalized = new ArrayList<>();

        for (ScheduleItemRequest item : request.schedules()) {
            if (item.dayOfWeek() == null || item.period() == null) {
                throw AppException.badRequest("Ngày trong tuần và ca trực không được để trống");
            }
            boolean active = Boolean.TRUE.equals(item.isActive());
            if (item.period() == Period.FULL_DAY) {
                normalized.add(new TargetSlot(item.dayOfWeek(), Period.MORNING, active));
                normalized.add(new TargetSlot(item.dayOfWeek(), Period.AFTERNOON, active));
            } else {
                normalized.add(new TargetSlot(item.dayOfWeek(), item.period(), active));
            }
        }

        // Kiểm tra trùng lặp
        Set<String> seen = new HashSet<>();
        for (TargetSlot t : normalized) {
            String key = t.dayOfWeek.name() + ":" + t.period.name();
            if (!seen.add(key)) {
                throw AppException.badRequest("Dữ liệu lịch trực bị trùng lặp ca khám: " + key);
            }
        }

        // UPSERT theo khoá (day, period)
        List<DoctorSchedule> existing = doctorScheduleRepository.findByDoctorId(doctorId);
        Map<String, DoctorSchedule> existingByKey = new HashMap<>();
        for (DoctorSchedule ds : existing) {
            if (ds.getDayOfWeek() != null && ds.getPeriod() != null) {
                existingByKey.put(ds.getDayOfWeek().name() + ":" + ds.getPeriod().name(), ds);
            }
        }

        Set<Long> retainedIds = new HashSet<>();
        for (TargetSlot t : normalized) {
            String key = t.dayOfWeek.name() + ":" + t.period.name();
            DoctorSchedule current = existingByKey.get(key);
            if (current != null) {
                current.setIsActive(t.isActive);
                current = doctorScheduleRepository.save(current);
                retainedIds.add(current.getId());
            } else if (t.isActive) {
                DoctorSchedule newSchedule = DoctorSchedule.builder()
                        .doctor(doctor)
                        .dayOfWeek(t.dayOfWeek)
                        .period(t.period)
                        .isActive(true)
                        .build();
                newSchedule = doctorScheduleRepository.save(newSchedule);
                retainedIds.add(newSchedule.getId());
            }
        }

        // Xóa những dòng cũ (bao gồm các dòng FULL_DAY cũ và dòng không còn được duy trì)
        for (DoctorSchedule ds : existing) {
            if (!retainedIds.contains(ds.getId())) {
                doctorScheduleRepository.delete(ds);
            }
        }
        doctorScheduleRepository.flush();

        log.info("Đã cập nhật lịch trực tuần cho bác sĩ ID {}", doctorId);
        return getWeeklyScheduleMatrix();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorDayOffDto> getUpcomingDayOffs(LocalDate fromDate) {
        LocalDate from = fromDate != null ? fromDate : LocalDate.now(clock);
        List<DoctorDayOff> list = doctorDayOffRepository.findByOffDateGreaterThanEqualOrderByOffDateAsc(from);

        return list.stream()
                .map(d -> DoctorDayOffDto.builder()
                        .id(d.getId())
                        .doctorId(d.getDoctor().getId())
                        .doctorName(d.getDoctor().getFullName())
                        .offDate(d.getOffDate())
                        .period(d.getPeriod())
                        .reason(d.getReason())
                        .createdAt(d.getCreatedAt())
                        .build())
                .sorted(Comparator.comparing(DoctorDayOffDto::getOffDate)
                        .thenComparing(DoctorDayOffDto::getDoctorName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    @Override
    @Transactional
    public CreateDoctorDayOffResult createDayOff(CreateDoctorDayOffRequest request) {
        if (request.doctorId() == null || request.offDate() == null) {
            throw AppException.badRequest("Mã bác sĩ và ngày nghỉ không được để trống");
        }
        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy bác sĩ"));
        if (!Boolean.TRUE.equals(doctor.getIsActive())) {
            throw AppException.unprocessable("Bác sĩ hiện không còn hoạt động");
        }

        LocalDate today = LocalDate.now(clock);
        if (request.offDate().isBefore(today)) {
            throw AppException.badRequest("Ngày nghỉ không được ở trong quá khứ");
        }

        Period period = request.period() != null ? request.period() : Period.FULL_DAY;

        // Kiểm tra trùng theo 4 nhánh nghiệp vụ
        List<DoctorDayOff> existingOffs = doctorDayOffRepository.findByDoctorIdAndOffDate(request.doctorId(), request.offDate());
        boolean hasFullDay = existingOffs.stream().anyMatch(o -> o.getPeriod() == Period.FULL_DAY);
        if (hasFullDay) {
            throw AppException.conflict("Bác sĩ đã có lịch nghỉ cả ngày vào ngày " + request.offDate());
        }
        if (period == Period.FULL_DAY && !existingOffs.isEmpty()) {
            throw AppException.conflict("Bác sĩ đã có lịch nghỉ trong ngày " + request.offDate() + ", không thể đăng ký thêm nghỉ cả ngày");
        }
        boolean hasSamePeriod = existingOffs.stream().anyMatch(o -> o.getPeriod() == period);
        if (hasSamePeriod) {
            throw AppException.conflict("Bác sĩ đã có lịch nghỉ ca này vào ngày " + request.offDate());
        }

        DoctorDayOff dayOff = DoctorDayOff.builder()
                .doctor(doctor)
                .offDate(request.offDate())
                .period(period)
                .reason(request.reason() != null ? request.reason().trim() : null)
                .build();
        try {
            dayOff = doctorDayOffRepository.save(dayOff);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw AppException.conflict("Bác sĩ đã được ghi nhận nghỉ trong ca này rồi.");
        }

        List<AppointmentStatus> statuses = List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);
        long affected = appointmentRepository.countByDoctorIdAndAppointmentDateAndStatusIn(
                request.doctorId(), request.offDate(), statuses
        );

        DoctorDayOffDto dto = DoctorDayOffDto.builder()
                .id(dayOff.getId())
                .doctorId(doctor.getId())
                .doctorName(doctor.getFullName())
                .offDate(dayOff.getOffDate())
                .period(dayOff.getPeriod())
                .reason(dayOff.getReason())
                .createdAt(dayOff.getCreatedAt())
                .build();

        log.info("Đã tạo ngày nghỉ cho bác sĩ ID {}, ngày {}, số phiếu bị ảnh hưởng: {}",
                doctor.getId(), request.offDate(), affected);

        return CreateDoctorDayOffResult.builder()
                .item(dto)
                .affectedAppointments((int) affected)
                .build();
    }

    @Override
    @Transactional
    public void deleteDayOff(Long id) {
        if (id == null) {
            throw AppException.badRequest("Mã ngày nghỉ không hợp lệ");
        }
        DoctorDayOff dayOff = doctorDayOffRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy ngày nghỉ"));
        doctorDayOffRepository.delete(dayOff);
        log.info("Đã xoá ngày nghỉ ID {}", id);
    }
}
