package com.chuctupk.controller.compat;

import com.chuctupk.dto.request.CreateMedicalRecordRequest;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.Doctor;
import com.chuctupk.entity.MedicalRecord;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.DoctorRepository;
import com.chuctupk.repository.MedicalRecordRepository;
import com.chuctupk.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Deprecated
@RestController
@RequestMapping("/api/v1/admin/medical-records")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'STAFF')")
public class AdminMedicalRecordController {

    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<MedicalRecordDetailResponse>> createMedicalRecord(
            @Valid @RequestBody CreateMedicalRecordRequest request
    ) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy lịch khám"));

        // Lấy bác sĩ
        Doctor doctor = appointment.getDoctor();
        if (doctor == null) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            doctor = doctorRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> AppException.unprocessable("Phòng khám chưa có danh sách bác sĩ"));
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
        // Ghép mạch & huyết áp
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
        record.setDiagnosis(diag != null && !diag.isBlank() ? diag : "Khám tổng quát YHCT");

        // Ghép điều trị & Châm cứu
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
        record.setPrescription(presc != null && !presc.isBlank() ? presc : "Theo chỉ dẫn");

        // Ghép lời dặn
        String adv = request.getAdvice();
        if (adv == null || adv.isBlank()) {
            adv = request.getDietaryAdvice();
        } else if (request.getDietaryAdvice() != null && !request.getDietaryAdvice().isBlank()) {
            adv = adv + "\nKiêng cữ: " + request.getDietaryAdvice();
        }
        record.setAdvice(adv != null && !adv.isBlank() ? adv : "Nghỉ ngơi, ăn uống thanh đạm");

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

        return ResponseEntity.ok(ApiResponse.success(toResponse(record), "Lưu hồ sơ bệnh án thành công"));
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<ApiResponse<MedicalRecordDetailResponse>> getByAppointment(@PathVariable Long appointmentId) {
        MedicalRecord record = medicalRecordRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> AppException.notFound("Chưa có hồ sơ bệnh án cho lượt khám này"));
        return ResponseEntity.ok(ApiResponse.success(toResponse(record), "Lấy bệnh án thành công"));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<MedicalRecordDetailResponse>>> getByPatient(@PathVariable Long patientId) {
        List<MedicalRecord> list = medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        return ResponseEntity.ok(ApiResponse.success(list.stream().map(this::toResponse).toList(), "Lấy lịch sử bệnh án"));
    }

    private MedicalRecordDetailResponse toResponse(MedicalRecord r) {
        return MedicalRecordDetailResponse.builder()
                .id(r.getId())
                .appointmentId(r.getAppointment().getId())
                .bookingCode(r.getAppointment().getBookingCode())
                .appointmentDate(r.getAppointment().getAppointmentDate())
                .patientId(r.getPatient().getId())
                .patientName(r.getPatient().getFullName())
                .patientCode(r.getPatient().getPatientCode())
                .doctorName(r.getDoctor() != null ? r.getDoctor().getFullName() : null)
                .pulseBp(r.getPulseBp())
                .diagnosis(r.getDiagnosis())
                .treatment(r.getTreatment())
                .prescription(r.getPrescription())
                .advice(r.getAdvice())
                .followUpDate(r.getFollowUpDate())
                .createdAt(r.getCreatedAt())
                .build();
    }

    @Data
    @Builder
    public static class MedicalRecordDetailResponse {
        private Long id;
        private Long appointmentId;
        private String bookingCode;
        private LocalDate appointmentDate;
        private Long patientId;
        private String patientName;
        private String patientCode;
        private String doctorName;
        private String pulseBp;
        private String diagnosis;
        private String treatment;
        private String prescription;
        private String advice;
        private LocalDate followUpDate;
        private LocalDateTime createdAt;
    }
}


