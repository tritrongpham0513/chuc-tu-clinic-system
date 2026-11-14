package com.chuctupk.service.impl;

import com.chuctupk.common.mapping.ContractMapper;
import com.chuctupk.dto.request.CancelAppointmentStaffRequest;
import com.chuctupk.dto.request.RescheduleAppointmentRequest;
import com.chuctupk.dto.request.StaffCreateAppointmentRequest;
import com.chuctupk.dto.response.*;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentSource;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.CheckinStatus;
import com.chuctupk.entity.enums.PriorityGroup;
import com.chuctupk.exception.AppException;
import com.chuctupk.exception.ErrorCode;
import com.chuctupk.repository.*;
import com.chuctupk.service.ClinicSettingService;
import com.chuctupk.service.SequenceGeneratorService;
import com.chuctupk.service.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final AppointmentRepository appointmentRepository;
    private final CheckinRepository checkinRepository;
    private final PatientRepository patientRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentSeatRepository appointmentSeatRepository;
    private final ClinicSettingService clinicSettingService;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final Clock clock;

    private static final String[] DEFAULT_DOCTOR_NAMES = {
            "BS. Nguyễn Văn Minh",
            "Lương y Chúc Từ",
            "Lương y Trần Văn C",
            "Lương y Lê Thị D",
            "Lương y Hoàng E"
    };

    @Override
    @Transactional(readOnly = true)
    public StaffAppointmentListResponse getAppointments(LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now(clock);
        List<Appointment> appts = appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(queryDate);

        List<Long> apptIds = appts.stream().map(Appointment::getId).toList();
        Map<Long, Checkin> checkinMap = apptIds.isEmpty() ? Map.of() :
                checkinRepository.findByAppointmentIdIn(apptIds).stream()
                        .filter(c -> c.getAppointment() != null)
                        .collect(toMap(c -> c.getAppointment().getId(), c -> c, (a, b) -> a));

        List<AppointmentDto> items = appts.stream()
                .map(a -> AppointmentDto.from(a, checkinMap.get(a.getId())))
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
            int capacity = slot.getMaxPatients() != null && slot.getMaxPatients() > 0 ? slot.getMaxPatients() : 6;
            int remaining = Math.max(0, capacity - (int) booked);

            boolean isPast = queryDate.isBefore(today) || (queryDate.equals(today) && slot.getEndTime().isBefore(nowTime));
            boolean isFull = remaining <= 0;
            boolean available = !isPast && !isFull;

            String reason = isPast ? "PAST" : (isFull ? "SLOT_FULL" : null);
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

        if (slotDate.isEqual(today) && !Boolean.TRUE.equals(request.checkInNow())
                && chosenSlot.getEndTime() != null && chosenSlot.getEndTime().isBefore(LocalTime.now(clock))) {
            throw AppException.badRequest("Khung giờ này đã trôi qua. Vui lòng chọn khung giờ hiện tại hoặc kế tiếp");
        }

        // Allocate seat (1..6)
        int capacity = chosenSlot.getMaxPatients() != null && chosenSlot.getMaxPatients() > 0 ? chosenSlot.getMaxPatients() : 6;
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

                    Appointment appt = Appointment.builder()
                            .bookingCode(bookingCode)
                            .patient(patient)
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
                    Appointment nextAppt = Appointment.builder()
                            .bookingCode(bookingCode)
                            .patient(oldAppt.getPatient())
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
        if (nowTime.isBefore(LocalTime.of(8, 0)) || nowTime.isAfter(LocalTime.of(16, 30))) {
            throw AppException.of(ErrorCode.CLOSED, "Phòng khám chỉ tiếp nhận bệnh nhân từ 08:00 đến 16:30");
        }

        if (appt.getStatus() == AppointmentStatus.CANCELLED || appt.getStatus() == AppointmentStatus.COMPLETED) {
            throw AppException.badRequest("Phiếu khám không thể check-in vì đã kết thúc hoặc đã huỷ");
        }
        if (appt.getStatus() == AppointmentStatus.CONFIRMED || appt.getStatus() == AppointmentStatus.IN_PROGRESS) {
            throw AppException.badRequest("Phiếu khám đã được check-in trước đó");
        }

        // Quy tắc nhóm ưu tiên: Phiếu đặt trước (WEB) đến trong [slotStart - 15m, slotStart + 60m]
        PriorityGroup priorityGroup = PriorityGroup.NORMAL;
        if (appt.getSource() == AppointmentSource.WEB && appt.getTimeSlot() != null) {
            int earlyMinutes = 15;
            try {
                earlyMinutes = Integer.parseInt(clinicSettingService.getSettingValue("priority_early_minutes", "15"));
            } catch (Exception ignored) {}

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

        List<Checkin> inRoom = checkinRepository.findInRoomToday(startOfDay, endOfDay);
        if (inRoom.size() >= 5) {
            throw AppException.of(ErrorCode.ROOM_FULL, "Cả 5 ghế khám đều đang có bệnh nhân");
        }

        Set<Integer> occupied = inRoom.stream().map(Checkin::getSeatNo).filter(Objects::nonNull).collect(toSet());
        int freeSeat = 1;
        for (int i = 1; i <= 5; i++) {
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

    @Override
    @Transactional(readOnly = true)
    public QueueStateDto getQueueState() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<Checkin> inRoom = checkinRepository.findInRoomToday(startOfDay, endOfDay);
        List<Checkin> waiting = checkinRepository.findWaitingTodayOrdered(startOfDay, endOfDay);

        long completedCount = appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(today).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .count();

        boolean autoInvite = "1".equals(clinicSettingService.getSettingValue("auto_invite", "0"));

        Map<Integer, Checkin> seatMap = inRoom.stream()
                .filter(c -> c.getSeatNo() != null)
                .collect(toMap(Checkin::getSeatNo, c -> c, (a, b) -> a));

        List<QueueSeatDto> seatDtos = new ArrayList<>();
        for (int s = 1; s <= 5; s++) {
            Checkin c = seatMap.get(s);
            if (c != null && c.getAppointment() != null) {
                Appointment a = c.getAppointment();
                String slotStart = a.getTimeSlot() != null && a.getTimeSlot().getStartTime() != null
                        ? a.getTimeSlot().getStartTime().toString().substring(0, 5) : "08:00";

                seatDtos.add(new QueueSeatDto(
                        s,
                        false,
                        String.valueOf(c.getId()),
                        String.valueOf(a.getId()),
                        a.getBookingCode(),
                        PatientDto.from(a.getPatient()),
                        a.getDoctor() != null ? a.getDoctor().getFullName() : DEFAULT_DOCTOR_NAMES[s - 1],
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
                        DEFAULT_DOCTOR_NAMES[s - 1],
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

            waitingDtos.add(new QueueWaitingDto(
                    String.valueOf(c.getId()),
                    String.valueOf(a.getId()),
                    c.getQueueNo(),
                    a.getBookingCode(),
                    PatientDto.from(a.getPatient()),
                    c.getPriorityGroup().name(),
                    ContractMapper.toFeSource(a.getSource()),
                    slotStart,
                    ContractMapper.formatIso(c.getCheckInTime()),
                    Math.max(0, waited)
            ));
        }

        return new QueueStateDto(
                today.toString(),
                5,
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

        List<Checkin> inRoom = checkinRepository.findInRoomToday(startOfDay, endOfDay);
        if (inRoom.size() >= 5) {
            throw AppException.of(ErrorCode.ROOM_FULL, "Cả 5 ghế khám đều đang có bệnh nhân");
        }

        Set<Integer> occupied = inRoom.stream().map(Checkin::getSeatNo).filter(Objects::nonNull).collect(toSet());
        int freeSeat = 1;
        for (int i = 1; i <= 5; i++) {
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
}

