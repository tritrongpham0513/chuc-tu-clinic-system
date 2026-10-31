package com.chuctupk.repository;

import com.chuctupk.entity.DoctorDayOff;
import com.chuctupk.entity.enums.Period;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DoctorDayOffRepository extends JpaRepository<DoctorDayOff, Long> {
    List<DoctorDayOff> findByDoctorIdAndOffDate(Long doctorId, LocalDate offDate);
    boolean existsByDoctorIdAndOffDate(Long doctorId, LocalDate offDate);

    @Query("SELECT COUNT(ddo) > 0 FROM DoctorDayOff ddo " +
           "WHERE ddo.doctor.id = :doctorId AND ddo.offDate = :offDate " +
           "AND (ddo.period = :period OR ddo.period = com.chuctupk.entity.enums.Period.FULL_DAY)")
    boolean isDoctorOff(@Param("doctorId") Long doctorId, @Param("offDate") LocalDate offDate, @Param("period") Period period);

    @Query("SELECT ddo.doctor.id FROM DoctorDayOff ddo " +
           "WHERE ddo.offDate = :offDate " +
           "AND (ddo.period = :period OR ddo.period = com.chuctupk.entity.enums.Period.FULL_DAY)")
    List<Long> findDoctorIdsOffOnDateAndPeriod(@Param("offDate") LocalDate offDate, @Param("period") Period period);
}
