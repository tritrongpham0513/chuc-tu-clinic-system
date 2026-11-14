package com.chuctupk.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleAppointmentRequest {

    @JsonAlias({"newDate", "date"})
    private String slotDate;

    @JsonAlias({"newSlotStart", "startTime"})
    private String slotStart;

    private LocalDate newDate;
    private Long newTimeSlotId;
    private String reason;
}
