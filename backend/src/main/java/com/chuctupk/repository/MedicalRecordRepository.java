package com.chuctupk.repository;

import com.chuctupk.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    Optional<MedicalRecord> findByAppointmentId(Long appointmentId);
    List<MedicalRecord> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    @Query("""
        SELECT mr FROM MedicalRecord mr
        JOIN FETCH mr.patient p
        LEFT JOIN FETCH mr.doctor d
        LEFT JOIN FETCH mr.appointment a
        WHERE mr.followUpDate IS NOT NULL
          AND mr.followUpDate BETWEEN :fromDate AND :toDate
          AND p.lockedAt IS NULL
          AND (p.isActive IS TRUE OR p.isActive IS NULL)
          AND NOT EXISTS (
              SELECT a2.id FROM Appointment a2
              WHERE a2.patient = p
                AND a2.appointmentDate >= :today
                AND a2.status IN (
                    com.chuctupk.entity.enums.AppointmentStatus.PENDING,
                    com.chuctupk.entity.enums.AppointmentStatus.CONFIRMED,
                    com.chuctupk.entity.enums.AppointmentStatus.IN_PROGRESS
                )
          )
        ORDER BY mr.followUpDate ASC, mr.id DESC
    """)
    List<MedicalRecord> findFollowUpsAll(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("today") LocalDate today
    );

    @Query("""
        SELECT mr FROM MedicalRecord mr
        JOIN FETCH mr.patient p
        LEFT JOIN FETCH mr.doctor d
        LEFT JOIN FETCH mr.appointment a
        WHERE mr.followUpDate IS NOT NULL
          AND mr.followUpDate BETWEEN :fromDate AND :toDate
          AND p.lockedAt IS NULL
          AND (p.isActive IS TRUE OR p.isActive IS NULL)
          AND NOT EXISTS (
              SELECT a2.id FROM Appointment a2
              WHERE a2.patient = p
                AND a2.appointmentDate >= :today
                AND a2.status IN (
                    com.chuctupk.entity.enums.AppointmentStatus.PENDING,
                    com.chuctupk.entity.enums.AppointmentStatus.CONFIRMED,
                    com.chuctupk.entity.enums.AppointmentStatus.IN_PROGRESS
                )
          )
          AND ((d IS NOT NULL AND d.user IS NOT NULL AND d.user.id = :doctorUserId)
               OR (a IS NOT NULL AND a.doctor IS NOT NULL AND a.doctor.user IS NOT NULL AND a.doctor.user.id = :doctorUserId))
        ORDER BY mr.followUpDate ASC, mr.id DESC
    """)
    List<MedicalRecord> findFollowUpsByDoctorUserId(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("today") LocalDate today,
            @Param("doctorUserId") Long doctorUserId
    );
}
