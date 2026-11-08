package com.chuctupk.dto.request;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMedicalRecordRequest {

    private String pulseBp;
    private String diagnosis;
    private String treatment;
    private String prescription;
    private String advice;
    private LocalDate followUpDate;
}
