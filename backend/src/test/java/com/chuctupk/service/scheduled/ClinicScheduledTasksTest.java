package com.chuctupk.service.scheduled;

import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.OtpVerification;
import com.chuctupk.entity.RefreshToken;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.OtpVerificationRepository;
import com.chuctupk.repository.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.time.*;
import java.util.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicScheduledTasksTest {

    static class StubAppointmentRepository implements AppointmentRepository {
        List<Appointment> foundAppointments = new ArrayList<>();
        List<Appointment> savedAppointments = new ArrayList<>();

        @Override
        public List<Appointment> findByAppointmentDateAndStatus(LocalDate appointmentDate, AppointmentStatus status) {
            return foundAppointments;
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
    @DisplayName("autoCancelNoShows: Hủy tất cả các lịch PENDING của ngày hôm nay lúc 17:00")
    void testAutoCancelNoShows_Success() {
        LocalDate fixedDate = LocalDate.of(2026, 11, 14);
        Instant fixedInstant = LocalDateTime.of(fixedDate, LocalTime.of(17, 0, 0))
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("Asia/Ho_Chi_Minh"));

        StubAppointmentRepository appointmentRepository = new StubAppointmentRepository();
        StubRefreshTokenRepository refreshTokenRepository = new StubRefreshTokenRepository();
        StubOtpVerificationRepository otpVerificationRepository = new StubOtpVerificationRepository();

        ClinicScheduledTasks tasks = new ClinicScheduledTasks(
                appointmentRepository, refreshTokenRepository, otpVerificationRepository, fixedClock
        );

        Appointment appt1 = Appointment.builder()
                .id(1L)
                .bookingCode("CT-261114-001")
                .appointmentDate(fixedDate)
                .status(AppointmentStatus.PENDING)
                .build();

        Appointment appt2 = Appointment.builder()
                .id(2L)
                .bookingCode("CT-261114-002")
                .appointmentDate(fixedDate)
                .status(AppointmentStatus.PENDING)
                .build();

        appointmentRepository.foundAppointments = List.of(appt1, appt2);

        int cancelledCount = tasks.autoCancelNoShows();

        assertThat(cancelledCount).isEqualTo(2);
        assertThat(appointmentRepository.savedAppointments).hasSize(2);
        assertThat(appointmentRepository.savedAppointments.get(0).getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointmentRepository.savedAppointments.get(0).getCancelReason()).isEqualTo(ClinicScheduledTasks.AUTO_CANCEL_REASON);
        assertThat(appointmentRepository.savedAppointments.get(0).getCancelledAt()).isNotNull();

        assertThat(appointmentRepository.savedAppointments.get(1).getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointmentRepository.savedAppointments.get(1).getCancelReason()).isEqualTo(ClinicScheduledTasks.AUTO_CANCEL_REASON);
    }

    @Test
    @DisplayName("autoCancelNoShows: Không có lịch nào PENDING thì không làm gì")
    void testAutoCancelNoShows_Empty() {
        LocalDate fixedDate = LocalDate.of(2026, 11, 14);
        Instant fixedInstant = LocalDateTime.of(fixedDate, LocalTime.of(17, 0, 0))
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("Asia/Ho_Chi_Minh"));

        StubAppointmentRepository appointmentRepository = new StubAppointmentRepository();
        StubRefreshTokenRepository refreshTokenRepository = new StubRefreshTokenRepository();
        StubOtpVerificationRepository otpVerificationRepository = new StubOtpVerificationRepository();

        ClinicScheduledTasks tasks = new ClinicScheduledTasks(
                appointmentRepository, refreshTokenRepository, otpVerificationRepository, fixedClock
        );

        int count = tasks.autoCancelNoShows();

        assertThat(count).isEqualTo(0);
        assertThat(appointmentRepository.savedAppointments).isEmpty();
    }

    @Test
    @DisplayName("cleanupExpiredTokensAndOtps: Gọi repository dọn dẹp các token và OTP hết hạn")
    void testCleanupExpiredTokensAndOtps() {
        Clock systemClock = Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
        StubAppointmentRepository appointmentRepository = new StubAppointmentRepository();
        StubRefreshTokenRepository refreshTokenRepository = new StubRefreshTokenRepository();
        StubOtpVerificationRepository otpVerificationRepository = new StubOtpVerificationRepository();

        ClinicScheduledTasks tasks = new ClinicScheduledTasks(
                appointmentRepository, refreshTokenRepository, otpVerificationRepository, systemClock
        );

        tasks.cleanupExpiredTokensAndOtps();

        assertThat(refreshTokenRepository.deleteAllCalled).isTrue();
        assertThat(otpVerificationRepository.deleteAllCalled).isTrue();
    }
}
