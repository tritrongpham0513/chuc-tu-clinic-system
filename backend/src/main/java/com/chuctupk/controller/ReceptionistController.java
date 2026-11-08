package com.chuctupk.controller;

import com.chuctupk.dto.request.WalkInAppointmentRequest;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.AppointmentDetailResponse;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentSource;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/v1/receptionist")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
public class ReceptionistController {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final DoctorRepository doctorRepository;
    private final ClinicServiceRepository clinicServiceRepository;
    private final UserRepository userRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    private static final Random RANDOM = new SecureRandom();

    private User getCurrentStaff() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * Danh sách lịch hẹn phục vụ bàn tiếp tân
     */
    @GetMapping("/appointments")
    public ResponseEntity<ApiResponse<List<AppointmentDetailResponse>>> getAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) String search
    ) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        List<Appointment> list = appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(queryDate);

        if (status != null) {
            list = list.stream().filter(a -> a.getStatus() == status).toList();
        }

        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            list = list.stream().filter(a ->
                    a.getBookingCode().toLowerCase().contains(q) ||
                    a.getPatient().getFullName().toLowerCase().contains(q) ||
                    a.getPatient().getPhone().contains(q)
            ).toList();
        }

        List<AppointmentDetailResponse> res = list.stream().map(this::toDetailResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(res, "Lấy danh sách tiếp nhận thành công"));
    }

    /**
     * Tiếp nhận bệnh nhân vãng lai tại quầy (Walk-in)
     */
    @PostMapping("/walk-in")
    @Transactional
    public ResponseEntity<ApiResponse<AppointmentDetailResponse>> createWalkInAppointment(
            @Valid @RequestBody WalkInAppointmentRequest request
    ) {
        User staff = getCurrentStaff();
        LocalDate today = LocalDate.now();

        // 1. Tìm hoặc tạo bệnh nhân
        String phone = request.getPhone().trim();
        String name = request.getFullName().trim();
        Patient patient = patientRepository.findByPhoneAndFullNameAndYearOfBirth(phone, name, request.getYearOfBirth())
                .orElseGet(() -> {
                    int year = today.getYear();
                    long count = patientRepository.countByPatientCodeStartingWith("BN-" + year + "-");
                    String code = String.format("BN-%d-%04d", year, count + 1);
                    Patient newP = Patient.builder()
                            .patientCode(code)
                            .fullName(name)
                            .phone(phone)
                            .yearOfBirth(request.getYearOfBirth())
                            .gender(request.getGender())
                            .address(request.getAddress())
                            .relationship("Bản thân")
                            .isPrimary(true)
                            .isActive(true)
                            .build();
                    return patientRepository.save(newP);
                });

        // 2. Khung giờ khám (nếu không chọn thì lấy khung giờ đầu tiên đang hoạt động)
        TimeSlot slot;
        if (request.getTimeSlotId() != null) {
            slot = timeSlotRepository.findById(request.getTimeSlotId())
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy khung giờ"));
        } else {
            slot = timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
                    .findFirst()
                    .orElseThrow(() -> AppException.notFound("Chưa có khung giờ khám"));
        }

        Doctor doctor = null;
        if (request.getDoctorId() != null) {
            doctor = doctorRepository.findById(request.getDoctorId()).orElse(null);
        }

        ClinicService service = null;
        if (request.getServiceId() != null) {
            service = clinicServiceRepository.findById(request.getServiceId()).orElse(null);
        }

        // 3. Sinh mã phiếu và lưu trạng thái CONFIRMED (đã có mặt)
        String bookingCode = generateBookingCode(today);
        Appointment appointment = Appointment.builder()
                .bookingCode(bookingCode)
                .patient(patient)
                .doctor(doctor)
                .service(service)
                .appointmentDate(today)
                .timeSlot(slot)
                .symptoms(request.getSymptoms())
                .bookerName(patient.getFullName())
                .bookerPhone(patient.getPhone())
                .source(AppointmentSource.WALK_IN)
                .status(AppointmentStatus.CONFIRMED)
                .confirmedBy(staff)
                .confirmedAt(LocalDateTime.now())
                .build();

        appointment = appointmentRepository.save(appointment);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(appointment), "Tiếp nhận bệnh nhân tại quầy thành công"));
    }

    /**
     * Điểm danh Check-in khi bệnh nhân có mặt tại quầy
     */
    @PatchMapping("/appointments/{id}/check-in")
    @Transactional
    public ResponseEntity<ApiResponse<AppointmentDetailResponse>> checkIn(@PathVariable Long id) {
        User staff = getCurrentStaff();
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy lịch hẹn"));
        a.setStatus(AppointmentStatus.CONFIRMED);
        a.setConfirmedBy(staff);
        a.setConfirmedAt(LocalDateTime.now());
        a = appointmentRepository.save(a);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(a), "Điểm danh check-in thành công"));
    }

    /**
     * F.7.4 - Hoàn tác Check-in (chuyển trạng thái từ CONFIRMED về lại PENDING)
     */
    @PatchMapping("/appointments/{id}/undo-checkin")
    @Transactional
    public ResponseEntity<ApiResponse<AppointmentDetailResponse>> undoCheckIn(@PathVariable Long id) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy lịch hẹn"));
        if (a.getStatus() != AppointmentStatus.CONFIRMED) {
            throw AppException.badRequest("Chỉ có thể hoàn tác check-in khi ca khám đang ở trạng thái 'Đã tiếp nhận (CONFIRMED)'.");
        }
        a.setStatus(AppointmentStatus.PENDING);
        a.setConfirmedAt(null);
        a = appointmentRepository.save(a);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(a), "Hoàn tác check-in thành công. Lịch hẹn chuyển về trạng thái Chờ tiếp nhận."));
    }

    /**
     * F.7.6 - Đổi giờ khám / ngày khám cho bệnh nhân
     */
    @PatchMapping("/appointments/{id}/reschedule")
    @Transactional
    public ResponseEntity<ApiResponse<AppointmentDetailResponse>> rescheduleAppointment(
            @PathVariable Long id,
            @Valid @RequestBody com.chuctupk.dto.request.RescheduleAppointmentRequest req
    ) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy lịch hẹn"));

        if (a.getStatus() == AppointmentStatus.COMPLETED || a.getStatus() == AppointmentStatus.CANCELLED) {
            throw AppException.badRequest("Không thể đổi giờ cho ca khám đã hoàn tất hoặc đã bị hủy.");
        }

        TimeSlot newSlot = timeSlotRepository.findById(req.getNewTimeSlotId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy khung giờ khám được chọn"));

        if (!Boolean.TRUE.equals(newSlot.getIsActive())) {
            throw AppException.badRequest("Khung giờ khám này hiện không khả dụng.");
        }

        long countInNewSlot = appointmentRepository.countByAppointmentDateAndTimeSlotIdAndStatusNotIn(
                req.getNewDate(),
                newSlot.getId(),
                List.of(AppointmentStatus.CANCELLED)
        );

        if (countInNewSlot >= newSlot.getMaxPatients()) {
            throw AppException.badRequest("Khung giờ " + newSlot.getSlotLabel() + " vào ngày " + req.getNewDate() + " đã đủ số lượng bệnh nhân.");
        }

        a.setAppointmentDate(req.getNewDate());
        a.setTimeSlot(newSlot);
        if (req.getReason() != null && !req.getReason().isBlank()) {
            a.setSymptoms((a.getSymptoms() != null ? a.getSymptoms() + " | " : "") + "Dời lịch: " + req.getReason().trim());
        }
        a = appointmentRepository.save(a);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(a), "Đổi ngày và giờ khám thành công"));
    }

    /**
     * Chỉ định hoặc đổi bác sĩ buồng khám
     */
    @PatchMapping("/appointments/{id}/assign-doctor")
    @Transactional
    public ResponseEntity<ApiResponse<AppointmentDetailResponse>> assignDoctor(
            @PathVariable Long id,
            @RequestParam Long doctorId
    ) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy lịch hẹn"));

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy bác sĩ"));

        appointment.setDoctor(doctor);
        appointment = appointmentRepository.save(appointment);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(appointment), "Chỉ định bác sĩ thành công"));
    }

    /**
     * Tiếp tân hủy lịch theo yêu cầu bệnh nhân
     */
    @PatchMapping("/appointments/{id}/cancel")
    @Transactional
    public ResponseEntity<ApiResponse<AppointmentDetailResponse>> cancelAppointment(
            @PathVariable Long id,
            @RequestParam(required = false) String cancelReason
    ) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy lịch hẹn"));

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(cancelReason != null && !cancelReason.isBlank() ? cancelReason.trim() : "Tiếp tân hủy lịch");
        appointment = appointmentRepository.save(appointment);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(appointment), "Hủy lịch hẹn thành công"));
    }

    private String generateBookingCode(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String code;
        do {
            int randomNum = 1000 + RANDOM.nextInt(9000);
            code = String.format("PK-%s-%d", dateStr, randomNum);
        } while (appointmentRepository.existsByBookingCode(code));
        return code;
    }

    private AppointmentDetailResponse toDetailResponse(Appointment a) {
        AppointmentDetailResponse.MedicalResultSummary resultSummary = null;
        if (a.getStatus() == AppointmentStatus.COMPLETED) {
            var medRecord = medicalRecordRepository.findByAppointmentId(a.getId());
            if (medRecord.isPresent()) {
                MedicalRecord r = medRecord.get();
                resultSummary = AppointmentDetailResponse.MedicalResultSummary.builder()
                        .diagnosis(r.getDiagnosis())
                        .prescription(r.getPrescription())
                        .advice(r.getAdvice())
                        .followUpDate(r.getFollowUpDate())
                        .recordedAt(r.getCreatedAt())
                        .build();
            }
        }

        String statusLabel = switch (a.getStatus()) {
            case PENDING -> "Chờ xác nhận";
            case CONFIRMED -> "Đã tiếp nhận (Chờ khám)";
            case IN_PROGRESS -> "Đang khám";
            case COMPLETED -> "Đã hoàn thành";
            case CANCELLED -> "Đã hủy";
            case NO_SHOW -> "Vắng mặt";
        };

        int queueNum = (int) (a.getId() % 100 == 0 ? 100 : a.getId() % 100);

        return AppointmentDetailResponse.builder()
                .id(a.getId())
                .bookingCode(a.getBookingCode())
                .queueNumber(queueNum)
                .appointmentDate(a.getAppointmentDate())
                .timeSlotLabel(a.getTimeSlot().getSlotLabel())
                .startTime(a.getTimeSlot().getStartTime())
                .endTime(a.getTimeSlot().getEndTime())
                .patientName(a.getPatient().getFullName())
                .patientCode(a.getPatient().getPatientCode())
                .patientPhone(a.getPatient().getPhone())
                .patientYearOfBirth(a.getPatient().getYearOfBirth())
                .relationship(a.getPatient().getRelationship())
                .source(a.getSource() != null ? a.getSource().name() : "WEB")
                .serviceName(a.getService() != null ? a.getService().getName() : null)
                .doctorName(a.getDoctor() != null ? a.getDoctor().getFullName() : null)
                .symptoms(a.getSymptoms())
                .status(a.getStatus())
                .statusLabel(statusLabel)
                .cancelReason(a.getCancelReason())
                .createdAt(a.getCreatedAt())
                .medicalResult(resultSummary)
                .build();
    }
}

