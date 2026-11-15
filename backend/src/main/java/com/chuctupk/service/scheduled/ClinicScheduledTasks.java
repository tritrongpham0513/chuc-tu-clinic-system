package com.chuctupk.service.scheduled;

import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.AppointmentSeatRepository;
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
    private final AppointmentSeatRepository appointmentSeatRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final ClinicSettingService clinicSettingService;
    private final Clock clock;

    public static final String AUTO_CANCEL_REASON = "Không đến khám";

    /**
     * Tự động chuyển NO_SHOW cho các lịch hẹn PENDING trong ngày khi slot đã kết thúc và quá mốc no_show_cancel_at.
     * Tuyệt đối không huỷ các phiếu CONFIRMED (đã check-in), IN_PROGRESS hay COMPLETED.
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
        // Chỉ xét PENDING; tuyệt đối không đụng CONFIRMED (đã check-in), IN_PROGRESS, COMPLETED
        List<Appointment> pendingAppointments = appointmentRepository.findByAppointmentDateAndStatus(
                today, AppointmentStatus.PENDING
        );

        if (pendingAppointments.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<Appointment> toNoShow = new ArrayList<>();

        for (Appointment appointment : pendingAppointments) {
            TimeSlot slot = appointment.getTimeSlot();
            // Chỉ huỷ khi slot đã kết thúc (slot.endTime < nowTime)
            boolean slotEnded = (slot == null || slot.getEndTime() == null || slot.getEndTime().isBefore(nowTime));
            if (slotEnded) {
                appointment.setStatus(AppointmentStatus.NO_SHOW);
                appointment.setCancelReason(AUTO_CANCEL_REASON);
                appointment.setCancelledAt(now);
                toNoShow.add(appointment);

                // Nhả ghế đã giữ
                if (appointment.getId() != null) {
                    appointmentSeatRepository.deleteByAppointmentId(appointment.getId());
                }

                // TODO (B6): Gửi tin nhắn thông báo NO_SHOW cho bệnh nhân khi tích hợp NotificationService
            }
        }

        if (toNoShow.isEmpty()) {
            return 0;
        }

        appointmentRepository.saveAll(toNoShow);
        log.info("[Scheduled] Đã tự động chuyển trạng thái NO_SHOW cho {} lịch khám không đến trong ngày {}",
                toNoShow.size(), today);

        return toNoShow.size();
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
