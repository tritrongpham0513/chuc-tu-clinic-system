package com.chuctupk.controller;

import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.ArticleRepository;
import com.chuctupk.repository.DoctorRepository;
import com.chuctupk.repository.PatientRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR')")
public class AdminDashboardController {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final ArticleRepository articleRepository;
    private final DoctorRepository doctorRepository;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats() {
        LocalDate today = LocalDate.now();
        List<Appointment> todayAppointments = appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(today);

        long pending = todayAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.PENDING).count();
        long confirmed = todayAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count();
        long inProgress = todayAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.IN_PROGRESS).count();
        long completed = todayAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
        long cancelled = todayAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();

        long totalPatients = patientRepository.count();
        long totalArticles = articleRepository.count();
        long totalDoctors = doctorRepository.count();

        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .today(today)
                .totalToday(todayAppointments.size())
                .pendingCount(pending)
                .waitingCount(confirmed)
                .inProgressCount(inProgress)
                .completedCount(completed)
                .cancelledCount(cancelled)
                .totalPatients(totalPatients)
                .totalArticles(totalArticles)
                .totalDoctors(totalDoctors)
                .build();

        return ResponseEntity.ok(ApiResponse.success(stats, "Lấy số liệu thống kê thành công"));
    }

    @Data
    @Builder
    public static class DashboardStatsResponse {
        private LocalDate today;
        private int totalToday;
        private long pendingCount;
        private long waitingCount;
        private long inProgressCount;
        private long completedCount;
        private long cancelledCount;
        private long totalPatients;
        private long totalArticles;
        private long totalDoctors;
    }
}

