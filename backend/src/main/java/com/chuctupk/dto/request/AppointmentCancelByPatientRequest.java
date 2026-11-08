package com.chuctupk.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AppointmentCancelByPatientRequest {
    @Size(max = 255, message = "Lý do hủy tối đa 255 ký tự")
    private String cancelReason;
}
