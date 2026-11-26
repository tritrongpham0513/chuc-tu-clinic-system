package com.chuctupk.repository;

import com.chuctupk.entity.UserNotification;
import com.chuctupk.entity.enums.NotificationInboxType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    @EntityGraph(attributePaths = {"appointment"})
    Page<UserNotification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(Long userId);

    Optional<UserNotification> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndAppointmentIdAndType(Long userId, Long appointmentId, NotificationInboxType type);

    @Modifying
    @Query("UPDATE UserNotification n SET n.readAt = :now WHERE n.user.id = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
