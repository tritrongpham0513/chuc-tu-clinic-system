package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateHolidayRequest {

    @NotNull(message = "Ngày nghỉ không được để trống")
    private LocalDate holidayDate;

    @NotBlank(message = "Tên ngày nghỉ không được để trống")
    @Size(max = 150, message = "Tên ngày nghỉ không được vượt quá 150 ký tự")
    private String name;

    @Builder.Default
    private Boolean isClosed = true;
}
