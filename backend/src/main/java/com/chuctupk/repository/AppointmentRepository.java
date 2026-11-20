package com.chuctupk.repository;

import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

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
}
