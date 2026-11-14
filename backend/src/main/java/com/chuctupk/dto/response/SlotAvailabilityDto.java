package com.chuctupk.dto.response;

public record SlotAvailabilityDto(
        String id,
        String start,
        String end,
        int booked,
        int capacity,
        int remaining,
        boolean available,
        String reason
) {}

