package com.chuctupk.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "record_access_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "scope", nullable = false, length = 20)
    private String scope;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.scope == null || this.scope.isBlank()) {
            this.scope = "BREAK_GLASS";
        }
    }
}
