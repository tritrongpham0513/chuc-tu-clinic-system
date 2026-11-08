package com.chuctupk.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class BookingRequest {

    @NotNull(message = "Ngày khám không được để trống")
    private LocalDate appointmentDate;

    @NotNull(message = "Khung giờ không được để trống")
    private Long timeSlotId;

    @NotNull(message = "Hồ sơ bệnh nhân không được để trống")
    private Long patientId;

    private Long serviceId;

    @Size(max = 500, message = "Triệu chứng tối đa 500 ký tự")
    private String symptoms;
}
