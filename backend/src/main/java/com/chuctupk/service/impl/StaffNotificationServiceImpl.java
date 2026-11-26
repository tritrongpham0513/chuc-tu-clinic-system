package com.chuctupk.service.impl;

import com.chuctupk.dto.response.StaffNotificationDto;
import com.chuctupk.dto.response.StaffNotificationListResponse;
import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.Doctor;
import com.chuctupk.entity.User;
import com.chuctupk.entity.UserNotification;
import com.chuctupk.entity.enums.NotificationInboxType;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.event.AppointmentCancelledEvent;
import com.chuctupk.event.AppointmentCreatedEvent;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.UserNotificationRepository;
import com.chuctupk.repository.UserRepository;
import com.chuctupk.service.StaffNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffNotificationServiceImpl implements StaffNotificationService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAppointmentCreated(AppointmentCreatedEvent event) {
        if (event == null || event.appointmentId() == null) {
            return;
        }
        handleAppointmentEvent(event.appointmentId(), NotificationInboxType.APPOINTMENT_CREATED);
    }

    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAppointmentCancelled(AppointmentCancelledEvent event) {
        if (event == null || event.appointmentId() == null) {
            return;
        }
        handleAppointmentEvent(event.appointmentId(), NotificationInboxType.APPOINTMENT_CANCELLED);
    }

    private void handleAppointmentEvent(Long appointmentId, NotificationInboxType type) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findWithDetailById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            log.warn("Không tìm thấy lịch hẹn id={} để tạo thông báo nội bộ ({})", appointmentId, type);
            return;
        }
        Appointment appointment = appointmentOpt.get();

        // Xác định danh sách người nhận không trùng lặp (giữ thứ tự)
        Map<Long, User> recipientsById = new LinkedHashMap<>();
        List<User> staffUsers = userRepository.findByRoleInAndIsActiveTrue(List.of(Role.ADMIN, Role.STAFF));
        for (User u : staffUsers) {
            if (u != null && u.getId() != null) {
                recipientsById.put(u.getId(), u);
            }
        }

        Doctor doctor = appointment.getDoctor();
        User doctorUser = (doctor != null) ? doctor.getUser() : null;
        if (doctorUser != null && doctorUser.getId() != null && Boolean.TRUE.equals(doctorUser.getIsActive())) {
            recipientsById.putIfAbsent(doctorUser.getId(), doctorUser);
        }

        if (recipientsById.isEmpty()) {
            return;
        }

        String title = type == NotificationInboxType.APPOINTMENT_CREATED
                ? "Lịch khám mới"
                : "Lịch khám đã huỷ";
        String body = buildBody(appointment, type);
        String staffLink = buildStaffLink(appointment.getAppointmentDate());
        String doctorLink = "/noi-bo/lich-cua-toi";
        LocalDateTime now = LocalDateTime.now(clock);

        List<UserNotification> toSave = new ArrayList<>();
        for (User recipient : recipientsById.values()) {
            if (userNotificationRepository.existsByUserIdAndAppointmentIdAndType(
                    recipient.getId(), appointment.getId(), type)) {
                continue;
            }
            String link = recipient.getRole() == Role.DOCTOR ? doctorLink : staffLink;
            toSave.add(UserNotification.builder()
                    .user(recipient)
                    .appointment(appointment)
                    .type(type)
                    .title(truncate(title, 150))
                    .body(truncate(body, 500))
                    .link(truncate(link, 200))
                    .createdAt(now)
                    .build());
        }

        if (toSave.isEmpty()) {
            return;
        }

        List<UserNotification> saved = userNotificationRepository.saveAll(toSave);
        userNotificationRepository.flush();

        pushRealtimeNotifications(saved, doctorUser);
    }

    private void pushRealtimeNotifications(List<UserNotification> saved, User doctorUser) {
        try {
            UserNotification staffSample = null;
            UserNotification doctorNotif = null;

            for (UserNotification n : saved) {
                User u = n.getUser();
                if (u.getRole() == Role.ADMIN || u.getRole() == Role.STAFF) {
                    if (staffSample == null) {
                        staffSample = n;
                    }
                }
                if (doctorUser != null && u.getId().equals(doctorUser.getId())) {
                    doctorNotif = n;
                }
            }

            if (staffSample != null) {
                StaffNotificationDto staffDto = StaffNotificationDto.from(staffSample);
                messagingTemplate.convertAndSend("/topic/staff/appointments", staffDto);
            }

            if (doctorUser != null && doctorNotif != null && doctorUser.getUsername() != null) {
                StaffNotificationDto doctorDto = StaffNotificationDto.from(doctorNotif);
                messagingTemplate.convertAndSendToUser(
                        doctorUser.getUsername(),
                        "/queue/appointments",
                        doctorDto
                );
            }
        } catch (Exception ex) {
            log.warn("Không thể đẩy thông báo WebSocket realtime: {}", ex.getMessage());
        }
    }

    private String buildBody(Appointment appointment, NotificationInboxType type) {
        String patientName = (appointment.getPatient() != null && appointment.getPatient().getFullName() != null)
                ? appointment.getPatient().getFullName().trim()
                : "Bệnh nhân";

        LocalTime startTime = appointment.getSlotStartSnapshot();
        if (startTime == null && appointment.getTimeSlot() != null) {
            startTime = appointment.getTimeSlot().getStartTime();
        }
        String timeStr = startTime != null ? startTime.format(TIME_FORMATTER) : "--:--";
        String dateStr = appointment.getAppointmentDate() != null
                ? appointment.getAppointmentDate().format(DATE_FORMATTER)
                : "";

        Doctor doctor = appointment.getDoctor();
        String doctorSuffix = (doctor != null && doctor.getFullName() != null && !doctor.getFullName().isBlank())
                ? " với BS. " + doctor.getFullName().trim()
                : " (chưa gán bác sĩ)";

        String actionPhrase = type == NotificationInboxType.APPOINTMENT_CREATED
                ? " đặt lịch "
                : " đã huỷ lịch ";

        return patientName + actionPhrase + timeStr + " ngày " + dateStr + doctorSuffix;
    }

    private String buildStaffLink(LocalDate appointmentDate) {
        if (appointmentDate == null) {
            return "/noi-bo/lich-trong-ngay";
        }
        return "/noi-bo/lich-trong-ngay?ngay=" + appointmentDate.format(ISO_DATE_FORMATTER);
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffNotificationListResponse getNotifications(User currentUser, int page, int limit) {
        if (currentUser == null || currentUser.getId() == null) {
            throw AppException.unauthorized("Yêu cầu đăng nhập để thực hiện thao tác này");
        }
        if (page < 1) {
            throw AppException.unprocessable("Số trang phải lớn hơn 0");
        }
        if (limit < 1) {
            throw AppException.unprocessable("Số dòng mỗi trang phải lớn hơn 0");
        }
        if (limit > 100) {
            throw AppException.unprocessable("Số dòng mỗi trang tối đa 100");
        }

        Page<UserNotification> resultPage = userNotificationRepository.findByUserIdOrderByCreatedAtDesc(
                currentUser.getId(),
                PageRequest.of(page - 1, limit)
        );
        long unread = userNotificationRepository.countByUserIdAndReadAtIsNull(currentUser.getId());
        List<StaffNotificationDto> items = resultPage.getContent().stream()
                .map(StaffNotificationDto::from)
                .toList();

        return new StaffNotificationListResponse(
                items,
                page,
                limit,
                resultPage.getTotalElements(),
                unread
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw AppException.unauthorized("Yêu cầu đăng nhập để thực hiện thao tác này");
        }
        return userNotificationRepository.countByUserIdAndReadAtIsNull(currentUser.getId());
    }

    @Override
    @Transactional
    public StaffNotificationDto markRead(User currentUser, Long notificationId) {
        if (currentUser == null || currentUser.getId() == null) {
            throw AppException.unauthorized("Yêu cầu đăng nhập để thực hiện thao tác này");
        }
        UserNotification notification = userNotificationRepository
                .findByIdAndUserId(notificationId, currentUser.getId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy thông báo"));

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now(clock));
            notification = userNotificationRepository.save(notification);
        }
        return StaffNotificationDto.from(notification);
    }

    @Override
    @Transactional
    public int markAllRead(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw AppException.unauthorized("Yêu cầu đăng nhập để thực hiện thao tác này");
        }
        return userNotificationRepository.markAllRead(currentUser.getId(), LocalDateTime.now(clock));
    }
}
