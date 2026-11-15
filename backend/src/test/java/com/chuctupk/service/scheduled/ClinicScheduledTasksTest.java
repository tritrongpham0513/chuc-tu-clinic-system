package com.chuctupk.service.scheduled;

import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.AppointmentSeat;
import com.chuctupk.entity.OtpVerification;
import com.chuctupk.entity.RefreshToken;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.AppointmentSeatRepository;
import com.chuctupk.repository.OtpVerificationRepository;
import com.chuctupk.repository.RefreshTokenRepository;
import com.chuctupk.service.ClinicSettingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.time.*;
import java.util.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ClinicScheduledTasksTest {

    static class StubAppointmentSeatRepository implements AppointmentSeatRepository {
        List<Long> deletedAppointmentIds = new ArrayList<>();

        @Override
        public void deleteByAppointmentId(Long appointmentId) {
            deletedAppointmentIds.add(appointmentId);
        }

        @Override public Optional<AppointmentSeat> findByAppointmentId(Long appointmentId) { return Optional.empty(); }
        @Override public List<AppointmentSeat> findByAppointmentDateAndTimeSlotIdOrderBySeatNoAsc(LocalDate appointmentDate, Long timeSlotId) { return Collections.emptyList(); }
        @Override public long countByAppointmentDateAndTimeSlotId(LocalDate appointmentDate, Long timeSlotId) { return 0; }
        @Override public void flush() {}
        @Override public <S extends AppointmentSeat> S saveAndFlush(S entity) { return null; }
        @Override public <S extends AppointmentSeat> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<AppointmentSeat> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public AppointmentSeat getOne(Long aLong) { return null; }
        @Override public AppointmentSeat getById(Long aLong) { return null; }
        @Override public AppointmentSeat getReferenceById(Long aLong) { return null; }
        @Override public <S extends AppointmentSeat> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends AppointmentSeat> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends AppointmentSeat> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends AppointmentSeat> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends AppointmentSeat> long count(Example<S> example) { return 0; }
        @Override public <S extends AppointmentSeat> boolean exists(Example<S> example) { return false; }
        @Override public <S extends AppointmentSeat, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends AppointmentSeat> S save(S entity) { return null; }
        @Override public <S extends AppointmentSeat> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public Optional<AppointmentSeat> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<AppointmentSeat> findAll() { return Collections.emptyList(); }
        @Override public List<AppointmentSeat> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(AppointmentSeat entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends AppointmentSeat> entities) {}
        @Override public void deleteAll() {}
        @Override public List<AppointmentSeat> findAll(Sort sort) { return Collections.emptyList(); }
        @Override public Page<AppointmentSeat> findAll(Pageable pageable) { return null; }
    }

    static class StubAppointmentRepository implements AppointmentRepository {
        List<Appointment> foundAppointments = new ArrayList<>();
        List<Appointment> savedAppointments = new ArrayList<>();

        @Override
        public List<Appointment> findByAppointmentDateAndStatus(LocalDate appointmentDate, AppointmentStatus status) {
            return foundAppointments.stream()
                    .filter(a -> a.getStatus() == status && appointmentDate.equals(a.getAppointmentDate()))
                    .toList();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <S extends Appointment> List<S> saveAll(Iterable<S> entities) {
            savedAppointments.clear();
            for (S entity : entities) {
                savedAppointments.add(entity);
            }
            return (List<S>) savedAppointments;
        }

        // --- Dummy no-op implementations ---
        @Override public Optional<Appointment> findByBookingCode(String bookingCode) { return Optional.empty(); }
        @Override public long countByAppointmentDate(LocalDate appointmentDate) { return 0; }
        @Override public boolean existsByBookingCode(String bookingCode) { return false; }
        @Override public List<Appointment> findByAppointmentDateOrderByTimeSlotStartTimeAsc(LocalDate appointmentDate) { return Collections.emptyList(); }
        @Override public List<Appointment> findByAppointmentDateBetween(LocalDate start, LocalDate end) { return Collections.emptyList(); }
        @Override public List<Appointment> findByPatientPhoneOrderByAppointmentDateDesc(String phone) { return Collections.emptyList(); }
        @Override public List<Appointment> findByStatusOrderByAppointmentDateDesc(AppointmentStatus status) { return Collections.emptyList(); }
        @Override public boolean existsByPatientIdAndAppointmentDateAndTimeSlotIdAndStatusNot(Long patientId, LocalDate appointmentDate, Long timeSlotId, AppointmentStatus excludeStatus) { return false; }
        @Override public long countBookedSlotsForDoctor(LocalDate date, Long doctorId, Long timeSlotId, AppointmentStatus excludeStatus) { return 0; }
        @Override public long countBookedSlotsTotal(LocalDate date, Long timeSlotId, AppointmentStatus excludeStatus) { return 0; }
        @Override public boolean existsByPatientId(Long patientId) { return false; }
        @Override public List<Appointment> findByPatientId(Long patientId) { return Collections.emptyList(); }
        @Override public boolean existsByPatientIdAndAppointmentDateAndStatusNot(Long patientId, LocalDate appointmentDate, AppointmentStatus excludeStatus) { return false; }
        @Override public List<Appointment> findByPatient_UserIdOrderByAppointmentDateDesc(Long userId) { return Collections.emptyList(); }
        @Override public List<Appointment> findByPatient_UserIdAndStatusNotOrderByAppointmentDateDesc(Long userId, AppointmentStatus status) { return Collections.emptyList(); }
        @Override public List<Appointment> findByPatient_UserIdOrderByAppointmentDateDescCreatedAtDesc(Long userId) { return Collections.emptyList(); }
        @Override public List<Appointment> findByUserIdAndStatuses(Long userId, List<AppointmentStatus> statuses) { return Collections.emptyList(); }
        @Override public List<Appointment> lookupByCodeOrPhoneOrPatientCode(String code) { return Collections.emptyList(); }
        @Override public List<Object[]> findLatestCompletedVisitDates(List<Long> patientIds) { return Collections.emptyList(); }
        @Override public void flush() {}
        @Override public <S extends Appointment> S saveAndFlush(S entity) { return null; }
        @Override public <S extends Appointment> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<Appointment> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Appointment getOne(Long aLong) { return null; }
        @Override public Appointment getById(Long aLong) { return null; }
        @Override public Appointment getReferenceById(Long aLong) { return null; }
        @Override public <S extends Appointment> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends Appointment> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends Appointment> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends Appointment> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends Appointment> long count(Example<S> example) { return 0; }
        @Override public <S extends Appointment> boolean exists(Example<S> example) { return false; }
        @Override public <S extends Appointment, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends Appointment> S save(S entity) { return null; }
        @Override public Optional<Appointment> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<Appointment> findAll() { return Collections.emptyList(); }
        @Override public List<Appointment> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(Appointment entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends Appointment> entities) {}
        @Override public void deleteAll() {}
        @Override public List<Appointment> findAll(Sort sort) { return Collections.emptyList(); }
        @Override public Page<Appointment> findAll(Pageable pageable) { return null; }
    }

    static class StubRefreshTokenRepository implements RefreshTokenRepository {
        boolean deleteAllCalled = false;

        @Override
        public void deleteAllExpiredOrRevoked(LocalDateTime now) {
            deleteAllCalled = true;
        }

        @Override public Optional<RefreshToken> findByTokenHash(String tokenHash) { return Optional.empty(); }
        @Override public Optional<RefreshToken> findByTokenHashAndRevokedFalseAndExpiresAtAfter(String tokenHash, LocalDateTime now) { return Optional.empty(); }
        @Override public void revokeAllByUserId(Long userId) {}
        @Override public void flush() {}
        @Override public <S extends RefreshToken> S saveAndFlush(S entity) { return null; }
        @Override public <S extends RefreshToken> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<RefreshToken> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public RefreshToken getOne(Long aLong) { return null; }
        @Override public RefreshToken getById(Long aLong) { return null; }
        @Override public RefreshToken getReferenceById(Long aLong) { return null; }
        @Override public <S extends RefreshToken> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends RefreshToken> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends RefreshToken> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends RefreshToken> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends RefreshToken> long count(Example<S> example) { return 0; }
        @Override public <S extends RefreshToken> boolean exists(Example<S> example) { return false; }
        @Override public <S extends RefreshToken, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends RefreshToken> S save(S entity) { return null; }
        @Override public <S extends RefreshToken> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public Optional<RefreshToken> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<RefreshToken> findAll() { return Collections.emptyList(); }
        @Override public List<RefreshToken> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(RefreshToken entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends RefreshToken> entities) {}
        @Override public void deleteAll() {}
        @Override public List<RefreshToken> findAll(Sort sort) { return Collections.emptyList(); }
        @Override public Page<RefreshToken> findAll(Pageable pageable) { return null; }
    }

    static class StubOtpVerificationRepository implements OtpVerificationRepository {
        boolean deleteAllCalled = false;

        @Override
        public void deleteAllExpiredOrVerified(LocalDateTime now, LocalDateTime cutoff) {
            deleteAllCalled = true;
        }

        @Override public Optional<OtpVerification> findTopByPhoneAndOtpTypeOrderByCreatedAtDesc(String phone, com.chuctupk.entity.enums.OtpType otpType) { return Optional.empty(); }
        @Override public Optional<OtpVerification> findTopByPhoneAndOtpTypeAndIsVerifiedFalseOrderByCreatedAtDesc(String phone, com.chuctupk.entity.enums.OtpType otpType) { return Optional.empty(); }
        @Override public Optional<OtpVerification> findTopByPhoneOrderByCreatedAtDesc(String phone) { return Optional.empty(); }
        @Override public long countByPhoneAndCreatedAtAfter(String phone, LocalDateTime since) { return 0; }
        @Override public Optional<OtpVerification> findTopByPhoneAndOtpTypeAndIsVerifiedTrueAndExpiresAtAfterOrderByCreatedAtDesc(String phone, com.chuctupk.entity.enums.OtpType otpType, LocalDateTime now) { return Optional.empty(); }
        @Override public void flush() {}
        @Override public <S extends OtpVerification> S saveAndFlush(S entity) { return null; }
        @Override public <S extends OtpVerification> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<OtpVerification> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public OtpVerification getOne(Long aLong) { return null; }
        @Override public OtpVerification getById(Long aLong) { return null; }
        @Override public OtpVerification getReferenceById(Long aLong) { return null; }
        @Override public <S extends OtpVerification> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends OtpVerification> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends OtpVerification> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends OtpVerification> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends OtpVerification> long count(Example<S> example) { return 0; }
        @Override public <S extends OtpVerification> boolean exists(Example<S> example) { return false; }
        @Override public <S extends OtpVerification, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends OtpVerification> S save(S entity) { return null; }
        @Override public <S extends OtpVerification> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public Optional<OtpVerification> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<OtpVerification> findAll() { return Collections.emptyList(); }
        @Override public List<OtpVerification> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(OtpVerification entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends OtpVerification> entities) {}
        @Override public void deleteAll() {}
        @Override public List<OtpVerification> findAll(Sort sort) { return Collections.emptyList(); }
        @Override public Page<OtpVerification> findAll(Pageable pageable) { return null; }
    }

    @Test
    @DisplayName("A3 - Case 1: PENDING slot đã qua -> NO_SHOW + xóa ghế")
    void testAutoCancelNoShows_PendingSlotEnded_MarkedNoShowAndSeatDeleted() {
        LocalDate fixedDate = LocalDate.of(2026, 11, 14);
        // Chạy lúc 17:05 (quá giờ no_show_cancel_at 17:00)
        Instant fixedInstant = LocalDateTime.of(fixedDate, LocalTime.of(17, 5, 0))
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("Asia/Ho_Chi_Minh"));

        StubAppointmentRepository appointmentRepository = new StubAppointmentRepository();
        StubAppointmentSeatRepository seatRepository = new StubAppointmentSeatRepository();
        StubRefreshTokenRepository refreshTokenRepository = new StubRefreshTokenRepository();
        StubOtpVerificationRepository otpVerificationRepository = new StubOtpVerificationRepository();
        ClinicSettingService clinicSettingService = Mockito.mock(ClinicSettingService.class);
        when(clinicSettingService.getNoShowCancelAt()).thenReturn(LocalTime.of(17, 0));

        com.chuctupk.service.NotificationService notificationService = Mockito.mock(com.chuctupk.service.NotificationService.class);
        ClinicScheduledTasks tasks = new ClinicScheduledTasks(
                appointmentRepository, seatRepository, refreshTokenRepository, otpVerificationRepository, clinicSettingService, notificationService, fixedClock
        );

        // Slot 15:00 - 16:00 (đã kết thúc trước 17:05)
        TimeSlot pastSlot = TimeSlot.builder()
                .id(1L)
                .startTime(LocalTime.of(15, 0))
                .endTime(LocalTime.of(16, 0))
                .build();

        Appointment appt = Appointment.builder()
                .id(101L)
                .bookingCode("CT-261114-001")
                .appointmentDate(fixedDate)
                .timeSlot(pastSlot)
                .status(AppointmentStatus.PENDING)
                .build();

        appointmentRepository.foundAppointments = List.of(appt);

        int count = tasks.autoCancelNoShows();

        assertThat(count).isEqualTo(1);
        assertThat(appointmentRepository.savedAppointments).hasSize(1);
        Appointment saved = appointmentRepository.savedAppointments.get(0);
        assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
        assertThat(saved.getCancelReason()).isEqualTo("Không đến khám");
        assertThat(saved.getCancelledAt()).isNotNull();
        assertThat(seatRepository.deletedAppointmentIds).contains(101L);
    }

    @Test
    @DisplayName("A3 - Case 2: PENDING slot chưa tới (ví dụ 17:30 - 18:30 lúc 17:05) -> giữ nguyên")
    void testAutoCancelNoShows_PendingSlotNotEnded_KeptPending() {
        LocalDate fixedDate = LocalDate.of(2026, 11, 14);
        Instant fixedInstant = LocalDateTime.of(fixedDate, LocalTime.of(17, 5, 0))
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("Asia/Ho_Chi_Minh"));

        StubAppointmentRepository appointmentRepository = new StubAppointmentRepository();
        StubAppointmentSeatRepository seatRepository = new StubAppointmentSeatRepository();
        StubRefreshTokenRepository refreshTokenRepository = new StubRefreshTokenRepository();
        StubOtpVerificationRepository otpVerificationRepository = new StubOtpVerificationRepository();
        ClinicSettingService clinicSettingService = Mockito.mock(ClinicSettingService.class);
        when(clinicSettingService.getNoShowCancelAt()).thenReturn(LocalTime.of(17, 0));

        com.chuctupk.service.NotificationService notificationService = Mockito.mock(com.chuctupk.service.NotificationService.class);
        ClinicScheduledTasks tasks = new ClinicScheduledTasks(
                appointmentRepository, seatRepository, refreshTokenRepository, otpVerificationRepository, clinicSettingService, notificationService, fixedClock
        );

        // Slot 17:30 - 18:30 (chưa kết thúc lúc 17:05)
        TimeSlot futureSlot = TimeSlot.builder()
                .id(2L)
                .startTime(LocalTime.of(17, 30))
                .endTime(LocalTime.of(18, 30))
                .build();

        Appointment appt = Appointment.builder()
                .id(102L)
                .bookingCode("CT-261114-002")
                .appointmentDate(fixedDate)
                .timeSlot(futureSlot)
                .status(AppointmentStatus.PENDING)
                .build();

        appointmentRepository.foundAppointments = List.of(appt);

        int count = tasks.autoCancelNoShows();

        assertThat(count).isEqualTo(0);
        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(seatRepository.deletedAppointmentIds).isEmpty();
    }

    @Test
    @DisplayName("A3 - Case 3: CONFIRMED (đã check-in) -> giữ nguyên tuyệt đối, không huỷ")
    void testAutoCancelNoShows_ConfirmedCheckedIn_NeverCancelled() {
        LocalDate fixedDate = LocalDate.of(2026, 11, 14);
        Instant fixedInstant = LocalDateTime.of(fixedDate, LocalTime.of(17, 5, 0))
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("Asia/Ho_Chi_Minh"));

        StubAppointmentRepository appointmentRepository = new StubAppointmentRepository();
        StubAppointmentSeatRepository seatRepository = new StubAppointmentSeatRepository();
        StubRefreshTokenRepository refreshTokenRepository = new StubRefreshTokenRepository();
        StubOtpVerificationRepository otpVerificationRepository = new StubOtpVerificationRepository();
        ClinicSettingService clinicSettingService = Mockito.mock(ClinicSettingService.class);
        when(clinicSettingService.getNoShowCancelAt()).thenReturn(LocalTime.of(17, 0));

        com.chuctupk.service.NotificationService notificationService = Mockito.mock(com.chuctupk.service.NotificationService.class);
        ClinicScheduledTasks tasks = new ClinicScheduledTasks(
                appointmentRepository, seatRepository, refreshTokenRepository, otpVerificationRepository, clinicSettingService, notificationService, fixedClock
        );

        Appointment confirmedAppt = Appointment.builder()
                .id(103L)
                .bookingCode("CT-261114-003")
                .appointmentDate(fixedDate)
                .status(AppointmentStatus.CONFIRMED)
                .build();

        appointmentRepository.foundAppointments = List.of(confirmedAppt);

        int count = tasks.autoCancelNoShows();

        assertThat(count).isEqualTo(0);
        assertThat(confirmedAppt.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(seatRepository.deletedAppointmentIds).isEmpty();
    }

    @Test
    @DisplayName("A3 - Case 4: IN_PROGRESS -> giữ nguyên tuyệt đối")
    void testAutoCancelNoShows_InProgress_NeverCancelled() {
        LocalDate fixedDate = LocalDate.of(2026, 11, 14);
        Instant fixedInstant = LocalDateTime.of(fixedDate, LocalTime.of(17, 5, 0))
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("Asia/Ho_Chi_Minh"));

        StubAppointmentRepository appointmentRepository = new StubAppointmentRepository();
        StubAppointmentSeatRepository seatRepository = new StubAppointmentSeatRepository();
        StubRefreshTokenRepository refreshTokenRepository = new StubRefreshTokenRepository();
        StubOtpVerificationRepository otpVerificationRepository = new StubOtpVerificationRepository();
        ClinicSettingService clinicSettingService = Mockito.mock(ClinicSettingService.class);
        when(clinicSettingService.getNoShowCancelAt()).thenReturn(LocalTime.of(17, 0));

        com.chuctupk.service.NotificationService notificationService = Mockito.mock(com.chuctupk.service.NotificationService.class);
        ClinicScheduledTasks tasks = new ClinicScheduledTasks(
                appointmentRepository, seatRepository, refreshTokenRepository, otpVerificationRepository, clinicSettingService, notificationService, fixedClock
        );

        Appointment inProgressAppt = Appointment.builder()
                .id(104L)
                .bookingCode("CT-261114-004")
                .appointmentDate(fixedDate)
                .status(AppointmentStatus.IN_PROGRESS)
                .build();

        appointmentRepository.foundAppointments = List.of(inProgressAppt);

        int count = tasks.autoCancelNoShows();

        assertThat(count).isEqualTo(0);
        assertThat(inProgressAppt.getStatus()).isEqualTo(AppointmentStatus.IN_PROGRESS);
        assertThat(seatRepository.deletedAppointmentIds).isEmpty();
    }

    @Test
    @DisplayName("A3 - Case 5: Chạy 2 lần -> không nhân đôi (idempotent)")
    void testAutoCancelNoShows_RunTwice_Idempotent() {
        LocalDate fixedDate = LocalDate.of(2026, 11, 14);
        Instant fixedInstant = LocalDateTime.of(fixedDate, LocalTime.of(17, 5, 0))
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("Asia/Ho_Chi_Minh"));

        StubAppointmentRepository appointmentRepository = new StubAppointmentRepository();
        StubAppointmentSeatRepository seatRepository = new StubAppointmentSeatRepository();
        StubRefreshTokenRepository refreshTokenRepository = new StubRefreshTokenRepository();
        StubOtpVerificationRepository otpVerificationRepository = new StubOtpVerificationRepository();
        ClinicSettingService clinicSettingService = Mockito.mock(ClinicSettingService.class);
        when(clinicSettingService.getNoShowCancelAt()).thenReturn(LocalTime.of(17, 0));

        com.chuctupk.service.NotificationService notificationService = Mockito.mock(com.chuctupk.service.NotificationService.class);
        ClinicScheduledTasks tasks = new ClinicScheduledTasks(
                appointmentRepository, seatRepository, refreshTokenRepository, otpVerificationRepository, clinicSettingService, notificationService, fixedClock
        );

        TimeSlot pastSlot = TimeSlot.builder()
                .id(1L)
                .startTime(LocalTime.of(15, 0))
                .endTime(LocalTime.of(16, 0))
                .build();

        Appointment appt = Appointment.builder()
                .id(105L)
                .bookingCode("CT-261114-005")
                .appointmentDate(fixedDate)
                .timeSlot(pastSlot)
                .status(AppointmentStatus.PENDING)
                .build();

        // Lần 1: appt đang là PENDING
        appointmentRepository.foundAppointments = new ArrayList<>(List.of(appt));
        int count1 = tasks.autoCancelNoShows();
        assertThat(count1).isEqualTo(1);
        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);

        // Lần 2: appt đã là NO_SHOW, findByAppointmentDateAndStatus(today, PENDING) sẽ không trả về nữa
        int count2 = tasks.autoCancelNoShows();
        assertThat(count2).isEqualTo(0);
        assertThat(seatRepository.deletedAppointmentIds).containsExactly(105L);
    }

    @Test
    @DisplayName("cleanupExpiredTokensAndOtps: Gọi repository dọn dẹp các token và OTP hết hạn")
    void testCleanupExpiredTokensAndOtps() {
        Clock systemClock = Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
        StubAppointmentRepository appointmentRepository = new StubAppointmentRepository();
        StubAppointmentSeatRepository seatRepository = new StubAppointmentSeatRepository();
        StubRefreshTokenRepository refreshTokenRepository = new StubRefreshTokenRepository();
        StubOtpVerificationRepository otpVerificationRepository = new StubOtpVerificationRepository();
        ClinicSettingService clinicSettingService = Mockito.mock(ClinicSettingService.class);

        com.chuctupk.service.NotificationService notificationService = Mockito.mock(com.chuctupk.service.NotificationService.class);
        ClinicScheduledTasks tasks = new ClinicScheduledTasks(
                appointmentRepository, seatRepository, refreshTokenRepository, otpVerificationRepository, clinicSettingService, notificationService, systemClock
        );

        tasks.cleanupExpiredTokensAndOtps();

        assertThat(refreshTokenRepository.deleteAllCalled).isTrue();
        assertThat(otpVerificationRepository.deleteAllCalled).isTrue();
    }
}
