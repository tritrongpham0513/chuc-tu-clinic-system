package com.chuctupk.repository;

import com.chuctupk.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByPhoneAndFullNameAndYearOfBirth(String phone, String fullName, Integer yearOfBirth);
    Optional<Patient> findByPatientCode(String patientCode);
    List<Patient> findByPhone(String phone);
    List<Patient> findByUserId(Long userId);
    long countByPatientCodeStartingWith(String prefix);
}
