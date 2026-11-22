package com.chuctupk.service.impl;

import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.AppointmentStatusHistory;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.AppointmentStatusHistoryRepository;
import com.chuctupk.service.AppointmentStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentStateServiceImpl implements AppointmentStateService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentStatusHistoryRepository statusHistoryRepository;
    private final Clock clock;

    @Override
    @Transactional
    public Appointment transition(Appointment appointment,
                                  AppointmentStatus newStatus,
                                  User actor,
                                  String reason) {
        if (appointment == null) {
            throw AppException.badRequest("Phiếu khám không được để trống");
        }
        if (newStatus == null) {
            throw AppException.badRequest("Trạng thái mới không được để trống");
        }

        // 1. Đọc oldStatus (nếu chưa có ID hoặc status null thì oldStatus = null)
        AppointmentStatus oldStatus = appointment.getId() == null ? null : appointment.getStatus();

        // 2. Kiểm tra chuyển đổi hợp lệ
        validateTransition(oldStatus, newStatus);

        LocalDateTime now = LocalDateTime.now(clock);

        // 3. Set trạng thái + cập nhật mốc thời gian chỉ những cột có thật
        switch (newStatus) {
            case CONFIRMED -> {
                if (oldStatus == null || oldStatus == AppointmentStatus.PENDING) {
                    // Check-in / xác nhận thật sự
                    appointment.setConfirmedAt(now);
                    if (actor != null) {
                        appointment.setConfirmedBy(actor);
                    }
                    if (appointment.getCheckInAt() == null) {
                        appointment.setCheckInAt(now);
                    }
                } else if (oldStatus == AppointmentStatus.IN_PROGRESS) {
                    // Hoàn tác mời vào khám: chỉ huỷ enteredAt, giữ nguyên mốc check-in ban đầu
                    appointment.setEnteredAt(null);
                }
                // oldStatus == CONFIRMED: đổi bác sĩ, không đụng mốc thời gian
            }
            case IN_PROGRESS -> {
                if (oldStatus != AppointmentStatus.IN_PROGRESS) {
                    appointment.setEnteredAt(now);
                }
            }
            case CANCELLED -> {
                appointment.setCancelledAt(now);
                if (reason != null && !reason.isBlank()) {
                    appointment.setCancelReason(reason);
                }
            }
            case NO_SHOW -> {
                appointment.setCancelledAt(now);
                if (reason != null && !reason.isBlank()) {
                    appointment.setCancelReason(reason);
                }
            }
            case PENDING -> {
                if (oldStatus == AppointmentStatus.CONFIRMED) {
                    // Hoàn tác check-in
                    appointment.setCheckInAt(null);
                    appointment.setConfirmedBy(null);
                    appointment.setConfirmedAt(null);
                }
            }
            case COMPLETED -> {
                // Không có cột completedAt trên Appointment
            }
        }

        appointment.setStatus(newStatus);
        Appointment saved = appointmentRepository.save(appointment);
        // Một số test double trả null từ save(); luôn có entity để ghi history và log
        if (saved == null) {
            saved = appointment;
        }

        // 4. Ghi đúng 1 dòng AppointmentStatusHistory
        if (statusHistoryRepository != null) {
            AppointmentStatusHistory history = AppointmentStatusHistory.builder()
                    .appointment(saved)
                    .oldStatus(oldStatus != null ? oldStatus.name() : null)
                    .newStatus(newStatus.name())
                    .changedBy(actor)
                    .reason(reason)
                    .changedAt(now)
                    .build();
            statusHistoryRepository.save(history);
        }

        log.info("Transitioned appointment id={} from {} to {} by {} (reason: {})",
                saved.getId(), oldStatus, newStatus, actor != null ? actor.getFullName() : "SYSTEM", reason);

        // 5. Trả lại appointment đã lưu
        return saved;
    }

    private void validateTransition(AppointmentStatus oldStatus, AppointmentStatus newStatus) {
        // Cho phép khởi tạo phiếu mới
        if (oldStatus == null) {
            if (newStatus == AppointmentStatus.PENDING || newStatus == AppointmentStatus.CONFIRMED) {
                return;
            }
            throw AppException.unprocessable("Không thể chuyển từ null sang " + newStatus);
        }

        // Cho phép trạng thái không đổi (Luồng 9: Đổi bác sĩ ghi log dấu vết)
        if (oldStatus == newStatus) {
            return;
        }

        boolean valid = switch (oldStatus) {
            case PENDING -> (newStatus == AppointmentStatus.CONFIRMED
                    || newStatus == AppointmentStatus.CANCELLED
                    || newStatus == AppointmentStatus.NO_SHOW);
            case CONFIRMED -> (newStatus == AppointmentStatus.PENDING
                    || newStatus == AppointmentStatus.IN_PROGRESS
                    || newStatus == AppointmentStatus.CANCELLED
                    || newStatus == AppointmentStatus.NO_SHOW);
            case IN_PROGRESS -> (newStatus == AppointmentStatus.COMPLETED
                    || newStatus == AppointmentStatus.CONFIRMED);
            case COMPLETED, CANCELLED, NO_SHOW -> false;
        };

        if (!valid) {
            throw AppException.unprocessable("Không thể chuyển từ " + oldStatus + " sang " + newStatus);
        }
    }
}
