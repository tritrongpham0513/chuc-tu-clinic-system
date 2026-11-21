package com.chuctupk.dto.response;

import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.enums.Period;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTimeSlotResponse {
    private Long id;
    private String slotLabel;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private Period period;
    private Integer maxPatients;
    private Integer sortOrder;
    private Boolean isActive;
    private long upcomingAppointmentCount;

    public static AdminTimeSlotResponse from(TimeSlot slot, long upcomingCount) {
        if (slot == null) return null;
        return AdminTimeSlotResponse.builder()
                .id(slot.getId())
                .slotLabel(slot.getSlotLabel())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .period(slot.getPeriod())
                .maxPatients(slot.getMaxPatients())
                .sortOrder(slot.getSortOrder())
                .isActive(slot.getIsActive())
                .upcomingAppointmentCount(upcomingCount)
                .build();
    }
}
