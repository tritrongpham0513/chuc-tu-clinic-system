package com.chuctupk.repository;

import com.chuctupk.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findByIsActiveTrue();
    List<Doctor> findByIsActiveTrueOrderBySortOrderAsc();
    List<Doctor> findBySpecialty(String specialty);
    Optional<Doctor> findByUserId(Long userId);

    List<Doctor> findAllByOrderBySortOrderAscFullNameAsc();

    boolean existsByUserId(Long userId);

    boolean existsByUserIdAndIdNot(Long userId, Long id);

    @Query("SELECT COALESCE(MAX(d.sortOrder), 0) FROM Doctor d")
    Integer findMaxSortOrder();
}
