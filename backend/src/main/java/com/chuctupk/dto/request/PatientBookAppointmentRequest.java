package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientBookAppointmentRequest {

    @NotNull(message = "Cô chú vui lòng chọn hồ sơ người khám")
    private Long patientId;

    private String serviceType;

    @NotBlank(message = "Cô chú vui lòng chọn ngày khám")
    private String slotDate;

    @NotBlank(message = "Cô chú vui lòng chọn khung giờ khám")
    private String slotStart;

    private Long rebookFromId;
}

