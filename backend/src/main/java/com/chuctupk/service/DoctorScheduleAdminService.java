package com.chuctupk.service;

import com.chuctupk.dto.request.CreateDoctorDayOffRequest;
import com.chuctupk.dto.request.UpdateDoctorScheduleRequest;
import com.chuctupk.dto.response.CreateDoctorDayOffResult;
import com.chuctupk.dto.response.DoctorDayOffDto;
import com.chuctupk.dto.response.DoctorScheduleMatrixResponse;

import java.time.LocalDate;
import java.util.List;

public interface DoctorScheduleAdminService {
    DoctorScheduleMatrixResponse getWeeklyScheduleMatrix();
    DoctorScheduleMatrixResponse updateDoctorSchedule(Long doctorId, UpdateDoctorScheduleRequest request);
    List<DoctorDayOffDto> getUpcomingDayOffs(LocalDate fromDate);
    CreateDoctorDayOffResult createDayOff(CreateDoctorDayOffRequest request);
    void deleteDayOff(Long id);
}
