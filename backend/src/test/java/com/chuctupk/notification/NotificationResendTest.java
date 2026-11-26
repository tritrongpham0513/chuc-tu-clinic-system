package com.chuctupk.notification;

import com.chuctupk.dto.response.AdminNotificationDto;
import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.NotificationLog;
import com.chuctupk.entity.Patient;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.repository.NotificationLogRepository;
import com.chuctupk.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class NotificationResendTest {

    private FakeNotificationLogRepository notificationLogRepository;
    private Clock fixedClock;
    private FakeNotificationProvider smsProvider;

    static class FakeNotificationProvider implements NotificationProvider {
        String channel = "SMS";
        DeliveryResult resultToReturn = new DeliveryResult(DeliveryResult.Status.SENT, null, "esms");
        boolean throwException = false;
        NotificationMessage lastMessage;

        @Override
        public String channel() {
            return channel;
        }

        @Override
        public DeliveryResult send(NotificationMessage message) {
            this.lastMessage = message;
            if (throwException) {
                throw new RuntimeException("SMS Gateway connection refused");
            }
            return resultToReturn;
        }
    }

    static class FakeNotificationLogRepository implements NotificationLogRepository {
        Map<Long, NotificationLog> storage = new HashMap<>();
        AtomicLong idSeq = new AtomicLong(100);

        @Override
        public Optional<NotificationLog> findById(Long id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public Optional<NotificationLog> findWithDetailsById(Long id) {
            return findById(id);
        }

        @Override
        public <S extends NotificationLog> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(idSeq.incrementAndGet());
            }
            storage.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public List<NotificationLog> findByStatusOrderByCreatedAtDesc(String status) {
            return storage.values().stream()
                    .filter(n -> status.equalsIgnoreCase(n.getStatus()))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .toList();
        }

        @Override
        public List<NotificationLog> findAllByOrderByCreatedAtDesc() {
            return storage.values().stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .toList();
        }

        @Override
        public List<NotificationLog> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable) {
            return findByStatusOrderByCreatedAtDesc(status).stream()
                    .skip(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .toList();
        }

        @Override
        public List<NotificationLog> findAllByOrderByCreatedAtDesc(Pageable pageable) {
            return findAllByOrderByCreatedAtDesc().stream()
                    .skip(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .toList();
        }

        @Override public List<NotificationLog> findByAppointmentId(Long appointmentId) { return Collections.emptyList(); }
        @Override public List<NotificationLog> findByTemplate(String template) { return Collections.emptyList(); }
        @Override public List<NotificationLog> findByAppointmentIdOrderByCreatedAtDesc(Long appointmentId) { return Collections.emptyList(); }
        @Override public List<Object[]> findLatestReminderSentAtByAppointmentIds(List<Long> appointmentIds) { return Collections.emptyList(); }
        @Override public void flush() {}
        @Override public <S extends NotificationLog> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends NotificationLog> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<NotificationLog> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public NotificationLog getOne(Long aLong) { return storage.get(aLong); }
        @Override public NotificationLog getById(Long aLong) { return storage.get(aLong); }
        @Override public NotificationLog getReferenceById(Long aLong) { return storage.get(aLong); }
        @Override public <S extends NotificationLog> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends NotificationLog> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends NotificationLog> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends NotificationLog> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
        @Override public <S extends NotificationLog> long count(Example<S> example) { return 0; }
        @Override public <S extends NotificationLog> boolean exists(Example<S> example) { return false; }
        @Override public <S extends NotificationLog, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends NotificationLog> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public boolean existsById(Long aLong) { return storage.containsKey(aLong); }
        @Override public List<NotificationLog> findAll() { return new ArrayList<>(storage.values()); }
        @Override public List<NotificationLog> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return storage.size(); }
        @Override public void deleteById(Long aLong) { storage.remove(aLong); }
        @Override public void delete(NotificationLog entity) { storage.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends NotificationLog> entities) {}
        @Override public void deleteAll() { storage.clear(); }
        @Override public List<NotificationLog> findAll(Sort sort) { return findAll(); }
        @Override public Page<NotificationLog> findAll(Pageable pageable) { return Page.empty(); }
    }

    @BeforeEach
    void setUp() {
        notificationLogRepository = new FakeNotificationLogRepository();
        smsProvider = new FakeNotificationProvider();

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        Instant instant = LocalDateTime.of(2026, 11, 22, 10, 0, 0).atZone(zone).toInstant();
        fixedClock = Clock.fixed(instant, zone);
    }

    private Appointment createTestAppointment(Long id, String code, String phone) {
        TimeSlot slot = TimeSlot.builder()
                .id(1L)
                .startTime(LocalTime.of(8, 30))
                .endTime(LocalTime.of(9, 30))
                .build();

        Patient patient = Patient.builder()
                .id(100L)
                .fullName("Bác Trần Văn B")
                .phone(phone)
                .build();

        return Appointment.builder()
                .id(id)
                .bookingCode(code)
                .patient(patient)
                .bookerPhone(phone)
                .appointmentDate(LocalDate.of(2026, 11, 25))
                .timeSlot(slot)
                .status(AppointmentStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Ticket 6: resend tạo dòng log mới, không sửa dòng cũ và giữ đúng lý do hủy nếu là CANCELLED")
    void testResendCreatesNewLogWithoutModifyingOld() {
        Appointment apt = createTestAppointment(10L, "CT-261125-001", "0987654321");
        apt.setCancelReason("Bac si ban dot xuat");

        NotificationLog oldLog = NotificationLog.builder()
                .id(100L)
                .appointment(apt)
                .channel("SMS")
                .template("BOOKING_SUCCESS")
                .recipientMasked("098 *** 321")
                .status("FAILED")
                .errorCode("101")
                .provider("esms")
                .createdAt(LocalDateTime.of(2026, 11, 22, 9, 0, 0))
                .build();

        notificationLogRepository.save(oldLog);

        Map<String, NotificationProvider> providers = Map.of("sms", smsProvider);
        NotificationServiceImpl service = new NotificationServiceImpl(
                providers, null, notificationLogRepository, fixedClock, "sms"
        );

        AdminNotificationDto result = service.resendNotification(100L);

        // 1. Kiểm tra dòng log cũ hoàn toàn không bị sửa đổi
        NotificationLog oldLogAfter = notificationLogRepository.findById(100L).orElseThrow();
        assertThat(oldLogAfter.getId()).isEqualTo(100L);
        assertThat(oldLogAfter.getStatus()).isEqualTo("FAILED");
        assertThat(oldLogAfter.getErrorCode()).isEqualTo("101");

        // 2. Kiểm tra dòng log mới được tạo
        assertThat(result.id()).isNotEqualTo(100L);
        assertThat(result.id()).isEqualTo(101L);
        assertThat(result.status()).isEqualTo("SENT");
        assertThat(result.errorCode()).isNull();

        NotificationLog newlySavedLog = notificationLogRepository.findById(result.id()).orElseThrow();
        assertThat(newlySavedLog.getAppointment().getId()).isEqualTo(10L);
        assertThat(newlySavedLog.getChannel()).isEqualTo("SMS");
        assertThat(newlySavedLog.getTemplate()).isEqualTo("BOOKING_SUCCESS");
        assertThat(newlySavedLog.getStatus()).isEqualTo("SENT");
        assertThat(newlySavedLog.getErrorCode()).isNull();
        assertThat(newlySavedLog.getProvider()).isEqualTo("esms");

        // 3. Nếu resend mẫu CANCELLED -> dùng cancelReason thực tế của appointment
        NotificationLog cancelLog = NotificationLog.builder()
                .id(105L)
                .appointment(apt)
                .channel("SMS")
                .template("CANCELLED")
                .recipientMasked("098 *** 321")
                .status("FAILED")
                .errorCode("TIMEOUT")
                .provider("esms")
                .createdAt(LocalDateTime.of(2026, 11, 22, 9, 5, 0))
                .build();
        notificationLogRepository.save(cancelLog);

        service.resendNotification(105L);
        assertThat(smsProvider.lastMessage.data().get("message").toString()).contains("Bac si ban dot xuat");
    }

    @Test
    @DisplayName("Ticket 6: Lỗi provider không làm hỏng luồng đặt lịch")
    void testProviderErrorDoesNotBreakBookingFlow() {
        Appointment apt = createTestAppointment(20L, "CT-261125-002", "0901234567");

        // Khi SMS provider ném RuntimeException (mất kết nối, timeout...)
        smsProvider.throwException = true;

        Map<String, NotificationProvider> providers = Map.of("sms", smsProvider);
        NotificationServiceImpl service = new NotificationServiceImpl(
                providers, null, notificationLogRepository, fixedClock, "sms"
        );

        // Luồng đặt lịch gọi notifyBookingSuccess KHÔNG được ném exception ra ngoài
        assertThatCode(() -> service.notifyBookingSuccess(apt)).doesNotThrowAnyException();

        // Ghi nhận dòng log FAILED mà không làm crash app
        List<NotificationLog> logs = notificationLogRepository.findAll();
        assertThat(logs).hasSize(1);
        NotificationLog saved = logs.get(0);
        assertThat(saved.getStatus()).isEqualTo("FAILED");
        assertThat(saved.getErrorCode()).contains("SMS Gateway connection refused");
    }

    @Test
    @DisplayName("Ticket 3: Dịch các mã lỗi nhà mạng và lỗi kết nối sang tiếng Việt trong danh sách Admin")
    void testGetAdminNotificationsTranslatesErrorCodes() {
        Appointment apt = createTestAppointment(30L, "CT-261125-003", "0909999999");

        notificationLogRepository.save(NotificationLog.builder()
                .id(201L).appointment(apt).channel("SMS").template("BOOKING_SUCCESS")
                .recipientMasked("090 *** 999").status("FAILED").errorCode("101").provider("esms")
                .createdAt(LocalDateTime.of(2026, 11, 22, 9, 30, 0)).build());
        notificationLogRepository.save(NotificationLog.builder()
                .id(202L).appointment(apt).channel("SMS").template("CANCELLED")
                .recipientMasked("090 *** 999").status("FAILED").errorCode("CONNECT_ERROR").provider("esms")
                .createdAt(LocalDateTime.of(2026, 11, 22, 9, 31, 0)).build());
        notificationLogRepository.save(NotificationLog.builder()
                .id(203L).appointment(apt).channel("SMS").template("REMINDER_2H")
                .recipientMasked("090 *** 999").status("FAILED").errorCode("TIMEOUT").provider("esms")
                .createdAt(LocalDateTime.of(2026, 11, 22, 9, 32, 0)).build());

        Map<String, NotificationProvider> providers = Map.of("sms", smsProvider);
        NotificationServiceImpl service = new NotificationServiceImpl(
                providers, null, notificationLogRepository, fixedClock, "sms"
        );

        List<AdminNotificationDto> list = service.getAdminNotifications("FAILED");
        assertThat(list).hasSize(3);
        assertThat(list.get(0).errorMessage()).isEqualTo("Lỗi kết nối mạng hoặc quá thời gian chờ (Timeout)");
        assertThat(list.get(1).errorMessage()).isEqualTo("Không thể kết nối tới máy chủ SMS Gateway");
        assertThat(list.get(2).errorMessage()).isEqualTo("Đăng nhập thất bại (sai ApiKey hoặc SecretKey)");
        assertThat(list.get(2).templateLabel()).isEqualTo("Xác nhận đặt lịch");
    }

    @Test
    @DisplayName("P0 Guard: getAdminNotifications có @Transactional(readOnly=true) và Repository dùng @EntityGraph")
    void testLazyLoadingProtectionMetadata() throws NoSuchMethodException {
        Method serviceMethod = NotificationServiceImpl.class.getMethod("getAdminNotifications", String.class, int.class, int.class);
        Transactional tx = serviceMethod.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isTrue();

        Method repoMethodAll = NotificationLogRepository.class.getMethod("findAllByOrderByCreatedAtDesc", Pageable.class);
        EntityGraph egAll = repoMethodAll.getAnnotation(EntityGraph.class);
        assertThat(egAll).isNotNull();
        assertThat(egAll.attributePaths()).contains("appointment", "appointment.patient");

        Method repoMethodStatus = NotificationLogRepository.class.getMethod("findByStatusOrderByCreatedAtDesc", String.class, Pageable.class);
        EntityGraph egStatus = repoMethodStatus.getAnnotation(EntityGraph.class);
        assertThat(egStatus).isNotNull();
        assertThat(egStatus.attributePaths()).contains("appointment", "appointment.patient");
    }
}
