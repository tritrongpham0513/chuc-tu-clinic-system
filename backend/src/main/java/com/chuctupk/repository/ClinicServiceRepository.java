package com.chuctupk.repository;

import com.chuctupk.entity.ClinicService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClinicServiceRepository extends JpaRepository<ClinicService, Long> {
    Optional<ClinicService> findBySlug(String slug);
    List<ClinicService> findByIsActiveTrueOrderBySortOrderAsc();
}

