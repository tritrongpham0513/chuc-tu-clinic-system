package com.chuctupk.repository;

import com.chuctupk.entity.DoctorSchedule;
import com.chuctupk.entity.enums.DayOfWeek;
import com.chuctupk.entity.enums.Period;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {
    List<DoctorSchedule> findByDoctorIdAndIsActiveTrue(Long doctorId);
    List<DoctorSchedule> findByDoctorIdAndDayOfWeekAndIsActiveTrue(Long doctorId, DayOfWeek dayOfWeek);
    List<DoctorSchedule> findByDayOfWeekAndIsActiveTrue(DayOfWeek dayOfWeek);

    @Query("SELECT ds.doctor.id FROM DoctorSchedule ds " +
           "WHERE ds.dayOfWeek = :dayOfWeek " +
           "AND (ds.period = :period OR ds.period = com.chuctupk.entity.enums.Period.FULL_DAY) " +
           "AND ds.isActive = true AND ds.doctor.isActive = true")
    List<Long> findScheduledDoctorIds(@Param("dayOfWeek") DayOfWeek dayOfWeek, @Param("period") Period period);

    @Query("SELECT COUNT(ds) > 0 FROM DoctorSchedule ds " +
           "WHERE ds.doctor.id = :doctorId " +
           "AND ds.dayOfWeek = :dayOfWeek " +
           "AND (ds.period = :period OR ds.period = com.chuctupk.entity.enums.Period.FULL_DAY) " +
           "AND ds.isActive = true AND ds.doctor.isActive = true")
    boolean isDoctorScheduled(@Param("doctorId") Long doctorId, @Param("dayOfWeek") DayOfWeek dayOfWeek, @Param("period") Period period);
}
