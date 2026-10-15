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

    List<Appointment> findByAppointmentDateOrderByTimeSlotStartTimeAsc(LocalDate appointmentDate);

    List<Appointment> findByPatientPhoneOrderByAppointmentDateDesc(String phone);

    List<Appointment> findByStatusOrderByAppointmentDateDesc(AppointmentStatus status);

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
}
