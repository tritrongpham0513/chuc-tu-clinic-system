package com.chuctupk.service.impl;

import com.chuctupk.dto.response.TimeSlotResponse;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.*;
import com.chuctupk.service.ClinicSettingService;
import com.chuctupk.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeSlotServiceImpl implements TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final DoctorRepository doctorRepository;
    private final ClinicHolidayRepository clinicHolidayRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentSeatRepository appointmentSeatRepository;
    private final ClinicSettingService clinicSettingService;
    private final com.chuctupk.service.SlotPolicy slotPolicy;
    private final com.chuctupk.service.DoctorAssignmentService doctorAssignmentService;
    private final com.chuctupk.service.CapacityResolver capacityResolver;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getAvailableTimeSlots(LocalDate date, Long doctorId) {
        if (date == null) {
            throw AppException.badRequest("Ngày khám không được để trống");
        }

        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);

        if (date.isBefore(today)) {
            throw AppException.unprocessable("Không thể xem khung giờ trong quá khứ");
        }

        int maxDays = clinicSettingService.getUserMaxDays();
        if (maxDays <= 0) {
            maxDays = clinicSettingService.getBookingAdvanceDays();
        }
        if (date.isAfter(today.plusDays(maxDays))) {
            throw AppException.unprocessable("Chỉ có thể đặt lịch trước tối đa " + maxDays + " ngày");
        }

        if (clinicHolidayRepository.existsByHolidayDateAndIsClosedTrue(date)) {
            throw AppException.unprocessable("Phòng khám nghỉ lễ vào ngày này");
        }

        List<TimeSlot> allSlots = timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc();
        List<TimeSlotResponse> availableSlots = new ArrayList<>();

        for (TimeSlot slot : allSlots) {
            long totalBooked = Math.max(
                    appointmentSeatRepository.countByAppointmentDateAndTimeSlotId(date, slot.getId()),
                    appointmentRepository.countBookedSlotsTotal(date, slot.getId(), AppointmentStatus.CANCELLED)
            );
            int capacity = capacityResolver.effectiveCapacity(date, slot);
            int remaining = Math.max(0, capacity - (int) totalBooked);

            // Đánh giá bằng SlotPolicy (Single Source of Truth)
            com.chuctupk.service.SlotPolicy.SlotStatus status = slotPolicy.evaluate(
                    date, slot, now, com.chuctupk.service.SlotPolicy.BookingChannel.PATIENT_PORTAL, remaining
            );
            if (status != com.chuctupk.service.SlotPolicy.SlotStatus.OK) {
                continue;
            }

            // Lấy danh sách bác sĩ trực ca (có lịch làm việc và không nghỉ)
            List<Long> activeDoctorIds = doctorAssignmentService.findOnDutyDoctorIds(date, slot.getPeriod());

            // Quy tắc 4: Slot không có bác sĩ trực thì loại khỏi danh sách
            if (activeDoctorIds.isEmpty()) {
                continue;
            }

            int finalRemaining;
            if (doctorId != null) {
                // Bệnh nhân chọn đích danh bác sĩ: Bác sĩ phải đang trực ca này
                if (!activeDoctorIds.contains(doctorId)) {
                    continue;
                }

                long doctorBooked = appointmentRepository.countBookedSlotsForDoctor(date, doctorId, slot.getId(), AppointmentStatus.CANCELLED);
                int doctorCapacity = capacityResolver.perDoctorCapacity(slot);
                int doctorRemaining = doctorCapacity - (int) doctorBooked;

                if (doctorRemaining <= 0) {
                    continue; // Ca của bác sĩ này đã đầy
                }

                finalRemaining = Math.min(doctorRemaining, remaining);
            } else {
                finalRemaining = remaining;
            }

            if (finalRemaining > 0) {
                availableSlots.add(TimeSlotResponse.builder()
                        .id(slot.getId())
                        .slotLabel(slot.getSlotLabel())
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .period(slot.getPeriod())
                        .remaining(finalRemaining)
                        .build());
            }
        }

        // Nếu cả ngày không còn slot nào thì trả về mảng rỗng với HTTP 200 (không trả 422)
        return availableSlots;
    }
}

