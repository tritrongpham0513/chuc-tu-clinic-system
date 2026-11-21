package com.chuctupk.controller;

import com.chuctupk.dto.request.CreateClinicServiceRequest;
import com.chuctupk.dto.request.UpdateClinicServiceRequest;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.ClinicServiceResponse;
import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.ClinicService;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.GlobalExceptionHandler;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.ClinicServiceRepository;
import com.chuctupk.service.impl.AdminServiceServiceImpl;
import com.chuctupk.service.impl.ClinicServiceServiceImpl;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Sprint 7 — Admin Service Catalog Tests (Ticket 3 & Ticket 5)")
class AdminServiceCatalogTest {

    private MockMvc mockMvcAdmin;
    private MockMvc mockMvcPublic;

    private FakeClinicServiceRepository serviceRepository;
    private FakeAppointmentRepository appointmentRepository;
    private AdminServiceServiceImpl adminServiceService;
    private ClinicServiceServiceImpl publicServiceService;
    private ClinicServiceController publicServiceController;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private User adminUser;
    private User staffUser;

    @BeforeEach
    void setUp() {
        serviceRepository = new FakeClinicServiceRepository();
        appointmentRepository = new FakeAppointmentRepository();
        adminServiceService = new AdminServiceServiceImpl(serviceRepository, appointmentRepository);
        publicServiceService = new ClinicServiceServiceImpl(serviceRepository);

        AdminServiceController adminController = new AdminServiceController(adminServiceService);
        mockMvcAdmin = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        publicServiceController = new ClinicServiceController(publicServiceService);
        mockMvcPublic = MockMvcBuilders.standaloneSetup(publicServiceController)
                .setControllerAdvice(new GlobalExceptionHandler())
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
    @DisplayName("Test 1: Tạo dịch vụ trùng slug -> 422")
    void test1_CreateDuplicateSlugService_Returns422() throws Exception {
        authenticateAs(adminUser);

        // Tạo lần 1 thành công
        CreateClinicServiceRequest request1 = CreateClinicServiceRequest.builder()
                .name("Châm cứu trị liệu")
                .slug("cham-cuu")
                .durationMin(30)
                .price(BigDecimal.ZERO)
                .build();

        mockMvcAdmin.perform(post("/api/v1/admin/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("cham-cuu"));

        // Tạo lần 2 cùng slug -> 422
        CreateClinicServiceRequest request2 = CreateClinicServiceRequest.builder()
                .name("Châm cứu phục hồi")
                .slug("cham-cuu")
                .durationMin(45)
                .price(BigDecimal.ZERO)
                .build();

        mockMvcAdmin.perform(post("/api/v1/admin/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Slug đã tồn tại")));
    }

    @Test
    @DisplayName("Test 2: Xoá dịch vụ đã có phiếu -> 422 'Dịch vụ đã được dùng, hãy ẩn thay vì xoá'")
    void test2_DeleteServiceWithAppointments_Returns422() throws Exception {
        authenticateAs(adminUser);

        ClinicService service = serviceRepository.save(ClinicService.builder()
                .name("Bốc thuốc Đông y")
                .slug("boc-thuoc")
                .durationMin(30)
                .price(BigDecimal.ZERO)
                .isActive(true)
                .build());

        // Đánh dấu dịch vụ này đã có phiếu khám
        appointmentRepository.usedServiceIds.add(service.getId());

        mockMvcAdmin.perform(delete("/api/v1/admin/services/" + service.getId()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dịch vụ đã được dùng, hãy ẩn thay vì xoá"));

        // Dịch vụ vẫn còn trong repository
        assertTrue(serviceRepository.findById(service.getId()).isPresent());
    }

    @Test
    @DisplayName("Test 3: Ẩn dịch vụ -> GET /api/v1/services không còn")
    void test3_HideService_DisappearsFromPublicApi() throws Exception {
        authenticateAs(adminUser);

        // Tạo 1 dịch vụ đang active
        ClinicService service = serviceRepository.save(ClinicService.builder()
                .name("Vật lý trị liệu chuyên sâu")
                .slug("vat-ly-tri-lieu")
                .durationMin(45)
                .price(BigDecimal.ZERO)
                .isActive(true)
                .sortOrder(1)
                .build());

        // Kiểm tra GET /api/v1/services: có chứa dịch vụ
        ResponseEntity<ApiResponse<List<ClinicServiceResponse>>> publicRes1 = publicServiceController.getServices();
        assertTrue(publicRes1.getBody().getData().stream().anyMatch(s -> "vat-ly-tri-lieu".equals(s.getSlug())));

        // PATCH ẩn dịch vụ (isActive = false)
        UpdateClinicServiceRequest update = UpdateClinicServiceRequest.builder()
                .isActive(false)
                .build();

        mockMvcAdmin.perform(patch("/api/v1/admin/services/" + service.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false));

        // Kiểm tra GET /api/v1/services lần 2: KHÔNG CÒN dịch vụ này
        ResponseEntity<ApiResponse<List<ClinicServiceResponse>>> publicRes2 = publicServiceController.getServices();
        assertFalse(publicRes2.getBody().getData().stream().anyMatch(s -> "vat-ly-tri-lieu".equals(s.getSlug())));
    }

    @Test
    @DisplayName("Test 4: STAFF gọi POST /api/v1/admin/services -> 403 Forbidden")
    void test4_StaffCallCreateService_Returns403() throws Exception {
        authenticateAs(staffUser);

        CreateClinicServiceRequest request = CreateClinicServiceRequest.builder()
                .name("Xoa bóp bấm huyệt")
                .slug("xoa-bop-bam-huyet")
                .durationMin(30)
                .price(BigDecimal.ZERO)
                .build();

        mockMvcAdmin.perform(post("/api/v1/admin/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện thao tác này"));
    }

    @Test
    @DisplayName("Test 5: Khóa lạc quan (Optimistic Locking) bằng updatedAt -> 409 STALE")
    void test5_OptimisticLocking_WithStaleUpdatedAt_Returns409() throws Exception {
        authenticateAs(adminUser);

        LocalDateTime initialTime = LocalDateTime.of(2026, 11, 20, 10, 0, 0);
        ClinicService service = serviceRepository.save(ClinicService.builder()
                .name("Xoa bóp điều trị")
                .slug("xoa-bop-dieu-tri")
                .durationMin(30)
                .price(BigDecimal.ZERO)
                .updatedAt(initialTime)
                .build());

        // Người dùng A cập nhật đẩy updatedAt lên 11:00:00
        service.setUpdatedAt(LocalDateTime.of(2026, 11, 20, 11, 0, 0));
        serviceRepository.save(service);

        // Người dùng B gửi request sửa với updatedAt cũ (10:00:00) -> STALE (409)
        UpdateClinicServiceRequest staleUpdate = UpdateClinicServiceRequest.builder()
                .name("Xoa bóp điều trị mới")
                .updatedAt(initialTime)
                .build();

        mockMvcAdmin.perform(patch("/api/v1/admin/services/" + service.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staleUpdate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Dữ liệu đã được người khác cập nhật trước đó")));
    }

    @Test
    @DisplayName("Test 6: Xoá dịch vụ chưa có phiếu -> 200 OK")
    void test6_DeleteUnusedService_Returns200() throws Exception {
        authenticateAs(adminUser);

        ClinicService service = serviceRepository.save(ClinicService.builder()
                .name("Dịch vụ thử nghiệm")
                .slug("dich-vu-thu-nghiem")
                .durationMin(30)
                .price(BigDecimal.ZERO)
                .build());

        // Chưa có phiếu khám nào
        mockMvcAdmin.perform(delete("/api/v1/admin/services/" + service.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertFalse(serviceRepository.findById(service.getId()).isPresent());
    }

    @Test
    @DisplayName("Test 7: Validation thời lượng ngoài 5-240 hoặc giá âm -> 422")
    void test7_InvalidDurationAndPrice_Returns422() throws Exception {
        authenticateAs(adminUser);

        // Duration < 5
        CreateClinicServiceRequest badDuration = CreateClinicServiceRequest.builder()
                .name("Dịch vụ quá ngắn")
                .durationMin(4)
                .build();

        mockMvcAdmin.perform(post("/api/v1/admin/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badDuration)))
                .andExpect(status().isUnprocessableEntity());

        // Price < 0
        CreateClinicServiceRequest badPrice = CreateClinicServiceRequest.builder()
                .name("Dịch vụ giá âm")
                .price(BigDecimal.valueOf(-1000))
                .build();

        mockMvcAdmin.perform(post("/api/v1/admin/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badPrice)))
                .andExpect(status().isUnprocessableEntity());
    }

    // --- Fake Stubs ---
    static class FakeClinicServiceRepository implements ClinicServiceRepository {
        Map<Long, ClinicService> storage = new LinkedHashMap<>();
        long seq = 1L;

        @Override
        public Optional<ClinicService> findBySlug(String slug) {
            return storage.values().stream()
                    .filter(s -> s.getSlug().equalsIgnoreCase(slug))
                    .findFirst();
        }

        @Override
        public List<ClinicService> findByIsActiveTrueOrderBySortOrderAsc() {
            return storage.values().stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                    .sorted(Comparator.comparingInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0))
                    .toList();
        }

        @Override
        public List<ClinicService> findAllByOrderBySortOrderAscIdAsc() {
            return storage.values().stream()
                    .sorted(Comparator.comparingInt((ClinicService s) -> s.getSortOrder() != null ? s.getSortOrder() : 0)
                            .thenComparing(ClinicService::getId))
                    .toList();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <S extends ClinicService> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(seq++);
            }
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(LocalDateTime.now());
            }
            if (entity.getUpdatedAt() == null) {
                entity.setUpdatedAt(LocalDateTime.now());
            }
            storage.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<ClinicService> findById(Long aLong) {
            return Optional.ofNullable(storage.get(aLong));
        }

        @Override
        public void delete(ClinicService entity) {
            storage.remove(entity.getId());
        }

        @Override public void flush() {}
        @Override public <S extends ClinicService> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends ClinicService> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<ClinicService> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public ClinicService getOne(Long aLong) { return storage.get(aLong); }
        @Override public ClinicService getById(Long aLong) { return storage.get(aLong); }
        @Override public ClinicService getReferenceById(Long aLong) { return storage.get(aLong); }
        @Override public <S extends ClinicService> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends ClinicService> List<S> findAll(Example<S> example) { return List.of(); }
        @Override public <S extends ClinicService> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
        @Override public <S extends ClinicService> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends ClinicService> long count(Example<S> example) { return 0; }
        @Override public <S extends ClinicService> boolean exists(Example<S> example) { return false; }
        @Override public <S extends ClinicService, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends ClinicService> List<S> saveAll(Iterable<S> entities) { return null; }
        @Override public List<ClinicService> findAll() { return new ArrayList<>(storage.values()); }
        @Override public List<ClinicService> findAllById(Iterable<Long> longs) { return List.of(); }
        @Override public boolean existsById(Long aLong) { return storage.containsKey(aLong); }
        @Override public long count() { return storage.size(); }
        @Override public void deleteById(Long aLong) { storage.remove(aLong); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends ClinicService> entities) {}
        @Override public void deleteAll() { storage.clear(); }
        @Override public List<ClinicService> findAll(Sort sort) { return List.of(); }
        @Override public Page<ClinicService> findAll(Pageable pageable) { return null; }
    }

    static class FakeAppointmentRepository implements AppointmentRepository {
        Set<Long> usedServiceIds = new HashSet<>();

        @Override
        public boolean isServiceUsed(Long serviceId, String slug, String slugUnderscore) {
            return usedServiceIds.contains(serviceId);
        }

        @Override
        public long countAppointmentsForService(Long serviceId, String slug, String slugUnderscore) {
            return usedServiceIds.contains(serviceId) ? 5L : 0L;
        }

        @Override public long countByAppointmentDateAndStatusNot(LocalDate appointmentDate, AppointmentStatus excludeStatus) { return 0; }
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
        @Override public long countByDoctorIdAndAppointmentDateAndStatusIn(Long doctorId, LocalDate appointmentDate, Collection<AppointmentStatus> statuses) { return 0; }
        @Override public List<Appointment> findUpcomingRemindersByUser(Long userId, Collection<AppointmentStatus> statuses, LocalDate startDate, LocalDate endDate, Pageable pageable) { return List.of(); }
        @Override public boolean existsByPatientIdAndDoctorUserId(Long patientId, Long userId) { return false; }
        @Override public boolean existsByPatientIdAndDoctorId(Long patientId, Long doctorId) { return false; }
        @Override public long countFutureAppointmentsForDoctor(Long doctorId, LocalDate today, Collection<AppointmentStatus> excludeStatuses) { return 0; }
        @Override public List<Long> findFutureSlotBookingCounts(LocalDate today, Pageable pageable) { return List.of(); }
        @Override public long countUpcomingAppointmentsForSlot(Long timeSlotId, LocalDate today, Collection<AppointmentStatus> excludeStatuses) { return 0; }
        @Override public List<Object[]> countUpcomingAppointmentsGroupedBySlot(LocalDate today, Collection<AppointmentStatus> excludeStatuses) { return List.of(); }
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
}
