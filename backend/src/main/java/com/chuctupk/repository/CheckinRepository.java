package com.chuctupk.repository;

import com.chuctupk.entity.Checkin;
import com.chuctupk.entity.enums.CheckinStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckinRepository extends JpaRepository<Checkin, Long> {

    Optional<Checkin> findByAppointmentId(Long appointmentId);

    List<Checkin> findByAppointmentIdIn(List<Long> appointmentIds);

    List<Checkin> findByStatusOrderByQueueNoAsc(CheckinStatus status);

    List<Checkin> findByStatusInOrderByQueueNoAsc(List<CheckinStatus> statuses);

    List<Checkin> findByCheckInTimeBetween(LocalDateTime start, LocalDateTime end);

    boolean existsBySeatNoAndStatus(Integer seatNo, CheckinStatus status);

    long countByCheckInTimeBetween(LocalDateTime start, LocalDateTime end);

    long countByStatusAndCheckInTimeBetween(CheckinStatus status, LocalDateTime start, LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Checkin c WHERE c.status = 'IN_ROOM' AND c.checkInTime BETWEEN :start AND :end ORDER BY c.seatNo ASC")
    List<Checkin> findInRoomToday(@org.springframework.data.repository.query.Param("start") LocalDateTime start,
                                  @org.springframework.data.repository.query.Param("end") LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Checkin c WHERE c.status = 'WAITING' AND c.checkInTime BETWEEN :start AND :end ORDER BY c.priorityGroup ASC, c.checkInTime ASC")
    List<Checkin> findWaitingTodayOrdered(@org.springframework.data.repository.query.Param("start") LocalDateTime start,
                                         @org.springframework.data.repository.query.Param("end") LocalDateTime end);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Checkin c WHERE c.appointment.id = :appointmentId")
    void deleteByAppointmentId(@org.springframework.data.repository.query.Param("appointmentId") Long appointmentId);
}

