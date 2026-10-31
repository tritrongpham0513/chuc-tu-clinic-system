package com.chuctupk.service.impl;

import com.chuctupk.dto.response.TimeSlotResponse;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.DayOfWeek;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.*;
import com.chuctupk.service.ClinicSettingService;
import com.chuctupk.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeSlotServiceImpl implements TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorDayOffRepository doctorDayOffRepository;
    private final ClinicHolidayRepository clinicHolidayRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClinicSettingService clinicSettingService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getAvailableTimeSlots(LocalDate date, Long doctorId) {
        if (date == null) {
            throw AppException.badRequest("Ngày khám không được để trống");
        }

        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);

        if (date.isBefore(today)) {
            throw AppException.unprocessable("Không thể xem khung giờ trong quá khứ");
        }

        int advanceDays = clinicSettingService.getBookingAdvanceDays();
        if (date.isAfter(today.plusDays(advanceDays))) {
            throw AppException.unprocessable("Chỉ có thể đặt lịch trước tối đa " + advanceDays + " ngày");
        }

        if (clinicHolidayRepository.existsByHolidayDateAndIsClosedTrue(date)) {
            throw AppException.unprocessable("Phòng khám nghỉ lễ vào ngày này");
        }

        DayOfWeek dow = DayOfWeek.from(date.getDayOfWeek());
        int minHours = clinicSettingService.getBookingMinHours();

        List<TimeSlot> allSlots = timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc();
        List<TimeSlotResponse> availableSlots = new ArrayList<>();

        for (TimeSlot slot : allSlots) {
            // Nếu là ngày hôm nay, loại các slot bắt đầu trước (now + booking_min_hours)
            if (date.isEqual(today)) {
                LocalTime cutoffTime = now.plusHours(minHours);
                if (slot.getStartTime().isBefore(cutoffTime)) {
                    continue;
                }
            }

            // Lấy danh sách bác sĩ trực ca (có lịch làm việc và không nghỉ)
            List<Long> scheduledDoctorIds = doctorScheduleRepository.findScheduledDoctorIds(dow, slot.getPeriod());
            List<Long> offDoctorIds = doctorDayOffRepository.findDoctorIdsOffOnDateAndPeriod(date, slot.getPeriod());

            List<Long> activeDoctorIds = scheduledDoctorIds.stream()
                    .filter(id -> !offDoctorIds.contains(id))
                    .toList();

            // Quy tắc 4: Slot không có bác sĩ trực thì loại khỏi danh sách
            if (activeDoctorIds.isEmpty()) {
                continue;
            }

            int activeDoctorCount = activeDoctorIds.size();
            long totalBooked = appointmentRepository.countBookedSlotsTotal(date, slot.getId(), AppointmentStatus.CANCELLED);
            int totalCapacity = slot.getMaxPatients() * activeDoctorCount;
            int totalRemaining = totalCapacity - (int) totalBooked;

            if (totalRemaining <= 0) {
                continue; // Khung giờ đã đầy tổng thể
            }

            int finalRemaining;
            if (doctorId != null) {
                // Bệnh nhân chọn đích danh bác sĩ: Bác sĩ phải đang trực ca này
                if (!activeDoctorIds.contains(doctorId)) {
                    continue;
                }

                long doctorBooked = appointmentRepository.countBookedSlotsForDoctor(date, doctorId, slot.getId(), AppointmentStatus.CANCELLED);
                int doctorRemaining = slot.getMaxPatients() - (int) doctorBooked;

                if (doctorRemaining <= 0) {
                    continue; // Ca của bác sĩ này đã đầy
                }

                // Sức chứa còn lại là giá trị nhỏ hơn giữa chỗ của bác sĩ và chỗ còn lại của slot
                finalRemaining = Math.min(doctorRemaining, totalRemaining);
            } else {
                finalRemaining = totalRemaining;
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

