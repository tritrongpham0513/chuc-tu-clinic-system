package com.chuctupk.dto.response;

import java.util.List;
import java.util.Map;

public record QueueStateDto(
        String date,
        int capacity,
        int inRoomCount,
        int waitingCount,
        long completedCount,
        boolean autoInvite,
        List<QueueSeatDto> seats,
        List<QueueWaitingDto> waiting,
        Map<String, Integer> slotMap
) {}

