package com.chuctupk.repository;

import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.enums.Period;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByIsActiveTrueOrderBySortOrderAsc();
    List<TimeSlot> findByPeriodAndIsActiveTrueOrderBySortOrderAsc(Period period);
}
