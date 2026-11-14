package com.chuctupk.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record QueueSeatDto(
        int seatNo,
        boolean empty,
        String id,
        String appointmentId,
        String ticketCode,
        PatientDto patient,
        String doctor,
        String enteredAt,
        String priorityGroup,
        String source,
        String slotStart,
        String checkInTime
) {}

