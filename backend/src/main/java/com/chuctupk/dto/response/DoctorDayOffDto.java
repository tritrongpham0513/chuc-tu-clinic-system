package com.chuctupk.dto.response;

import com.chuctupk.entity.enums.Period;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorDayOffDto {
    private Long id;
    private Long doctorId;
    private String doctorName;
    private LocalDate offDate;
    private Period period;
    private String reason;
    private LocalDateTime createdAt;
}
