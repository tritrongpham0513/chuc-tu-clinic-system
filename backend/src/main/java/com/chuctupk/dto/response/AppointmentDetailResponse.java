package com.chuctupk.dto.response;

import com.chuctupk.entity.enums.AppointmentStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class AppointmentDetailResponse {
    private Long id;
    private String bookingCode;
    private LocalDate appointmentDate;
    private String timeSlotLabel;
    private LocalTime startTime;
    private LocalTime endTime;
    private String patientName;
    private String patientCode;
    private String relationship;
    private String serviceName;
    private String doctorName;
    private String symptoms;
    private Integer queueNumber;
    private String patientPhone;
    private Integer patientYearOfBirth;
    private String source;
    private AppointmentStatus status;
    private String statusLabel;
    private String cancelReason;
    private LocalDateTime createdAt;

    // Kết quả khám (nếu COMPLETED)
    private MedicalResultSummary medicalResult;

    @Data
    @Builder
    public static class MedicalResultSummary {
        private String diagnosis;
        private String prescription;
        private String advice;
        private LocalDate followUpDate;
        private LocalDateTime recordedAt;
    }
}
