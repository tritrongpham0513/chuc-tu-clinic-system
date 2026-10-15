package com.chuctupk.repository;

import com.chuctupk.entity.DoctorSchedule;
import com.chuctupk.entity.enums.DayOfWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {
    List<DoctorSchedule> findByDoctorIdAndIsActiveTrue(Long doctorId);
    List<DoctorSchedule> findByDoctorIdAndDayOfWeekAndIsActiveTrue(Long doctorId, DayOfWeek dayOfWeek);
    List<DoctorSchedule> findByDayOfWeekAndIsActiveTrue(DayOfWeek dayOfWeek);
}
