package com.chuctupk.service;

import com.chuctupk.entity.TimeSlot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Trái tim tính toán sức chứa cho các khung giờ khám bệnh.
 * Nguồn sự thật duy nhất (Single Source of Truth) kết hợp số bác sĩ trực và cấu hình phòng khám.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CapacityResolver {

    private final DoctorAssignmentService doctorAssignmentService;
    private final ClinicSettingService clinicSettingService;

    /**
     * Sức chứa thực tế của khung giờ tại một ngày cụ thể dựa trên số bác sĩ trực ca.
     * Công thức: Math.min(slotCap, onDuty * perDoctor), trả về 0 nếu không có bác sĩ trực.
     */
    public int effectiveCapacity(LocalDate date, TimeSlot slot) {
        if (slot == null || date == null) {
            return 0;
        }
        int onDuty = doctorAssignmentService.findOnDutyDoctorIds(date, slot.getPeriod()).size();
        if (onDuty == 0) {
            return 0;
        }
        int perDoctor = clinicSettingService.getPatientsPerDoctor(); // default 3
        int slotCap = (slot.getMaxPatients() != null && slot.getMaxPatients() > 0)
                ? slot.getMaxPatients()
                : clinicSettingService.getSlotCapacity();
        return Math.min(slotCap, onDuty * perDoctor);
    }

    /**
     * Sức chứa tối đa của 1 bác sĩ trong khung giờ.
     * Công thức: Math.min(perDoctor, slotCap)
     */
    public int perDoctorCapacity(TimeSlot slot) {
        if (slot == null) {
            return 0;
        }
        int perDoctor = clinicSettingService.getPatientsPerDoctor();
        int slotCap = (slot.getMaxPatients() != null && slot.getMaxPatients() > 0)
                ? slot.getMaxPatients()
                : clinicSettingService.getSlotCapacity();
        return Math.min(perDoctor, slotCap);
    }
}
