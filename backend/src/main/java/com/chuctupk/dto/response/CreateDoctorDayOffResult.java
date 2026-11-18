package com.chuctupk.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDoctorDayOffResult {
    private DoctorDayOffDto item;
    private int affectedAppointments;
}
