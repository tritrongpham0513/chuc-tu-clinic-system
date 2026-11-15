package com.chuctupk.service.scheduled;

import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.OtpVerificationRepository;
import com.chuctupk.repository.RefreshTokenRepository;
import com.chuctupk.service.ClinicSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClinicScheduledTasks {

    private final AppointmentRepository appointmentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final ClinicSettingService clinicSettingService;
    private final Clock clock;

    public static final String AUTO_CANCEL_REASON = "Không đến khám (hệ thống tự huỷ)";

    /**
     * Tự động hủy các lịch hẹn chưa đến (PENDING, CONFIRMED) trong ngày khi đã quá giờ no_show_cancel_at.
     * Chạy định kỳ mỗi 5 phút.
     */
    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public int autoCancelNoShows() {
        LocalTime nowTime = LocalTime.now(clock);
        LocalTime noShowCancelAt = clinicSettingService.getNoShowCancelAt();
        if (nowTime.isBefore(noShowCancelAt)) {
            return 0;
        }

        LocalDate today = LocalDate.now(clock);
        List<Appointment> pendingAppointments = appointmentRepository.findByAppointmentDateAndStatus(
                today, AppointmentStatus.PENDING
        );
        List<Appointment> confirmedAppointments = appointmentRepository.findByAppointmentDateAndStatus(
                today, AppointmentStatus.CONFIRMED
        );

        List<Appointment> toCancel = new ArrayList<>(pendingAppointments);
        toCancel.addAll(confirmedAppointments);

        if (toCancel.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        String reason = "Không đến khám (hệ thống tự huỷ lúc " + noShowCancelAt + ")";
        for (Appointment appointment : toCancel) {
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointment.setCancelReason(reason);
            appointment.setCancelledAt(now);
        }

        appointmentRepository.saveAll(toCancel);
        log.info("[Scheduled] Đã tự động hủy {} lịch khám không đến (no-show) trong ngày {}",
                toCancel.size(), today);

        return toCancel.size();
    }

    /**
     * Dọn dẹp Refresh Token và mã OTP hết hạn vào lúc 02:00 sáng mỗi ngày.
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void cleanupExpiredTokensAndOtps() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime otpCutoff = now.minusDays(1);

        try {
            refreshTokenRepository.deleteAllExpiredOrRevoked(now);
            log.info("[Scheduled] Đã hoàn tất dọn dẹp các Refresh Token hết hạn hoặc bị thu hồi");
        } catch (Exception e) {
            log.warn("[Scheduled] Lỗi dọn dẹp Refresh Token: {}", e.getMessage());
        }

        try {
            otpVerificationRepository.deleteAllExpiredOrVerified(now, otpCutoff);
            log.info("[Scheduled] Đã hoàn tất dọn dẹp các mã OTP hết hạn");
        } catch (Exception e) {
            log.warn("[Scheduled] Lỗi dọn dẹp OTP: {}", e.getMessage());
        }
    }
}
