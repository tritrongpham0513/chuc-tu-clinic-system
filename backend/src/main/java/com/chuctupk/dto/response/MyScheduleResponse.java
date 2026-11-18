package com.chuctupk.dto.response;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Phản hồi lịch trực cá nhân và ngày nghỉ sắp tới dành riêng cho bác sĩ.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyScheduleResponse {
    private Long doctorId;
    private String fullName;
    private String title;
    private String specialty;
    private List<String> days;
    private Map<String, List<String>> shifts;
    private List<DoctorDayOffDto> dayOffs;
}
