package com.chuctupk.service.impl;

import com.chuctupk.dto.request.CreateRecordRequest;
import com.chuctupk.dto.request.UpdateRecordRequest;
import com.chuctupk.dto.response.MedicalRecordPatientDto;
import com.chuctupk.dto.response.RecordDetailResponse;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.CheckinStatus;
import com.chuctupk.exception.AppException;
import com.chuctupk.exception.ErrorCode;
import com.chuctupk.repository.*;
import com.chuctupk.service.ClinicSettingService;
import com.chuctupk.service.StaffRecordService;
import com.chuctupk.service.StaffService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffRecordServiceImpl implements StaffRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordEditRepository medicalRecordEditRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final CheckinRepository checkinRepository;
    private final ClinicSettingService clinicSettingService;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Lazy
    private final StaffService staffService;

    @Override
    @Transactional
    public RecordDetailResponse createRecord(User staff, Long appointmentId, CreateRecordRequest request) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy phiếu khám"));

        if (medicalRecordRepository.findByAppointmentId(appointmentId).isPresent()) {
            throw AppException.of(ErrorCode.RECORD_EXISTS, "Phiếu khám này đã được lập kết quả khám bệnh");
        }

        if (appt.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw AppException.badRequest("Phiếu khám phải ở trạng thái đang khám mới có thể lập phiếu kết quả");
        }

        Patient patient = appt.getPatient();
        if (patient != null && (Boolean.FALSE.equals(patient.getIsActive()) || patient.getLockedAt() != null)) {
            throw AppException.of(ErrorCode.FORBIDDEN, "Hồ sơ bệnh nhân đã bị khóa, không thể lập phiếu kết quả");
        }

        Doctor doctor = appt.getDoctor();
        if (doctor == null && request.doctorName() != null && !request.doctorName().isBlank()) {
            String dName = request.doctorName().trim();
            doctor = doctorRepository.findAll().stream()
                    .filter(d -> d.getFullName() != null && d.getFullName().equalsIgnoreCase(dName))
                    .findFirst()
                    .orElse(null);
        }
        if (doctor == null) {
            doctor = doctorRepository.findAll().stream().findFirst().orElse(null);
        }

        String doctorDisplayName = request.doctorName() != null && !request.doctorName().isBlank()
                ? request.doctorName().trim()
                : (doctor != null ? doctor.getFullName() : "BS. Nguyễn Văn Minh");

        String pulse = request.pulse() != null ? request.pulse().trim() : "";
        String bp = request.bloodPressure() != null ? request.bloodPressure().trim() : "";
        String pulseBp = (!pulse.isBlank() || !bp.isBlank()) ? (pulse + "|" + bp) : "";

        String treatmentJson = "";
        String acup = null;
        String pressure = null;
        String other = null;
        if (request.treatmentNotes() != null) {
            acup = request.treatmentNotes().get("acupuncture");
            pressure = request.treatmentNotes().get("acupressure");
            other = request.treatmentNotes().get("other");
            try {
                treatmentJson = objectMapper.writeValueAsString(request.treatmentNotes());
            } catch (Exception ignored) {}
        } else if (request.treatment() != null) {
            treatmentJson = request.treatment();
        }

        LocalDate followUp = request.followUpDate() != null && !request.followUpDate().isBlank()
                ? LocalDate.parse(request.followUpDate()) : null;

        MedicalRecord record = MedicalRecord.builder()
                .appointment(appt)
                .patient(patient)
                .doctor(doctor)
                .doctorName(doctorDisplayName)
                .visitDate(appt.getAppointmentDate() != null ? appt.getAppointmentDate() : LocalDate.now(clock))
                .diagnosis(request.diagnosis() != null && !request.diagnosis().isBlank() ? request.diagnosis().trim() : "Chẩn đoán Y học cổ truyền")
                .pulse(pulse)
                .bloodPressure(bp)
                .pulseBp(pulseBp)
                .treatment(treatmentJson)
                .treatmentAcupuncture(acup)
                .treatmentAcupressure(pressure)
                .treatmentOther(other)
                .prescription(request.prescription())
                .advice(request.advice())
                .followUpDate(followUp)
                .recordedBy(staff != null ? staff.getId() : null)
                .build();

        MedicalRecord saved = medicalRecordRepository.save(record);

        if (request.medicalHistory() != null && patient != null) {
            patient.setMedicalHistory(request.medicalHistory());
            patientRepository.save(patient);
        }

        appt.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appt);

        Optional<Checkin> checkinOpt = checkinRepository.findByAppointmentId(appointmentId);
        checkinOpt.ifPresent(c -> {
            c.setStatus(CheckinStatus.COMPLETED);
            c.setCompletedAt(LocalDateTime.now(clock));
            checkinRepository.save(c);
        });

        boolean autoInvite = "1".equals(clinicSettingService.getSettingValue("auto_invite", "0"));
        if (autoInvite && staffService != null) {
            try {
                staffService.inviteNext();
            } catch (Exception ignored) {}
        }

        return new RecordDetailResponse(MedicalRecordPatientDto.from(saved, true, null, clock));
    }

    @Override
    @Transactional(readOnly = true)
    public RecordDetailResponse getRecord(Long id) {
        MedicalRecord r = medicalRecordRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy phiếu kết quả khám"));

        List<MedicalRecordEdit> edits = medicalRecordEditRepository.findByRecordIdOrderByEditedAtDesc(id);
        return new RecordDetailResponse(MedicalRecordPatientDto.from(r, true, edits, clock));
    }

    @Override
    @Transactional
    public RecordDetailResponse updateRecord(User staff, Long id, UpdateRecordRequest request) {
        MedicalRecord r = medicalRecordRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy phiếu kết quả khám"));

        LocalDateTime created = r.getCreatedAt() != null ? r.getCreatedAt() : LocalDateTime.now(clock);
        LocalDateTime expiresAt = created.plusHours(24);
        if (LocalDateTime.now(clock).isAfter(expiresAt)) {
            throw AppException.of(ErrorCode.RECORD_EXPIRED, "Đã quá thời hạn 24 giờ cho phép chỉnh sửa phiếu kết quả");
        }

        Patient patient = r.getPatient();
        if (patient != null && (Boolean.FALSE.equals(patient.getIsActive()) || patient.getLockedAt() != null)) {
            throw AppException.of(ErrorCode.FORBIDDEN, "Hồ sơ bệnh nhân đã bị khóa");
        }

        Map<String, Map<String, Object>> diff = new LinkedHashMap<>();

        if (request.diagnosis() != null && !request.diagnosis().isBlank() && !request.diagnosis().equals(r.getDiagnosis())) {
            diff.put("diagnosis", Map.of("from", r.getDiagnosis() != null ? r.getDiagnosis() : "", "to", request.diagnosis().trim()));
            r.setDiagnosis(request.diagnosis().trim());
        }

        String newPulse = request.pulse() != null ? request.pulse().trim() : "";
        if (!newPulse.equals(r.getPulse())) {
            diff.put("pulse", Map.of("from", r.getPulse() != null ? r.getPulse() : "", "to", newPulse));
            r.setPulse(newPulse);
        }

        String newBp = request.bloodPressure() != null ? request.bloodPressure().trim() : "";
        if (!newBp.equals(r.getBloodPressure())) {
            diff.put("bloodPressure", Map.of("from", r.getBloodPressure() != null ? r.getBloodPressure() : "", "to", newBp));
            r.setBloodPressure(newBp);
        }
        if (!newPulse.isBlank() || !newBp.isBlank()) {
            r.setPulseBp(newPulse + "|" + newBp);
        }

        if (request.treatmentNotes() != null) {
            String acup = request.treatmentNotes().get("acupuncture");
            String pressure = request.treatmentNotes().get("acupressure");
            String other = request.treatmentNotes().get("other");
            if (!Objects.equals(acup, r.getTreatmentAcupuncture())
                    || !Objects.equals(pressure, r.getTreatmentAcupressure())
                    || !Objects.equals(other, r.getTreatmentOther())) {
                diff.put("treatmentNotes", Map.of(
                        "from", Map.of("acupuncture", r.getTreatmentAcupuncture() != null ? r.getTreatmentAcupuncture() : "",
                                       "acupressure", r.getTreatmentAcupressure() != null ? r.getTreatmentAcupressure() : "",
                                       "other", r.getTreatmentOther() != null ? r.getTreatmentOther() : ""),
                        "to", request.treatmentNotes()
                ));
                r.setTreatmentAcupuncture(acup);
                r.setTreatmentAcupressure(pressure);
                r.setTreatmentOther(other);
                try {
                    r.setTreatment(objectMapper.writeValueAsString(request.treatmentNotes()));
                } catch (Exception ignored) {}
            }
        }

        if (request.prescription() != null && !Objects.equals(request.prescription(), r.getPrescription())) {
            diff.put("prescription", Map.of("from", r.getPrescription() != null ? r.getPrescription() : "", "to", request.prescription()));
            r.setPrescription(request.prescription());
        }

        if (request.advice() != null && !Objects.equals(request.advice(), r.getAdvice())) {
            diff.put("advice", Map.of("from", r.getAdvice() != null ? r.getAdvice() : "", "to", request.advice()));
            r.setAdvice(request.advice());
        }

        LocalDate newFollowUp = request.followUpDate() != null && !request.followUpDate().isBlank()
                ? LocalDate.parse(request.followUpDate()) : null;
        if (!Objects.equals(newFollowUp, r.getFollowUpDate())) {
            diff.put("followUpDate", Map.of("from", r.getFollowUpDate() != null ? r.getFollowUpDate().toString() : "", "to", newFollowUp != null ? newFollowUp.toString() : ""));
            r.setFollowUpDate(newFollowUp);
        }

        if (request.doctorName() != null && !request.doctorName().isBlank() && !request.doctorName().equals(r.getDoctorName())) {
            diff.put("doctorName", Map.of("from", r.getDoctorName() != null ? r.getDoctorName() : "", "to", request.doctorName().trim()));
            r.setDoctorName(request.doctorName().trim());
        }

        if (!diff.isEmpty()) {
            try {
                MedicalRecordEdit edit = MedicalRecordEdit.builder()
                        .record(r)
                        .editedBy(staff)
                        .editedAt(LocalDateTime.now(clock))
                        .changes(objectMapper.writeValueAsString(diff))
                        .build();
                medicalRecordEditRepository.save(edit);
            } catch (Exception ex) {
                log.error("Không thể ghi nhận lịch sử chỉnh sửa phiếu kết quả", ex);
            }
        }

        MedicalRecord saved = medicalRecordRepository.save(r);

        if (request.medicalHistory() != null && patient != null) {
            patient.setMedicalHistory(request.medicalHistory());
            patientRepository.save(patient);
        }

        List<MedicalRecordEdit> edits = medicalRecordEditRepository.findByRecordIdOrderByEditedAtDesc(id);
        return new RecordDetailResponse(MedicalRecordPatientDto.from(saved, true, edits, clock));
    }
}

