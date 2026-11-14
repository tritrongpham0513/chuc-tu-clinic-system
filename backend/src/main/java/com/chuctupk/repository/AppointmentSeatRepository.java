package com.chuctupk.repository;

import com.chuctupk.entity.AppointmentSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentSeatRepository extends JpaRepository<AppointmentSeat, Long> {

    Optional<AppointmentSeat> findByAppointmentId(Long appointmentId);

    List<AppointmentSeat> findByAppointmentDateAndTimeSlotIdOrderBySeatNoAsc(LocalDate appointmentDate, Long timeSlotId);

    long countByAppointmentDateAndTimeSlotId(LocalDate appointmentDate, Long timeSlotId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM AppointmentSeat s WHERE s.appointment.id = :appointmentId")
    void deleteByAppointmentId(@org.springframework.data.repository.query.Param("appointmentId") Long appointmentId);
}

