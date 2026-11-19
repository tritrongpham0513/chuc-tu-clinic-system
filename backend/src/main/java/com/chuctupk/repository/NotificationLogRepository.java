package com.chuctupk.repository;

import com.chuctupk.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByAppointmentId(Long appointmentId);
    List<NotificationLog> findByTemplate(String template);
}
