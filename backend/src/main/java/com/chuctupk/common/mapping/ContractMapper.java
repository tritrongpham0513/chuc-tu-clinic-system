package com.chuctupk.common.mapping;

import com.chuctupk.entity.enums.AppointmentSource;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.entity.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Anti-Corruption Layer: Nơi duy nhất thực hiện ánh xạ giữa Enums / Quy ước trong DB
 * và Enums / Quy ước của Hợp đồng giao tiếp Frontend (Contract API v1).
 */
public final class ContractMapper {

    private ContractMapper() {}

    private static final DateTimeFormatter ISO_DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // ================== 1. VAI TRÒ (ROLE) ==================
    // DB: ADMIN, DOCTOR, STAFF, PATIENT <-> FE: ADMIN, DOCTOR, RECEPTIONIST, USER

    public static String toFeRole(Role dbRole) {
        if (dbRole == null) return "USER";
        return switch (dbRole) {
            case ADMIN -> "ADMIN";
            case DOCTOR -> "DOCTOR";
            case STAFF -> "RECEPTIONIST";
            case PATIENT -> "USER";
        };
    }

    public static Role toDbRole(String feRole) {
        if (feRole == null || feRole.isBlank()) return Role.PATIENT;
        String upper = feRole.trim().toUpperCase();
        return switch (upper) {
            case "ADMIN" -> Role.ADMIN;
            case "DOCTOR" -> Role.DOCTOR;
            case "RECEPTIONIST", "STAFF" -> Role.STAFF;
            default -> Role.PATIENT;
        };
    }

    // ================== 2. GIỚI TÍNH (GENDER) ==================
    // DB: MALE, FEMALE, OTHER <-> FE: NAM, NU, KHAC

    public static String toFeGender(Gender dbGender) {
        if (dbGender == null) return "NAM";
        return switch (dbGender) {
            case FEMALE -> "NU";
            case OTHER -> "KHAC";
            case MALE -> "NAM";
        };
    }

    public static Gender toDbGender(String feGender) {
        if (feGender == null || feGender.isBlank()) return Gender.MALE;
        String upper = feGender.trim().toUpperCase();
        return switch (upper) {
            case "NU", "FEMALE" -> Gender.FEMALE;
            case "KHAC", "OTHER" -> Gender.OTHER;
            default -> Gender.MALE;
        };
    }

    // ================== 3. TRẠNG THÁI PHIẾU KHÁM (APPOINTMENT STATUS) ==================
    // DB: PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW
    // FE: BOOKED, CHECKED_IN, IN_PROGRESS, COMPLETED, CANCELLED

    public static String toFeApptStatus(AppointmentStatus dbStatus) {
        if (dbStatus == null) return "BOOKED";
        return switch (dbStatus) {
            case PENDING -> "BOOKED";
            case CONFIRMED -> "CHECKED_IN";
            case IN_PROGRESS -> "IN_PROGRESS";
            case COMPLETED -> "COMPLETED";
            case CANCELLED, NO_SHOW -> "CANCELLED";
        };
    }

    public static AppointmentStatus toDbApptStatus(String feStatus) {
        if (feStatus == null || feStatus.isBlank()) return AppointmentStatus.PENDING;
        String upper = feStatus.trim().toUpperCase();
        return switch (upper) {
            case "CHECKED_IN" -> AppointmentStatus.CONFIRMED;
            case "IN_PROGRESS" -> AppointmentStatus.IN_PROGRESS;
            case "COMPLETED" -> AppointmentStatus.COMPLETED;
            case "CANCELLED" -> AppointmentStatus.CANCELLED;
            default -> AppointmentStatus.PENDING; // BOOKED
        };
    }

    // ================== 4. NGUỒN ĐẶT (SOURCE) ==================
    // DB: WEB, PHONE, WALK_IN, ADMIN <-> FE: ONLINE, COUNTER

    public static String toFeSource(AppointmentSource dbSource) {
        if (dbSource == null) return "ONLINE";
        return dbSource == AppointmentSource.WEB ? "ONLINE" : "COUNTER";
    }

    public static AppointmentSource toDbSource(String feSource) {
        if (feSource == null || feSource.isBlank()) return AppointmentSource.WEB;
        String upper = feSource.trim().toUpperCase();
        return "COUNTER".equalsIgnoreCase(upper) ? AppointmentSource.WALK_IN : AppointmentSource.WEB;
    }

    // ================== 5. ĐỊNH DẠNG NGÀY GIỜ & MASK ==================

    public static String formatIso(LocalDateTime dt) {
        return dt != null ? dt.format(ISO_DT_FORMATTER) : null;
    }

    public static String formatDate(LocalDate date) {
        return date != null ? date.toString() : null;
    }

    public static String formatTime(LocalTime time) {
        return time != null ? time.format(TIME_FORMATTER) : null;
    }

    // ================== 6. LOẠI DỊCH VỤ (SERVICE TYPE) ==================

    public static String toFeServiceType(String dbServiceType) {
        if (dbServiceType == null || dbServiceType.isBlank()) return "CHAM_CUU";
        return dbServiceType.trim().toUpperCase();
    }

    public static String toDbServiceType(String feServiceType) {
        if (feServiceType == null || feServiceType.isBlank()) return "CHAM_CUU";
        return feServiceType.trim().toUpperCase();
    }

    /**
     * Che thông tin nhạy cảm (PII) khi ghi log: chỉ giữ 4 số cuối số điện thoại
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return "";
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() <= 4) return digits;
        return "*".repeat(digits.length() - 4) + digits.substring(digits.length() - 4);
    }
}

