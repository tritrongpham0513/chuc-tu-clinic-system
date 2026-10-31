package com.chuctupk.repository;

import com.chuctupk.entity.ClinicSetting;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClinicSettingRepository extends JpaRepository<ClinicSetting, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ClinicSetting s WHERE s.settingKey = :key")
    Optional<ClinicSetting> findBySettingKeyWithLock(@Param("key") String key);
}
