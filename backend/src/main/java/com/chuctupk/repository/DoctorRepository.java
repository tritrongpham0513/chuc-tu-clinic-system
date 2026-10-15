package com.chuctupk.repository;

import com.chuctupk.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findByIsActiveTrue();
    List<Doctor> findBySpecialty(String specialty);
    Optional<Doctor> findByUserId(Long userId);
}
