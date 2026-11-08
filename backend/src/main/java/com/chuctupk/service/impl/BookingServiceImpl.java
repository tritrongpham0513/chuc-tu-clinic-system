package com.chuctupk.service.impl;

import com.chuctupk.dto.request.AppointmentCancelByPatientRequest;
import com.chuctupk.dto.request.BookingRequest;
import com.chuctupk.dto.response.AppointmentDetailResponse;
import com.chuctupk.dto.response.SlotAvailabilityResponse;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentSource;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.*;
import com.chuctupk.service.BookingService;
import com.chuctupk.service.ClinicSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final Random RANDOM = new SecureRandom();
    private static final List<AppointmentStatus> UPCOMING_STATUSES =
            Arrays.asList(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS);
    private static final List<AppointmentStatus> HISTORY_STATUSES =
            Arrays.asList(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW);

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final ClinicServiceRepository clinicServiceRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final ClinicHolidayRepository clinicHolidayRepository;
    private final ClinicSettingService clinicSettingService;
    private final Clock clock;

    // ----------------------------------------------------------------
    // Helper: lấy user hiện tại từ JWT
    // ----------------------------------------------------------------
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy tài khoản"));
    }

    // ----------------------------------------------------------------
    // GET /api/v1/booking/slots?date=YYYY-MM-DD
    // ----------------------------------------------------------------
    @Override
    public List<SlotAvailabilityResponse> getAvailableSlots(LocalDate date) {
        List<TimeSlot> allSlots = timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc();
        return allSlots.stream().map(slot -> {
            long booked = appointmentRepository.countBookedSlotsTotal(
                    date, slot.getId(), AppointmentStatus.CANCELLED);
            long available = Math.max(0, slot.getMaxPatients() - booked);
            return SlotAvailabilityResponse.builder()
                    .id(slot.getId())
                    .slotLabel(slot.getSlotLabel())
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .period(slot.getPeriod().name())
                    .maxPatients(slot.getMaxPatients())
                    .bookedCount(booked)
                    .available(available)
                    .isFull(available == 0)
                    .sortOrder(slot.getSortOrder())
                    .build();
        }).toList();
    }

    // ----------------------------------------------------------------
    // POST /api/v1/booking  (yêu cầu ROLE_PATIENT + JWT)
    // ----------------------------------------------------------------
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentDetailResponse bookAppointment(BookingRequest request) {
        User user = getCurrentUser();
        LocalDate today = LocalDate.now(clock);

        // --- Validate ngày ---
        if (request.getAppointmentDate().isBefore(today)) {
            throw AppException.unprocessable("Không thể đặt lịch vào ngày trong quá khứ");
        }
        int advanceDays = clinicSettingService.getBookingAdvanceDays();
        if (request.getAppointmentDate().isAfter(today.plusDays(advanceDays))) {
            throw AppException.unprocessable("Chỉ có thể đặt lịch trước tối đa " + advanceDays + " ngày");
        }
        // Kiểm tra ngày nghỉ lễ
        if (clinicHolidayRepository.existsByHolidayDateAndIsClosedTrue(request.getAppointmentDate())) {
            throw AppException.unprocessable("Phòng khám nghỉ lễ vào ngày này");
        }

        // --- Khóa bi quan slot để chống race condition ---
        TimeSlot slot = timeSlotRepository.findByIdWithLock(request.getTimeSlotId())
                .orElseThrow(() -> AppException.notFound("Khung giờ không tồn tại"));
        if (!Boolean.TRUE.equals(slot.getIsActive())) {
            throw AppException.unprocessable("Khung giờ này không hoạt động");
        }

        // --- Kiểm tra thời gian tối thiểu trước giờ khám ---
        int minHours = clinicSettingService.getBookingMinHours();
        if (request.getAppointmentDate().isEqual(today)) {
            LocalTime cutoff = LocalTime.now(clock).plusHours(minHours);
            if (slot.getStartTime().isBefore(cutoff)) {
                throw AppException.unprocessable(
                        "Đã quá thời gian nhận lịch cho khung giờ này (cần đặt trước ít nhất "
                                + minHours + " giờ)");
            }
        }

        // --- Kiểm tra quota slot ---
        long booked = appointmentRepository.countBookedSlotsTotal(
                request.getAppointmentDate(), slot.getId(), AppointmentStatus.CANCELLED);
        if (booked >= slot.getMaxPatients()) {
            throw AppException.unprocessable("Khung giờ này đã đầy, vui lòng chọn khung giờ khác");
        }

        // --- Tìm patient & verify quyền sở hữu ---
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy hồ sơ bệnh nhân"));
        if (patient.getUser() == null || !patient.getUser().getId().equals(user.getId())) {
            throw AppException.badRequest("Hồ sơ bệnh nhân không thuộc tài khoản của bạn");
        }

        // --- Chống double-booking ---
        boolean duplicate = appointmentRepository.existsByPatientIdAndAppointmentDateAndTimeSlotIdAndStatusNot(
                patient.getId(), request.getAppointmentDate(), slot.getId(), AppointmentStatus.CANCELLED);
        if (duplicate) {
            throw AppException.unprocessable("Hồ sơ này đã có lịch khám vào khung giờ này rồi");
        }

        // --- Lấy dịch vụ (optional) ---
        ClinicService clinicService = null;
        if (request.getServiceId() != null) {
            clinicService = clinicServiceRepository.findById(request.getServiceId()).orElse(null);
        }

        // --- Sinh booking code ---
        String bookingCode = generateBookingCode(request.getAppointmentDate());

        Appointment appointment = Appointment.builder()
                .bookingCode(bookingCode)
                .patient(patient)
                .service(clinicService)
                .appointmentDate(request.getAppointmentDate())
                .timeSlot(slot)
                .symptoms(request.getSymptoms())
                .bookerName(user.getFullName())
                .bookerPhone(user.getPhone())
                .source(AppointmentSource.WEB)
                .status(AppointmentStatus.PENDING)
                .build();

        try {
            appointment = appointmentRepository.save(appointment);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate booking attempt for user={}, date={}, slot={}",
                    user.getId(), request.getAppointmentDate(), slot.getId());
            throw AppException.unprocessable("Lịch khám bị trùng, vui lòng thử lại");
        }

        log.info("Booking created: code={}, patient={}, date={}, slot={}",
                bookingCode, patient.getId(), request.getAppointmentDate(), slot.getId());
        return toDetailResponse(appointment);
    }

    // ----------------------------------------------------------------
    // GET /api/v1/booking/my/upcoming
    // ----------------------------------------------------------------
    @Override
    public List<AppointmentDetailResponse> getMyUpcoming() {
        User user = getCurrentUser();
        List<Appointment> list = appointmentRepository.findByUserIdAndStatuses(user.getId(), UPCOMING_STATUSES);
        return list.stream().map(this::toDetailResponse).toList();
    }

    // ----------------------------------------------------------------
    // GET /api/v1/booking/my/history
    // ----------------------------------------------------------------
    @Override
    public List<AppointmentDetailResponse> getMyHistory() {
        User user = getCurrentUser();
        List<Appointment> list = appointmentRepository.findByUserIdAndStatuses(user.getId(), HISTORY_STATUSES);
        return list.stream().map(this::toDetailResponse).toList();
    }

    // ----------------------------------------------------------------
    // GET /api/v1/booking/my/{id}
    // ----------------------------------------------------------------
    @Override
    public AppointmentDetailResponse getMyAppointmentById(Long id) {
        User user = getCurrentUser();
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy phiếu khám"));
        verifyOwnership(appointment, user);
        return toDetailResponse(appointment);
    }

    // ----------------------------------------------------------------
    // POST /api/v1/booking/my/{id}/cancel
    // ----------------------------------------------------------------
    @Override
    @Transactional
    public AppointmentDetailResponse cancelMyAppointment(Long id, AppointmentCancelByPatientRequest request) {
        User user = getCurrentUser();
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy phiếu khám"));
        verifyOwnership(appointment, user);

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw AppException.unprocessable("Chỉ có thể hủy phiếu khám đang chờ xác nhận");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(request.getCancelReason());
        appointment = appointmentRepository.save(appointment);

        log.info("Appointment cancelled by patient: id={}, user={}", id, user.getId());
        return toDetailResponse(appointment);
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private void verifyOwnership(Appointment appointment, User user) {
        if (appointment.getPatient().getUser() == null
                || !appointment.getPatient().getUser().getId().equals(user.getId())) {
            // Trả 404 để không lộ thông tin tồn tại
            throw AppException.notFound("Không tìm thấy phiếu khám");
        }
    }

    private String generateBookingCode(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 10; i++) {
            int num = RANDOM.nextInt(9000) + 1000;
            String code = "PKCT-" + dateStr + "-" + num;
            if (!appointmentRepository.existsByBookingCode(code)) return code;
        }
        throw AppException.internal("Không thể sinh mã phiếu khám, vui lòng thử lại");
    }

    private AppointmentDetailResponse toDetailResponse(Appointment a) {
        // Lấy kết quả khám nếu trạng thái COMPLETED
        AppointmentDetailResponse.MedicalResultSummary resultSummary = null;
        if (a.getStatus() == AppointmentStatus.COMPLETED) {
            medicalRecordRepository.findByAppointmentId(a.getId()).ifPresent(r ->
                    // sẽ gán bên dưới sau khi builder hoàn chỉnh
                    log.debug("Medical record found for appointment {}", a.getId())
            );
            var optRecord = medicalRecordRepository.findByAppointmentId(a.getId());
            if (optRecord.isPresent()) {
                MedicalRecord r = optRecord.get();
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
            case PENDING     -> "Chờ xác nhận";
            case CONFIRMED   -> "Đã xác nhận";
            case IN_PROGRESS -> "Đang khám";
            case COMPLETED   -> "Đã hoàn thành";
            case CANCELLED   -> "Đã hủy";
            case NO_SHOW     -> "Không đến";
        };

        return AppointmentDetailResponse.builder()
                .id(a.getId())
                .bookingCode(a.getBookingCode())
                .appointmentDate(a.getAppointmentDate())
                .timeSlotLabel(a.getTimeSlot().getSlotLabel())
                .startTime(a.getTimeSlot().getStartTime())
                .endTime(a.getTimeSlot().getEndTime())
                .patientName(a.getPatient().getFullName())
                .patientCode(a.getPatient().getPatientCode())
                .relationship(a.getPatient().getRelationship())
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
