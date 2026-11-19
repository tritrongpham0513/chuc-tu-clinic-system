package com.chuctupk.service.impl;

import com.chuctupk.common.PhoneValidator;
import com.chuctupk.common.mapping.ContractMapper;
import com.chuctupk.dto.request.*;
import com.chuctupk.dto.response.*;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentSource;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.CheckinStatus;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.entity.enums.PriorityGroup;
import com.chuctupk.exception.AppException;
import com.chuctupk.exception.ErrorCode;
import com.chuctupk.notification.DeliveryResult;
import com.chuctupk.notification.ManualProvider;
import com.chuctupk.repository.*;
import com.chuctupk.service.ClinicSettingService;
import com.chuctupk.service.DoctorScheduleAdminService;
import com.chuctupk.service.SequenceGeneratorService;
import com.chuctupk.service.StaffService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Slf4j
@Service
public class StaffServiceImpl implements StaffService {

    private final AppointmentRepository appointmentRepository;
    private final CheckinRepository checkinRepository;
    private final PatientRepository patientRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentSeatRepository appointmentSeatRepository;
    private final ClinicSettingService clinicSettingService;
    private final com.chuctupk.service.SlotPolicy slotPolicy;
    private final com.chuctupk.service.DoctorAssignmentService doctorAssignmentService;
    private final com.chuctupk.repository.DoctorRepository doctorRepository;
    private final DoctorScheduleAdminService doctorScheduleAdminService;
    private final DoctorDayOffRepository doctorDayOffRepository;
    private final com.chuctupk.service.NotificationService notificationService;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final Clock clock;
    private final NotificationLogRepository notificationLogRepository;
    private final ManualProvider manualProvider;

    @Autowired
    public StaffServiceImpl(
            AppointmentRepository appointmentRepository,
            CheckinRepository checkinRepository,
            PatientRepository patientRepository,
            TimeSlotRepository timeSlotRepository,
            MedicalRecordRepository medicalRecordRepository,
            AppointmentSeatRepository appointmentSeatRepository,
            ClinicSettingService clinicSettingService,
            com.chuctupk.service.SlotPolicy slotPolicy,
            com.chuctupk.service.DoctorAssignmentService doctorAssignmentService,
            com.chuctupk.repository.DoctorRepository doctorRepository,
            DoctorScheduleAdminService doctorScheduleAdminService,
            DoctorDayOffRepository doctorDayOffRepository,
            com.chuctupk.service.NotificationService notificationService,
            SequenceGeneratorService sequenceGeneratorService,
            Clock clock,
            NotificationLogRepository notificationLogRepository,
            ManualProvider manualProvider
    ) {
        this.appointmentRepository = appointmentRepository;
        this.checkinRepository = checkinRepository;
        this.patientRepository = patientRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.appointmentSeatRepository = appointmentSeatRepository;
        this.clinicSettingService = clinicSettingService;
        this.slotPolicy = slotPolicy;
        this.doctorAssignmentService = doctorAssignmentService;
        this.doctorRepository = doctorRepository;
        this.doctorScheduleAdminService = doctorScheduleAdminService;
        this.doctorDayOffRepository = doctorDayOffRepository;
        this.notificationService = notificationService;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.clock = clock;
        this.notificationLogRepository = notificationLogRepository;
        this.manualProvider = manualProvider;
    }

    public StaffServiceImpl(
            AppointmentRepository appointmentRepository,
            CheckinRepository checkinRepository,
            PatientRepository patientRepository,
            TimeSlotRepository timeSlotRepository,
            MedicalRecordRepository medicalRecordRepository,
            AppointmentSeatRepository appointmentSeatRepository,
            ClinicSettingService clinicSettingService,
            com.chuctupk.service.SlotPolicy slotPolicy,
            com.chuctupk.service.DoctorAssignmentService doctorAssignmentService,
            com.chuctupk.repository.DoctorRepository doctorRepository,
            DoctorScheduleAdminService doctorScheduleAdminService,
            DoctorDayOffRepository doctorDayOffRepository,
            com.chuctupk.service.NotificationService notificationService,
            SequenceGeneratorService sequenceGeneratorService,
            Clock clock
    ) {
        this(
                appointmentRepository,
                checkinRepository,
                patientRepository,
                timeSlotRepository,
                medicalRecordRepository,
                appointmentSeatRepository,
                clinicSettingService,
                slotPolicy,
                doctorAssignmentService,
                doctorRepository,
                doctorScheduleAdminService,
                doctorDayOffRepository,
                notificationService,
                sequenceGeneratorService,
                clock,
                null,
                null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StaffAppointmentListResponse getAppointments(LocalDate date) {
        return buildAppointmentList(date, null);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffAppointmentListResponse getMyAppointments(LocalDate date, User currentUser) {
        if (currentUser == null) {
            throw AppException.unauthorized("Chưa đăng nhập");
        }
        Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> AppException.unprocessable(
                        "Tài khoản này không gắn với hồ sơ bác sĩ nào."));
        return buildAppointmentList(date, doctor.getId());
    }

    private StaffAppointmentListResponse buildAppointmentList(LocalDate date, Long onlyDoctorId) {
        LocalDate queryDate = date != null ? date : LocalDate.now(clock);
        List<Appointment> appts = appointmentRepository
                .findByAppointmentDateOrderByTimeSlotStartTimeAsc(queryDate);

        if (onlyDoctorId != null) {
            appts = appts.stream()
                    .filter(a -> a.getDoctor() != null && onlyDoctorId.equals(a.getDoctor().getId()))
                    .toList();
        }

        // Chỉ 2 query cho cả ngày, không N+1; bỏ qua khi ngày không có phiếu nào
        Map<Period, Set<Long>> onDutyByPeriod = new EnumMap<>(Period.class);
        if (!appts.isEmpty()) {
            for (Period p : List.of(Period.MORNING, Period.AFTERNOON)) {
                onDutyByPeriod.put(p,
                        new HashSet<>(doctorAssignmentService.findOnDutyDoctorIds(queryDate, p)));
            }
        }

        List<Long> apptIds = appts.stream().map(Appointment::getId).toList();
        Map<Long, Checkin> checkinMap = apptIds.isEmpty() ? Map.of()
                : checkinRepository.findByAppointmentIdIn(apptIds).stream()
                    .filter(c -> c.getAppointment() != null)
                    .collect(toMap(c -> c.getAppointment().getId(), c -> c, (a, b) -> a));

        // Ticket 6: Lấy thời điểm nhắc lịch gần nhất cho toàn bộ danh sách phiếu qua 1 query tổng hợp (tránh N+1)
        Map<Long, LocalDateTime> reminderSentAtMap = new HashMap<>();
        if (!apptIds.isEmpty() && notificationLogRepository != null) {
            List<Object[]> reminderRows = notificationLogRepository.findLatestReminderSentAtByAppointmentIds(apptIds);
            for (Object[] row : reminderRows) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    reminderSentAtMap.put((Long) row[0], (LocalDateTime) row[1]);
                }
            }
        }

        List<AppointmentDto> items = appts.stream()
                .map(a -> {
                    LocalDateTime sentAt = reminderSentAtMap.get(a.getId());
                    String reminderSentAt = ContractMapper.formatIso(sentAt);
                    return AppointmentDto.from(a, checkinMap.get(a.getId()), isDoctorOnDuty(a, onDutyByPeriod), reminderSentAt);
                })
                .toList();

        int total = appts.size();
        int checkedIn = (int) appts.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count();
        int inProgress = (int) appts.stream().filter(a -> a.getStatus() == AppointmentStatus.IN_PROGRESS).count();
        int completed = (int) appts.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
        int cancelled = (int) appts.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();
        int booked = (int) appts.stream().filter(a -> a.getStatus() == AppointmentStatus.PENDING).count();

        StaffAppointmentSummaryDto summary = new StaffAppointmentSummaryDto(
                total, checkedIn, inProgress, completed, cancelled, booked
        );

        return new StaffAppointmentListResponse(queryDate.toString(), items, summary);
    }

    /** null = không xác định (phiếu không có bác sĩ hoặc không có khung giờ). */
    private Boolean isDoctorOnDuty(Appointment a, Map<Period, Set<Long>> onDutyByPeriod) {
        if (a.getDoctor() == null || a.getTimeSlot() == null || a.getTimeSlot().getPeriod() == null) {
            return null;
        }
        Set<Long> ids = onDutyByPeriod.get(a.getTimeSlot().getPeriod());
        return ids != null && ids.contains(a.getDoctor().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public StaffAppointmentDatesResponse getAppointmentDates(String month) {
        String queryMonth = month != null && !month.isBlank() ? month : LocalDate.now(clock).toString().substring(0, 7);
        LocalDate start = LocalDate.parse(queryMonth + "-01");
        LocalDate end = start.plusMonths(1);

        List<Appointment> list = appointmentRepository.findByAppointmentDateBetween(start, end);
        Set<String> dateSet = new TreeSet<>();
        for (Appointment a : list) {
            if (a.getAppointmentDate() != null) {
                dateSet.add(a.getAppointmentDate().toString());
            }
        }
        return new StaffAppointmentDatesResponse(queryMonth, new ArrayList<>(dateSet));
    }

    @Override
    @Transactional(readOnly = true)
    public StaffAppointmentLookupResponse lookupAppointment(String code) {
        if (code == null || code.isBlank()) {
            return new StaffAppointmentLookupResponse(null);
        }

        String clean = code.trim();
        List<Appointment> list = appointmentRepository.lookupByCodeOrPhoneOrPatientCode(clean);
        if (list.isEmpty()) {
            return new StaffAppointmentLookupResponse(null);
        }

        Appointment found = list.get(0);
        Checkin checkin = checkinRepository.findByAppointmentId(found.getId()).orElse(null);
        return new StaffAppointmentLookupResponse(AppointmentDto.from(found, checkin));
    }

    @Override
    @Transactional(readOnly = true)
    public StaffAppointmentDetailResponse getAppointmentDetail(Long id) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy phiếu"));
        MedicalRecord rec = medicalRecordRepository.findByAppointmentId(id).orElse(null);
        Checkin checkin = checkinRepository.findByAppointmentId(id).orElse(null);

        return new StaffAppointmentDetailResponse(
                AppointmentDto.from(a, checkin),
                rec != null ? MedicalRecordPatientDto.from(rec) : null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StaffAvailabilityResponse getAvailability(LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now(clock);
        List<TimeSlot> slots = timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc();

        List<SlotAvailabilityDto> slotDtos = new ArrayList<>();
        LocalDate today = LocalDate.now(clock);
        LocalTime nowTime = LocalTime.now(clock);

        for (TimeSlot slot : slots) {
            long booked = appointmentSeatRepository.countByAppointmentDateAndTimeSlotId(queryDate, slot.getId());
            int capacity = clinicSettingService.getSlotCapacity();
            int remaining = Math.max(0, capacity - (int) booked);

            boolean isPast = queryDate.isBefore(today) || (queryDate.equals(today) && slot.getEndTime().isBefore(nowTime));
            boolean isFull = remaining <= 0;
            boolean hasDoctor = doctorAssignmentService.hasDoctorOnDuty(queryDate, slot);
            boolean available = !isPast && !isFull && hasDoctor;

            String reason;
            if (isPast) {
                reason = "PAST";
            } else if (!hasDoctor) {
                reason = "NO_DOCTOR";
            } else if (isFull) {
                reason = "SLOT_FULL";
            } else {
                reason = null;
            }
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

        return new StaffAvailabilityResponse(queryDate.toString(), slotDtos);
    }

    @Override
    @Transactional
    public Map<String, Object> createCounterAppointment(User staff, StaffCreateAppointmentRequest request) {
        if (request.patientId() == null) {
            throw AppException.badRequest("Mã bệnh nhân không được để trống");
        }

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy bệnh nhân"));

        if (Boolean.FALSE.equals(patient.getIsActive())) {
            throw AppException.of(ErrorCode.FORBIDDEN, "Hồ sơ bệnh nhân đã bị khóa, không thể đặt lịch");
        }

        LocalDate slotDate = LocalDate.parse(request.slotDate());
        LocalDate today = LocalDate.now(clock);

        if (slotDate.isBefore(today)) {
            throw AppException.badRequest("Không thể đặt lịch cho ngày trong quá khứ");
        }
        if (slotDate.isAfter(today.plusDays(7))) {
            throw AppException.badRequest("Tiếp tân chỉ được phép đặt lịch trước tối đa 7 ngày");
        }

        boolean hasExisting = appointmentRepository.existsByPatientIdAndAppointmentDateAndStatusNot(
                patient.getId(), slotDate, AppointmentStatus.CANCELLED
        );
        if (hasExisting) {
            throw AppException.of(ErrorCode.CONFLICT,
                    "Người bệnh đã có phiếu hẹn trong ngày " + slotDate + ". Mỗi người bệnh chỉ đặt tối đa 1 phiếu mỗi ngày");
        }

        List<TimeSlot> slots = timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc();
        String reqStart = request.slotStart() != null ? request.slotStart().trim() : "";
        TimeSlot chosenSlot = slots.stream()
                .filter(s -> s.getStartTime().toString().startsWith(reqStart))
                .findFirst()
                .orElseThrow(() -> AppException.notFound("Không tìm thấy khung giờ khám " + reqStart));

        LocalDateTime now = LocalDateTime.now(clock);
        if (!Boolean.TRUE.equals(request.checkInNow())) {
            com.chuctupk.service.SlotPolicy.SlotStatus slotStatus = slotPolicy.evaluate(
                    slotDate, chosenSlot, now, com.chuctupk.service.SlotPolicy.BookingChannel.COUNTER
            );
            if (slotStatus != com.chuctupk.service.SlotPolicy.SlotStatus.OK) {
                switch (slotStatus) {
                    case PAST, TOO_LATE -> throw AppException.badRequest("Khung giờ này đã trôi qua. Vui lòng chọn khung giờ hiện tại hoặc kế tiếp");
                    case OUTSIDE_RECEPTION -> throw AppException.unprocessable(
                            "Phòng khám chỉ tiếp nhận trong khoảng từ " + clinicSettingService.getReceptionOpen() + " đến " + clinicSettingService.getReceptionClose());
                    case TOO_FAR -> throw AppException.badRequest("Tiếp tân chỉ được phép đặt lịch trước tối đa " + clinicSettingService.getCounterMaxDays() + " ngày");
                    case FULL -> throw AppException.of(ErrorCode.SLOT_FULL, "Khung giờ này đã đủ bệnh nhân. Cô chú chọn khung khác giúp phòng khám nhé");
                    default -> throw AppException.badRequest("Khung giờ không hợp lệ");
                }
            }
        }

        // Allocate seat
        int capacity = clinicSettingService.getSlotCapacity();
        int maxRetries = 6;
        AppointmentSeat allocatedSeat = null;
        Appointment savedAppointment = null;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            List<AppointmentSeat> currentSeats = appointmentSeatRepository
                    .findByAppointmentDateAndTimeSlotIdOrderBySeatNoAsc(slotDate, chosenSlot.getId());

            Set<Integer> takenSeats = currentSeats.stream()
                    .map(AppointmentSeat::getSeatNo)
                    .collect(Collectors.toSet());

            if (takenSeats.size() >= capacity) {
                throw AppException.of(ErrorCode.SLOT_FULL,
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
                throw AppException.of(ErrorCode.SLOT_FULL,
                        "Khung giờ này đã đủ bệnh nhân. Cô chú chọn khung khác giúp phòng khám nhé");
            }

            try {
                if (savedAppointment == null) {
                    String bookingCode = sequenceGeneratorService.nextAppointmentCode(slotDate);
                    boolean checkInNow = Boolean.TRUE.equals(request.checkInNow());
                    Doctor doctor = doctorAssignmentService.pickDoctor(slotDate, chosenSlot);

                    Appointment appt = Appointment.builder()
                            .bookingCode(bookingCode)
                            .patient(patient)
                            .doctor(doctor)
                            .serviceType(ContractMapper.toDbServiceType(request.serviceType()))
                            .appointmentDate(slotDate)
                            .timeSlot(chosenSlot)
                            .source(AppointmentSource.WALK_IN)
                            .status(checkInNow ? AppointmentStatus.CONFIRMED : AppointmentStatus.PENDING)
                            .bookerName(staff != null ? staff.getFullName() : "Tiếp tân")
                            .bookerPhone(staff != null ? staff.getPhone() : "")
                            .checkInAt(checkInNow ? LocalDateTime.now(clock) : null)
                            .confirmedBy(checkInNow ? staff : null)
                            .confirmedAt(checkInNow ? LocalDateTime.now(clock) : null)
                            .build();

                    savedAppointment = appointmentRepository.save(appt);
                }

                AppointmentSeat seat = AppointmentSeat.builder()
                        .appointmentDate(slotDate)
                        .timeSlot(chosenSlot)
                        .seatNo(candidateSeat)
                        .appointment(savedAppointment)
                        .build();

                allocatedSeat = appointmentSeatRepository.save(seat);
                break;
            } catch (DataIntegrityViolationException ex) {
                log.warn("Trùng số ghế {} cho khung {}, đang thử lại...", candidateSeat, chosenSlot.getId());
            }
        }

        if (allocatedSeat == null) {
            throw AppException.of(ErrorCode.SLOT_FULL, "Khung giờ này đã đủ chỗ, vui lòng thử lại");
        }

        Checkin checkin = null;
        if (Boolean.TRUE.equals(request.checkInNow())) {
            checkin = Checkin.builder()
                    .appointment(savedAppointment)
                    .priorityGroup(PriorityGroup.NORMAL)
                    .queueNo(sequenceGeneratorService.nextQueueNumber(slotDate))
                    .status(CheckinStatus.WAITING)
                    .checkInTime(LocalDateTime.now(clock))
                    .build();
            checkin = checkinRepository.save(checkin);
        }

        AppointmentDto dto = AppointmentDto.from(savedAppointment, checkin);
        notificationService.notifyBookingSuccess(savedAppointment);
        return Map.of("appointment", dto, "ticket", dto);
    }

    @Override
    @Transactional
    public StaffAppointmentLookupResponse rescheduleAppointment(User staff, Long id, RescheduleAppointmentRequest request) {
        Appointment oldAppt = appointmentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy phiếu"));

        if (oldAppt.getStatus() == AppointmentStatus.CANCELLED || oldAppt.getStatus() == AppointmentStatus.COMPLETED) {
            throw AppException.badRequest("Phiếu khám đã kết thúc hoặc đã huỷ, không thể đổi giờ");
        }

        String slotDateStr = request.getSlotDate() != null ? request.getSlotDate() : (request.getNewDate() != null ? request.getNewDate().toString() : null);
        if (slotDateStr == null) {
            throw AppException.badRequest("Ngày khám mới không được để trống");
        }
        LocalDate newDate = LocalDate.parse(slotDateStr);

        // Nhả ghế cũ & checkin cũ
        appointmentSeatRepository.deleteByAppointmentId(oldAppt.getId());
        checkinRepository.deleteByAppointmentId(oldAppt.getId());

        oldAppt.setStatus(AppointmentStatus.CANCELLED);
        oldAppt.setCancelReason("Đổi sang ngày " + newDate + " " + (request.getSlotStart() != null ? request.getSlotStart() : ""));
        oldAppt.setCancelledAt(LocalDateTime.now(clock));
        appointmentRepository.save(oldAppt);

        List<TimeSlot> slots = timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc();
        String reqStart = request.getSlotStart() != null ? request.getSlotStart().trim() : "";
        TimeSlot newSlot = slots.stream()
                .filter(s -> (request.getNewTimeSlotId() != null && s.getId().equals(request.getNewTimeSlotId()))
                        || (!reqStart.isBlank() && s.getStartTime().toString().startsWith(reqStart)))
                .findFirst()
                .orElseThrow(() -> AppException.notFound("Không tìm thấy khung giờ khám"));

        // Cấp ghế mới
        int capacity = newSlot.getMaxPatients() != null && newSlot.getMaxPatients() > 0 ? newSlot.getMaxPatients() : 6;
        Appointment savedNewAppt = null;

        for (int attempt = 0; attempt < 6; attempt++) {
            List<AppointmentSeat> currentSeats = appointmentSeatRepository
                    .findByAppointmentDateAndTimeSlotIdOrderBySeatNoAsc(newDate, newSlot.getId());

            Set<Integer> takenSeats = currentSeats.stream().map(AppointmentSeat::getSeatNo).collect(toSet());
            if (takenSeats.size() >= capacity) {
                throw AppException.of(ErrorCode.SLOT_FULL, "Khung giờ mới này đã đủ bệnh nhân");
            }

            int candidateSeat = -1;
            for (int s = 1; s <= capacity; s++) {
                if (!takenSeats.contains(s)) {
                    candidateSeat = s;
                    break;
                }
            }

            try {
                if (savedNewAppt == null) {
                    String bookingCode = sequenceGeneratorService.nextAppointmentCode(newDate);
                    Doctor doctor = doctorAssignmentService.pickDoctor(newDate, newSlot);

                    Appointment nextAppt = Appointment.builder()
                            .bookingCode(bookingCode)
                            .patient(oldAppt.getPatient())
                            .doctor(doctor)
                            .service(oldAppt.getService())
                            .serviceType(oldAppt.getServiceType())
                            .appointmentDate(newDate)
                            .timeSlot(newSlot)
                            .source(oldAppt.getSource())
                            .status(AppointmentStatus.PENDING)
                            .rescheduledFrom(oldAppt)
                            .bookerName(staff != null ? staff.getFullName() : oldAppt.getBookerName())
                            .bookerPhone(staff != null ? staff.getPhone() : oldAppt.getBookerPhone())
                            .build();

                    savedNewAppt = appointmentRepository.save(nextAppt);
                }

                AppointmentSeat seat = AppointmentSeat.builder()
                        .appointmentDate(newDate)
                        .timeSlot(newSlot)
                        .seatNo(candidateSeat)
                        .appointment(savedNewAppt)
                        .build();

                appointmentSeatRepository.save(seat);
                break;
            } catch (DataIntegrityViolationException ex) {
                log.warn("Thử lại cấp ghế mới khi đổi giờ...");
            }
        }

        return new StaffAppointmentLookupResponse(AppointmentDto.from(savedNewAppt, (Checkin) null));
    }

    @Override
    @Transactional
    public StaffAppointmentLookupResponse cancelAppointment(User staff, Long id, CancelAppointmentStaffRequest request) {
        if (request.reason() == null || request.reason().isBlank()) {
            throw AppException.badRequest("Vui lòng ghi rõ lý do huỷ");
        }

        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy phiếu"));

        if (appt.getStatus() == AppointmentStatus.COMPLETED) {
            throw AppException.of(ErrorCode.NOT_CANCELLABLE, "Phiếu khám đã hoàn thành, không thể huỷ");
        }

        Optional<Checkin> checkinOpt = checkinRepository.findByAppointmentId(id);
        if (checkinOpt.isPresent() && checkinOpt.get().getStatus() == CheckinStatus.IN_ROOM) {
            throw AppException.of(ErrorCode.NOT_CANCELLABLE, "Phiếu khám đang trong phòng khám, không thể huỷ");
        }

        appointmentSeatRepository.deleteByAppointmentId(id);
        checkinRepository.deleteByAppointmentId(id);

        appt.setStatus(AppointmentStatus.CANCELLED);
        appt.setCancelReason(request.reason().trim());
        appt.setCancelledAt(LocalDateTime.now(clock));
        Appointment saved = appointmentRepository.save(appt);

        notificationService.notifyCancellation(saved, request.reason());

        return new StaffAppointmentLookupResponse(AppointmentDto.from(saved, (Checkin) null));
    }

    @Override
    @Transactional
    public StaffAppointmentLookupResponse checkin(User staff, Long id) {
        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy phiếu"));

        LocalDate today = LocalDate.now(clock);
        if (!today.equals(appt.getAppointmentDate())) {
            throw AppException.of(ErrorCode.WRONG_DAY, "Chỉ có thể tiếp nhận cho lịch khám trong ngày hôm nay");
        }

        LocalTime nowTime = LocalTime.now(clock);
        LocalTime openTime = clinicSettingService.getReceptionOpen();
        LocalTime closeTime = clinicSettingService.getReceptionClose();
        if (nowTime.isBefore(openTime) || nowTime.isAfter(closeTime)) {
            String openStr = ContractMapper.formatTime(openTime);
            String closeStr = ContractMapper.formatTime(closeTime);
            throw AppException.of(ErrorCode.CLOSED, "Phòng khám chỉ tiếp nhận bệnh nhân từ " + openStr + " đến " + closeStr);
        }

        if (appt.getStatus() == AppointmentStatus.CANCELLED || appt.getStatus() == AppointmentStatus.COMPLETED) {
            throw AppException.badRequest("Phiếu khám không thể check-in vì đã kết thúc hoặc đã huỷ");
        }
        if (appt.getStatus() == AppointmentStatus.CONFIRMED || appt.getStatus() == AppointmentStatus.IN_PROGRESS) {
            throw AppException.badRequest("Phiếu khám đã được check-in trước đó");
        }

        // Quy tắc nhóm ưu tiên: Phiếu đặt trước (WEB) đến trong [slotStart - priority_early_minutes, slotStart + 60m]
        PriorityGroup priorityGroup = PriorityGroup.NORMAL;
        if (appt.getSource() == AppointmentSource.WEB && appt.getTimeSlot() != null) {
            int earlyMinutes = clinicSettingService.getPriorityEarlyMinutes();

            LocalTime slotStart = appt.getTimeSlot().getStartTime();
            LocalTime earlyLimit = slotStart.minusMinutes(earlyMinutes);
            LocalTime lateLimit = slotStart.plusMinutes(60);

            if (!nowTime.isBefore(earlyLimit) && !nowTime.isAfter(lateLimit)) {
                priorityGroup = PriorityGroup.PRIORITY;
            }
        }

        int queueNo = sequenceGeneratorService.nextQueueNumber(today);
        Checkin checkin = Checkin.builder()
                .appointment(appt)
                .priorityGroup(priorityGroup)
                .queueNo(queueNo)
                .status(CheckinStatus.WAITING)
                .checkInTime(LocalDateTime.now(clock))
                .build();

        Checkin savedCheckin = checkinRepository.save(checkin);

        appt.setStatus(AppointmentStatus.CONFIRMED);
        appt.setCheckInAt(LocalDateTime.now(clock));
        appt.setConfirmedBy(staff);
        appt.setConfirmedAt(LocalDateTime.now(clock));
        Appointment savedAppt = appointmentRepository.save(appt);

        return new StaffAppointmentLookupResponse(AppointmentDto.from(savedAppt, savedCheckin));
    }

    @Override
    @Transactional
    public StaffAppointmentLookupResponse undoCheckin(User staff, Long id) {
        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy phiếu"));

        Optional<Checkin> checkinOpt = checkinRepository.findByAppointmentId(id);
        if (checkinOpt.isPresent()) {
            Checkin c = checkinOpt.get();
            if (c.getStatus() == CheckinStatus.IN_ROOM || c.getStatus() == CheckinStatus.COMPLETED) {
                throw AppException.of(ErrorCode.NOT_CANCELLABLE, "Không thể hoàn tác khi bệnh nhân đã vào phòng khám");
            }
            checkinRepository.delete(c);
        }

        appt.setStatus(AppointmentStatus.PENDING);
        appt.setCheckInAt(null);
        appt.setConfirmedBy(null);
        appt.setConfirmedAt(null);
        Appointment saved = appointmentRepository.save(appt);

        return new StaffAppointmentLookupResponse(AppointmentDto.from(saved, (Checkin) null));
    }

    @Override
    @Transactional
    public StaffAppointmentLookupResponse enterRoom(User staff, Long id) {
        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy phiếu"));

        LocalDate today = LocalDate.now(clock);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        int totalSeats = clinicSettingService.getSeats();
        List<Checkin> inRoom = checkinRepository.findInRoomToday(startOfDay, endOfDay);
        if (inRoom.size() >= totalSeats) {
            throw AppException.of(ErrorCode.ROOM_FULL, "Tất cả các ghế khám hiện đều đang có bệnh nhân");
        }

        Set<Integer> occupied = inRoom.stream().map(Checkin::getSeatNo).filter(Objects::nonNull).collect(toSet());
        int freeSeat = 1;
        for (int i = 1; i <= totalSeats; i++) {
            if (!occupied.contains(i)) {
                freeSeat = i;
                break;
            }
        }

        Checkin checkin = checkinRepository.findByAppointmentId(id).orElseGet(() -> Checkin.builder()
                .appointment(appt)
                .queueNo(sequenceGeneratorService.nextQueueNumber(today))
                .priorityGroup(PriorityGroup.NORMAL)
                .checkInTime(LocalDateTime.now(clock))
                .build());

        checkin.setStatus(CheckinStatus.IN_ROOM);
        checkin.setSeatNo(freeSeat);
        checkin.setEnteredAt(LocalDateTime.now(clock));
        Checkin savedCheckin = checkinRepository.save(checkin);

        appt.setStatus(AppointmentStatus.IN_PROGRESS);
        appt.setEnteredAt(LocalDateTime.now(clock));
        Appointment savedAppt = appointmentRepository.save(appt);

        return new StaffAppointmentLookupResponse(AppointmentDto.from(savedAppt, savedCheckin));
    }

    private boolean isStaffOrDoctorCaller() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (auth instanceof AnonymousAuthenticationToken) return false;
        if ("anonymousUser".equals(auth.getPrincipal())) return false;
        return auth.getAuthorities().stream().anyMatch(a -> {
            String role = a.getAuthority();
            return "ROLE_STAFF".equals(role) || "ROLE_ADMIN".equals(role) || "ROLE_DOCTOR".equals(role)
                    || "STAFF".equals(role) || "ADMIN".equals(role) || "DOCTOR".equals(role);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public QueueStateDto getQueueState() {
        return getQueueState(null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public QueueStateDto getQueueState(User currentUser, Long doctorId) {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        boolean isStaff = isStaffOrDoctorCaller();

        Long targetDoctorId = doctorId;
        if (currentUser != null && currentUser.getRole() == com.chuctupk.entity.enums.Role.DOCTOR) {
            Doctor currentDoc = doctorRepository.findByUserId(currentUser.getId()).orElse(null);
            if (currentDoc != null) {
                targetDoctorId = currentDoc.getId();
            }
        }

        List<Checkin> inRoom = checkinRepository.findInRoomToday(startOfDay, endOfDay);
        List<Checkin> waiting = checkinRepository.findWaitingTodayOrdered(startOfDay, endOfDay);

        if (targetDoctorId != null) {
            final Long docFilter = targetDoctorId;
            waiting = waiting.stream()
                    .filter(c -> c.getAppointment() != null && c.getAppointment().getDoctor() != null
                            && docFilter.equals(c.getAppointment().getDoctor().getId()))
                    .toList();
        }

        final Long docFilter = targetDoctorId;
        long completedCount = appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(today).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .filter(a -> docFilter == null || (a.getDoctor() != null && docFilter.equals(a.getDoctor().getId())))
                .count();

        boolean autoInvite = "1".equals(clinicSettingService.getSettingValue("auto_invite", "0"));
        int totalSeats;
        try {
            totalSeats = Integer.parseInt(clinicSettingService.getSettingValue("seats", "5"));
        } catch (NumberFormatException e) {
            totalSeats = 5;
        }

        Map<Integer, Checkin> seatMap = inRoom.stream()
                .filter(c -> c.getSeatNo() != null)
                .collect(toMap(Checkin::getSeatNo, c -> c, (a, b) -> a));

        List<QueueSeatDto> seatDtos = new ArrayList<>();
        for (int s = 1; s <= totalSeats; s++) {
            Checkin c = seatMap.get(s);
            if (c != null && c.getAppointment() != null) {
                Appointment a = c.getAppointment();
                String slotStart = a.getTimeSlot() != null && a.getTimeSlot().getStartTime() != null
                        ? a.getTimeSlot().getStartTime().toString().substring(0, 5) : "08:00";

                PatientDto pDto = isStaff ? PatientDto.from(a.getPatient()) : PatientDto.masked(a.getPatient());
                String docName = a.getDoctor() != null ? a.getDoctor().getFullName() : "Chưa phân bác sĩ";
                String ticketCode = isStaff ? a.getBookingCode() : null;

                seatDtos.add(new QueueSeatDto(
                        s,
                        false,
                        String.valueOf(c.getId()),
                        String.valueOf(a.getId()),
                        ticketCode,
                        pDto,
                        docName,
                        ContractMapper.formatIso(c.getEnteredAt()),
                        c.getPriorityGroup().name(),
                        ContractMapper.toFeSource(a.getSource()),
                        slotStart,
                        ContractMapper.formatIso(c.getCheckInTime())
                ));
            } else {
                seatDtos.add(new QueueSeatDto(
                        s,
                        true,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ));
            }
        }

        Map<String, Integer> slotMap = new LinkedHashMap<>();
        List<QueueWaitingDto> waitingDtos = new ArrayList<>();
        for (Checkin c : waiting) {
            Appointment a = c.getAppointment();
            if (a == null) continue;

            String slotStart = a.getTimeSlot() != null && a.getTimeSlot().getStartTime() != null
                    ? a.getTimeSlot().getStartTime().toString().substring(0, 5) : "08:00";
            slotMap.merge(slotStart, 1, Integer::sum);

            long waited = c.getCheckInTime() != null
                    ? ChronoUnit.MINUTES.between(c.getCheckInTime(), LocalDateTime.now(clock))
                    : 0;

            PatientDto pDto = isStaff ? PatientDto.from(a.getPatient()) : PatientDto.masked(a.getPatient());
            String ticketCode = isStaff ? a.getBookingCode() : null;

            waitingDtos.add(new QueueWaitingDto(
                    String.valueOf(c.getId()),
                    String.valueOf(a.getId()),
                    c.getQueueNo(),
                    ticketCode,
                    pDto,
                    c.getPriorityGroup().name(),
                    ContractMapper.toFeSource(a.getSource()),
                    slotStart,
                    ContractMapper.formatIso(c.getCheckInTime()),
                    Math.max(0, waited)
            ));
        }

        return new QueueStateDto(
                today.toString(),
                totalSeats,
                inRoom.size(),
                waitingDtos.size(),
                completedCount,
                autoInvite,
                seatDtos,
                waitingDtos,
                slotMap
        );
    }

    @Override
    @Transactional
    public QueueStateDto inviteNext() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        int totalSeats = clinicSettingService.getSeats();
        List<Checkin> inRoom = checkinRepository.findInRoomToday(startOfDay, endOfDay);
        if (inRoom.size() >= totalSeats) {
            throw AppException.of(ErrorCode.ROOM_FULL, "Tất cả các ghế khám hiện đều đang có bệnh nhân");
        }

        Set<Integer> occupied = inRoom.stream().map(Checkin::getSeatNo).filter(Objects::nonNull).collect(toSet());
        int freeSeat = 1;
        for (int i = 1; i <= totalSeats; i++) {
            if (!occupied.contains(i)) {
                freeSeat = i;
                break;
            }
        }

        List<Checkin> waiting = checkinRepository.findWaitingTodayOrdered(startOfDay, endOfDay);
        if (waiting.isEmpty()) {
            throw AppException.badRequest("Không còn bệnh nhân nào đang chờ trong hàng đợi");
        }

        Checkin next = waiting.get(0);
        next.setStatus(CheckinStatus.IN_ROOM);
        next.setSeatNo(freeSeat);
        next.setEnteredAt(LocalDateTime.now(clock));
        checkinRepository.save(next);

        Appointment appt = next.getAppointment();
        if (appt != null) {
            appt.setStatus(AppointmentStatus.IN_PROGRESS);
            appt.setEnteredAt(LocalDateTime.now(clock));
            appointmentRepository.save(appt);
        }

        return getQueueState();
    }

    @Override
    @Transactional
    public QueueStateDto undoEnter(Long id) {
        Checkin c = checkinRepository.findById(id)
                .or(() -> checkinRepository.findByAppointmentId(id))
                .orElseThrow(() -> AppException.notFound("Không tìm thấy thông tin lượt khám"));

        c.setStatus(CheckinStatus.WAITING);
        c.setSeatNo(null);
        c.setEnteredAt(null);
        checkinRepository.save(c);

        Appointment a = c.getAppointment();
        if (a != null) {
            a.setStatus(AppointmentStatus.CONFIRMED);
            a.setEnteredAt(null);
            appointmentRepository.save(a);
        }

        boolean autoInvite = "1".equals(clinicSettingService.getSettingValue("auto_invite", "0"));
        if (autoInvite) {
            try {
                inviteNext();
            } catch (Exception ignored) {}
        }

        return getQueueState();
    }

    @Override
    @Transactional
    public QueueStateDto setAutoInvite(boolean enabled) {
        clinicSettingService.setSettingValue("auto_invite", enabled ? "1" : "0");
        if (enabled) {
            while (true) {
                try {
                    inviteNext();
                } catch (Exception e) {
                    break;
                }
            }
        }
        return getQueueState();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardBadges() {
        LocalDate today = LocalDate.now(clock);
        long newBookings = appointmentRepository.countByAppointmentDate(today);
        long cancelledToday = appointmentRepository.findByAppointmentDateAndStatus(today, AppointmentStatus.CANCELLED).size();
        return Map.of(
                "newBookings", newBookings,
                "cancelledToday", cancelledToday
        );
    }

    @Override
    @Transactional(readOnly = true)
    public MyScheduleResponse getMySchedule(User currentUser) {
        if (currentUser == null) {
            throw AppException.unauthorized("Chưa đăng nhập");
        }
        Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> AppException.unprocessable(
                        "Tài khoản này không gắn với hồ sơ bác sĩ nào."));
        if (!Boolean.TRUE.equals(doctor.getIsActive())) {
            throw AppException.unprocessable("Hồ sơ bác sĩ đang tạm ngưng hoạt động.");
        }

        // TÁI SỬ DỤNG logic chuẩn hoá FULL_DAY của trang admin
        DoctorScheduleMatrixResponse matrix = doctorScheduleAdminService.getWeeklyScheduleMatrix();
        DoctorScheduleMatrixResponse.DoctorScheduleRow row = matrix.getDoctors().stream()
                .filter(d -> doctor.getId().equals(d.getId()))
                .findFirst()
                .orElse(null);   // bác sĩ inactive sẽ không có trong matrix -> coi như lịch trống

        LocalDate today = LocalDate.now(clock);
        List<DoctorDayOffDto> offs = doctorDayOffRepository
                .findByDoctorIdAndOffDateGreaterThanEqualOrderByOffDateAsc(doctor.getId(), today)
                .stream()
                .map(o -> DoctorDayOffDto.builder()
                        .id(o.getId())
                        .doctorId(doctor.getId())
                        .doctorName(doctor.getFullName())
                        .offDate(o.getOffDate())
                        .period(o.getPeriod())
                        .reason(o.getReason())
                        .createdAt(o.getCreatedAt())
                        .build())
                .sorted(Comparator.comparing(DoctorDayOffDto::getOffDate)
                        .thenComparing(d -> periodOrder(d.getPeriod())))  // FULL_DAY, MORNING, AFTERNOON
                .toList();

        return MyScheduleResponse.builder()
                .doctorId(doctor.getId())
                .fullName(doctor.getFullName())
                .title(doctor.getTitle())
                .specialty(doctor.getSpecialty())
                .days(matrix.getDays())
                .shifts(row != null ? row.getSchedules() : emptySevenDays())
                .dayOffs(offs)
                .build();
    }

    private int periodOrder(Period period) {
        if (period == null) return 99;
        return switch (period) {
            case FULL_DAY -> 1;
            case MORNING -> 2;
            case AFTERNOON -> 3;
        };
    }

    private Map<String, List<String>> emptySevenDays() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (String day : List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")) {
            map.put(day, List.of());
        }
        return map;
    }

    // =========================================================================
    // SPRINT 2: NHẮC LỊCH THỦ CÔNG 1 CLICK
    // =========================================================================

    private String getRecipientPhone(Appointment appointment) {
        if (appointment == null) return null;
        if (appointment.getBookerPhone() != null && !appointment.getBookerPhone().isBlank()) {
            return appointment.getBookerPhone();
        }
        if (appointment.getPatient() != null && appointment.getPatient().getPhone() != null) {
            return appointment.getPatient().getPhone();
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public ReminderPreviewDto getReminderPreview(Long id) {
        if (id == null) {
            throw AppException.badRequest("Mã phiếu khám không hợp lệ");
        }
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy phiếu khám với ID: " + id));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED || appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw AppException.unprocessable("Phiếu đã huỷ/hoàn tất, không cần nhắc");
        }

        String rawPhone = getRecipientPhone(appointment);
        String maskedPhone = PhoneValidator.mask(rawPhone);

        ManualProvider provider = this.manualProvider != null ? this.manualProvider : new ManualProvider(null);
        String content = provider.compose(appointment);

        String patientName = appointment.getPatient() != null ? appointment.getPatient().getFullName() : appointment.getBookerName();

        return new ReminderPreviewDto(
                appointment.getId(),
                maskedPhone,
                content,
                ManualProvider.CHANNEL,
                appointment.getBookingCode(),
                patientName,
                rawPhone
        );
    }

    @Override
    @Transactional
    public ReminderMarkSentResponse markReminderSent(User staff, Long id, ReminderMarkSentRequest request) {
        if (id == null) {
            throw AppException.badRequest("Mã phiếu khám không hợp lệ");
        }
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy phiếu khám với ID: " + id));

        String rawPhone = getRecipientPhone(appointment);
        String maskedPhone = PhoneValidator.mask(rawPhone);
        if (maskedPhone == null || maskedPhone.isBlank()) {
            maskedPhone = "---";
        }

        LocalDateTime now = LocalDateTime.now(clock);

        NotificationLog logEntry = NotificationLog.builder()
                .appointment(appointment)
                .channel(ManualProvider.CHANNEL)
                .template("REMINDER_MANUAL")
                .recipientMasked(maskedPhone)
                .status(DeliveryResult.Status.SENT.name())
                .errorCode(null)
                .provider("manual")
                .createdAt(now)
                .build();

        if (notificationLogRepository != null) {
            notificationLogRepository.save(logEntry);
        }

        String sentIso = ContractMapper.formatIso(now);
        log.info("Lễ tân {} đã xác nhận gửi nhắc lịch cho phiếu {} lúc {}",
                staff != null ? staff.getUsername() : "system",
                appointment.getBookingCode(),
                sentIso);

        return new ReminderMarkSentResponse(appointment.getId(), sentIso);
    }

    @Override
    @Transactional(readOnly = true)
    public BulkReminderPreviewResponse bulkReminderPreview(BulkReminderPreviewRequest request) {
        List<Long> ids = request != null && request.ids() != null ? request.ids() : List.of();
        if (ids.isEmpty()) {
            return new BulkReminderPreviewResponse(List.of(), List.of());
        }

        List<ReminderPreviewDto> items = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        ManualProvider provider = this.manualProvider != null ? this.manualProvider : new ManualProvider(null);

        for (Long id : ids) {
            if (id == null) continue;
            Optional<Appointment> opt = appointmentRepository.findById(id);
            if (opt.isEmpty()) {
                warnings.add("Không tìm thấy phiếu khám có ID: " + id);
                continue;
            }
            Appointment a = opt.get();
            if (a.getStatus() == AppointmentStatus.CANCELLED || a.getStatus() == AppointmentStatus.COMPLETED) {
                warnings.add("Phiếu " + (a.getBookingCode() != null ? a.getBookingCode() : id) + " đã huỷ/hoàn tất, không cần nhắc");
                continue;
            }

            String rawPhone = getRecipientPhone(a);
            String maskedPhone = PhoneValidator.mask(rawPhone);
            String content = provider.compose(a);
            String patientName = a.getPatient() != null ? a.getPatient().getFullName() : a.getBookerName();

            items.add(new ReminderPreviewDto(
                    a.getId(),
                    maskedPhone,
                    content,
                    ManualProvider.CHANNEL,
                    a.getBookingCode(),
                    patientName,
                    rawPhone
            ));
        }

        return new BulkReminderPreviewResponse(items, warnings);
    }

    @Override
    @Transactional
    public BulkReminderMarkSentResponse bulkReminderMarkSent(User staff, BulkReminderMarkSentRequest request) {
        List<Long> ids = request != null && request.ids() != null ? request.ids() : List.of();
        if (ids.isEmpty()) {
            return new BulkReminderMarkSentResponse(List.of(), ContractMapper.formatIso(LocalDateTime.now(clock)));
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> updatedIds = new ArrayList<>();

        for (Long id : ids) {
            if (id == null) continue;
            Optional<Appointment> opt = appointmentRepository.findById(id);
            if (opt.isEmpty()) continue;
            Appointment a = opt.get();

            String rawPhone = getRecipientPhone(a);
            String maskedPhone = PhoneValidator.mask(rawPhone);
            if (maskedPhone == null || maskedPhone.isBlank()) {
                maskedPhone = "---";
            }

            NotificationLog logEntry = NotificationLog.builder()
                    .appointment(a)
                    .channel(ManualProvider.CHANNEL)
                    .template("REMINDER_MANUAL")
                    .recipientMasked(maskedPhone)
                    .status(DeliveryResult.Status.SENT.name())
                    .errorCode(null)
                    .provider("manual")
                    .createdAt(now)
                    .build();

            if (notificationLogRepository != null) {
                notificationLogRepository.save(logEntry);
            }
            updatedIds.add(id);
        }

        String sentIso = ContractMapper.formatIso(now);
        return new BulkReminderMarkSentResponse(updatedIds, sentIso);
    }
}

