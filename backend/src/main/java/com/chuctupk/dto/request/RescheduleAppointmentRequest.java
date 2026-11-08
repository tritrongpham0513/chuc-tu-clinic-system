package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleAppointmentRequest {

    @NotNull(message = "Ngày khám mới không được để trống")
    private LocalDate newDate;

    @NotNull(message = "Khung giờ mới không được để trống")
    private Long newTimeSlotId;

    private String reason;
}
