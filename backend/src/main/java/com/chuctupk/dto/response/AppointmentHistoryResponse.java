package com.chuctupk.dto.response;

import java.util.List;

public record AppointmentHistoryResponse(
        List<AppointmentStatusHistoryItemDto> items
) {
}
