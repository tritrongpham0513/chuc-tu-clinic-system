package com.chuctupk.dto.response;

import com.chuctupk.common.mapping.ContractMapper;
import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.Checkin;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AppointmentDto(
        String id,
        String bookingCode,
        String ticketCode,
        PatientDto patient,
        String doctor,
        String serviceType,
        String slotDate,
        String slotStart,
        String slotEnd,
        String expectedTime,
        String status,
        String source,
        String cancelReason,
        String bookerName,
        String bookerPhone,
        String createdAt,
        String updatedAt,
        String checkInAt,
        String enteredAt,
        Map<String, Object> checkin,
        Boolean doctorOnDuty,
        String doctorId,
        String reminderSentAt,
        String serviceLabel
) {
    public AppointmentDto(
            String id,
            String bookingCode,
            String ticketCode,
            PatientDto patient,
            String doctor,
            String serviceType,
            String slotDate,
            String slotStart,
            String slotEnd,
            String expectedTime,
            String status,
            String source,
            String cancelReason,
            String bookerName,
            String bookerPhone,
            String createdAt,
            String updatedAt,
            String checkInAt,
            String enteredAt,
            Map<String, Object> checkin,
            Boolean doctorOnDuty,
            String doctorId,
            String reminderSentAt
    ) {
        this(
                id,
                bookingCode,
                ticketCode,
                patient,
                doctor,
                serviceType,
                slotDate,
                slotStart,
                slotEnd,
                expectedTime,
                status,
                source,
                cancelReason,
                bookerName,
                bookerPhone,
                createdAt,
                updatedAt,
                checkInAt,
                enteredAt,
                checkin,
                doctorOnDuty,
                doctorId,
                reminderSentAt,
                resolveServiceLabel(serviceType)
        );
    }

    public AppointmentDto(
            String id,
            String bookingCode,
            String ticketCode,
            PatientDto patient,
            String doctor,
            String serviceType,
            String slotDate,
            String slotStart,
            String slotEnd,
            String expectedTime,
            String status,
            String source,
            String cancelReason,
            String bookerName,
            String bookerPhone,
            String createdAt,
            String updatedAt,
            String checkInAt,
            String enteredAt,
            Map<String, Object> checkin
    ) {
        this(
                id,
                bookingCode,
                ticketCode,
                patient,
                doctor,
                serviceType,
                slotDate,
                slotStart,
                slotEnd,
                expectedTime,
                status,
                source,
                cancelReason,
                bookerName,
                bookerPhone,
                createdAt,
                updatedAt,
                checkInAt,
                enteredAt,
                checkin,
                null,
                null,
                null,
                resolveServiceLabel(serviceType)
        );
    }

    public AppointmentDto(
            String id,
            String bookingCode,
            String ticketCode,
            PatientDto patient,
            String doctor,
            String serviceType,
            String slotDate,
            String slotStart,
            String slotEnd,
            String expectedTime,
            String status,
            String source,
            String cancelReason,
            String bookerName,
            String bookerPhone,
            String createdAt,
            String updatedAt,
            String checkInAt,
            String enteredAt,
            Map<String, Object> checkin,
            Boolean doctorOnDuty,
            String doctorId
    ) {
        this(
                id,
                bookingCode,
                ticketCode,
                patient,
                doctor,
                serviceType,
                slotDate,
                slotStart,
                slotEnd,
                expectedTime,
                status,
                source,
                cancelReason,
                bookerName,
                bookerPhone,
                createdAt,
                updatedAt,
                checkInAt,
                enteredAt,
                checkin,
                doctorOnDuty,
                doctorId,
                null,
                resolveServiceLabel(serviceType)
        );
    }

    public static AppointmentDto from(Appointment a) {
        return from(a, (Checkin) null, null, null);
    }

    public static AppointmentDto from(Appointment a, Integer queueNo) {
        if (a == null) return null;
        Map<String, Object> checkinMap = queueNo != null ? Map.of("queueNo", queueNo) : null;
        return buildDto(a, checkinMap, null, null);
    }

    public static AppointmentDto from(Appointment a, Checkin checkin) {
        return from(a, checkin, null, null);
    }

    public static AppointmentDto from(Appointment a, Checkin checkin, Boolean doctorOnDuty) {
        return from(a, checkin, doctorOnDuty, null);
    }

    public static AppointmentDto from(Appointment a, Checkin checkin, Boolean doctorOnDuty, String reminderSentAt) {
        if (a == null) return null;
        Map<String, Object> checkinMap = null;
        if (checkin != null) {
            checkinMap = new LinkedHashMap<>();
            checkinMap.put("id", String.valueOf(checkin.getId()));
            checkinMap.put("queueNo", checkin.getQueueNo());
            checkinMap.put("seatNo", checkin.getSeatNo());
            checkinMap.put("priorityGroup", checkin.getPriorityGroup() != null ? checkin.getPriorityGroup().name() : "NORMAL");
            checkinMap.put("status", checkin.getStatus() != null ? checkin.getStatus().name() : "WAITING");
            checkinMap.put("checkInTime", ContractMapper.formatIso(checkin.getCheckInTime()));
            checkinMap.put("enteredAt", ContractMapper.formatIso(checkin.getEnteredAt()));
        }
        return buildDto(a, checkinMap, doctorOnDuty, reminderSentAt);
    }

    private static AppointmentDto buildDto(Appointment a, Map<String, Object> checkinMap, Boolean doctorOnDuty, String reminderSentAt) {
        String slotStart = "08:00";
        String slotEnd = "09:00";
        if (a.getSlotStartSnapshot() != null) {
            slotStart = a.getSlotStartSnapshot().toString().substring(0, 5);
        } else if (a.getTimeSlot() != null && a.getTimeSlot().getStartTime() != null) {
            slotStart = a.getTimeSlot().getStartTime().toString().substring(0, 5);
        }

        if (a.getSlotEndSnapshot() != null) {
            slotEnd = a.getSlotEndSnapshot().toString().substring(0, 5);
        } else if (a.getTimeSlot() != null && a.getTimeSlot().getEndTime() != null) {
            slotEnd = a.getTimeSlot().getEndTime().toString().substring(0, 5);
        }

        String serviceType = a.getServiceType() != null && !a.getServiceType().isBlank()
                ? a.getServiceType()
                : (a.getService() != null && a.getService().getSlug() != null ? a.getService().getSlug() : "KHAM_BENH");

        String serviceLabel = null;
        if (a.getService() != null && a.getService().getName() != null && !a.getService().getName().isBlank()) {
            serviceLabel = a.getService().getName();
        } else {
            serviceLabel = resolveServiceLabel(serviceType);
        }

        String code = a.getBookingCode() != null ? a.getBookingCode() : "";

        return new AppointmentDto(
                String.valueOf(a.getId()),
                code,
                code, // ticketCode alias for FE
                PatientDto.from(a.getPatient()),
                a.getDoctor() != null ? a.getDoctor().getFullName() : null,
                ContractMapper.toFeServiceType(serviceType),
                ContractMapper.formatDate(a.getAppointmentDate()),
                slotStart,
                slotEnd,
                ContractMapper.formatTime(a.getExpectedTime()),
                ContractMapper.toFeApptStatus(a.getStatus()),
                ContractMapper.toFeSource(a.getSource()),
                a.getCancelReason(),
                a.getBookerName(),
                a.getBookerPhone(),
                ContractMapper.formatIso(a.getCreatedAt()),
                ContractMapper.formatIso(a.getUpdatedAt()),
                ContractMapper.formatIso(a.getCheckInAt()),
                ContractMapper.formatIso(a.getEnteredAt()),
                checkinMap,
                doctorOnDuty,
                a.getDoctor() != null ? String.valueOf(a.getDoctor().getId()) : null,
                reminderSentAt,
                serviceLabel
        );
    }

    public static String resolveServiceLabel(String serviceType) {
        if (serviceType == null || serviceType.isBlank()) return "Khám Y học cổ truyền";
        String normalized = serviceType.trim().replace("-", "_").toUpperCase();
        return switch (normalized) {
            case "CHAM_CUU" -> "Châm cứu";
            case "XOA_BOP_BAM_HUYET", "BAM_HUYET_XOA_BOP" -> "Xoa bóp — bấm huyệt";
            case "BOC_THUOC" -> "Bốc thuốc — thuốc Nam, thuốc Bắc";
            case "VAT_LY_TRI_LIEU", "PHUC_HOI_CHUC_NANG" -> "Vật lý trị liệu — phục hồi chức năng";
            case "CHUA_RO" -> "Chưa rõ — nhờ bác sĩ khám và chỉ định";
            case "KHAM_BENH", "KHAM_Y_HOC_CO_TRUYEN" -> "Khám Y học cổ truyền";
            default -> serviceType;
        };
    }
}
