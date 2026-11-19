package com.chuctupk.repository;

import com.chuctupk.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByAppointmentId(Long appointmentId);
    List<NotificationLog> findByTemplate(String template);
    List<NotificationLog> findByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);

    @Query("SELECT nl.appointment.id, MAX(nl.createdAt) FROM NotificationLog nl " +
           "WHERE nl.appointment.id IN :appointmentIds " +
           "AND (nl.channel = 'MANUAL' OR nl.template = 'REMINDER_MANUAL') " +
           "AND nl.status = 'SENT' " +
           "GROUP BY nl.appointment.id")
    List<Object[]> findLatestReminderSentAtByAppointmentIds(@Param("appointmentIds") List<Long> appointmentIds);
}
