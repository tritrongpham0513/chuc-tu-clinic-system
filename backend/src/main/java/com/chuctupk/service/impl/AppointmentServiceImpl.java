package com.chuctupk.service.impl;

import com.chuctupk.dto.request.AppointmentCancelRequest;
import com.chuctupk.dto.request.AppointmentCreateRequest;
import com.chuctupk.dto.response.AppointmentResponse;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentSource;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.DayOfWeek;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.*;
import com.chuctupk.service.AppointmentService;
import com.chuctupk.service.ClinicSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private static final String NOT_FOUND_MESSAGE = "Không tìm thấy lịch hẹn";
    private static final Random RANDOM = new SecureRandom();

    public static final String KEY_PATIENT_SEQ_MUTEX = "sys_patient_seq_mutex";

    private final AppointmentRepository appointmentRepository;
    private final AppointmentStatusHistoryRepository statusHistoryRepository;
    private final PatientRepository patientRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorDayOffRepository doctorDayOffRepository;
    private final ClinicHolidayRepository clinicHolidayRepository;
    private final ClinicServiceRepository clinicServiceRepository;
    private final ClinicSettingRepository clinicSettingRepository;
    private final ClinicSettingService clinicSettingService;
    private final Clock clock;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentResponse createAppointment(AppointmentCreateRequest request) {
        LocalDate appointmentDate = request.getAppointmentDate();
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);

        // 1. Kiểm tra ngày quá khứ
        if (appointmentDate.isBefore(today)) {
            throw AppException.unprocessable("Không thể đặt lịch vào ngày trong quá khứ");
        }

        // 2. Kiểm tra số ngày đặt trước tối đa
        int advanceDays = clinicSettingService.getBookingAdvanceDays();
        if (appointmentDate.isAfter(today.plusDays(advanceDays))) {
            throw AppException.unprocessable("Chỉ có thể đặt lịch trước tối đa " + advanceDays + " ngày");
        }

        // 3. Kiểm tra ngày nghỉ lễ
        if (clinicHolidayRepository.existsByHolidayDateAndIsClosedTrue(appointmentDate)) {
            throw AppException.unprocessable("Phòng khám nghỉ lễ vào ngày này");
        }

        // 4. Khóa bi quan dòng time_slot để chống trùng lịch đồng thời (PESSIMISTIC_WRITE)
        TimeSlot slot = timeSlotRepository.findByIdWithLock(request.getTimeSlotId())
                .orElseThrow(() -> AppException.notFound("Khung giờ khám không tồn tại"));

        if (!Boolean.TRUE.equals(slot.getIsActive())) {
            throw AppException.unprocessable("Khung giờ này hiện không hoạt động");
        }

        // 5. Kiểm tra thời gian tối thiểu trước giờ khám nếu đặt cho ngày hôm nay
        int minHours = clinicSettingService.getBookingMinHours();
        if (appointmentDate.isEqual(today)) {
            LocalTime cutoffTime = now.plusHours(minHours);
            if (slot.getStartTime().isBefore(cutoffTime)) {
                throw AppException.unprocessable("Đã quá thời gian nhận lịch cho khung giờ này (cần đặt trước ít nhất " + minHours + " giờ)");
            }
        }

        DayOfWeek dow = DayOfWeek.from(appointmentDate.getDayOfWeek());

        // 6. Xác định danh sách bác sĩ trực ca
        List<Long> scheduledDoctorIds = doctorScheduleRepository.findScheduledDoctorIds(dow, slot.getPeriod());
        List<Long> offDoctorIds = doctorDayOffRepository.findDoctorIdsOffOnDateAndPeriod(appointmentDate, slot.getPeriod());
        List<Long> activeDoctorIds = scheduledDoctorIds.stream()
                .filter(id -> !offDoctorIds.contains(id))
                .toList();

        // Slot không có bác sĩ trực thì không nhận lịch (422)
        if (activeDoctorIds.isEmpty()) {
            throw AppException.unprocessable("Khung giờ này không có bác sĩ trực khám");
        }

        Doctor selectedDoctor = null;
        if (request.getDoctorId() != null) {
            selectedDoctor = doctorRepository.findById(request.getDoctorId())
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy bác sĩ"));

            if (!Boolean.TRUE.equals(selectedDoctor.getIsActive())) {
                throw AppException.unprocessable("Bác sĩ hiện không nhận lịch khám");
            }

            if (!activeDoctorIds.contains(selectedDoctor.getId())) {
                throw AppException.unprocessable("Bác sĩ không có lịch khám trong khung giờ này");
            }

            // Kiểm tra điều kiện (b): lịch chưa hủy của riêng bác sĩ đó <= max_patients
            long doctorBooked = appointmentRepository.countBookedSlotsForDoctor(
                    appointmentDate, selectedDoctor.getId(), slot.getId(), AppointmentStatus.CANCELLED);
            if (doctorBooked >= slot.getMaxPatients()) {
                throw AppException.conflict("Khung giờ này của bác sĩ đã đầy, vui lòng chọn giờ hoặc bác sĩ khác");
            }
        }

        // Kiểm tra điều kiện (a): <=tổng lịch chưa hủy của slot trong ngày  max_patients x số bác sĩ trực ca
        int totalCapacity = slot.getMaxPatients() * activeDoctorIds.size();
        long totalBooked = appointmentRepository.countBookedSlotsTotal(
                appointmentDate, slot.getId(), AppointmentStatus.CANCELLED);
        if (totalBooked >= totalCapacity) {
            throw AppException.conflict("Khung giờ này đã đầy, vui lòng chọn giờ khác");
        }

        // 7. Kiểm tra dịch vụ khám (nếu chọn)
        ClinicService selectedService = null;
        if (request.getServiceId() != null) {
            selectedService = clinicServiceRepository.findById(request.getServiceId())
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy dịch vụ"));
            if (!Boolean.TRUE.equals(selectedService.getIsActive())) {
                throw AppException.unprocessable("Dịch vụ này hiện không hoạt động");
            }
        }

        // 8. Tìm hoặc tạo hồ sơ bệnh nhân
        Patient patient = findOrCreatePatient(request);

        // 9. Kiểm tra bệnh nhân đã có lịch trong cùng ngày, cùng khung giờ (chưa hủy)
        boolean hasDuplicate = appointmentRepository.existsByPatientIdAndAppointmentDateAndTimeSlotIdAndStatusNot(
                patient.getId(), appointmentDate, slot.getId(), AppointmentStatus.CANCELLED);
        if (hasDuplicate) {
            throw AppException.conflict("Bệnh nhân đã có lịch hẹn trong khung giờ này");
        }

        // 10. Sinh mã hẹn PKCT-YYYYMMDD-XXXX (ngẫu nhiên 4 số, thử lại 5 lần)
        String bookingCode = generateUniqueBookingCode(appointmentDate);

        // 11. Lưu lịch hẹn mới
        Appointment appointment = Appointment.builder()
                .bookingCode(bookingCode)
                .patient(patient)
                .doctor(selectedDoctor)
                .service(selectedService)
                .appointmentDate(appointmentDate)
                .timeSlot(slot)
                .symptoms(request.getSymptoms())
                .bookerName(request.getBookerName())
                .bookerPhone(cleanPhone(request.getBookerPhone()))
                .source(AppointmentSource.WEB)
                .status(AppointmentStatus.PENDING)
                .build();

        appointment = appointmentRepository.save(appointment);

        // 12. Ghi 1 bản ghi vào lịch sử trạng thái
        statusHistoryRepository.save(AppointmentStatusHistory.builder()
                .appointment(appointment)
                .oldStatus(null)
                .newStatus(AppointmentStatus.PENDING.name())
                .reason("Bệnh nhân đăng ký khám trực tuyến qua website")
                .changedBy(null)
                .build());

        log.info("Đặt lịch thành công mã: {}", bookingCode);
        return toResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentByBookingCode(String bookingCode, String xPhone) {
        String cleanPhone = validateAndCleanPhone(xPhone);

        Appointment appointment = appointmentRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> AppException.notFound(NOT_FOUND_MESSAGE));

        // So khớp SĐT: phải khớp với SĐT bệnh nhân hoặc SĐT người đặt hộ
        boolean matchPatient = cleanPhone.equals(cleanPhone(appointment.getPatient().getPhone()));
        boolean matchBooker = appointment.getBookerPhone() != null && cleanPhone.equals(cleanPhone(appointment.getBookerPhone()));

        if (!matchPatient && !matchBooker) {
            // Trả cùng thông báo 404 để chống dò mã hẹn của người khác
            throw AppException.notFound(NOT_FOUND_MESSAGE);
        }

        return toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse cancelAppointment(String bookingCode, String xPhone, AppointmentCancelRequest request) {
        String cleanPhone = validateAndCleanPhone(xPhone);

        Appointment appointment = appointmentRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> AppException.notFound(NOT_FOUND_MESSAGE));

        boolean matchPatient = cleanPhone.equals(cleanPhone(appointment.getPatient().getPhone()));
        boolean matchBooker = appointment.getBookerPhone() != null && cleanPhone.equals(cleanPhone(appointment.getBookerPhone()));

        if (!matchPatient && !matchBooker) {
            throw AppException.notFound(NOT_FOUND_MESSAGE);
        }

        // Chỉ cho phép hủy khi đang PENDING hoặc CONFIRMED
        if (appointment.getStatus() != AppointmentStatus.PENDING && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
                throw AppException.conflict("Lịch hẹn này đã được hủy trước đó");
            } else if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
                throw AppException.conflict("Lịch hẹn này đã được khám hoàn tất, không thể hủy");
            } else if (appointment.getStatus() == AppointmentStatus.IN_PROGRESS) {
                throw AppException.conflict("Lịch hẹn đang trong quá trình khám, không thể hủy");
            } else {
                throw AppException.conflict("Lịch hẹn hiện không ở trạng thái cho phép hủy");
            }
        }

        // Kiểm tra thời gian: Cách giờ hẹn dưới 2 giờ thì không cho hủy online
        LocalDateTime appointmentDateTime = LocalDateTime.of(
                appointment.getAppointmentDate(),
                appointment.getTimeSlot().getStartTime()
        );
        LocalDateTime currentDateTime = LocalDateTime.now(clock);

        if (currentDateTime.plusHours(2).isAfter(appointmentDateTime)) {
            throw AppException.unprocessable("Đã sát giờ khám, vui lòng gọi hotline để hủy");
        }

        String oldStatus = appointment.getStatus().name();
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(request.getCancelReason());
        appointment = appointmentRepository.save(appointment);

        // Ghi lịch sử đổi trạng thái
        statusHistoryRepository.save(AppointmentStatusHistory.builder()
                .appointment(appointment)
                .oldStatus(oldStatus)
                .newStatus(AppointmentStatus.CANCELLED.name())
                .reason(request.getCancelReason() != null ? request.getCancelReason() : "Bệnh nhân hủy qua website")
                .changedBy(null)
                .build());

        log.info("Đã hủy lịch hẹn mã: {}", bookingCode);
        return toResponse(appointment);
    }

    /**
     * Tìm bệnh nhân theo (phone, fullName, yearOfBirth) với so khớp chính xác trong Java,
     * nếu chưa có thì tạo mới an toàn đồng thời bằng khóa bi quan sequence trong transaction.
     */
    private Patient findOrCreatePatient(AppointmentCreateRequest req) {
        String cleanPhone = cleanPhone(req.getPhone());
        String normalizedName = req.getFullName().trim().replaceAll("\\s+", " ");

        // 1. Tìm danh sách bệnh nhân cùng SĐT
        List<Patient> patients = patientRepository.findByPhone(cleanPhone);
        for (Patient p : patients) {
            String existingName = p.getFullName() != null ? p.getFullName().trim().replaceAll("\\s+", " ") : "";
            if (existingName.equals(normalizedName) && p.getYearOfBirth().equals(req.getYearOfBirth())) {
                return p;
            }
        }

        // 2. Chưa có -> sinh mã tuần tự an toàn đồng thời qua khóa bi quan trên clinic_settings
        String patientCode = generateNextPatientCode();

        Patient newPatient = Patient.builder()
                .patientCode(patientCode)
                .fullName(normalizedName)
                .phone(cleanPhone)
                .yearOfBirth(req.getYearOfBirth())
                .gender(req.getGender())
                .address(req.getAddress())
                .relationship(req.getRelationship())
                .build();

        return patientRepository.save(newPatient);
    }

    /**
     * Sinh mã BN-YYYY-XXXX tuần tự trong transaction đã khóa (PESSIMISTIC_WRITE).
     * 
     * THỨ TỰ KHÓA TRÁNH DEADLOCK:
     * 1. Khóa bảng time_slots trước (đã thực hiện ở Bước 4 qua timeSlotRepository.findByIdWithLock).
     * 2. Khóa bảng clinic_settings sau (qua clinicSettingRepository.findBySettingKeyWithLock).
     * Tuyệt đối không đảo ngược thứ tự 2 khóa này để tránh deadlock giữa các luồng đồng thời.
     * 
     * BỘ ĐẾM NĂM MỚI (YEAR ROLLOVER):
     * Nếu sang năm mới (ví dụ 31/12 sang 01/01), bộ đếm patient_seq_YYYY chưa có trong database,
     * hệ thống sẽ khóa dòng anchor KEY_BOOKING_ADVANCE_DAYS để tuần tự hóa việc tạo mới bộ đếm
     * ngay trong code đã khóa, không cần khởi động lại server.
     * 
     * QUY TẮC BẢO MẬT & CMS:
     * Toàn bộ các khóa bắt đầu bằng "patient_seq_" trong bảng clinic_settings là dữ liệu nội bộ
     * phục vụ sinh mã hệ thống. Mọi API / CMS quản trị cấu hình phòng khám sau này PHẢI LOẠI TRỪ
     * (filter out / ẩn) các khóa này để tránh người dùng hay nhân viên chỉnh sửa sai lệch.
     */
    @Override
    public String generateNextPatientCode() {
        return generateNextPatientCode(LocalDate.now(clock).getYear());
    }

    public String generateNextPatientCode(int year) {
        String seqKey = "patient_seq_" + year;

        // 1. Thử khóa bi quan dòng sequence của năm
        Optional<ClinicSetting> optionalSeq = clinicSettingRepository.findBySettingKeyWithLock(seqKey);

        ClinicSetting seq;
        if (optionalSeq.isPresent()) {
            seq = optionalSeq.get();
        } else {
            // 2. Sang năm mới hoặc chưa có bộ đếm: Khóa mutex chuyên dụng nội bộ (tự phục hồi nếu bị xóa)
            lockPatientSeqMutex();
            seq = clinicSettingRepository.findBySettingKeyWithLock(seqKey)
                    .orElseGet(() -> {
                        long count = patientRepository.countByPatientCodeStartingWith("BN-" + year + "-");
                        ClinicSetting newSeq = ClinicSetting.builder()
                                .settingKey(seqKey)
                                .settingValue(String.valueOf(count))
                                .description("Bộ đếm mã hồ sơ bệnh nhân năm " + year)
                                .build();
                        return clinicSettingRepository.saveAndFlush(newSeq);
                    });
        }

        long next = Long.parseLong(seq.getSettingValue().trim()) + 1;
        seq.setSettingValue(String.valueOf(next));
        clinicSettingRepository.save(seq);

        return String.format("BN-%d-%04d", year, next);
    }

    private ClinicSetting lockPatientSeqMutex() {
        Optional<ClinicSetting> mutexOpt = clinicSettingRepository.findBySettingKeyWithLock(KEY_PATIENT_SEQ_MUTEX);
        if (mutexOpt.isPresent()) {
            return mutexOpt.get();
        }
        // Chưa có mutex hoặc đã bị xóa -> tự tạo mới (self-healing)
        try {
            ClinicSetting newMutex = ClinicSetting.builder()
                    .settingKey(KEY_PATIENT_SEQ_MUTEX)
                    .settingValue("1")
                    .description("Khóa mutex nội bộ sinh mã bệnh nhân an toàn đồng thời")
                    .build();
            clinicSettingRepository.saveAndFlush(newMutex);
        } catch (DataIntegrityViolationException ignored) {
            // Đã có luồng khác insert đồng thời
        }
        return clinicSettingRepository.findBySettingKeyWithLock(KEY_PATIENT_SEQ_MUTEX)
                .orElseThrow(() -> AppException.internal("Không thể khởi tạo khóa sinh mã bệnh nhân"));
    }

    private String generateUniqueBookingCode(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD
        for (int i = 0; i < 5; i++) {
            int randomNum = RANDOM.nextInt(9000) + 1000; // 4 chữ số (1000 - 9999)
            String code = "PKCT-" + dateStr + "-" + randomNum;
            if (!appointmentRepository.existsByBookingCode(code)) {
                return code;
            }
        }
        throw AppException.internal("Không thể sinh mã đặt lịch ngẫu nhiên, vui lòng thử lại");
    }

    private String cleanPhone(String phone) {
        if (phone == null) return null;
        return phone.replaceAll("\\s+", "").trim();
    }

    private String validateAndCleanPhone(String xPhone) {
        if (xPhone == null || xPhone.isBlank()) {
            throw AppException.badRequest("Thiếu tiêu đề bắt buộc X-Phone");
        }
        return cleanPhone(xPhone);
    }

    private AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder()
                .bookingCode(a.getBookingCode())
                .patientName(a.getPatient().getFullName())
                .appointmentDate(a.getAppointmentDate())
                .timeSlot(AppointmentResponse.TimeSlotSummary.builder()
                        .id(a.getTimeSlot().getId())
                        .label(a.getTimeSlot().getSlotLabel())
                        .startTime(a.getTimeSlot().getStartTime())
                        .endTime(a.getTimeSlot().getEndTime())
                        .build())
                .doctor(a.getDoctor() != null ? AppointmentResponse.DoctorSummary.builder()
                        .id(a.getDoctor().getId())
                        .fullName(a.getDoctor().getFullName())
                        .build() : null)
                .service(a.getService() != null ? AppointmentResponse.ServiceSummary.builder()
                        .id(a.getService().getId())
                        .name(a.getService().getName())
                        .build() : null)
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .build();
    }
}

