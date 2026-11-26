package com.chuctupk.controller;

import com.chuctupk.dto.request.CreateTimeSlotRequest;
import com.chuctupk.dto.request.UpdateTimeSlotRequest;
import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.GlobalExceptionHandler;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.TimeSlotRepository;
import com.chuctupk.service.impl.AdminTimeSlotServiceImpl;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Sprint 6 — Admin TimeSlot Tests (Ticket 4, 5, 8)")
class AdminTimeSlotTest {

    private MockMvc mockMvc;

    private FakeTimeSlotRepository timeSlotRepository;
    private FakeAppointmentRepository appointmentRepository;
    private AdminTimeSlotServiceImpl adminTimeSlotService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private User adminUser;
    private User staffUser;

    @BeforeEach
    void setUp() {
        timeSlotRepository = new FakeTimeSlotRepository();
        appointmentRepository = new FakeAppointmentRepository();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-11-20T08:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

        adminTimeSlotService = new AdminTimeSlotServiceImpl(timeSlotRepository, appointmentRepository, fixedClock);
        AdminTimeSlotController controller = new AdminTimeSlotController(adminTimeSlotService);

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
                .username("letan")
                .fullName("Lễ tân phòng khám")
                .role(Role.STAFF)
                .isActive(true)
                .build();
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
    @DisplayName("Test 1: Tạo khung 12:00–13:00 AFTERNOON -> 201, label đúng '12:00 - 13:00'")
    void test1_CreateSlot_12To13_Afternoon_Returns201_LabelCorrect() throws Exception {
        authenticateAs(adminUser);

        CreateTimeSlotRequest request = CreateTimeSlotRequest.builder()
                .startTime(LocalTime.of(12, 0))
                .endTime(LocalTime.of(13, 0))
                .period(Period.AFTERNOON)
                .maxPatients(5)
                .sortOrder(0)
                .isActive(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slotLabel").value("12:00 - 13:00"))
                .andExpect(jsonPath("$.data.startTime").value("12:00"))
                .andExpect(jsonPath("$.data.endTime").value("13:00"))
                .andExpect(jsonPath("$.data.period").value("AFTERNOON"))
                .andExpect(jsonPath("$.data.maxPatients").value(5))
                .andExpect(jsonPath("$.data.sortOrder").value(1));
    }

    @Test
    @DisplayName("Test 2: Tạo khung chồng lấn 08:30–09:30 khi có 08:00–09:00 -> 422")
    void test2_CreateSlot_Overlap_Returns422() throws Exception {
        authenticateAs(adminUser);

        TimeSlot existingSlot = TimeSlot.builder()
                .id(1L)
                .slotLabel("08:00 - 09:00")
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .period(Period.MORNING)
                .isActive(true)
                .build();
        timeSlotRepository.save(existingSlot);

        CreateTimeSlotRequest request = CreateTimeSlotRequest.builder()
                .startTime(LocalTime.of(8, 30))
                .endTime(LocalTime.of(9, 30))
                .period(Period.MORNING)
                .maxPatients(5)
                .sortOrder(0)
                .isActive(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Trùng với khung 08:00 - 09:00")));
    }

    @Test
    @DisplayName("Test 3: Tạo khung period=MORNING với start 13:00 -> 422")
    void test3_CreateSlot_MorningWithStart13_Returns422() throws Exception {
        authenticateAs(adminUser);

        CreateTimeSlotRequest request = CreateTimeSlotRequest.builder()
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(14, 0))
                .period(Period.MORNING)
                .maxPatients(5)
                .sortOrder(0)
                .isActive(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Khung giờ sáng phải bắt đầu trước 12:00")));
    }

    @Test
    @DisplayName("Test 4: end <= start -> 422")
    void test4_CreateSlot_EndLessThanOrEqualToStart_Returns422() throws Exception {
        authenticateAs(adminUser);

        // Case A: end < start
        CreateTimeSlotRequest request1 = CreateTimeSlotRequest.builder()
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(9, 0))
                .period(Period.MORNING)
                .maxPatients(5)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Giờ kết thúc phải sau giờ bắt đầu")));

        // Case B: end == start
        CreateTimeSlotRequest request2 = CreateTimeSlotRequest.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 0))
                .period(Period.MORNING)
                .maxPatients(5)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Giờ kết thúc phải sau giờ bắt đầu")));
    }

    @Test
    @DisplayName("Test 5: Sửa giờ khung có phiếu tương lai -> 422, message chứa số phiếu")
    void test5_UpdateSlotTime_WithFutureAppointments_Returns422_ContainsCount() throws Exception {
        authenticateAs(adminUser);

        TimeSlot existingSlot = TimeSlot.builder()
                .id(1L)
                .slotLabel("08:00 - 09:00")
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .period(Period.MORNING)
                .isActive(true)
                .build();
        timeSlotRepository.save(existingSlot);

        // Có 3 phiếu tương lai
        appointmentRepository.upcomingCounts.put(1L, 3L);

        UpdateTimeSlotRequest request = UpdateTimeSlotRequest.builder()
                .startTime(LocalTime.of(8, 15))
                .endTime(LocalTime.of(9, 15))
                .build();

        mockMvc.perform(patch("/api/v1/admin/time-slots/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Khung này đang có 3 phiếu. Hãy tạo khung mới và ngưng khung cũ."));
    }

    @Test
    @DisplayName("Test 6: Sửa giờ khung không có phiếu -> 200")
    void test6_UpdateSlotTime_WithoutAppointments_Returns200() throws Exception {
        authenticateAs(adminUser);

        TimeSlot existingSlot = TimeSlot.builder()
                .id(1L)
                .slotLabel("08:00 - 09:00")
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .period(Period.MORNING)
                .isActive(true)
                .build();
        timeSlotRepository.save(existingSlot);

        // 0 phiếu tương lai
        appointmentRepository.upcomingCounts.put(1L, 0L);

        UpdateTimeSlotRequest request = UpdateTimeSlotRequest.builder()
                .startTime(LocalTime.of(8, 30))
                .endTime(LocalTime.of(9, 30))
                .build();

        mockMvc.perform(patch("/api/v1/admin/time-slots/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.startTime").value("08:30"))
                .andExpect(jsonPath("$.data.endTime").value("09:30"))
                .andExpect(jsonPath("$.data.slotLabel").value("08:30 - 09:30"));
    }

    @Test
    @DisplayName("Test 7: Ngưng khung có phiếu tương lai -> 422; khung trống -> 200")
    void test7_DeactivateSlot_WithFutureAppointments_Returns422_AndEmptySlot_Returns200() throws Exception {
        authenticateAs(adminUser);

        // Case A: Khung có 2 phiếu tương lai -> 422
        TimeSlot busySlot = TimeSlot.builder()
                .id(1L)
                .slotLabel("08:00 - 09:00")
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .period(Period.MORNING)
                .isActive(true)
                .build();
        timeSlotRepository.save(busySlot);
        appointmentRepository.upcomingCounts.put(1L, 2L);

        mockMvc.perform(post("/api/v1/admin/time-slots/1/deactivate"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Khung này đang có 2 phiếu sắp tới, không thể ngưng hoạt động.")));

        // Case B: Khung trống (0 phiếu) -> 200
        TimeSlot emptySlot = TimeSlot.builder()
                .id(2L)
                .slotLabel("09:00 - 10:00")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .period(Period.MORNING)
                .isActive(true)
                .build();
        timeSlotRepository.save(emptySlot);
        appointmentRepository.upcomingCounts.put(2L, 0L);

        mockMvc.perform(post("/api/v1/admin/time-slots/2/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    @DisplayName("Test 9: STAFF gọi POST /api/v1/admin/time-slots -> 403 Forbidden")
    void test9_StaffCallCreateSlot_Returns403() throws Exception {
        authenticateAs(staffUser);

        CreateTimeSlotRequest request = CreateTimeSlotRequest.builder()
                .startTime(LocalTime.of(12, 0))
                .endTime(LocalTime.of(13, 0))
                .period(Period.AFTERNOON)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện thao tác này"));
    }

    // --- In-memory Stubs to completely eliminate ByteBuddy/Agent attach issues on Windows Sandbox ---
    static class FakeTimeSlotRepository implements TimeSlotRepository {
        Map<Long, TimeSlot> storage = new LinkedHashMap<>();
        long idSeq = 1L;

        @Override
        public List<TimeSlot> findAllByOrderBySortOrderAsc() {
            return storage.values().stream()
                    .sorted(Comparator.comparingInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0))
                    .toList();
        }

        @Override
        public List<TimeSlot> findByIsActiveTrue() {
            return storage.values().stream().filter(s -> Boolean.TRUE.equals(s.getIsActive())).toList();
        }

        @Override
        public List<TimeSlot> findByIsActiveTrueOrderBySortOrderAsc() {
            return storage.values().stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                    .sorted(Comparator.comparingInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0))
                    .toList();
        }

        @Override
        public List<TimeSlot> findByPeriodAndIsActiveTrueOrderBySortOrderAsc(Period period) {
            return storage.values().stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsActive()) && s.getPeriod() == period)
                    .sorted(Comparator.comparingInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0))
                    .toList();
        }

        @Override
        public Optional<TimeSlot> findTopByOrderBySortOrderDesc() {
            return storage.values().stream()
                    .max(Comparator.comparingInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0));
        }

        @Override
        public Optional<TimeSlot> findById(Long id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <S extends TimeSlot> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(idSeq++);
            }
            storage.put(entity.getId(), entity);
            return entity;
        }

        @Override public Optional<TimeSlot> findByIdWithLock(Long id) { return findById(id); }
        @Override public void flush() {}
        @Override public <S extends TimeSlot> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends TimeSlot> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<TimeSlot> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public TimeSlot getOne(Long aLong) { return storage.get(aLong); }
        @Override public TimeSlot getById(Long aLong) { return storage.get(aLong); }
        @Override public TimeSlot getReferenceById(Long aLong) { return storage.get(aLong); }
        @Override public <S extends TimeSlot> List<S> findAll(Example<S> example) { return null; }
        @Override public <S extends TimeSlot> List<S> findAll(Example<S> example, Sort sort) { return null; }
        @Override public <S extends TimeSlot> List<S> saveAll(Iterable<S> entities) { return null; }
        @Override public List<TimeSlot> findAll() { return new ArrayList<>(storage.values()); }
        @Override public List<TimeSlot> findAllById(Iterable<Long> longs) { return null; }
        @Override public boolean existsById(Long aLong) { return storage.containsKey(aLong); }
        @Override public long count() { return storage.size(); }
        @Override public void deleteById(Long aLong) { storage.remove(aLong); }
        @Override public void delete(TimeSlot entity) { storage.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends TimeSlot> entities) {}
        @Override public void deleteAll() { storage.clear(); }
        @Override public List<TimeSlot> findAll(Sort sort) { return findAll(); }
        @Override public Page<TimeSlot> findAll(Pageable pageable) { return null; }
        @Override public <S extends TimeSlot> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends TimeSlot> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends TimeSlot> long count(Example<S> example) { return 0; }
        @Override public <S extends TimeSlot> boolean exists(Example<S> example) { return false; }
        @Override public <S extends TimeSlot, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    }

    static class FakeAppointmentRepository implements AppointmentRepository {
        Map<Long, Long> upcomingCounts = new HashMap<>();

        @Override
        public long countUpcomingAppointmentsForSlot(Long timeSlotId, LocalDate today, Collection<AppointmentStatus> excludeStatuses) {
            return upcomingCounts.getOrDefault(timeSlotId, 0L);
        }

        @Override
        public List<Object[]> countUpcomingAppointmentsGroupedBySlot(LocalDate today, Collection<AppointmentStatus> excludeStatuses) {
            List<Object[]> result = new ArrayList<>();
            for (Map.Entry<Long, Long> e : upcomingCounts.entrySet()) {
                result.add(new Object[]{e.getKey(), e.getValue()});
            }
            return result;
        }

        @Override public Optional<Appointment> findWithDetailById(Long id) { return Optional.empty(); }
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
        @Override public List<Appointment> findWithDetailsByAppointmentDateAndStatus(LocalDate date, AppointmentStatus status) { return List.of(); }
        @Override public long countByDoctorChangeRequestedAtIsNotNullAndAppointmentDateGreaterThanEqual(LocalDate date) { return 0; }
        @Override public boolean hasActiveAppointmentOnDate(Long patientId, LocalDate date) { return false; }
        @Override public List<Appointment> findByPatient_UserIdOrderByAppointmentDateDesc(Long userId) { return List.of(); }
        @Override public List<Appointment> findByPatient_UserIdAndStatusNotOrderByAppointmentDateDesc(Long userId, AppointmentStatus status) { return List.of(); }
        @Override public List<Appointment> findByPatient_UserIdOrderByAppointmentDateDescCreatedAtDesc(Long userId) { return List.of(); }
        @Override public List<Appointment> findByUserIdAndStatuses(Long userId, List<AppointmentStatus> statuses) { return List.of(); }
        @Override public List<Appointment> lookupByCodeOrPhoneOrPatientCode(String code) { return List.of(); }
        @Override public List<Object[]> findLatestCompletedVisitDates(List<Long> patientIds) { return List.of(); }
        @Override public List<Appointment> findByAppointmentDateAndStatus(LocalDate appointmentDate, AppointmentStatus status) { return List.of(); }
        @Override public long countByDoctorIdAndAppointmentDateAndStatusIn(Long doctorId, LocalDate appointmentDate, Collection<AppointmentStatus> statuses) { return 0; }
        @Override public List<Appointment> findUpcomingRemindersByUser(Long userId, Collection<AppointmentStatus> statuses, LocalDate startDate, LocalDate endDate, Pageable pageable) { return List.of(); }
        @Override public boolean existsByPatientIdAndDoctorUserId(Long patientId, Long userId) { return false; }
        @Override public boolean existsByPatientIdAndDoctorId(Long patientId, Long doctorId) { return false; }
        @Override public long countFutureAppointmentsForDoctor(Long doctorId, LocalDate today, Collection<AppointmentStatus> excludeStatuses) { return 0; }
        @Override public List<Long> findFutureSlotBookingCounts(LocalDate today, Pageable pageable) { return List.of(); }
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
        @Override public long countByAppointmentDateAndStatusNot(LocalDate appointmentDate, AppointmentStatus excludeStatus) { return 0; }
        @Override public boolean isServiceUsed(Long serviceId, String slug, String slugUnderscore) { return false; }
        @Override public long countAppointmentsForService(Long serviceId, String slug, String slugUnderscore) { return 0; }
    }
}
