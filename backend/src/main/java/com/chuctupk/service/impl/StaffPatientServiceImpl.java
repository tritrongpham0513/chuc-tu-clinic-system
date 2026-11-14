package com.chuctupk.service.impl;

import com.chuctupk.common.mapping.ContractMapper;
import com.chuctupk.common.PhoneValidator;
import com.chuctupk.dto.request.StaffCreatePatientRequest;
import com.chuctupk.dto.request.StaffUpdatePatientRequest;
import com.chuctupk.dto.response.*;
import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.MedicalRecord;
import com.chuctupk.entity.Patient;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.exception.AppException;
import com.chuctupk.exception.ErrorCode;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.MedicalRecordRepository;
import com.chuctupk.repository.PatientRepository;
import com.chuctupk.repository.UserRepository;
import com.chuctupk.service.SequenceGeneratorService;
import com.chuctupk.service.StaffPatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffPatientServiceImpl implements StaffPatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final UserRepository userRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public StaffPatientListResponse getPatients(String q, Boolean locked, int page, int limit) {
        int pageSize = limit > 0 ? Math.min(limit, 100) : 10;
        List<Patient> all = patientRepository.findAll();

        List<Patient> filtered = all.stream()
                .filter(p -> {
                    if (locked == null) return true;
                    boolean isLocked = Boolean.FALSE.equals(p.getIsActive()) || p.getLockedAt() != null;
                    return locked ? isLocked : !isLocked;
                })
                .filter(p -> {
                    if (q == null || q.isBlank()) return true;
                    String query = q.trim().toLowerCase();
                    return (p.getFullName() != null && p.getFullName().toLowerCase().contains(query))
                            || (p.getPhone() != null && p.getPhone().contains(query))
                            || (p.getPatientCode() != null && p.getPatientCode().toLowerCase().contains(query));
                })
                .sorted(Comparator.comparing(Patient::getId).reversed())
                .toList();

        int total = filtered.size();
        int pages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        int curPage = Math.max(1, Math.min(page, pages));
        int fromIndex = (curPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<Patient> pageItems = fromIndex < total ? filtered.subList(fromIndex, toIndex) : List.of();

        // Batch query lastVisitDate
        Map<Long, LocalDate> lastVisitMap = new HashMap<>();
        if (!pageItems.isEmpty()) {
            List<Long> patientIds = pageItems.stream().map(Patient::getId).toList();
            List<Object[]> rawDates = appointmentRepository.findLatestCompletedVisitDates(patientIds);
            for (Object[] row : rawDates) {
                if (row != null && row.length >= 2 && row[0] instanceof Long pid && row[1] instanceof LocalDate date) {
                    lastVisitMap.put(pid, date);
                }
            }
        }

        List<PatientDto> items = pageItems.stream()
                .map(p -> PatientDto.from(p, lastVisitMap.get(p.getId())))
                .toList();

        return new StaffPatientListResponse(items, total, pages, curPage, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDuplicatePatients(String phone, Long excludeId) {
        if (phone == null || phone.isBlank()) {
            return Map.of("items", List.of());
        }

        String clean = PhoneValidator.validate(phone);
        List<Patient> dupes = patientRepository.findByPhone(clean).stream()
                .filter(p -> excludeId == null || !p.getId().equals(excludeId))
                .toList();

        List<PatientDto> items = dupes.stream().map(PatientDto::from).toList();
        return Map.of("items", items);
    }

    @Override
    @Transactional
    public Map<String, Object> createPatient(StaffCreatePatientRequest request) {
        if (request.fullName() == null || request.fullName().trim().length() < 2) {
            throw AppException.badRequest("Vui lòng nhập họ và tên bệnh nhân");
        }
        if (request.birthYear() == null) {
            throw AppException.badRequest("Vui lòng nhập năm sinh bệnh nhân");
        }

        String cleanPhone = PhoneValidator.validate(request.phone());
        List<Patient> dupes = patientRepository.findByPhone(cleanPhone);

        if (!dupes.isEmpty() && !Boolean.TRUE.equals(request.confirmDuplicate())) {
            List<PatientDto> dupDtos = dupes.stream().map(PatientDto::from).toList();
            throw new AppException(
                    ErrorCode.DUPLICATE_PHONE,
                    "Số điện thoại này đã có hồ sơ trong hệ thống",
                    Map.of("duplicates", dupDtos)
            );
        }

        String patientCode = sequenceGeneratorService.nextPatientCode();
        Gender gender = request.gender() != null ? ContractMapper.toDbGender(request.gender()) : Gender.OTHER;
        LocalDate dob = request.dob() != null && !request.dob().isBlank() ? LocalDate.parse(request.dob()) : null;

        Patient patient = Patient.builder()
                .patientCode(patientCode)
                .fullName(request.fullName().trim())
                .phone(cleanPhone)
                .yearOfBirth(request.birthYear())
                .dob(dob)
                .gender(gender)
                .relationship("BAN_THAN")
                .address(request.address() != null ? request.address().trim() : "")
                .medicalHistory(request.medicalHistory() != null ? request.medicalHistory() : "")
                .isPrimary(false)
                .isActive(true)
                .infoUpdatedAt(LocalDateTime.now(clock))
                .build();

        Patient saved = patientRepository.save(patient);
        return Map.of("patient", PatientDto.from(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public StaffPatientDetailResponse getPatientDetail(Long id) {
        Patient p = patientRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy hồ sơ người bệnh"));

        List<Appointment> appts = appointmentRepository.findByPatientId(id).stream()
                .sorted(Comparator.comparing(Appointment::getAppointmentDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Appointment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<AppointmentDto> apptDtos = appts.stream()
                .map(AppointmentDto::from)
                .toList();

        List<MedicalRecord> records = medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(id);
        List<MedicalRecordPatientDto> recordDtos = records.stream()
                .map(r -> MedicalRecordPatientDto.from(r, true, null, clock))
                .toList();

        return new StaffPatientDetailResponse(PatientDto.from(p), apptDtos, recordDtos);
    }

    @Override
    @Transactional
    public Map<String, Object> updatePatient(Long id, StaffUpdatePatientRequest request) {
        Patient p = patientRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy hồ sơ người bệnh"));

        // Khóa lạc quan (STALE check)
        if (request.expectedUpdatedAt() != null && !request.expectedUpdatedAt().isBlank()) {
            String currentVersion = p.getInfoUpdatedAt() != null
                    ? ContractMapper.formatIso(p.getInfoUpdatedAt())
                    : (p.getUpdatedAt() != null ? ContractMapper.formatIso(p.getUpdatedAt()) : "");

            if (!currentVersion.equals(request.expectedUpdatedAt())) {
                throw new AppException(ErrorCode.STALE, "Dữ liệu đã được người khác cập nhật trước đó. Vui lòng tải lại trang");
            }
        }

        if (request.fullName() != null && !request.fullName().isBlank()) {
            p.setFullName(request.fullName().trim());
            if (Boolean.TRUE.equals(p.getIsPrimary()) && p.getUser() != null) {
                p.getUser().setFullName(request.fullName().trim());
                userRepository.save(p.getUser());
            }
        }

        if (request.phone() != null && !request.phone().isBlank()) {
            // Không cho phép sửa SĐT nếu hồ sơ là BAN_THAN (số tài khoản)
            if (!"BAN_THAN".equalsIgnoreCase(p.getRelationship())) {
                p.setPhone(PhoneValidator.validate(request.phone()));
            }
        }

        if (request.birthYear() != null) {
            p.setYearOfBirth(request.birthYear());
        }

        if (request.dob() != null && !request.dob().isBlank()) {
            p.setDob(LocalDate.parse(request.dob()));
        }

        if (request.gender() != null && !request.gender().isBlank()) {
            p.setGender(ContractMapper.toDbGender(request.gender()));
        }

        if (request.address() != null) {
            p.setAddress(request.address().trim());
        }

        if (request.medicalHistory() != null) {
            p.setMedicalHistory(request.medicalHistory());
        }

        p.setInfoUpdatedAt(LocalDateTime.now(clock));
        Patient saved = patientRepository.save(p);

        return Map.of("patient", PatientDto.from(saved));
    }
}
