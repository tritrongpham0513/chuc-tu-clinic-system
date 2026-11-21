package com.chuctupk.repository;

import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.enums.Period;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findAllByOrderBySortOrderAsc();
    List<TimeSlot> findByIsActiveTrueOrderBySortOrderAsc();
    List<TimeSlot> findByPeriodAndIsActiveTrueOrderBySortOrderAsc(Period period);
    Optional<TimeSlot> findTopByOrderBySortOrderDesc();
    List<TimeSlot> findByIsActiveTrue();

    @Query(value = "SELECT * FROM time_slots WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<TimeSlot> findByIdWithLock(@Param("id") Long id);
}
