package com.chuctupk.repository;

import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @EntityGraph(attributePaths = {"patient", "doctor", "doctor.user", "timeSlot"})
    @Query("SELECT a FROM Appointment a WHERE a.id = :id")
    Optional<Appointment> findWithDetailById(@Param("id") Long id);

    Optional<Appointment> findByBookingCode(String bookingCode);

    long countByAppointmentDate(LocalDate appointmentDate);

    boolean existsByBookingCode(String bookingCode);

    List<Appointment> findByAppointmentDateOrderByTimeSlotStartTimeAsc(LocalDate appointmentDate);

    List<Appointment> findByAppointmentDateBetween(LocalDate start, LocalDate end);

    List<Appointment> findByPatientPhoneOrderByAppointmentDateDesc(String phone);

    List<Appointment> findByStatusOrderByAppointmentDateDesc(AppointmentStatus status);

    boolean existsByPatientIdAndAppointmentDateAndTimeSlotIdAndStatusNot(
            Long patientId,
            LocalDate appointmentDate,
            Long timeSlotId,
            AppointmentStatus excludeStatus
    );

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.appointmentDate = :date " +
           "AND a.doctor.id = :doctorId AND a.timeSlot.id = :timeSlotId " +
           "AND a.status <> :excludeStatus")
    long countBookedSlotsForDoctor(
            @Param("date") LocalDate date,
            @Param("doctorId") Long doctorId,
            @Param("timeSlotId") Long timeSlotId,
            @Param("excludeStatus") AppointmentStatus excludeStatus
    );

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.appointmentDate = :date " +
           "AND a.timeSlot.id = :timeSlotId AND a.status <> :excludeStatus")
    long countBookedSlotsTotal(
            @Param("date") LocalDate date,
            @Param("timeSlotId") Long timeSlotId,
            @Param("excludeStatus") AppointmentStatus excludeStatus
    );

    // --- Module 1: Patient Profile Management ---
    boolean existsByPatientId(Long patientId);
    List<Appointment> findByPatientId(Long patientId);
    boolean existsByPatientIdAndAppointmentDateAndStatusNot(Long patientId, LocalDate appointmentDate, AppointmentStatus excludeStatus);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.patient.id = :patientId " +
           "AND a.appointmentDate = :date AND a.status IN (com.chuctupk.entity.enums.AppointmentStatus.PENDING, com.chuctupk.entity.enums.AppointmentStatus.CONFIRMED, com.chuctupk.entity.enums.AppointmentStatus.IN_PROGRESS)")
    boolean hasActiveAppointmentOnDate(@Param("patientId") Long patientId, @Param("date") LocalDate date);
    List<Appointment> findByPatient_UserIdOrderByAppointmentDateDesc(Long userId);
    List<Appointment> findByPatient_UserIdAndStatusNotOrderByAppointmentDateDesc(Long userId, AppointmentStatus status);

    // --- Module 6: Booking (JWT auth) ---
    List<Appointment> findByPatient_UserIdOrderByAppointmentDateDescCreatedAtDesc(Long userId);

    @Query("SELECT a FROM Appointment a WHERE a.patient.user.id = :userId AND a.status IN :statuses ORDER BY a.appointmentDate DESC, a.createdAt DESC")
    List<Appointment> findByUserIdAndStatuses(@Param("userId") Long userId, @Param("statuses") List<AppointmentStatus> statuses);

    @Query("SELECT a FROM Appointment a WHERE UPPER(a.bookingCode) = UPPER(:code) " +
           "OR a.patient.phone = :code OR UPPER(a.patient.patientCode) = UPPER(:code) " +
           "ORDER BY a.appointmentDate DESC, a.createdAt DESC")
    List<Appointment> lookupByCodeOrPhoneOrPatientCode(@Param("code") String code);

    @Query("SELECT a.patient.id, MAX(a.appointmentDate) FROM Appointment a " +
           "WHERE a.status = 'COMPLETED' AND a.patient.id IN :patientIds " +
           "GROUP BY a.patient.id")
    List<Object[]> findLatestCompletedVisitDates(@Param("patientIds") List<Long> patientIds);

    List<Appointment> findByAppointmentDateAndStatus(LocalDate appointmentDate, AppointmentStatus status);

    @Query("SELECT a FROM Appointment a " +
           "LEFT JOIN FETCH a.patient " +
           "LEFT JOIN FETCH a.doctor " +
           "LEFT JOIN FETCH a.timeSlot " +
           "WHERE a.appointmentDate = :date AND a.status = :status " +
           "ORDER BY a.timeSlot.startTime ASC, a.id ASC")
    List<Appointment> findWithDetailsByAppointmentDateAndStatus(
            @Param("date") LocalDate date,
            @Param("status") AppointmentStatus status
    );

    long countByDoctorChangeRequestedAtIsNotNullAndAppointmentDateGreaterThanEqual(LocalDate date);

    long countByDoctorIdAndAppointmentDateAndStatusIn(Long doctorId, LocalDate appointmentDate, java.util.Collection<AppointmentStatus> statuses);

    @Query("SELECT a FROM Appointment a WHERE a.patient.user.id = :userId " +
           "AND a.status IN :statuses " +
           "AND a.appointmentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY a.appointmentDate ASC, a.timeSlot.startTime ASC")
    List<Appointment> findUpcomingRemindersByUser(
            @Param("userId") Long userId,
            @Param("statuses") java.util.Collection<AppointmentStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.patient.id = :patientId AND a.doctor.user.id = :userId")
    boolean existsByPatientIdAndDoctorUserId(@Param("patientId") Long patientId, @Param("userId") Long userId);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.patient.id = :patientId AND a.doctor.id = :doctorId")
    boolean existsByPatientIdAndDoctorId(@Param("patientId") Long patientId, @Param("doctorId") Long doctorId);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId AND a.appointmentDate >= :today AND a.status NOT IN :excludeStatuses")
    long countFutureAppointmentsForDoctor(
            @Param("doctorId") Long doctorId,
            @Param("today") LocalDate today,
            @Param("excludeStatuses") java.util.Collection<AppointmentStatus> excludeStatuses
    );

    @Query("SELECT COUNT(a) FROM Appointment a " +
           "WHERE a.appointmentDate >= :today AND a.status != com.chuctupk.entity.enums.AppointmentStatus.CANCELLED " +
           "GROUP BY a.appointmentDate, a.timeSlot.id " +
           "ORDER BY COUNT(a) DESC")
    List<Long> findFutureSlotBookingCounts(
            @Param("today") LocalDate today,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.timeSlot.id = :timeSlotId AND a.appointmentDate >= :today AND a.status NOT IN :excludeStatuses")
    long countUpcomingAppointmentsForSlot(
            @Param("timeSlotId") Long timeSlotId,
            @Param("today") LocalDate today,
            @Param("excludeStatuses") java.util.Collection<AppointmentStatus> excludeStatuses
    );

    @Query("SELECT a.timeSlot.id, COUNT(a.id) FROM Appointment a WHERE a.appointmentDate >= :today AND a.status NOT IN :excludeStatuses GROUP BY a.timeSlot.id")
    List<Object[]> countUpcomingAppointmentsGroupedBySlot(
            @Param("today") LocalDate today,
            @Param("excludeStatuses") java.util.Collection<AppointmentStatus> excludeStatuses
    );

    long countByAppointmentDateAndStatusNot(LocalDate appointmentDate, AppointmentStatus excludeStatus);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE (a.service IS NOT NULL AND a.service.id = :serviceId) OR (a.serviceType IS NOT NULL AND (UPPER(a.serviceType) = UPPER(:slug) OR UPPER(a.serviceType) = UPPER(:slugUnderscore)))")
    boolean isServiceUsed(
            @Param("serviceId") Long serviceId,
            @Param("slug") String slug,
            @Param("slugUnderscore") String slugUnderscore
    );

    @Query("SELECT COUNT(a) FROM Appointment a WHERE (a.service IS NOT NULL AND a.service.id = :serviceId) OR (a.serviceType IS NOT NULL AND (UPPER(a.serviceType) = UPPER(:slug) OR UPPER(a.serviceType) = UPPER(:slugUnderscore)))")
    long countAppointmentsForService(
            @Param("serviceId") Long serviceId,
            @Param("slug") String slug,
            @Param("slugUnderscore") String slugUnderscore
    );

    // --- Sprint 13: Báo cáo tổng hợp theo kỳ (GROUP BY ở DB) ---
    @Query("""
            SELECT a.appointmentDate, a.status, COUNT(a)
            FROM Appointment a
            WHERE a.appointmentDate BETWEEN :from AND :to
            GROUP BY a.appointmentDate, a.status
            """)
    List<Object[]> countByDayAndStatus(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT d.id, d.fullName, a.status, COUNT(a)
            FROM Appointment a
            LEFT JOIN a.doctor d
            WHERE a.appointmentDate BETWEEN :from AND :to
            GROUP BY d.id, d.fullName, a.status
            """)
    List<Object[]> countByDoctorAndStatus(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT ts.id, ts.slotLabel, ts.startTime, ts.endTime, ts.maxPatients, ts.sortOrder, a.status, COUNT(a)
            FROM Appointment a
            JOIN a.timeSlot ts
            WHERE a.appointmentDate BETWEEN :from AND :to
            GROUP BY ts.id, ts.slotLabel, ts.startTime, ts.endTime, ts.maxPatients, ts.sortOrder, a.status
            ORDER BY ts.sortOrder ASC, ts.startTime ASC
            """)
    List<Object[]> countBySlotAndStatus(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT COUNT(DISTINCT a.patient.id)
            FROM Appointment a
            WHERE a.appointmentDate BETWEEN :from AND :to
              AND a.status = com.chuctupk.entity.enums.AppointmentStatus.COMPLETED
            """)
    long countDistinctCompletedPatientsInPeriod(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT a.patient.id
            FROM Appointment a
            WHERE a.status = com.chuctupk.entity.enums.AppointmentStatus.COMPLETED
              AND a.patient.id IN (
                  SELECT DISTINCT a2.patient.id
                  FROM Appointment a2
                  WHERE a2.appointmentDate BETWEEN :from AND :to
                    AND a2.status = com.chuctupk.entity.enums.AppointmentStatus.COMPLETED
              )
            GROUP BY a.patient.id
            HAVING MIN(a.appointmentDate) BETWEEN :from AND :to
            """)
    List<Long> findNewPatientIdsInPeriod(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
