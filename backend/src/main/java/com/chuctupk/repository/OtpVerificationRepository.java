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

    Optional<OtpVerification> findTopByPhoneAndOtpTypeAndIsVerifiedTrueAndExpiresAtAfterOrderByCreatedAtDesc(
            String phone, OtpType otpType, LocalDateTime now);
}
