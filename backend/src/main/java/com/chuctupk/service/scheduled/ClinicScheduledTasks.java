package com.chuctupk.service.scheduled;

import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.OtpVerificationRepository;
import com.chuctupk.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClinicScheduledTasks {

    private final AppointmentRepository appointmentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final Clock clock;

    public static final String AUTO_CANCEL_REASON = "Không đến khám (hệ thống tự huỷ lúc 17:00)";

    /**
     * Tự động hủy các lịch hẹn chưa đến (PENDING) trong ngày vào lúc 17:00 giờ Việt Nam.
     */
    @Scheduled(cron = "0 0 17 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public int autoCancelNoShows() {
        LocalDate today = LocalDate.now(clock);
        List<Appointment> pendingAppointments = appointmentRepository.findByAppointmentDateAndStatus(
                today, AppointmentStatus.PENDING
        );

        if (pendingAppointments.isEmpty()) {
            log.info("[Scheduled] Không có lịch khám PENDING nào cần tự hủy trong ngày {}", today);
            return 0;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        for (Appointment appointment : pendingAppointments) {
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointment.setCancelReason(AUTO_CANCEL_REASON);
            appointment.setCancelledAt(now);
        }

        appointmentRepository.saveAll(pendingAppointments);
        log.info("[Scheduled] Đã tự động hủy {} lịch khám không đến (no-show) trong ngày {}",
                pendingAppointments.size(), today);

        return pendingAppointments.size();
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
