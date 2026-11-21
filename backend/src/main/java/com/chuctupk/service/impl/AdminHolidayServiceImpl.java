package com.chuctupk.service.impl;

import com.chuctupk.dto.request.CreateHolidayRequest;
import com.chuctupk.dto.request.UpdateHolidayRequest;
import com.chuctupk.dto.response.AdminHolidayResponse;
import com.chuctupk.entity.ClinicHoliday;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.ClinicHolidayRepository;
import com.chuctupk.service.AdminHolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminHolidayServiceImpl implements AdminHolidayService {

    private final ClinicHolidayRepository holidayRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminHolidayResponse> getHolidays(Integer year) {
        List<ClinicHoliday> list;
        if (year != null) {
            LocalDate start = LocalDate.of(year, 1, 1);
            LocalDate end = LocalDate.of(year, 12, 31);
            list = holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(start, end);
        } else {
            list = holidayRepository.findAllByOrderByHolidayDateAsc();
        }

        return list.stream().map(h -> {
            long count = appointmentRepository.countByAppointmentDateAndStatusNot(h.getHolidayDate(), AppointmentStatus.CANCELLED);
            return AdminHolidayResponse.from(h, count);
        }).toList();
    }

    @Override
    public AdminHolidayResponse createHoliday(CreateHolidayRequest request) {
        if (request.getHolidayDate() == null) {
            throw AppException.unprocessable("Ngày nghỉ không được để trống");
        }
        if (request.getName() == null || request.getName().trim().isBlank()) {
            throw AppException.unprocessable("Tên ngày nghỉ không được để trống");
        }

        if (holidayRepository.findByHolidayDate(request.getHolidayDate()).isPresent()) {
            throw AppException.unprocessable("Ngày này đã được khai báo");
        }

        ClinicHoliday holiday = ClinicHoliday.builder()
                .holidayDate(request.getHolidayDate())
                .name(request.getName().trim())
                .isClosed(request.getIsClosed() != null ? request.getIsClosed() : true)
                .build();

        ClinicHoliday saved = holidayRepository.save(holiday);
        long count = appointmentRepository.countByAppointmentDateAndStatusNot(saved.getHolidayDate(), AppointmentStatus.CANCELLED);
        return AdminHolidayResponse.from(saved, count);
    }

    @Override
    public AdminHolidayResponse updateHoliday(Long id, UpdateHolidayRequest request) {
        ClinicHoliday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy ngày nghỉ"));

        if (request.getName() != null) {
            if (request.getName().trim().isBlank()) {
                throw AppException.unprocessable("Tên ngày nghỉ không được để trống");
            }
            holiday.setName(request.getName().trim());
        }

        if (request.getIsClosed() != null) {
            holiday.setIsClosed(request.getIsClosed());
        }

        ClinicHoliday saved = holidayRepository.save(holiday);
        long count = appointmentRepository.countByAppointmentDateAndStatusNot(saved.getHolidayDate(), AppointmentStatus.CANCELLED);
        return AdminHolidayResponse.from(saved, count);
    }

    @Override
    public void deleteHoliday(Long id) {
        ClinicHoliday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy ngày nghỉ"));
        holidayRepository.delete(holiday);
    }

    @Override
    @Transactional(readOnly = true)
    public long getAppointmentCount(LocalDate date) {
        if (date == null) return 0;
        return appointmentRepository.countByAppointmentDateAndStatusNot(date, AppointmentStatus.CANCELLED);
    }
}
