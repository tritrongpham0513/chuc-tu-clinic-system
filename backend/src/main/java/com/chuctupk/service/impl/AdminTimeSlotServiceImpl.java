package com.chuctupk.service.impl;

import com.chuctupk.dto.request.CreateTimeSlotRequest;
import com.chuctupk.dto.request.UpdateTimeSlotRequest;
import com.chuctupk.dto.response.AdminTimeSlotResponse;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.TimeSlotRepository;
import com.chuctupk.service.AdminTimeSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTimeSlotServiceImpl implements AdminTimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    private static final List<AppointmentStatus> EXCLUDED_STATUSES = List.of(
            AppointmentStatus.CANCELLED,
            AppointmentStatus.COMPLETED
    );

    @Override
    @Transactional(readOnly = true)
    public List<AdminTimeSlotResponse> getAllTimeSlots() {
        LocalDate today = LocalDate.now(clock);
        List<Object[]> groupedCounts = appointmentRepository.countUpcomingAppointmentsGroupedBySlot(today, EXCLUDED_STATUSES);
        Map<Long, Long> countMap = new HashMap<>();
        for (Object[] row : groupedCounts) {
            Long slotId = (Long) row[0];
            Long count = (Long) row[1];
            if (slotId != null && count != null) {
                countMap.put(slotId, count);
            }
        }

        List<TimeSlot> allSlots = timeSlotRepository.findAllByOrderBySortOrderAsc();
        List<AdminTimeSlotResponse> responses = new ArrayList<>();
        for (TimeSlot slot : allSlots) {
            long upcomingCount = countMap.getOrDefault(slot.getId(), 0L);
            responses.add(AdminTimeSlotResponse.from(slot, upcomingCount));
        }
        return responses;
    }

    @Override
    @Transactional
    public AdminTimeSlotResponse createTimeSlot(CreateTimeSlotRequest request) {
        if (request == null) {
            throw AppException.badRequest("Dữ liệu yêu cầu không được để trống");
        }

        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();
        Period period = request.getPeriod();

        if (startTime == null || endTime == null) {
            throw AppException.badRequest("Giờ bắt đầu và giờ kết thúc không được để trống");
        }
        if (period == null) {
            throw AppException.badRequest("Ca khám không được để trống");
        }

        // 1. end > start
        validateStartEndTime(startTime, endTime);

        // 3. period khớp giờ
        validatePeriodWithTime(period, startTime);

        // 4. maxPatients trong khoảng 1–50
        int maxPatients = request.getMaxPatients() != null ? request.getMaxPatients() : 5;
        validateMaxPatients(maxPatients);

        boolean isActive = request.getIsActive() == null || Boolean.TRUE.equals(request.getIsActive());

        // 2. Chồng lấn với khung đang active
        if (isActive) {
            checkOverlap(null, startTime, endTime);
        }

        // Tự sinh slotLabel nếu chưa có
        String slotLabel = (request.getSlotLabel() != null && !request.getSlotLabel().isBlank())
                ? request.getSlotLabel().trim()
                : formatTimeRange(startTime, endTime);

        // sortOrder: nếu = 0 hoặc null -> tự gán max + 1
        int sortOrder;
        if (request.getSortOrder() == null || request.getSortOrder() <= 0) {
            sortOrder = resolveNextSortOrder();
        } else {
            sortOrder = request.getSortOrder();
        }

        TimeSlot timeSlot = TimeSlot.builder()
                .slotLabel(slotLabel)
                .startTime(startTime)
                .endTime(endTime)
                .period(period)
                .maxPatients(maxPatients)
                .sortOrder(sortOrder)
                .isActive(isActive)
                .build();

        TimeSlot saved = timeSlotRepository.save(timeSlot);
        log.info("Admin đã tạo khung giờ mới: {} ({}-{}) ca {}", saved.getSlotLabel(), saved.getStartTime(), saved.getEndTime(), saved.getPeriod());

        return AdminTimeSlotResponse.from(saved, 0L);
    }

    @Override
    @Transactional
    public AdminTimeSlotResponse updateTimeSlot(Long id, UpdateTimeSlotRequest request) {
        if (id == null) {
            throw AppException.badRequest("Mã khung giờ không được để trống");
        }
        TimeSlot slot = timeSlotRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy khung giờ"));

        if (request == null) {
            long upcomingCount = countUpcomingAppointments(id);
            return AdminTimeSlotResponse.from(slot, upcomingCount);
        }

        LocalTime newStart = request.getStartTime() != null ? request.getStartTime() : slot.getStartTime();
        LocalTime newEnd = request.getEndTime() != null ? request.getEndTime() : slot.getEndTime();
        Period newPeriod = request.getPeriod() != null ? request.getPeriod() : slot.getPeriod();
        boolean timeChanged = !newStart.equals(slot.getStartTime()) || !newEnd.equals(slot.getEndTime());

        // 5. Sửa startTime/endTime của khung có >=1 phiếu appointmentDate >= today và trạng thái chưa huỷ/hoàn tất -> 422
        if (timeChanged) {
            long upcomingCount = countUpcomingAppointments(id);
            if (upcomingCount > 0) {
                throw AppException.unprocessable("Khung này đang có " + upcomingCount + " phiếu. Hãy tạo khung mới và ngưng khung cũ.");
            }
        }

        // 6. Ngưng (isActive=false) khung còn phiếu tương lai -> 422 kèm số phiếu
        boolean targetIsActive = request.getIsActive() != null ? request.getIsActive() : slot.getIsActive();
        if (Boolean.TRUE.equals(slot.getIsActive()) && !targetIsActive) {
            long upcomingCount = countUpcomingAppointments(id);
            if (upcomingCount > 0) {
                throw AppException.unprocessable("Khung này đang có " + upcomingCount + " phiếu sắp tới, không thể ngưng hoạt động.");
            }
        }

        // Validate time nếu có thay đổi
        if (timeChanged) {
            validateStartEndTime(newStart, newEnd);
        }

        // Validate period nếu thay đổi period hoặc time
        if (timeChanged || request.getPeriod() != null) {
            validatePeriodWithTime(newPeriod, newStart);
        }

        // Validate maxPatients nếu có truyền
        if (request.getMaxPatients() != null) {
            validateMaxPatients(request.getMaxPatients());
            slot.setMaxPatients(request.getMaxPatients());
        }

        // 2. Chồng lấn với khung đang active
        if (targetIsActive && (timeChanged || !Boolean.TRUE.equals(slot.getIsActive()))) {
            checkOverlap(id, newStart, newEnd);
        }

        if (timeChanged) {
            slot.setStartTime(newStart);
            slot.setEndTime(newEnd);
        }
        if (request.getPeriod() != null) {
            slot.setPeriod(newPeriod);
        }
        if (request.getSortOrder() != null) {
            slot.setSortOrder(request.getSortOrder());
        }
        if (request.getIsActive() != null) {
            slot.setIsActive(targetIsActive);
        }

        if (request.getSlotLabel() != null && !request.getSlotLabel().isBlank()) {
            slot.setSlotLabel(request.getSlotLabel().trim());
        } else if (timeChanged) {
            slot.setSlotLabel(formatTimeRange(newStart, newEnd));
        }

        TimeSlot saved = timeSlotRepository.save(slot);
        long upcomingCount = countUpcomingAppointments(id);
        log.info("Admin đã cập nhật khung giờ ID {}: {}", saved.getId(), saved.getSlotLabel());

        return AdminTimeSlotResponse.from(saved, upcomingCount);
    }

    @Override
    @Transactional
    public AdminTimeSlotResponse activateTimeSlot(Long id) {
        if (id == null) {
            throw AppException.badRequest("Mã khung giờ không được để trống");
        }
        TimeSlot slot = timeSlotRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy khung giờ"));

        if (!Boolean.TRUE.equals(slot.getIsActive())) {
            // Kiểm tra chồng lấn trước khi bật active
            checkOverlap(id, slot.getStartTime(), slot.getEndTime());
            slot.setIsActive(true);
            slot = timeSlotRepository.save(slot);
            log.info("Admin đã kích hoạt khung giờ ID {}: {}", slot.getId(), slot.getSlotLabel());
        }

        long upcomingCount = countUpcomingAppointments(id);
        return AdminTimeSlotResponse.from(slot, upcomingCount);
    }

    @Override
    @Transactional
    public AdminTimeSlotResponse deactivateTimeSlot(Long id) {
        if (id == null) {
            throw AppException.badRequest("Mã khung giờ không được để trống");
        }
        TimeSlot slot = timeSlotRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy khung giờ"));

        if (Boolean.TRUE.equals(slot.getIsActive())) {
            long upcomingCount = countUpcomingAppointments(id);
            if (upcomingCount > 0) {
                throw AppException.unprocessable("Khung này đang có " + upcomingCount + " phiếu sắp tới, không thể ngưng hoạt động.");
            }
            slot.setIsActive(false);
            slot = timeSlotRepository.save(slot);
            log.info("Admin đã ngưng hoạt động khung giờ ID {}: {}", slot.getId(), slot.getSlotLabel());
        }

        long upcomingCount = countUpcomingAppointments(id);
        return AdminTimeSlotResponse.from(slot, upcomingCount);
    }

    private void validateStartEndTime(LocalTime start, LocalTime end) {
        if (!end.isAfter(start)) {
            throw AppException.unprocessable("Giờ kết thúc phải sau giờ bắt đầu");
        }
    }

    private void validatePeriodWithTime(Period period, LocalTime start) {
        LocalTime noon = LocalTime.of(12, 0);
        if (period == Period.MORNING && !start.isBefore(noon)) {
            throw AppException.unprocessable("Khung giờ sáng phải bắt đầu trước 12:00");
        }
        if (period == Period.AFTERNOON && start.isBefore(noon)) {
            throw AppException.unprocessable("Khung giờ chiều phải bắt đầu từ 12:00 trở đi");
        }
    }

    private void validateMaxPatients(Integer maxPatients) {
        if (maxPatients == null || maxPatients < 1 || maxPatients > 50) {
            throw AppException.unprocessable("Sức chứa mỗi khung giờ phải từ 1 đến 50 bệnh nhân");
        }
    }

    private void checkOverlap(Long currentSlotId, LocalTime start, LocalTime end) {
        List<TimeSlot> activeSlots = timeSlotRepository.findByIsActiveTrue();
        for (TimeSlot existing : activeSlots) {
            if (currentSlotId != null && existing.getId().equals(currentSlotId)) {
                continue;
            }
            // Chồng lấn: start < existing.endTime && existing.startTime < end
            if (start.isBefore(existing.getEndTime()) && existing.getStartTime().isBefore(end)) {
                String existingLabel = (existing.getSlotLabel() != null && !existing.getSlotLabel().isBlank())
                        ? existing.getSlotLabel()
                        : formatTimeRange(existing.getStartTime(), existing.getEndTime());
                throw AppException.unprocessable("Trùng với khung " + existingLabel);
            }
        }
    }

    private int resolveNextSortOrder() {
        return timeSlotRepository.findTopByOrderBySortOrderDesc()
                .map(s -> s.getSortOrder() != null ? s.getSortOrder() + 1 : 1)
                .orElse(1);
    }

    private long countUpcomingAppointments(Long slotId) {
        LocalDate today = LocalDate.now(clock);
        return appointmentRepository.countUpcomingAppointmentsForSlot(slotId, today, EXCLUDED_STATUSES);
    }

    private String formatTimeRange(LocalTime start, LocalTime end) {
        return String.format("%02d:%02d - %02d:%02d",
                start.getHour(), start.getMinute(),
                end.getHour(), end.getMinute());
    }
}
