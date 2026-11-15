package com.chuctupk.service.impl;

import com.chuctupk.common.PhoneValidator;
import com.chuctupk.common.mapping.ContractMapper;
import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.service.NotificationService;
import com.chuctupk.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final SmsService smsService;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    @Override
    public void notifyBookingSuccess(Appointment appointment) {
        if (appointment == null) return;
        String phone = getRecipientPhone(appointment);
        if (phone == null || phone.isBlank()) return;

        String code = appointment.getBookingCode();
        String dateStr = ContractMapper.formatDate(appointment.getAppointmentDate());
        String timeStr = appointment.getTimeSlot() != null && appointment.getTimeSlot().getStartTime() != null
                ? ContractMapper.formatTime(appointment.getTimeSlot().getStartTime())
                : "08:00";

        String message = String.format(
                "Phong Kham Chuc Tu: Phieu hen %s ngay %s luc %s da tao thanh cong. Vui long den truoc 15 phut de duoc huong dan.",
                code, dateStr, timeStr
        );

        log.info("Gửi SMS xác nhận đặt lịch cho SĐT {}: mã {}", PhoneValidator.mask(phone), code);
        smsService.sendMessage(phone, message);
    }

    @Override
    public void notifyCancellation(Appointment appointment, String reason) {
        if (appointment == null) return;
        String phone = getRecipientPhone(appointment);
        if (phone == null || phone.isBlank()) return;

        String code = appointment.getBookingCode();
        String dateStr = ContractMapper.formatDate(appointment.getAppointmentDate());
        String reasonStr = reason != null && !reason.isBlank() ? reason.trim() : "theo yeu cau";

        String message = String.format(
                "Phong Kham Chuc Tu: Phieu hen %s ngay %s da duoc huy (%s). Co chu co the dat lai tren website hoac lien he hotline.",
                code, dateStr, reasonStr
        );

        log.info("Gửi SMS thông báo hủy lịch cho SĐT {}: mã {}", PhoneValidator.mask(phone), code);
        smsService.sendMessage(phone, message);
    }

    @Override
    @Transactional(readOnly = true)
    public void sendUpcomingReminders() {
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);

        List<Appointment> upcoming = appointmentRepository.findByAppointmentDateAndStatus(today, AppointmentStatus.PENDING);
        for (Appointment a : upcoming) {
            if (a.getTimeSlot() == null || a.getTimeSlot().getStartTime() == null) continue;

            LocalTime slotStart = a.getTimeSlot().getStartTime();
            // Nhắc lịch trước khoảng 2 tiếng: slotStart trong khoảng [now + 1h45m, now + 2h15m]
            LocalTime windowStart = now.plusMinutes(105);
            LocalTime windowEnd = now.plusMinutes(135);

            if (!slotStart.isBefore(windowStart) && !slotStart.isAfter(windowEnd)) {
                String phone = getRecipientPhone(a);
                if (phone != null && !phone.isBlank()) {
                    String timeStr = ContractMapper.formatTime(slotStart);
                    String message = String.format(
                            "Phong Kham Chuc Tu: Nhac lich hen %s luc %s hom nay. Co chu vui long den dung gio de duoc tiep don tot nhat.",
                            a.getBookingCode(), timeStr
                    );
                    log.info("Gửi SMS nhắc lịch hẹn trước 2h cho SĐT {}: mã {}", PhoneValidator.mask(phone), a.getBookingCode());
                    smsService.sendMessage(phone, message);
                }
            }
        }
    }

    private String getRecipientPhone(Appointment appointment) {
        if (appointment.getBookerPhone() != null && !appointment.getBookerPhone().isBlank()) {
            return appointment.getBookerPhone();
        }
        if (appointment.getPatient() != null && appointment.getPatient().getPhone() != null) {
            return appointment.getPatient().getPhone();
        }
        return null;
    }
}
