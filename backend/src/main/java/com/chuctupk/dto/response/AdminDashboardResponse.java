package com.chuctupk.dto.response;

import java.util.List;
import java.util.Map;

public record AdminDashboardResponse(
        String date,
        AdminDashboardAppointments appointments,
        AdminDashboardPatients patients,
        AdminDashboardPosts posts,
        Map<String, Integer> accounts,
        List<PostDto> drafts,
        QueueStateDto queue
) {
    public record AdminDashboardAppointments(
            int total,
            int active,
            Map<String, Integer> byStatus
    ) {}

    public record AdminDashboardPatients(
            long total
    ) {}

    public record AdminDashboardPosts(
            int published,
            int draft,
            int hidden
    ) {}
}

