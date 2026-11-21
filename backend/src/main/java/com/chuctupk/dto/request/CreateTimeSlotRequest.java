package com.chuctupk.dto.request;

import com.chuctupk.entity.enums.Period;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTimeSlotRequest {

    private String slotLabel;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    @JsonFormat(pattern = "HH:mm[:ss]")
    private LocalTime startTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    @JsonFormat(pattern = "HH:mm[:ss]")
    private LocalTime endTime;

    @NotNull(message = "Ca khám không được để trống")
    private Period period;

    private Integer maxPatients;

    private Integer sortOrder;

    private Boolean isActive;
}
