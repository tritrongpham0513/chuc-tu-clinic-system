package com.chuctupk.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "medical_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @Column(name = "visit_date")
    private LocalDate visitDate;

    @Column(name = "pulse_bp", length = 50)
    private String pulseBp;

    @Column(length = 50)
    private String pulse;

    @Column(name = "blood_pressure", length = 50)
    private String bloodPressure;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(columnDefinition = "TEXT")
    private String treatment;

    @Column(name = "treatment_acupuncture", columnDefinition = "TEXT")
    private String treatmentAcupuncture;

    @Column(name = "treatment_acupressure", columnDefinition = "TEXT")
    private String treatmentAcupressure;

    @Column(name = "treatment_other", columnDefinition = "TEXT")
    private String treatmentOther;

    @Column(name = "prescription", columnDefinition = "TEXT")
    private String prescription;

    @Column(name = "advice", columnDefinition = "TEXT")
    private String advice;

    @Column(name = "doctor_name", length = 100)
    private String doctorName;

    @Column(name = "recorded_by")
    private Long recordedBy;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
