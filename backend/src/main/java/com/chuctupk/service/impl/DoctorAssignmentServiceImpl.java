package com.chuctupk.service.impl;

import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.Doctor;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.DayOfWeek;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.DoctorDayOffRepository;
import com.chuctupk.repository.DoctorRepository;
import com.chuctupk.repository.DoctorScheduleRepository;
import com.chuctupk.service.DoctorAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorAssignmentServiceImpl implements DoctorAssignmentService {

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorDayOffRepository doctorDayOffRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Long> findOnDutyDoctorIds(LocalDate date, Period period) {
        if (date == null || period == null) {
            return List.of();
        }
        DayOfWeek dow = DayOfWeek.from(date.getDayOfWeek());
        List<Long> scheduledDoctorIds = doctorScheduleRepository.findScheduledDoctorIds(dow, period);
        List<Long> offDoctorIds = doctorDayOffRepository.findDoctorIdsOffOnDateAndPeriod(date, period);

        Set<Long> offSet = new HashSet<>(offDoctorIds);
        List<Long> availableDoctorIds = scheduledDoctorIds.stream()
                .filter(id -> !offSet.contains(id))
                .distinct()
                .toList();

        if (availableDoctorIds.isEmpty()) {
            return List.of();
        }

        List<Doctor> doctors = doctorRepository.findAllById(availableDoctorIds);
        return doctors.stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsActive()))
                .filter(d -> d.getUser() == null || Boolean.TRUE.equals(d.getUser().getIsActive()))
                .map(Doctor::getId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasDoctorOnDuty(LocalDate date, TimeSlot slot) {
        if (date == null || slot == null || slot.getPeriod() == null) {
            return false;
        }
        return !findOnDutyDoctorIds(date, slot.getPeriod()).isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public Doctor pickDoctor(LocalDate date, TimeSlot slot) {
        if (date == null || slot == null) {
            throw AppException.badRequest("Ngày khám hoặc khung giờ không hợp lệ");
        }

        List<Long> onDutyDoctorIds = findOnDutyDoctorIds(date, slot.getPeriod());
        if (onDutyDoctorIds.isEmpty()) {
            throw AppException.unprocessable("Khung giờ này chưa có bác sĩ trực, cô chú chọn khung khác hoặc gọi hotline");
        }

        List<Doctor> activeDoctors = doctorRepository.findAllById(onDutyDoctorIds);
        if (activeDoctors.isEmpty()) {
            throw AppException.unprocessable("Khung giờ này chưa có bác sĩ trực, cô chú chọn khung khác hoặc gọi hotline");
        }

        // Cân bằng tải: chọn bác sĩ có ít ca hẹn nhất trong ngày & khung giờ này, tie-break theo ID nhỏ nhất
        return activeDoctors.stream()
                .min(Comparator
                        .comparingLong((Doctor d) -> appointmentRepository.countBookedSlotsForDoctor(
                                date, d.getId(), slot.getId(), AppointmentStatus.CANCELLED))
                        .thenComparing(Doctor::getId))
                .orElseThrow(() -> AppException.unprocessable("Khung giờ này chưa có bác sĩ trực, cô chú chọn khung khác hoặc gọi hotline"));
    }

    @Override
    @Transactional
    public void assignDoctor(Long appointmentId, Long doctorId) {
        if (appointmentId == null || doctorId == null) {
            throw AppException.badRequest("Mã lịch hẹn hoặc mã bác sĩ không hợp lệ");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy lịch hẹn"));

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy bác sĩ"));

        if (!Boolean.TRUE.equals(doctor.getIsActive())
                || (doctor.getUser() != null && !Boolean.TRUE.equals(doctor.getUser().getIsActive()))) {
            throw AppException.unprocessable("Bác sĩ hiện không nhận lịch khám");
        }

        LocalDate apptDate = appointment.getAppointmentDate();
        TimeSlot slot = appointment.getTimeSlot();
        if (apptDate != null && slot != null) {
            DayOfWeek dow = DayOfWeek.from(apptDate.getDayOfWeek());
            Period period = slot.getPeriod();

            boolean scheduled = doctorScheduleRepository.isDoctorScheduled(doctorId, dow, period);
            boolean isOff = doctorDayOffRepository.isDoctorOff(doctorId, apptDate, period);

            if (!scheduled || isOff) {
                throw AppException.unprocessable("Bác sĩ không có lịch trực trong ca khám này");
            }
        }

        appointment.setDoctor(doctor);
        appointmentRepository.save(appointment);
        log.info("Phân công bác sĩ ID {} cho lịch hẹn ID {}", doctorId, appointmentId);
    }
}
