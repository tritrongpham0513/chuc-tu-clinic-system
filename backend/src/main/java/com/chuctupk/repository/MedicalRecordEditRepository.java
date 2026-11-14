package com.chuctupk.repository;

import com.chuctupk.entity.MedicalRecordEdit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordEditRepository extends JpaRepository<MedicalRecordEdit, Long> {

    List<MedicalRecordEdit> findByRecordIdOrderByEditedAtDesc(Long recordId);
}

