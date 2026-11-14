package com.chuctupk.repository;

import com.chuctupk.entity.OtpVerification;
import com.chuctupk.entity.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByPhoneAndOtpTypeOrderByCreatedAtDesc(
            String phone, OtpType otpType);

    Optional<OtpVerification> findTopByPhoneAndOtpTypeAndIsVerifiedFalseOrderByCreatedAtDesc(
            String phone, OtpType otpType);

    Optional<OtpVerification> findTopByPhoneOrderByCreatedAtDesc(String phone);

    long countByPhoneAndCreatedAtAfter(String phone, LocalDateTime since);

    Optional<OtpVerification> findTopByPhoneAndOtpTypeAndIsVerifiedTrueAndExpiresAtAfterOrderByCreatedAtDesc(
            String phone, OtpType otpType, LocalDateTime now);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM OtpVerification o WHERE o.expiresAt < :now OR (o.isVerified = true AND o.createdAt < :cutoff)")
    void deleteAllExpiredOrVerified(@org.springframework.data.repository.query.Param("now") LocalDateTime now,
                                    @org.springframework.data.repository.query.Param("cutoff") LocalDateTime cutoff);
}
