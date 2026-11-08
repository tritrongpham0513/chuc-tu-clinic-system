package com.chuctupk.controller;

import com.chuctupk.dto.request.CreateMedicalRecordRequest;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.AppointmentDetailResponse;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.*;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/doctor")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
public class DoctorWorkspaceController {

    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * Hàng đợi ca khám của Bác sĩ trong ngày
     */
    @GetMapping("/my-queue")
    public ResponseEntity<ApiResponse<List<AppointmentDetailResponse>>> getMyQueue(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        List<Appointment> list = appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(queryDate);

        // Lọc các ca đang chờ ở sảnh (CONFIRMED) hoặc đang trong phòng khám (IN_PROGRESS)
        list = list.stream().filter(a ->
                a.getStatus() == AppointmentStatus.CONFIRMED ||
                a.getStatus() == AppointmentStatus.IN_PROGRESS
        ).toList();

        Long targetDoctorId = doctorId;
        if (targetDoctorId == null) {
            User current = getCurrentUser();
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

        // Ưu tiên ca IN_PROGRESS lên đầu, tiếp theo là CONFIRMED theo giờ khám
        list = list.stream().sorted((a, b) -> {
            if (a.getStatus() == AppointmentStatus.IN_PROGRESS && b.getStatus() != AppointmentStatus.IN_PROGRESS) return -1;
            if (a.getStatus() != AppointmentStatus.IN_PROGRESS && b.getStatus() == AppointmentStatus.IN_PROGRESS) return 1;
            return a.getTimeSlot().getStartTime().compareTo(b.getTimeSlot().getStartTime());
        }).toList();

        List<AppointmentDetailResponse> res = list.stream().map(this::toDetailResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(res, "Lấy hàng đợi bác sĩ thành công"));
    }

    /**
     * Bác sĩ mời bệnh nhân vào buồng khám (Call-in)
     */
    @PatchMapping("/appointments/{id}/call-in")
    @Transactional
    public ResponseEntity<ApiResponse<AppointmentDetailResponse>> callIn(@PathVariable Long id) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy ca khám"));

        a.setStatus(AppointmentStatus.IN_PROGRESS);
        if (a.getDoctor() == null) {
            User current = getCurrentUser();
            if (current != null) {
                doctorRepository.findByUserId(current.getId()).ifPresent(a::setDoctor);
            }
        }
        a = appointmentRepository.save(a);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(a), "Mời bệnh nhân vào phòng khám"));
    }

    /**
     * F.10.3 - Hoàn tác vào khám (từ IN_PROGRESS chuyển về lại CONFIRMED)
     */
    @PatchMapping("/appointments/{id}/undo-call-in")
    @Transactional
    public ResponseEntity<ApiResponse<AppointmentDetailResponse>> undoCallIn(@PathVariable Long id) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy ca khám"));

        if (a.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw AppException.badRequest("Chỉ có thể hoàn tác khi ca khám đang ở trạng thái 'Đang khám (IN_PROGRESS)'.");
        }

        a.setStatus(AppointmentStatus.CONFIRMED);
        a = appointmentRepository.save(a);
        return ResponseEntity.ok(ApiResponse.success(toDetailResponse(a), "Hoàn tác mời vào khám thành công. Bệnh nhân trở về danh sách chờ."));
    }

    /**
     * Bác sĩ lưu Bệnh án YHCT, Kê đơn thuốc thảo mộc và Hoàn tất ca khám
     */
    @PostMapping("/medical-records")
    @Transactional
    public ResponseEntity<ApiResponse<DoctorMedicalRecordResponse>> createMedicalRecord(
            @Valid @RequestBody CreateMedicalRecordRequest request
    ) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy ca khám"));

        // Lấy hoặc gán bác sĩ
        Doctor doctor = appointment.getDoctor();
        if (doctor == null) {
            User current = getCurrentUser();
            if (current != null) {
                doctor = doctorRepository.findByUserId(current.getId()).orElse(null);
            }
            if (doctor == null) {
                doctor = doctorRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> AppException.unprocessable("Phòng khám chưa có thông tin bác sĩ"));
            }
            appointment.setDoctor(doctor);
        }

        // Tạo hoặc cập nhật medical record
        MedicalRecord record = medicalRecordRepository.findByAppointmentId(appointment.getId())
                .orElseGet(() -> MedicalRecord.builder()
                        .appointment(appointment)
                        .patient(appointment.getPatient())
                        .doctor(appointment.getDoctor())
                        .build());

        record.setDoctor(appointment.getDoctor());

        // Ghép sinh hiệu & Tứ chẩn
        String pulseBp = request.getPulseBp();
        if (pulseBp == null || pulseBp.isBlank()) {
            StringBuilder sb = new StringBuilder();
            if (request.getBloodPressure() != null && !request.getBloodPressure().isBlank()) {
                sb.append("HA: ").append(request.getBloodPressure()).append(" mmHg; ");
            }
            if (request.getPulseRate() != null) {
                sb.append("Mạch: ").append(request.getPulseRate()).append(" l/p; ");
            }
            if (request.getPulseDiagnosis() != null && !request.getPulseDiagnosis().isBlank()) {
                sb.append(request.getPulseDiagnosis());
            }
            pulseBp = sb.toString().trim();
        }
        record.setPulseBp(pulseBp.isEmpty() ? "Bình thường" : (pulseBp.length() > 50 ? pulseBp.substring(0, 50) : pulseBp));

        // Ghép chẩn đoán
        String diag = request.getDiagnosis();
        if (diag == null || diag.isBlank()) {
            diag = request.getTraditionalDiagnosis();
        } else if (request.getTraditionalDiagnosis() != null && !request.getTraditionalDiagnosis().isBlank()) {
            diag = diag + " | YHCT: " + request.getTraditionalDiagnosis();
        }
        if (request.getTongueDiagnosis() != null && !request.getTongueDiagnosis().isBlank()) {
            diag = (diag != null ? diag + "\n" : "") + "Thiệt chẩn: " + request.getTongueDiagnosis();
        }
        record.setDiagnosis(diag != null && !diag.isBlank() ? diag : "Khám Y học cổ truyền");

        // Ghép phương pháp điều trị
        String treat = request.getTreatment();
        if (treat == null || treat.isBlank()) {
            treat = request.getAcupointTherapy();
        } else if (request.getAcupointTherapy() != null && !request.getAcupointTherapy().isBlank()) {
            treat = treat + "\nChâm cứu/Bấm huyệt: " + request.getAcupointTherapy();
        }
        record.setTreatment(treat != null && !treat.isBlank() ? treat : "Dùng thuốc thảo mộc");

        // Ghép đơn thuốc thảo mộc
        String presc = request.getPrescription();
        if (presc == null || presc.isBlank()) {
            presc = request.getHerbalPrescription();
        } else if (request.getHerbalPrescription() != null && !request.getHerbalPrescription().isBlank()) {
            presc = presc + "\n" + request.getHerbalPrescription();
        }
        record.setPrescription(presc != null && !presc.isBlank() ? presc : "Theo hướng dẫn");

        // Ghép dặn dò kiêng cữ
        String adv = request.getAdvice();
        if (adv == null || adv.isBlank()) {
            adv = request.getDietaryAdvice();
        } else if (request.getDietaryAdvice() != null && !request.getDietaryAdvice().isBlank()) {
            adv = adv + "\nKiêng cữ: " + request.getDietaryAdvice();
        }
        record.setAdvice(adv != null && !adv.isBlank() ? adv : "Nghỉ ngơi, ăn uống điều độ");

        // Tái khám
        LocalDate followDate = request.getFollowUpDate();
        if (followDate == null && request.getFollowUpDays() != null && request.getFollowUpDays() > 0) {
            followDate = LocalDate.now().plusDays(request.getFollowUpDays());
        }
        record.setFollowUpDate(followDate);

        record = medicalRecordRepository.save(record);

        // Chuyển trạng thái lịch khám sang COMPLETED
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        DoctorMedicalRecordResponse response = DoctorMedicalRecordResponse.builder()
                .id(record.getId())
                .appointmentId(appointment.getId())
                .bookingCode(appointment.getBookingCode())
                .patientId(appointment.getPatient().getId())
                .patientName(appointment.getPatient().getFullName())
                .doctorName(doctor.getFullName())
                .diagnosis(record.getDiagnosis())
                .prescription(record.getPrescription())
                .advice(record.getAdvice())
                .followUpDate(record.getFollowUpDate())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Lưu bệnh án & Kê đơn thuốc thành công"));
    }

    /**
     * Bác sĩ tra cứu sổ bệnh sử cũ trọn đời của bệnh nhân
     */
    @GetMapping("/medical-records/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<DoctorMedicalRecordResponse>>> getPatientHistory(
            @PathVariable Long patientId
    ) {
        List<MedicalRecord> list = medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        List<DoctorMedicalRecordResponse> res = list.stream().map(r -> DoctorMedicalRecordResponse.builder()
                .id(r.getId())
                .appointmentId(r.getAppointment().getId())
                .bookingCode(r.getAppointment().getBookingCode())
                .patientId(r.getPatient().getId())
                .patientName(r.getPatient().getFullName())
                .doctorName(r.getDoctor() != null ? r.getDoctor().getFullName() : "Bác sĩ YHCT")
                .pulseBp(r.getPulseBp())
                .diagnosis(r.getDiagnosis())
                .prescription(r.getPrescription())
                .advice(r.getAdvice())
                .followUpDate(r.getFollowUpDate())
                .appointmentDate(r.getAppointment().getAppointmentDate())
                .build()
        ).toList();

        return ResponseEntity.ok(ApiResponse.success(res, "Lấy lịch sử khám bệnh"));
    }

    /**
     * F.9.9 - Sửa phiếu kết quả khám / Bệnh án YHCT
     */
    @PutMapping("/medical-records/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<DoctorMedicalRecordResponse>> updateMedicalRecord(
            @PathVariable Long id,
            @Valid @RequestBody com.chuctupk.dto.request.UpdateMedicalRecordRequest req
    ) {
        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy phiếu kết quả khám"));

        if (req.getPulseBp() != null && !req.getPulseBp().isBlank()) {
            record.setPulseBp(req.getPulseBp().trim());
        }
        if (req.getDiagnosis() != null && !req.getDiagnosis().isBlank()) {
            record.setDiagnosis(req.getDiagnosis().trim());
        }
        if (req.getTreatment() != null && !req.getTreatment().isBlank()) {
            record.setTreatment(req.getTreatment().trim());
        }
        if (req.getPrescription() != null && !req.getPrescription().isBlank()) {
            record.setPrescription(req.getPrescription().trim());
        }
        if (req.getAdvice() != null && !req.getAdvice().isBlank()) {
            record.setAdvice(req.getAdvice().trim());
        }
        if (req.getFollowUpDate() != null) {
            record.setFollowUpDate(req.getFollowUpDate());
        }

        record = medicalRecordRepository.save(record);

        Appointment appointment = record.getAppointment();
        Doctor doctor = record.getDoctor();

        DoctorMedicalRecordResponse response = DoctorMedicalRecordResponse.builder()
                .id(record.getId())
                .appointmentId(appointment.getId())
                .bookingCode(appointment.getBookingCode())
                .patientId(appointment.getPatient().getId())
                .patientName(appointment.getPatient().getFullName())
                .doctorName(doctor != null ? doctor.getFullName() : "Bác sĩ YHCT")
                .pulseBp(record.getPulseBp())
                .diagnosis(record.getDiagnosis())
                .prescription(record.getPrescription())
                .advice(record.getAdvice())
                .followUpDate(record.getFollowUpDate())
                .appointmentDate(appointment.getAppointmentDate())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật phiếu kết quả khám thành công"));
    }

    /**
     * F.10.6 - Bật / tắt cấu hình tự động mời người kế tiếp (Auto-call next)
     */
    private static volatile boolean autoCallNextEnabled = false;

    @GetMapping("/settings/auto-call")
    public ResponseEntity<ApiResponse<Boolean>> getAutoCallSetting() {
        return ResponseEntity.ok(ApiResponse.success(autoCallNextEnabled, "Lấy trạng thái tự động gọi số"));
    }

    @PostMapping("/settings/auto-call")
    public ResponseEntity<ApiResponse<Boolean>> toggleAutoCallSetting(@RequestParam boolean enabled) {
        autoCallNextEnabled = enabled;
        String msg = autoCallNextEnabled ? "Đã bật chế độ tự động mời người kế tiếp" : "Đã tắt chế độ tự động mời người kế tiếp";
        return ResponseEntity.ok(ApiResponse.success(autoCallNextEnabled, msg));
    }

    private AppointmentDetailResponse toDetailResponse(Appointment a) {
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
                .serviceName(a.getService() != null ? a.getService().getName() : null)
                .doctorName(a.getDoctor() != null ? a.getDoctor().getFullName() : null)
                .symptoms(a.getSymptoms())
                .status(a.getStatus())
                .build();
    }

    @Data
    @Builder
    public static class DoctorMedicalRecordResponse {
        private Long id;
        private Long appointmentId;
        private String bookingCode;
        private Long patientId;
        private String patientName;
        private String doctorName;
        private String pulseBp;
        private String diagnosis;
        private String prescription;
        private String advice;
        private LocalDate followUpDate;
        private LocalDate appointmentDate;
    }
}

