package com.chuctupk.repository;

import com.chuctupk.entity.ClinicSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClinicSettingRepository extends JpaRepository<ClinicSetting, String> {

    @Query(value = "SELECT * FROM clinic_settings WHERE setting_key = :key FOR UPDATE", nativeQuery = true)
    Optional<ClinicSetting> findBySettingKeyWithLock(@Param("key") String key);
}
