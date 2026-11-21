package com.chuctupk.dto.request;

import com.chuctupk.entity.enums.Period;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTimeSlotRequest {

    private String slotLabel;

    @JsonFormat(pattern = "HH:mm[:ss]")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm[:ss]")
    private LocalTime endTime;

    private Period period;

    private Integer maxPatients;

    private Integer sortOrder;

    private Boolean isActive;
}
