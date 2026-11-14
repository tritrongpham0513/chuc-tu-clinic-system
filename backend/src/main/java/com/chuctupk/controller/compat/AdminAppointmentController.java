package com.chuctupk.controller.compat;

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

@Deprecated
@RestController
@RequestMapping("/api/v1/admin/appointments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR')")
public class AdminAppointmentController {

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
     * Danh sách lịch hẹn phục vụ lễ tân & điều phối
     */
    @GetMapping
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
        return ResponseEntity.ok(ApiResponse.success(res, "Lấy danh sách lịch hẹn thành công"));
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

        // 2. Khung giờ
        TimeSlot slot;
        if (request.getTimeSlotId() != null) {
            slot = timeSlotRepository.findById(request.getTimeSlotId())
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy khung giờ"));
        } else {
            slot = timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
                    .findFirst()
                    .orElseThrow(() -> AppException.notFound("Chưa có khung giờ khám trong hệ thống"));
        }

        Doctor doctor = null;
        if (request.getDoctorId() != null) {
            doctor = doctorRepository.findById(request.getDoctorId()).orElse(null);
        }

        ClinicService service = null;
        if (request.getServiceId() != null) {
            service = clinicServiceRepository.findById(request.getServiceId()).orElse(null);
        }

        // 3. Sinh mã và lưu trạng thái CONFIRMED (đã có mặt tại phòng khám)
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
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(appointment), "Tiếp nhận bệnh nhân thành công"));
    }

    /**
     * Bác sĩ hoặc lễ tân lấy danh sách hàng đợi khám trong ngày
     */
    @GetMapping("/doctor-queue")
    public ResponseEntity<ApiResponse<List<AppointmentDetailResponse>>> getDoctorQueue(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        List<Appointment> list = appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(queryDate);

        // Lọc ca đang ngồi sảnh (CONFIRMED) hoặc đang trong phòng khám (IN_PROGRESS)
        list = list.stream().filter(a ->
                a.getStatus() == AppointmentStatus.CONFIRMED ||
                a.getStatus() == AppointmentStatus.IN_PROGRESS
        ).toList();

        Long targetDoctorId = doctorId;
        if (targetDoctorId == null) {
            User current = getCurrentStaff();
            if (current != null) {
                Doctor myDoc = doctorRepository.findByUserId(current.getId()).orElse(null);
                if (myDoc != null) {
                    targetDoctorId = myDoc.getId();
                }
            }
        }

        if (targetDoctorId != null) {
            final Long finalDocId = targetDoctorId;
            list = list.stream().filter(a -> a.getDoctor() == null || a.getDoctor().getId().equals(finalDocId)).toList();
        }

        // Ưu tiên IN_PROGRESS lên đầu, tiếp theo là CONFIRMED theo giờ khám
        list = list.stream().sorted((a, b) -> {
            if (a.getStatus() == AppointmentStatus.IN_PROGRESS && b.getStatus() != AppointmentStatus.IN_PROGRESS) return -1;
            if (a.getStatus() != AppointmentStatus.IN_PROGRESS && b.getStatus() == AppointmentStatus.IN_PROGRESS) return 1;
            return a.getTimeSlot().getStartTime().compareTo(b.getTimeSlot().getStartTime());
        }).toList();

        return ResponseEntity.ok(ApiResponse.success(list.stream().map(this::toDetailResponse).toList(), "Hàng đợi khám hôm nay"));
    }

    /**
     * Điểm danh Check-in khi bệnh nhân có mặt tại quầy
     */
    @PatchMapping("/{id}/check-in")
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
     * Bác sĩ mời bệnh nhân vào buồng khám
     */
    @PatchMapping("/{id}/call-in")
    @Transactional
    public ResponseEntity<ApiResponse<AppointmentDetailResponse>> callIn(@PathVariable Long id) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy lịch hẹn"));
        a.setStatus(AppointmentStatus.IN_PROGRESS);
        if (a.getDoctor() == null) {
            User staff = getCurrentStaff();
            if (staff != null) {
                doctorRepository.findByUserId(staff.getId()).ifPresent(a::setDoctor);
            }
        }
        a = appointmentRepository.save(a);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(a), "Mời bệnh nhân vào phòng khám"));
    }

    /**
     * Cập nhật trạng thái phiếu khám (Check-in, Bắt đầu khám, Hoàn thành, Hủy)
     */
    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<ApiResponse<AppointmentDetailResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam AppointmentStatus status,
            @RequestParam(required = false) String cancelReason
    ) {
        User staff = getCurrentStaff();
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy lịch hẹn"));

        appointment.setStatus(status);
        if (status == AppointmentStatus.CONFIRMED && appointment.getConfirmedAt() == null) {
            appointment.setConfirmedBy(staff);
            appointment.setConfirmedAt(LocalDateTime.now());
        }
        if (status == AppointmentStatus.CANCELLED) {
            appointment.setCancelReason(cancelReason != null ? cancelReason : "Lễ tân hủy lịch");
        }

        appointment = appointmentRepository.save(appointment);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(appointment), "Cập nhật trạng thái thành công"));
    }

    /**
     * Chỉ định bác sĩ khám
     */
    @PatchMapping("/{id}/assign-doctor")
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

    private String generateBookingCode(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 10; i++) {
            int num = RANDOM.nextInt(9000) + 1000;
            String code = "PKCT-" + dateStr + "-" + num;
            if (!appointmentRepository.existsByBookingCode(code)) return code;
        }
        return "PKCT-" + dateStr + "-" + (System.currentTimeMillis() % 10000);
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

