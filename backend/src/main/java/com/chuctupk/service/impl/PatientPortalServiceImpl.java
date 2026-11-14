package com.chuctupk.service.impl;

import com.chuctupk.common.HashUtils;
import com.chuctupk.common.PhoneValidator;
import com.chuctupk.common.mapping.ContractMapper;
import com.chuctupk.dto.request.PatientBookAppointmentRequest;
import com.chuctupk.dto.request.PatientProfileUpsertRequest;
import com.chuctupk.dto.request.UpdateMeRequest;
import com.chuctupk.dto.response.*;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentSource;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.exception.AppException;
import com.chuctupk.exception.ErrorCode;
import com.chuctupk.repository.*;
import com.chuctupk.service.PatientPortalService;
import com.chuctupk.service.SequenceGeneratorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientPortalServiceImpl implements PatientPortalService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentSeatRepository appointmentSeatRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    // =========================================================================
    // 1. TÀI KHOẢN & THÔNG TIN CÁ NHÂN (/me)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMyAccount(User currentUser) {
        if (currentUser == null) {
            throw AppException.unauthorized("Vui lòng đăng nhập");
        }
        Patient primary = patientRepository.findByUserIdAndIsPrimaryTrue(currentUser.getId()).orElse(null);
        return Map.of("user", UserWithPrimaryProfileDto.of(currentUser, primary));
    }

    @Override
    @Transactional
    public Map<String, Object> updateMyAccount(User currentUser, UpdateMeRequest request) {
        if (currentUser == null) {
            throw AppException.unauthorized("Vui lòng đăng nhập");
        }

        Patient primary = patientRepository.findByUserIdAndIsPrimaryTrue(currentUser.getId()).orElse(null);

        // Optimistic locking qua expectedUpdatedAt
        if (request.getExpectedUpdatedAt() != null && !request.getExpectedUpdatedAt().isBlank()) {
            String currentVersion = primary != null && primary.getInfoUpdatedAt() != null
                    ? ContractMapper.formatIso(primary.getInfoUpdatedAt())
                    : ContractMapper.formatIso(currentUser.getUpdatedAt());
            if (currentVersion != null && !currentVersion.equals(request.getExpectedUpdatedAt())) {
                throw new AppException(ErrorCode.STALE,
                        "Dữ liệu đã được cập nhật trước đó. Cô chú vui lòng tải lại trang");
            }
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            currentUser.setFullName(request.getFullName().trim());
        }
        if (request.getEmail() != null) {
            currentUser.setEmail(request.getEmail().trim());
        }
        User savedUser = userRepository.save(currentUser);

        if (primary != null) {
            if (request.getFullName() != null && !request.getFullName().isBlank()) {
                primary.setFullName(request.getFullName().trim());
            }
            if (request.getDob() != null && !request.getDob().isBlank()) {
                LocalDate parsedDob = parseDobString(request.getDob());
                primary.setDob(parsedDob);
                if (parsedDob != null) primary.setYearOfBirth(parsedDob.getYear());
            }
            if (request.getGender() != null) {
                primary.setGender(ContractMapper.toDbGender(request.getGender()));
            }
            if (request.getAddress() != null) {
                primary.setAddress(request.getAddress().trim());
            }
            primary.setInfoUpdatedAt(LocalDateTime.now(clock));
            primary = patientRepository.save(primary);
        }

        return Map.of("user", UserWithPrimaryProfileDto.of(savedUser, primary));
    }

    // =========================================================================
    // 2. HỒ SƠ BỆNH NHÂN (/me/patients)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMyPatients(User currentUser) {
        if (currentUser == null) {
            throw AppException.unauthorized("Vui lòng đăng nhập");
        }
        List<Patient> list = patientRepository.findByUserIdAndIsActiveTrue(currentUser.getId());
        List<PatientDto> items = list.stream().map(PatientDto::from).toList();
        return Map.of("items", items, "max", 10);
    }

    @Override
    @Transactional
    public Map<String, Object> createPatient(User currentUser, PatientProfileUpsertRequest request) {
        if (currentUser == null) {
            throw AppException.unauthorized("Vui lòng đăng nhập");
        }

        long activeCount = patientRepository.countByUserIdAndIsActiveTrue(currentUser.getId());
        if (activeCount >= 10) {
            throw new AppException(ErrorCode.PROFILE_LIMIT,
                    "Tài khoản chỉ được tạo tối đa 10 hồ sơ người thân");
        }

        LocalDate dob = parseDob(request.getDob(), request.getBirthYear());
        Integer birthYear = dob != null ? dob.getYear() : (request.getBirthYear() != null ? request.getBirthYear() : 1970);
        Gender gender = ContractMapper.toDbGender(request.getGender());
        String patientCode = sequenceGeneratorService.nextPatientCode();

        String phone = request.getPhone() != null && !request.getPhone().isBlank()
                ? PhoneValidator.clean(request.getPhone())
                : currentUser.getPhone();

        Patient p = Patient.builder()
                .user(currentUser)
                .patientCode(patientCode)
                .fullName(request.getFullName().trim())
                .phone(phone)
                .dob(dob)
                .yearOfBirth(birthYear)
                .gender(gender)
                .relationship(request.getRelationship() != null && !request.getRelationship().isBlank() ? request.getRelationship() : "NGUOI_THAN")
                .address(request.getAddress() != null ? request.getAddress().trim() : "")
                .medicalHistory(request.getMedicalHistory() != null ? request.getMedicalHistory().trim() : "")
                .isPrimary(false)
                .isActive(true)
                .infoUpdatedAt(LocalDateTime.now(clock))
                .build();

        Patient saved = patientRepository.save(p);
        return Map.of("patient", PatientDto.from(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPatient(User currentUser, Long patientId) {
        if (currentUser == null) {
            throw AppException.unauthorized("Vui lòng đăng nhập");
        }
        Patient p = patientRepository.findByIdAndUserId(patientId, currentUser.getId())
                .filter(patient -> Boolean.TRUE.equals(patient.getIsActive()))
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy hồ sơ người bệnh"));
        return Map.of("patient", PatientDto.from(p));
    }

    @Override
    @Transactional
    public Map<String, Object> updatePatient(User currentUser, Long patientId, PatientProfileUpsertRequest request) {
        if (currentUser == null) {
            throw AppException.unauthorized("Vui lòng đăng nhập");
        }

        Patient p = patientRepository.findByIdAndUserId(patientId, currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy hồ sơ người bệnh"));

        if (request.getExpectedUpdatedAt() != null && !request.getExpectedUpdatedAt().isBlank()) {
            String currentVersion = p.getInfoUpdatedAt() != null
                    ? ContractMapper.formatIso(p.getInfoUpdatedAt())
                    : ContractMapper.formatIso(p.getUpdatedAt());
            if (currentVersion != null && !currentVersion.equals(request.getExpectedUpdatedAt())) {
                throw new AppException(ErrorCode.STALE,
                        "Dữ liệu đã được cập nhật trước đó. Cô chú vui lòng tải lại trang");
            }
        }

        p.setFullName(request.getFullName().trim());
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            p.setPhone(PhoneValidator.clean(request.getPhone()));
        }

        LocalDate dob = parseDob(request.getDob(), request.getBirthYear());
        if (dob != null) {
            p.setDob(dob);
            p.setYearOfBirth(dob.getYear());
        } else if (request.getBirthYear() != null) {
            p.setYearOfBirth(request.getBirthYear());
        }

        if (request.getGender() != null) {
            p.setGender(ContractMapper.toDbGender(request.getGender()));
        }
        if (request.getRelationship() != null && !Boolean.TRUE.equals(p.getIsPrimary())) {
            p.setRelationship(request.getRelationship());
        }
        if (request.getAddress() != null) {
            p.setAddress(request.getAddress().trim());
        }
        if (request.getMedicalHistory() != null) {
            p.setMedicalHistory(request.getMedicalHistory().trim());
        }
        p.setInfoUpdatedAt(LocalDateTime.now(clock));
        Patient saved = patientRepository.save(p);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("patient", PatientDto.from(saved));

        // Nếu là hồ sơ chính chủ, đồng bộ họ tên tài khoản
        if (Boolean.TRUE.equals(p.getIsPrimary())) {
            currentUser.setFullName(saved.getFullName());
            userRepository.save(currentUser);
            res.put("user", UserDto.from(currentUser));
        }

        return res;
    }

    @Override
    @Transactional
    public Map<String, Object> deletePatient(User currentUser, Long patientId) {
        if (currentUser == null) {
            throw AppException.unauthorized("Vui lòng đăng nhập");
        }

        Patient p = patientRepository.findByIdAndUserId(patientId, currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy hồ sơ người bệnh"));

        if (Boolean.TRUE.equals(p.getIsPrimary()) || "BAN_THAN".equalsIgnoreCase(p.getRelationship())) {
            throw new AppException(ErrorCode.CANNOT_DELETE_PRIMARY,
                    "Không thể xóa hồ sơ chính chủ (Bản thân)");
        }

        boolean hasAppointments = appointmentRepository.existsByPatientId(p.getId());
        if (hasAppointments) {
            // Detach khỏi tài khoản để vẫn giữ bệnh án lịch sử
            p.setIsActive(false);
            p.setUser(null);
            patientRepository.save(p);
            return Map.of("success", true, "detached", true, "message", "Đã xóa hồ sơ khỏi tài khoản (lịch sử khám vẫn được lưu giữ)");
        } else {
            p.setIsActive(false);
            patientRepository.save(p);
            return Map.of("success", true, "detached", false, "message", "Đã xóa hồ sơ thành công");
        }
    }

    // =========================================================================
    // 3. KHUNG GIỜ & ĐẶT LỊCH KHÁM BỆNH (/me/availability, /me/appointments)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAvailability(LocalDate queryDate) {
        LocalDate date = queryDate != null ? queryDate : LocalDate.now(clock);
        List<TimeSlot> slots = timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc();

        List<SlotAvailabilityDto> slotDtos = new ArrayList<>();
        LocalDate today = LocalDate.now(clock);
        LocalTime nowTime = LocalTime.now(clock);

        for (TimeSlot slot : slots) {
            long booked = appointmentSeatRepository.countByAppointmentDateAndTimeSlotId(date, slot.getId());
            int capacity = slot.getMaxPatients() != null && slot.getMaxPatients() > 0 ? slot.getMaxPatients() : 6;
            int remaining = Math.max(0, capacity - (int) booked);

            boolean isPast = date.isBefore(today) || (date.equals(today) && slot.getEndTime().isBefore(nowTime));
            boolean isFull = remaining <= 0;
            boolean available = !isPast && !isFull;

            String reason = null;
            if (isPast) reason = "PAST";
            else if (isFull) reason = "SLOT_FULL";

            String start = slot.getStartTime().toString().substring(0, 5);
            String end = slot.getEndTime().toString().substring(0, 5);

            slotDtos.add(new SlotAvailabilityDto(
                    String.valueOf(slot.getId()),
                    start,
                    end,
                    (int) booked,
                    capacity,
                    remaining,
                    available,
                    reason
            ));
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("date", date.toString());
        res.put("slots", slotDtos);
        return res;
    }

    @Override
    @Transactional
    public Map<String, Object> bookAppointment(User currentUser, PatientBookAppointmentRequest request, String idempotencyKey) {
        if (currentUser == null) {
            throw AppException.unauthorized("Vui lòng đăng nhập để đặt lịch khám");
        }

        // 1. Kiểm tra Idempotency-Key
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String keyHash = HashUtils.sha256(idempotencyKey.trim());
            Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findByKeyHashAndUserId(keyHash, currentUser.getId());
            if (existingKey.isPresent()) {
                try {
                    return objectMapper.readValue(existingKey.get().getResponseJson(), Map.class);
                } catch (Exception ex) {
                    log.warn("Không thể parse response JSON của idempotency key {}: {}", idempotencyKey, ex.getMessage());
                }
            }
        }

        // 2. Kiểm tra hồ sơ bệnh nhân
        Patient patient = patientRepository.findByIdAndUserId(request.getPatientId(), currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy hồ sơ người đi khám"));

        if (!Boolean.TRUE.equals(patient.getIsActive())) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED,
                    "Hồ sơ người bệnh này đang bị khóa hoặc ngưng hoạt động");
        }

        // 3. Kiểm tra ngày hẹn (chuẩn múi giờ Việt Nam)
        LocalDate today = LocalDate.now(clock);
        LocalTime nowTime = LocalTime.now(clock);
        LocalDate slotDate = LocalDate.parse(request.getSlotDate());
        if (slotDate.isBefore(today)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Không thể đặt lịch cho ngày trong quá khứ");
        }
        if (slotDate.isAfter(today.plusDays(30))) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Chỉ được phép đặt lịch trước tối đa 30 ngày");
        }

        // 4. Chống 1 bệnh nhân đặt nhiều phiếu trong cùng 1 ngày
        boolean hasExisting = appointmentRepository.existsByPatientIdAndAppointmentDateAndStatusNot(
                patient.getId(), slotDate, AppointmentStatus.CANCELLED);
        if (hasExisting) {
            throw new AppException(ErrorCode.CONFLICT,
                    "Người bệnh " + patient.getFullName() + " đã có phiếu hẹn trong ngày " + slotDate + ". Mỗi người bệnh chỉ đặt tối đa 1 phiếu mỗi ngày");
        }

        // 5. Tìm khung giờ
        List<TimeSlot> slots = timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc();
        String reqStart = request.getSlotStart() != null ? request.getSlotStart().trim() : "";
        TimeSlot chosenSlot = slots.stream()
                .filter(s -> s.getStartTime().toString().startsWith(reqStart))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy khung giờ khám " + reqStart));

        // 5.1. Chặn đặt khung giờ đã trôi qua trong ngày hôm nay
        if (slotDate.isEqual(today) && chosenSlot.getEndTime() != null && chosenSlot.getEndTime().isBefore(nowTime)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Khung giờ này đã trôi qua. Cô chú vui lòng chọn khung giờ khác nhé");
        }

        // 6. Cấp ghế đồng thời an toàn (appointment_seats <= 6)
        int maxRetries = 6;
        AppointmentSeat allocatedSeat = null;
        Appointment savedAppointment = null;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            List<AppointmentSeat> currentSeats = appointmentSeatRepository
                    .findByAppointmentDateAndTimeSlotIdOrderBySeatNoAsc(slotDate, chosenSlot.getId());

            Set<Integer> takenSeats = currentSeats.stream()
                    .map(AppointmentSeat::getSeatNo)
                    .collect(Collectors.toSet());

            int capacity = chosenSlot.getMaxPatients() != null && chosenSlot.getMaxPatients() > 0 ? chosenSlot.getMaxPatients() : 6;
            if (takenSeats.size() >= capacity) {
                throw new AppException(ErrorCode.SLOT_FULL,
                        "Khung giờ này đã đủ bệnh nhân. Cô chú chọn khung khác giúp phòng khám nhé");
            }

            int candidateSeat = -1;
            for (int s = 1; s <= capacity; s++) {
                if (!takenSeats.contains(s)) {
                    candidateSeat = s;
                    break;
                }
            }

            if (candidateSeat == -1) {
                throw new AppException(ErrorCode.SLOT_FULL,
                        "Khung giờ này đã đủ bệnh nhân. Cô chú chọn khung khác giúp phòng khám nhé");
            }

            try {
                if (savedAppointment == null) {
                    String bookingCode = sequenceGeneratorService.nextAppointmentCode(slotDate);
                    Appointment rebookedFrom = null;
                    if (request.getRebookFromId() != null) {
                        rebookedFrom = appointmentRepository.findById(request.getRebookFromId()).orElse(null);
                    }

                    Appointment appt = Appointment.builder()
                            .bookingCode(bookingCode)
                            .patient(patient)
                            .serviceType(ContractMapper.toDbServiceType(request.getServiceType()))
                            .appointmentDate(slotDate)
                            .timeSlot(chosenSlot)
                            .expectedTime(chosenSlot.getStartTime())
                            .source(AppointmentSource.WEB)
                            .status(AppointmentStatus.PENDING)
                            .bookerName(currentUser.getFullName())
                            .bookerPhone(currentUser.getPhone())
                            .rescheduledFrom(rebookedFrom)
                            .build();
                    savedAppointment = appointmentRepository.saveAndFlush(appt);
                }

                AppointmentSeat seat = AppointmentSeat.builder()
                        .appointmentDate(slotDate)
                        .timeSlot(chosenSlot)
                        .seatNo(candidateSeat)
                        .appointment(savedAppointment)
                        .build();

                allocatedSeat = appointmentSeatRepository.saveAndFlush(seat);
                break; // Thành công
            } catch (DataIntegrityViolationException ex) {
                log.warn("Trùng số ghế {} trong khung giờ {}, đang thử số ghế kế tiếp...", candidateSeat, chosenSlot.getId());
            }
        }

        if (allocatedSeat == null) {
            throw new AppException(ErrorCode.SLOT_FULL,
                    "Khung giờ này đã đủ bệnh nhân. Cô chú chọn khung khác giúp phòng khám nhé");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("appointment", AppointmentDto.from(savedAppointment));

        // 7. Ghi nhận Idempotency-Key
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                String keyHash = HashUtils.sha256(idempotencyKey.trim());
                IdempotencyKey idKey = IdempotencyKey.builder()
                        .keyHash(keyHash)
                        .userId(currentUser.getId())
                        .endpoint("/api/v1/me/appointments")
                        .responseJson(objectMapper.writeValueAsString(result))
                        .build();
                idempotencyKeyRepository.save(idKey);
            } catch (Exception ex) {
                log.warn("Lỗi lưu idempotency key: {}", ex.getMessage());
            }
        }

        return result;
    }

    // =========================================================================
    // 4. QUẢN LÝ PHIẾU KHÁM CỦA TÔI
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMyAppointments(User currentUser, String statusFilter) {
        if (currentUser == null) {
            throw AppException.unauthorized("Vui lòng đăng nhập");
        }

        List<AppointmentStatus> statuses;
        if ("ALL".equalsIgnoreCase(statusFilter)) {
            statuses = List.of(AppointmentStatus.values());
        } else if ("PAST".equalsIgnoreCase(statusFilter) || "HISTORY".equalsIgnoreCase(statusFilter)) {
            statuses = List.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW);
        } else {
            // Mặc định: các phiếu sắp tới / đang xử lý
            statuses = List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS);
        }

        List<Appointment> list = appointmentRepository.findByUserIdAndStatuses(currentUser.getId(), statuses);
        List<AppointmentDto> items = list.stream().map(AppointmentDto::from).toList();
        return Map.of("items", items);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAppointmentDetail(User currentUser, Long id) {
        if (currentUser == null) {
            throw AppException.unauthorized("Vui lòng đăng nhập");
        }

        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy phiếu khám"));

        if (a.getPatient() == null || a.getPatient().getUser() == null || !a.getPatient().getUser().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xem phiếu khám này");
        }

        MedicalRecord rec = medicalRecordRepository.findByAppointmentId(id).orElse(null);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("appointment", AppointmentDto.from(a));
        res.put("record", rec != null ? MedicalRecordPatientDto.from(rec) : null);
        return res;
    }

    @Override
    @Transactional
    public Map<String, Object> cancelAppointment(User currentUser, Long id, String reason) {
        if (currentUser == null) {
            throw AppException.unauthorized("Vui lòng đăng nhập");
        }

        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy phiếu khám"));

        if (a.getPatient() == null || a.getPatient().getUser() == null || !a.getPatient().getUser().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền hủy phiếu khám này");
        }

        if (a.getCheckInAt() != null || a.getStatus() == AppointmentStatus.CONFIRMED
                || a.getStatus() == AppointmentStatus.IN_PROGRESS || a.getStatus() == AppointmentStatus.COMPLETED) {
            throw new AppException(ErrorCode.NOT_CANCELLABLE,
                    "Phiếu khám đã tiếp nhận hoặc đang khám, không thể hủy");
        }

        a.setStatus(AppointmentStatus.CANCELLED);
        a.setCancelledAt(LocalDateTime.now(clock));
        a.setCancelReason(reason != null && !reason.isBlank() ? reason.trim() : "Bệnh nhân chủ động hủy trên trang web");
        Appointment saved = appointmentRepository.save(a);

        // Nhả ghế khám để người khác có thể đặt
        appointmentSeatRepository.deleteByAppointmentId(saved.getId());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("appointment", AppointmentDto.from(saved));
        res.put("success", true);
        res.put("message", "Hủy phiếu khám thành công");
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAppointmentResult(User currentUser, Long id) {
        return getAppointmentDetail(currentUser, id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMyHistory(User currentUser) {
        return getMyAppointments(currentUser, "HISTORY");
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private LocalDate parseDob(String dobStr, Integer birthYear) {
        LocalDate parsed = parseDobString(dobStr);
        if (parsed != null) return parsed;
        if (birthYear != null && birthYear >= 1900 && birthYear <= 2100) {
            return LocalDate.of(birthYear, 1, 1);
        }
        return null;
    }

    private LocalDate parseDobString(String dobStr) {
        if (dobStr != null && !dobStr.isBlank()) {
            String clean = dobStr.trim();
            try {
                if (clean.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                    return LocalDate.parse(clean, DateTimeFormatter.ISO_LOCAL_DATE);
                } else if (clean.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
                    return LocalDate.parse(clean, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } else if (clean.matches("^\\d{4}$")) {
                    return LocalDate.of(Integer.parseInt(clean), 1, 1);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}
