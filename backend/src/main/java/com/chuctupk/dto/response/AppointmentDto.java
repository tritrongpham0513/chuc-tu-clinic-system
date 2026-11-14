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
        Map<String, Object> checkin
) {
    public static AppointmentDto from(Appointment a) {
        return from(a, (Checkin) null);
    }

    public static AppointmentDto from(Appointment a, Integer queueNo) {
        if (a == null) return null;
        Map<String, Object> checkinMap = queueNo != null ? Map.of("queueNo", queueNo) : null;
        return buildDto(a, checkinMap);
    }

    public static AppointmentDto from(Appointment a, Checkin checkin) {
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
        return buildDto(a, checkinMap);
    }

    private static AppointmentDto buildDto(Appointment a, Map<String, Object> checkinMap) {
        String slotStart = "08:00";
        String slotEnd = "09:00";
        if (a.getTimeSlot() != null) {
            if (a.getTimeSlot().getStartTime() != null) {
                slotStart = a.getTimeSlot().getStartTime().toString().substring(0, 5);
            }
            if (a.getTimeSlot().getEndTime() != null) {
                slotEnd = a.getTimeSlot().getEndTime().toString().substring(0, 5);
            }
        }

        String serviceType = a.getServiceType() != null && !a.getServiceType().isBlank()
                ? a.getServiceType()
                : (a.getService() != null && a.getService().getSlug() != null ? a.getService().getSlug() : "CHAM_CUU");

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
                checkinMap
        );
    }
}
