package com.chuctupk.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueueWaitingDto(
        String id,
        String appointmentId,
        int queueNo,
        String ticketCode,
        PatientDto patient,
        String priorityGroup,
        String source,
        String slotStart,
        String checkInTime,
        long waitedMinutes
) {}

