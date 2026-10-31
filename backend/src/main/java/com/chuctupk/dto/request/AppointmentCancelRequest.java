package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentCancelRequest {

    @NotBlank(message = "Trạng thái cập nhật không được để trống")
    @Pattern(regexp = "^CANCELLED$", message = "Trạng thái cập nhật chỉ có thể là CANCELLED")
    private String status;

    @Size(max = 255, message = "Lý do hủy lịch không vượt quá 255 ký tự")
    private String cancelReason;
}

