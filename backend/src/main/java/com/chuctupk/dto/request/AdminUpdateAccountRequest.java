package com.chuctupk.dto.request;

public record AdminUpdateAccountRequest(
        String fullName,
        String phone,
        String email,
        String expectedUpdatedAt
) {}

