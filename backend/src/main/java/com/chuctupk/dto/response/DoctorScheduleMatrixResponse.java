package com.chuctupk.dto.response;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorScheduleMatrixResponse {
    private List<String> days;
    private List<DoctorScheduleRow> doctors;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DoctorScheduleRow {
        private Long id;
        private String fullName;
        private String title;
        private String specialty;
        private Boolean isActive;
        private Map<String, List<String>> schedules;
    }
}
