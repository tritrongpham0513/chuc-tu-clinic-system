package com.chuctupk.controller.compat;

import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.repository.AppointmentRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Deprecated
@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController {

    private final AppointmentRepository appointmentRepository;

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<QueueTodayResponse>> getQueueToday() {
        LocalDate today = LocalDate.now();
        List<Appointment> allToday = appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(today);

        // Đang trong phòng khám
        List<QueueItem> inProgress = allToday.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.IN_PROGRESS)
                .map(this::toQueueItem)
                .toList();

        // Đang chờ tại phòng chờ (Đã tiếp nhận check-in)
        List<QueueItem> waiting = allToday.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                .map(this::toQueueItem)
                .toList();

        // Đã hoàn thành hôm nay
        long completedCount = allToday.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .count();

        QueueTodayResponse response = QueueTodayResponse.builder()
                .date(today)
                .currentTime(LocalTime.now())
                .inProgressList(inProgress)
                .waitingList(waiting)
                .totalBookedToday(allToday.size())
                .completedTodayCount(completedCount)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách hàng đợi thành công"));
    }

    private QueueItem toQueueItem(Appointment a) {
        String patientName = a.getPatient().getFullName();
        // Mask tên cho lịch sự và bảo mật y tế: "Nguyễn V. A" hoặc "Trần T. M"
        String[] parts = patientName.split("\\s+");
        String displayName = patientName;
        if (parts.length > 2) {
            displayName = parts[0] + " " + parts[1].substring(0, 1) + ". " + parts[parts.length - 1];
        }

        return QueueItem.builder()
                .appointmentId(a.getId())
                .bookingCode(a.getBookingCode())
                .patientName(patientName)
                .displayName(displayName)
                .patientCode(a.getPatient().getPatientCode())
                .timeSlotLabel(a.getTimeSlot().getSlotLabel())
                .doctorName(a.getDoctor() != null ? a.getDoctor().getFullName() : "Bác sĩ trực")
                .serviceName(a.getService() != null ? a.getService().getName() : "Khám Y học cổ truyền")
                .status(a.getStatus())
                .build();
    }

    @Data
    @Builder
    public static class QueueTodayResponse {
        private LocalDate date;
        private LocalTime currentTime;
        private List<QueueItem> inProgressList;
        private List<QueueItem> waitingList;
        private int totalBookedToday;
        private long completedTodayCount;
    }

    @Data
    @Builder
    public static class QueueItem {
        private Long appointmentId;
        private String bookingCode;
        private String patientName;
        private String displayName;
        private String patientCode;
        private String timeSlotLabel;
        private String doctorName;
        private String serviceName;
        private AppointmentStatus status;
    }
}


