package com.chuctupk.dto.response;

import com.chuctupk.entity.ClinicHoliday;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminHolidayResponse {
    private Long id;
    private LocalDate holidayDate;
    private String name;
    private Boolean isClosed;
    private LocalDateTime createdAt;
    private Long appointmentCount;

    public static AdminHolidayResponse from(ClinicHoliday h, Long appointmentCount) {
        return AdminHolidayResponse.builder()
                .id(h.getId())
                .holidayDate(h.getHolidayDate())
                .name(h.getName())
                .isClosed(h.getIsClosed())
                .createdAt(h.getCreatedAt())
                .appointmentCount(appointmentCount != null ? appointmentCount : 0L)
                .build();
    }
}
