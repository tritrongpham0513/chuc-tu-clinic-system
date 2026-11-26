package com.chuctupk.controller;

import com.chuctupk.dto.request.CreateHolidayRequest;
import com.chuctupk.dto.request.UpdateHolidayRequest;
import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.ClinicHoliday;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.exception.GlobalExceptionHandler;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.AppointmentSeatRepository;
import com.chuctupk.repository.ClinicHolidayRepository;
import com.chuctupk.repository.TimeSlotRepository;
import com.chuctupk.service.CapacityResolver;
import com.chuctupk.service.ClinicSettingService;
import com.chuctupk.service.SlotPolicy;
import com.chuctupk.service.impl.AdminHolidayServiceImpl;
import com.chuctupk.service.impl.TimeSlotServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Sprint 7 — Admin Holiday Tests (Ticket 1 & Ticket 5)")
class AdminHolidayTest {

    private MockMvc mockMvc;
    private FakeHolidayRepository holidayRepository;
    private FakeAppointmentRepository appointmentRepository;
    private AdminHolidayServiceImpl holidayService;
    private TimeSlotServiceImpl timeSlotService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private User adminUser;
    private User staffUser;

    @BeforeEach
    void setUp() {
        holidayRepository = new FakeHolidayRepository();
        appointmentRepository = new FakeAppointmentRepository();
        holidayService = new AdminHolidayServiceImpl(holidayRepository, appointmentRepository);

        AdminHolidayController controller = new AdminHolidayController(holidayService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        adminUser = User.builder()
                .id(1L)
                .username("admin")
                .fullName("Quản trị viên")
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        staffUser = User.builder()
                .id(2L)
                .username("staff")
                .fullName("Nhân viên tiếp tân")
                .role(Role.STAFF)
                .isActive(true)
                .build();

        Clock fixedClock = Clock.fixed(Instant.parse("2026-08-01T08:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        ClinicSettingService stubSettingService = new StubClinicSettingService();

        timeSlotService = new TimeSlotServiceImpl(
                null,
                null,
                holidayRepository,
                null,
                null,
                stubSettingService,
                null,
                null,
                null,
                fixedClock
        );
    }

    private void authenticateAs(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Test 1: Tạo ngày lễ trùng -> 422 Ngày này đã được khai báo")
    void test1_CreateDuplicateHoliday_Returns422() throws Exception {
        authenticateAs(adminUser);

        // Tạo lần 1 thành công
        CreateHolidayRequest request1 = CreateHolidayRequest.builder()
                .holidayDate(LocalDate.of(2026, 9, 2))
                .name("Quốc khánh")
                .isClosed(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Quốc khánh"));

        // Tạo lần 2 cùng ngày 2026-09-02 -> 422
        CreateHolidayRequest request2 = CreateHolidayRequest.builder()
                .holidayDate(LocalDate.of(2026, 9, 2))
                .name("Kỷ niệm Quốc khánh")
                .isClosed(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Ngày này đã được khai báo")));
    }

    @Test
    @DisplayName("Test 2: Ngày lễ chặn đặt lịch -> 422 khi lấy khung giờ")
    void test2_HolidayBlocksTimeSlots_Returns422() {
        // Khai báo ngày nghỉ lễ 2026-09-02
        holidayRepository.save(ClinicHoliday.builder()
                .holidayDate(LocalDate.of(2026, 9, 2))
                .name("Quốc khánh")
                .isClosed(true)
                .build());

        // Lấy khung giờ vào ngày 2026-09-02 -> Phải ném 422 "Phòng khám nghỉ lễ vào ngày này"
        AppException ex = assertThrows(AppException.class, () ->
                timeSlotService.getAvailableTimeSlots(LocalDate.of(2026, 9, 2), null)
        );

        assertEquals("Phòng khám nghỉ lễ vào ngày này", ex.getMessage());
        assertEquals(422, ex.getStatusCode());
    }

    @Test
    @DisplayName("Test 3: Sửa ngày lễ (name, isClosed) -> 200 OK")
    void test3_UpdateHoliday_Returns200() throws Exception {
        authenticateAs(adminUser);

        ClinicHoliday saved = holidayRepository.save(ClinicHoliday.builder()
                .holidayDate(LocalDate.of(2026, 4, 30))
                .name("Giải phóng miền Nam")
                .isClosed(true)
                .build());

        UpdateHolidayRequest update = UpdateHolidayRequest.builder()
                .name("Ngày Thống nhất đất nước")
                .isClosed(true)
                .build();

        mockMvc.perform(patch("/api/v1/admin/holidays/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Ngày Thống nhất đất nước"));
    }

    @Test
    @DisplayName("Test 4: Xoá ngày lễ -> 200 OK")
    void test4_DeleteHoliday_Returns200() throws Exception {
        authenticateAs(adminUser);

        ClinicHoliday saved = holidayRepository.save(ClinicHoliday.builder()
                .holidayDate(LocalDate.of(2026, 5, 1))
                .name("Quốc tế Lao động")
                .isClosed(true)
                .build());

        mockMvc.perform(delete("/api/v1/admin/holidays/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals(Optional.empty(), holidayRepository.findById(saved.getId()));
    }

    @Test
    @DisplayName("Test 5: STAFF gọi POST /api/v1/admin/holidays -> 403 Forbidden")
    void test5_StaffCallCreateHoliday_Returns403() throws Exception {
        authenticateAs(staffUser);

        CreateHolidayRequest request = CreateHolidayRequest.builder()
                .holidayDate(LocalDate.of(2026, 12, 25))
                .name("Giáng sinh")
                .isClosed(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện thao tác này"));
    }

    @Test
    @DisplayName("Test 6: Ngày trong quá khứ -> vẫn cho phép tạo để ghi nhận lịch sử")
    void test6_PastDateHoliday_Allowed() throws Exception {
        authenticateAs(adminUser);

        CreateHolidayRequest request = CreateHolidayRequest.builder()
                .holidayDate(LocalDate.of(2025, 1, 1))
                .name("Tết Dương lịch 2025")
                .isClosed(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Tết Dương lịch 2025"));
    }

    // --- Fake Stubs ---
    static class FakeHolidayRepository implements ClinicHolidayRepository {
        Map<Long, ClinicHoliday> storage = new LinkedHashMap<>();
        long seq = 1L;

        @Override
        public Optional<ClinicHoliday> findByHolidayDate(LocalDate holidayDate) {
            return storage.values().stream()
                    .filter(h -> h.getHolidayDate().equals(holidayDate))
                    .findFirst();
        }

        @Override
        public boolean existsByHolidayDateAndIsClosedTrue(LocalDate holidayDate) {
            return storage.values().stream()
                    .anyMatch(h -> h.getHolidayDate().equals(holidayDate) && Boolean.TRUE.equals(h.getIsClosed()));
        }

        @Override
        public boolean existsByHolidayDate(LocalDate holidayDate) {
            return storage.values().stream().anyMatch(h -> h.getHolidayDate().equals(holidayDate));
        }

        @Override
        public List<ClinicHoliday> findAllByOrderByHolidayDateAsc() {
            return storage.values().stream()
                    .sorted(Comparator.comparing(ClinicHoliday::getHolidayDate))
                    .toList();
        }

        @Override
        public List<ClinicHoliday> findByHolidayDateBetweenOrderByHolidayDateAsc(LocalDate start, LocalDate end) {
            return storage.values().stream()
                    .filter(h -> !h.getHolidayDate().isBefore(start) && !h.getHolidayDate().isAfter(end))
                    .sorted(Comparator.comparing(ClinicHoliday::getHolidayDate))
                    .toList();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <S extends ClinicHoliday> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(seq++);
            }
            storage.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<ClinicHoliday> findById(Long aLong) {
            return Optional.ofNullable(storage.get(aLong));
        }

        @Override
        public void delete(ClinicHoliday entity) {
            storage.remove(entity.getId());
        }

        @Override public void flush() {}
        @Override public <S extends ClinicHoliday> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends ClinicHoliday> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<ClinicHoliday> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public ClinicHoliday getOne(Long aLong) { return storage.get(aLong); }
        @Override public ClinicHoliday getById(Long aLong) { return storage.get(aLong); }
        @Override public ClinicHoliday getReferenceById(Long aLong) { return storage.get(aLong); }
        @Override public <S extends ClinicHoliday> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends ClinicHoliday> List<S> findAll(Example<S> example) { return List.of(); }
        @Override public <S extends ClinicHoliday> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
        @Override public <S extends ClinicHoliday> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends ClinicHoliday> long count(Example<S> example) { return 0; }
        @Override public <S extends ClinicHoliday> boolean exists(Example<S> example) { return false; }
        @Override public <S extends ClinicHoliday, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends ClinicHoliday> List<S> saveAll(Iterable<S> entities) { return null; }
        @Override public List<ClinicHoliday> findAll() { return new ArrayList<>(storage.values()); }
        @Override public List<ClinicHoliday> findAllById(Iterable<Long> longs) { return List.of(); }
        @Override public boolean existsById(Long aLong) { return storage.containsKey(aLong); }
        @Override public long count() { return storage.size(); }
        @Override public void deleteById(Long aLong) { storage.remove(aLong); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends ClinicHoliday> entities) {}
        @Override public void deleteAll() { storage.clear(); }
        @Override public List<ClinicHoliday> findAll(Sort sort) { return List.of(); }
        @Override public Page<ClinicHoliday> findAll(Pageable pageable) { return null; }
    }

    static class FakeAppointmentRepository implements AppointmentRepository {
        @Override public Optional<Appointment> findWithDetailById(Long id) { return Optional.empty(); }
        @Override public long countByAppointmentDateAndStatusNot(LocalDate appointmentDate, AppointmentStatus excludeStatus) { return 0; }
        @Override public boolean isServiceUsed(Long serviceId, String slug, String slugUnderscore) { return false; }
        @Override public long countAppointmentsForService(Long serviceId, String slug, String slugUnderscore) { return 0; }
        @Override public Optional<Appointment> findByBookingCode(String bookingCode) { return Optional.empty(); }
        @Override public long countByAppointmentDate(LocalDate appointmentDate) { return 0; }
        @Override public boolean existsByBookingCode(String bookingCode) { return false; }
        @Override public List<Appointment> findByAppointmentDateOrderByTimeSlotStartTimeAsc(LocalDate appointmentDate) { return List.of(); }
        @Override public List<Appointment> findByAppointmentDateBetween(LocalDate start, LocalDate end) { return List.of(); }
        @Override public List<Appointment> findByPatientPhoneOrderByAppointmentDateDesc(String phone) { return List.of(); }
        @Override public List<Appointment> findByStatusOrderByAppointmentDateDesc(AppointmentStatus status) { return List.of(); }
        @Override public boolean existsByPatientIdAndAppointmentDateAndTimeSlotIdAndStatusNot(Long patientId, LocalDate appointmentDate, Long timeSlotId, AppointmentStatus excludeStatus) { return false; }
        @Override public long countBookedSlotsForDoctor(LocalDate date, Long doctorId, Long timeSlotId, AppointmentStatus excludeStatus) { return 0; }
        @Override public long countBookedSlotsTotal(LocalDate date, Long timeSlotId, AppointmentStatus excludeStatus) { return 0; }
        @Override public boolean existsByPatientId(Long patientId) { return false; }
        @Override public List<Appointment> findByPatientId(Long patientId) { return List.of(); }
        @Override public boolean existsByPatientIdAndAppointmentDateAndStatusNot(Long patientId, LocalDate appointmentDate, AppointmentStatus excludeStatus) { return false; }
        @Override public boolean hasActiveAppointmentOnDate(Long patientId, LocalDate date) { return false; }
        @Override public List<Appointment> findByPatient_UserIdOrderByAppointmentDateDesc(Long userId) { return List.of(); }
        @Override public List<Appointment> findByPatient_UserIdAndStatusNotOrderByAppointmentDateDesc(Long userId, AppointmentStatus status) { return List.of(); }
        @Override public List<Appointment> findByPatient_UserIdOrderByAppointmentDateDescCreatedAtDesc(Long userId) { return List.of(); }
        @Override public List<Appointment> findByUserIdAndStatuses(Long userId, List<AppointmentStatus> statuses) { return List.of(); }
        @Override public List<Appointment> lookupByCodeOrPhoneOrPatientCode(String code) { return List.of(); }
        @Override public List<Object[]> findLatestCompletedVisitDates(List<Long> patientIds) { return List.of(); }
        @Override public List<Appointment> findByAppointmentDateAndStatus(LocalDate appointmentDate, AppointmentStatus status) { return List.of(); }
        @Override public List<Appointment> findWithDetailsByAppointmentDateAndStatus(LocalDate date, AppointmentStatus status) { return List.of(); }
        @Override public long countByDoctorChangeRequestedAtIsNotNullAndAppointmentDateGreaterThanEqual(LocalDate date) { return 0; }
        @Override public long countByDoctorIdAndAppointmentDateAndStatusIn(Long doctorId, LocalDate appointmentDate, Collection<AppointmentStatus> statuses) { return 0; }
        @Override public List<Appointment> findUpcomingRemindersByUser(Long userId, Collection<AppointmentStatus> statuses, LocalDate startDate, LocalDate endDate, Pageable pageable) { return List.of(); }
        @Override public boolean existsByPatientIdAndDoctorUserId(Long patientId, Long userId) { return false; }
        @Override public boolean existsByPatientIdAndDoctorId(Long patientId, Long doctorId) { return false; }
        @Override public long countFutureAppointmentsForDoctor(Long doctorId, LocalDate today, Collection<AppointmentStatus> excludeStatuses) { return 0; }
        @Override public List<Long> findFutureSlotBookingCounts(LocalDate today, Pageable pageable) { return List.of(); }
        @Override public long countUpcomingAppointmentsForSlot(Long timeSlotId, LocalDate today, Collection<AppointmentStatus> excludeStatuses) { return 0; }
        @Override public List<Object[]> countUpcomingAppointmentsGroupedBySlot(LocalDate today, Collection<AppointmentStatus> excludeStatuses) { return List.of(); }
        @Override public List<Object[]> countByDayAndStatus(LocalDate from, LocalDate to) { return List.of(); }
        @Override public List<Object[]> countByDoctorAndStatus(LocalDate from, LocalDate to) { return List.of(); }
        @Override public List<Object[]> countBySlotAndStatus(LocalDate from, LocalDate to) { return List.of(); }
        @Override public long countDistinctCompletedPatientsInPeriod(LocalDate from, LocalDate to) { return 0; }
        @Override public List<Long> findNewPatientIdsInPeriod(LocalDate from, LocalDate to) { return List.of(); }
        @Override public void flush() {}
        @Override public <S extends Appointment> S saveAndFlush(S entity) { return null; }
        @Override public <S extends Appointment> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<Appointment> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Appointment getOne(Long aLong) { return null; }
        @Override public Appointment getById(Long aLong) { return null; }
        @Override public Appointment getReferenceById(Long aLong) { return null; }
        @Override public <S extends Appointment> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends Appointment> List<S> findAll(Example<S> example) { return List.of(); }
        @Override public <S extends Appointment> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
        @Override public <S extends Appointment> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends Appointment> long count(Example<S> example) { return 0; }
        @Override public <S extends Appointment> boolean exists(Example<S> example) { return false; }
        @Override public <S extends Appointment, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends Appointment> List<S> saveAll(Iterable<S> entities) { return null; }
        @Override public List<Appointment> findAll() { return List.of(); }
        @Override public List<Appointment> findAllById(Iterable<Long> longs) { return List.of(); }
        @Override public <S extends Appointment> S save(S entity) { return entity; }
        @Override public Optional<Appointment> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(Appointment entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends Appointment> entities) {}
        @Override public void deleteAll() {}
        @Override public List<Appointment> findAll(Sort sort) { return List.of(); }
        @Override public Page<Appointment> findAll(Pageable pageable) { return null; }
    }

    static class StubClinicSettingService implements ClinicSettingService {
        @Override public int getSlotCapacity() { return 5; }
        @Override public int getSeats() { return 5; }
        @Override public int getUserMaxDays() { return 60; }
        @Override public int getCounterMaxDays() { return 7; }
        @Override public int getMaxProfiles() { return 10; }
        @Override public int getPriorityEarlyMinutes() { return 15; }
        @Override public int getBookingAdvanceDays() { return 60; }
        @Override public int getBookingMinHours() { return 1; }
        @Override public int getPatientsPerDoctor() { return 3; }
        @Override public java.time.LocalTime getNoShowCancelAt() { return java.time.LocalTime.of(17, 0); }
        @Override public java.time.LocalTime getReceptionOpen() { return java.time.LocalTime.of(8, 0); }
        @Override public java.time.LocalTime getReceptionClose() { return java.time.LocalTime.of(16, 30); }
        @Override public boolean isAutoInvite() { return true; }
        @Override public Map<String, String> getAllSettings() { return Map.of(); }
        @Override public String getSettingValue(String key, String defaultValue) { return defaultValue; }
        @Override public void setSettingValue(String key, String value) {}
        @Override public void invalidateCache() {}
    }
}
