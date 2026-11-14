package com.chuctupk.concurrency;

import com.chuctupk.dto.request.AppointmentCreateRequest;
import com.chuctupk.dto.response.AppointmentResponse;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.DayOfWeek;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.*;
import com.chuctupk.service.AppointmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Kiểm thử đồng thời: KHÔNG gắn @Transactional ở cấp lớp test.
 * Mỗi luồng chạy và commit transaction thật trên MySQL để kiểm chứng khóa bi quan @Lock(PESSIMISTIC_WRITE).
 * Tự dọn sạch dữ liệu trong @AfterEach.
 */
@SpringBootTest
@ActiveProfiles("test")
public class AppointmentConcurrencyTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Clock clock;

    private TimeSlot testSlot;
    private Doctor testDoctor;
    private DoctorSchedule testSchedule;
    private User testUser;
    private LocalDate targetDate;
    private final List<String> createdBookingCodes = Collections.synchronizedList(new ArrayList<>());
    private final List<Long> createdPatientIds = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void setUp() {
        targetDate = LocalDate.now(clock).plusDays(5);
        DayOfWeek dow = DayOfWeek.from(targetDate.getDayOfWeek());

        // 1. Tạo khung giờ chiều riêng cho bài kiểm thử với sức chứa 3 bệnh nhân
        testSlot = timeSlotRepository.findAll().stream()
                .filter(s -> s.getPeriod() == Period.AFTERNOON
                        && s.getStartTime().equals(LocalTime.of(12, 0)))
                .findFirst()
                .orElseGet(() -> timeSlotRepository.save(TimeSlot.builder()
                        .slotLabel("12:00 - 13:00")
                        .startTime(LocalTime.of(12, 0))
                        .endTime(LocalTime.of(13, 0))
                        .period(Period.AFTERNOON)
                        .maxPatients(3)
                        .sortOrder(99)
                        .isActive(true)
                        .build()));
        testSlot.setMaxPatients(3);
        testSlot.setIsActive(true);
        testSlot = timeSlotRepository.save(testSlot);

        // 2. Tạo bác sĩ trực ca
        testUser = userRepository.save(User.builder()
                .username("bs_concur_" + System.currentTimeMillis())
                .passwordHash("secret")
                .fullName("Bác Sĩ Kiểm Thử Đồng Thời")
                .phone("0988001122")
                .isActive(true)
                .build());

        testDoctor = doctorRepository.save(Doctor.builder()
                .user(testUser)
                .fullName("Bác Sĩ Kiểm Thử Đồng Thời")
                .specialty("Châm cứu trị liệu")
                .phone("0988001122")
                .isActive(true)
                .build());

        testSchedule = doctorScheduleRepository.save(DoctorSchedule.builder()
                .doctor(testDoctor)
                .dayOfWeek(dow)
                .period(Period.FULL_DAY)
                .isActive(true)
                .build());
    }

    @AfterEach
    void tearDown() {
        // Dọn sạch dữ liệu sinh ra bởi test
        for (String code : createdBookingCodes) {
            appointmentRepository.findByBookingCode(code).ifPresent(app -> {
                statusHistoryRepository.deleteAll(statusHistoryRepository.findByAppointmentIdOrderByChangedAtAsc(app.getId()));
                appointmentRepository.delete(app);
            });
        }
        for (Long pid : createdPatientIds) {
            patientRepository.deleteById(pid);
        }

        if (testSchedule != null) doctorScheduleRepository.delete(testSchedule);
        if (testDoctor != null) doctorRepository.delete(testDoctor);
        if (testUser != null) userRepository.delete(testUser);
        if (testSlot != null && "12:00 - 13:00".equals(testSlot.getSlotLabel())) {
            timeSlotRepository.delete(testSlot);
        }
    }

    @Test
    @DisplayName("Đồng thời: 10 luồng cùng đặt 1 slot max_patients=3 -> đúng 3 thành công (201), 7 nhận 409")
    void testConcurrentBooking_ExactThreeSucceed_SevenConflict() throws InterruptedException {
        int totalThreads = 10;
        int maxCapacity = 3;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherErrors = new AtomicInteger(0);

        for (int i = 1; i <= totalThreads; i++) {
            final int index = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Chờ tất cả 10 luồng sẵn sàng để bắn cùng lúc

                    AppointmentCreateRequest req = AppointmentCreateRequest.builder()
                            .fullName("Bệnh nhân ĐT " + index)
                            .phone("09770000" + String.format("%02d", index))
                            .yearOfBirth(1960 + index)
                            .gender(index % 2 == 0 ? Gender.FEMALE : Gender.MALE)
                            .appointmentDate(targetDate)
                            .timeSlotId(testSlot.getId())
                            .doctorId(testDoctor.getId())
                            .build();

                    AppointmentResponse res = appointmentService.createAppointment(req);
                    successCount.incrementAndGet();
                    createdBookingCodes.add(res.getBookingCode());
                    appointmentRepository.findByBookingCode(res.getBookingCode())
                            .ifPresent(a -> createdPatientIds.add(a.getPatient().getId()));

                } catch (AppException e) {
                    if (e.getStatusCode() == 409) {
                        conflictCount.incrementAndGet();
                    } else {
                        System.err.println("Unexpected AppException: " + e.getStatusCode() + " - " + e.getMessage());
                        otherErrors.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("Unexpected Exception: " + e.getClass().getName() + " - " + e.getMessage());
                    otherErrors.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown(); // BẮT ĐẦU CÙNG LÚC
        doneLatch.await();
        executor.shutdown();

        // Kiểm tra kết quả
        assertEquals(maxCapacity, successCount.get(), "Chỉ được phép có đúng 3 yêu cầu đặt lịch thành công");
        assertEquals(totalThreads - maxCapacity, conflictCount.get(), "7 yêu cầu còn lại phải nhận mã 409 Conflict");
        assertEquals(0, otherErrors.get(), "Không được có lỗi không mong muốn khác");
    }
}

