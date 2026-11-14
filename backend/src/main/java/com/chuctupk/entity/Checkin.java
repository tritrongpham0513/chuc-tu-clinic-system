package com.chuctupk.entity;

import com.chuctupk.entity.enums.CheckinStatus;
import com.chuctupk.entity.enums.PriorityGroup;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "checkins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Checkin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CheckinStatus status = CheckinStatus.WAITING;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_group", nullable = false, length = 20)
    @Builder.Default
    private PriorityGroup priorityGroup = PriorityGroup.NORMAL;

    @Column(name = "seat_no")
    private Integer seatNo;

    @Column(name = "queue_no", nullable = false)
    private Integer queueNo;

    @Column(name = "check_in_time", nullable = false)
    @Builder.Default
    private LocalDateTime checkInTime = LocalDateTime.now();

    @Column(name = "entered_at")
    private LocalDateTime enteredAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.checkInTime == null) {
            this.checkInTime = LocalDateTime.now();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

