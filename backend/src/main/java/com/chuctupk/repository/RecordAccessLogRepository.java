package com.chuctupk.repository;

import com.chuctupk.entity.RecordAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordAccessLogRepository extends JpaRepository<RecordAccessLog, Long> {

    List<RecordAccessLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<RecordAccessLog> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    boolean existsByUserIdAndPatientIdAndScope(Long userId, Long patientId, String scope);
}
