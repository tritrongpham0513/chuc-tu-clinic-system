package com.chuctupk.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;

@Data
@Builder
public class SlotAvailabilityResponse {
    private Long id;
    private String slotLabel;
    private LocalTime startTime;
    private LocalTime endTime;
    private String period;
    private Integer maxPatients;
    private Long bookedCount;
    private Long available;
    private Boolean isFull;
    private Integer sortOrder;
}
