package com.chuctupk.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(nullable = false, length = 50)
    private String template;

    @Column(name = "recipient_masked", nullable = false, length = 20)
    private String recipientMasked;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(length = 30)
    private String provider;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
