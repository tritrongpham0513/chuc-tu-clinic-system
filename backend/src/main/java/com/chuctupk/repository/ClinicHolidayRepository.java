package com.chuctupk.repository;

import com.chuctupk.entity.ClinicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClinicHolidayRepository extends JpaRepository<ClinicHoliday, Long> {
    Optional<ClinicHoliday> findByHolidayDate(LocalDate holidayDate);
    boolean existsByHolidayDateAndIsClosedTrue(LocalDate holidayDate);
    boolean existsByHolidayDate(LocalDate holidayDate);
    List<ClinicHoliday> findAllByOrderByHolidayDateAsc();
    List<ClinicHoliday> findByHolidayDateBetweenOrderByHolidayDateAsc(LocalDate start, LocalDate end);
}

