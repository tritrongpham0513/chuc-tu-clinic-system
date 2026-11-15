package com.chuctupk.service;

import com.chuctupk.entity.Doctor;
import com.chuctupk.entity.TimeSlot;

import java.time.LocalDate;

public interface DoctorAssignmentService {

    /**
     * Tự động chọn bác sĩ trực phù hợp nhất cho khung giờ khám:
     * - Có lịch trực trong ca (Period của slot hoặc FULL_DAY).
     * - Không xin nghỉ phép trong ngày/ca đó.
     * - Tài khoản và hồ sơ bác sĩ đang hoạt động (isActive = true).
     * - Cân bằng tải: ưu tiên bác sĩ có số lượt đặt ít nhất trong khung giờ; tie-break theo ID nhỏ nhất.
     *
     * @param date Ngày khám
     * @param slot Khung giờ khám
     * @return Bác sĩ được chỉ định
     * @throws com.chuctupk.exception.AppException (422) nếu không có bác sĩ trực khả dụng
     */
    Doctor pickDoctor(LocalDate date, TimeSlot slot);

    /**
     * Quản trị viên hoặc nhân viên phân công / đổi bác sĩ cho phiếu khám.
     * Kiểm tra bác sĩ có lịch trực trong ca khám và không xin nghỉ phép.
     *
     * @param appointmentId ID phiếu khám
     * @param doctorId ID bác sĩ được phân công
     */
    void assignDoctor(Long appointmentId, Long doctorId);
}
