package com.chuctupk.service.impl;

import com.chuctupk.common.PhoneValidator;
import com.chuctupk.common.mapping.ContractMapper;
import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.NotificationLog;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.notification.DeliveryResult;
import com.chuctupk.notification.LogProvider;
import com.chuctupk.notification.NotificationMessage;
import com.chuctupk.notification.NotificationProvider;
import com.chuctupk.notification.NotificationLogSaver;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.NotificationLogRepository;
import com.chuctupk.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final Map<String, NotificationProvider> providers;
    private final AppointmentRepository appointmentRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final NotificationLogSaver notificationLogSaver;
    private final Clock clock;
    private final String configuredProvider;

    @Autowired
    public NotificationServiceImpl(
            Map<String, NotificationProvider> providers,
            AppointmentRepository appointmentRepository,
            NotificationLogRepository notificationLogRepository,
            NotificationLogSaver notificationLogSaver,
            Clock clock,
            @Value("${notification.provider:log}") String configuredProvider
    ) {
        this.providers = providers != null ? providers : Collections.emptyMap();
        this.appointmentRepository = appointmentRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.notificationLogSaver = notificationLogSaver != null
                ? notificationLogSaver
                : (notificationLogRepository != null ? notificationLogRepository::save : null);
        this.clock = clock;
        this.configuredProvider = configuredProvider;
    }

    public NotificationServiceImpl(
            Map<String, NotificationProvider> providers,
            AppointmentRepository appointmentRepository,
            NotificationLogRepository notificationLogRepository,
            Clock clock,
            String configuredProvider
    ) {
        this(providers, appointmentRepository, notificationLogRepository,
                notificationLogRepository != null ? notificationLogRepository::save : null,
                clock, configuredProvider);
    }

    private NotificationProvider resolveProvider() {
        String key = (configuredProvider != null && !configuredProvider.isBlank())
                ? configuredProvider.trim().toLowerCase()
                : "log";
        NotificationProvider provider = providers.get(key);
        if (provider == null) {
            for (NotificationProvider p : providers.values()) {
                if (p.channel().equalsIgnoreCase(key)) {
                    provider = p;
                    break;
                }
            }
        }
        if (provider == null) {
            log.warn("Không tìm thấy NotificationProvider [{}] theo cấu hình, chuyển sang dùng provider mặc định [log]", configuredProvider);
            provider = providers.get("log");
        }
        if (provider == null && !providers.isEmpty()) {
            provider = providers.values().iterator().next();
        }
        return provider;
    }

    private String sanitizeErrorCode(String rawError) {
        if (rawError == null || rawError.isBlank()) return null;
        // Che số điện thoại xuất hiện trong thông điệp lỗi (VD: 0912345678 hoặc 84912345678)
        String sanitized = rawError.replaceAll("\\b(0[35789]\\d{2})\\d{3,4}(\\d{3})\\b", "$1 *** $2")
                                   .replaceAll("\\b(84[35789]\\d{2})\\d{3,4}(\\d{3})\\b", "$1 *** $2");
        // Giới hạn độ dài tối đa 100 ký tự khớp với cột VARCHAR(100)
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }
        return sanitized;
    }

    private void sendAndLog(Appointment appointment, String template, String phone, Map<String, Object> data) {
        if (phone == null || phone.isBlank()) return;

        NotificationProvider provider = resolveProvider();
        if (provider == null) {
            log.warn("Không có NotificationProvider nào khả dụng trong hệ thống để gửi thông báo");
            return;
        }

        String channel = provider.channel();
        Long appointmentId = appointment != null ? appointment.getId() : null;

        DeliveryResult result;
        try {
            NotificationMessage message = new NotificationMessage(appointmentId, phone, template, data);
            result = provider.send(message);
        } catch (Exception e) {
            String safeError = sanitizeErrorCode(e.getMessage());
            log.warn("Lỗi khi gửi thông báo qua provider [{}]: {}", channel, safeError);
            result = new DeliveryResult(DeliveryResult.Status.FAILED, safeError, channel.toLowerCase());
        }

        try {
            String masked = PhoneValidator.mask(phone);
            String providerRef = (result != null && result.providerRef() != null) ? result.providerRef() : channel.toLowerCase();
            String status = (result != null && result.status() != null) ? result.status().name() : DeliveryResult.Status.FAILED.name();
            String errorCode = result != null ? sanitizeErrorCode(result.errorCode()) : null;

            NotificationLog logEntry = NotificationLog.builder()
                    .appointment(appointment)
                    .channel(channel)
                    .template(template)
                    .recipientMasked(masked)
                    .status(status)
                    .errorCode(errorCode)
                    .provider(providerRef)
                    .createdAt(LocalDateTime.now(clock))
                    .build();
            if (notificationLogSaver != null) {
                notificationLogSaver.save(logEntry);
            }
        } catch (Exception e) {
            log.warn("Không thể lưu notification_log: {}", sanitizeErrorCode(e.getMessage()));
        }
    }

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

        log.info("Xử lý thông báo xác nhận đặt lịch cho SĐT {}: mã {}", PhoneValidator.mask(phone), code);
        Map<String, Object> data = new HashMap<>();
        data.put("code", code != null ? code : "");
        data.put("date", dateStr);
        data.put("time", timeStr);
        data.put("message", message);

        sendAndLog(appointment, "BOOKING_SUCCESS", phone, data);
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

        log.info("Xử lý thông báo hủy lịch cho SĐT {}: mã {}", PhoneValidator.mask(phone), code);
        Map<String, Object> data = new HashMap<>();
        data.put("code", code != null ? code : "");
        data.put("date", dateStr);
        data.put("reason", reasonStr);
        data.put("message", message);

        sendAndLog(appointment, "CANCELLED", phone, data);
    }

    @Override
    @Transactional
    public void sendUpcomingReminders() {
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);

        List<Appointment> upcoming = appointmentRepository.findByAppointmentDateAndStatus(today, AppointmentStatus.PENDING);
        for (Appointment a : upcoming) {
            if (a.getTimeSlot() == null || a.getTimeSlot().getStartTime() == null) continue;

            LocalTime slotStart = a.getTimeSlot().getStartTime();
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
                    log.info("Xử lý thông báo nhắc lịch hẹn trước 2h cho SĐT {}: mã {}", PhoneValidator.mask(phone), a.getBookingCode());
                    Map<String, Object> data = new HashMap<>();
                    data.put("code", a.getBookingCode() != null ? a.getBookingCode() : "");
                    data.put("time", timeStr);
                    data.put("message", message);

                    sendAndLog(a, "REMINDER_2H", phone, data);
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
