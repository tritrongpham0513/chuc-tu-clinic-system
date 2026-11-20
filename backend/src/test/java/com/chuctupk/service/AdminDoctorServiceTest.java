package com.chuctupk.service;

import com.chuctupk.dto.request.CreateDoctorRequest;
import com.chuctupk.dto.request.UpdateDoctorRequest;
import com.chuctupk.dto.response.AdminDoctorDetailResponse;
import com.chuctupk.dto.response.AdminDoctorListResponse;
import com.chuctupk.dto.response.DoctorResponse;
import com.chuctupk.dto.response.DoctorUserOptionResponse;
import com.chuctupk.entity.Doctor;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.exception.ErrorCode;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.DoctorRepository;
import com.chuctupk.repository.UserRepository;
import com.chuctupk.service.impl.AdminDoctorServiceImpl;
import com.chuctupk.service.impl.DoctorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Sprint 4 — Admin Doctor Service Tests (Ticket 1, 2, 3)")
class AdminDoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    private Clock clock;
    private AdminDoctorServiceImpl adminDoctorService;
    private DoctorServiceImpl publicDoctorService;

    private User doctorUser;
    private User patientUser;
    private Doctor doctor1;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-11-20T08:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        adminDoctorService = new AdminDoctorServiceImpl(doctorRepository, userRepository, appointmentRepository, clock);
        publicDoctorService = new DoctorServiceImpl(doctorRepository);

        doctorUser = User.builder()
                .id(10L)
                .username("bacsi_lan")
                .fullName("Bác sĩ Nguyễn Thị Lan")
                .role(Role.DOCTOR)
                .isActive(true)
                .build();

        patientUser = User.builder()
                .id(20L)
                .username("benhnhan_an")
                .fullName("Bệnh nhân Trần Văn An")
                .role(Role.PATIENT)
                .isActive(true)
                .build();

        doctor1 = Doctor.builder()
                .id(1L)
                .user(doctorUser)
                .fullName("Nguyễn Thị Lan")
                .title("Bác sĩ CKI")
                .specialty("Y học cổ truyền")
                .experienceYears(15)
                .bio("Chuyên trị cơ xương khớp")
                .phone("0903123456")
                .avatarUrl("https://example.com/lan.jpg")
                .sortOrder(1)
                .isActive(true)
                .createdAt(LocalDateTime.of(2026, 11, 1, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 11, 20, 8, 0))
                .build();
    }

    @Test
    @DisplayName("Test 1: ADMIN tạo bác sĩ không gán tài khoản -> sortOrder mặc định = max + 1, thành công")
    void testCreateDoctor_WithoutAccount_Success() {
        CreateDoctorRequest request = CreateDoctorRequest.builder()
                .fullName("Lê Văn Bình")
                .title("Lương y")
                .specialty("Châm cứu - Bấm huyệt")
                .experienceYears(8)
                .bio("Kinh nghiệm châm cứu trị liệu")
                .phone("0908765432")
                .accountId(null)
                .build();

        when(doctorRepository.findMaxSortOrder()).thenReturn(5);
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(invocation -> {
            Doctor d = invocation.getArgument(0);
            d.setId(2L);
            d.setCreatedAt(LocalDateTime.now(clock));
            d.setUpdatedAt(LocalDateTime.now(clock));
            return d;
        });

        AdminDoctorDetailResponse response = adminDoctorService.createDoctor(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getFullName()).isEqualTo("Lê Văn Bình");
        assertThat(response.getAccountId()).isNull();
        assertThat(response.getSortOrder()).isEqualTo(6); // max + 1
        assertThat(response.getIsActive()).isTrue();

        verify(userRepository, never()).findById(any());
        verify(doctorRepository, times(1)).save(any(Doctor.class));
    }

    @Test
    @DisplayName("Test 2: Tạo bác sĩ với accountId có vai trò không phải DOCTOR (PATIENT/USER) -> lỗi 422")
    void testCreateDoctor_WithNonDoctorRole_Throws422() {
        CreateDoctorRequest request = CreateDoctorRequest.builder()
                .fullName("Trần Văn An")
                .specialty("Y học cổ truyền")
                .phone("0908765432")
                .accountId(20L)
                .build();

        when(userRepository.findById(20L)).thenReturn(Optional.of(patientUser));

        assertThatThrownBy(() -> adminDoctorService.createDoctor(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getStatusCode()).isEqualTo(422);
                    assertThat(appEx.getMessage()).contains("vai trò Bác sĩ (DOCTOR)");
                });

        verify(doctorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 3: Tạo bác sĩ với accountId đã được gán cho bác sĩ khác -> lỗi 422")
    void testCreateDoctor_WithAlreadyAssignedAccount_Throws422() {
        CreateDoctorRequest request = CreateDoctorRequest.builder()
                .fullName("Bác sĩ Trùng Lặp")
                .specialty("Y học cổ truyền")
                .phone("0908765432")
                .accountId(10L)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.existsByUserId(10L)).thenReturn(true);

        assertThatThrownBy(() -> adminDoctorService.createDoctor(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getStatusCode()).isEqualTo(422);
                    assertThat(appEx.getMessage()).contains("Tài khoản này đã được gán cho một bác sĩ khác");
                });

        verify(doctorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 4: PATCH ẩn bác sĩ còn 3 phiếu khám tương lai -> lỗi 422, message chứa '3'")
    void testUpdateDoctor_HideDoctorWith3FutureAppointments_Throws422() {
        UpdateDoctorRequest request = UpdateDoctorRequest.builder()
                .isActive(false)
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor1));
        when(appointmentRepository.countFutureAppointmentsForDoctor(
                eq(1L),
                eq(LocalDate.now(clock)),
                any()
        )).thenReturn(3L);

        assertThatThrownBy(() -> adminDoctorService.updateDoctor(1L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getStatusCode()).isEqualTo(422);
                    assertThat(appEx.getMessage()).contains("3");
                    assertThat(appEx.getMessage()).contains("Bác sĩ còn 3 phiếu khám sắp tới, không thể ẩn");
                });

        verify(doctorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 4b: Ẩn bác sĩ khi không còn phiếu khám tương lai (0 phiếu) -> ẩn thành công")
    void testUpdateDoctor_HideDoctorWithoutFutureAppointments_Success() {
        UpdateDoctorRequest request = UpdateDoctorRequest.builder()
                .isActive(false)
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor1));
        when(appointmentRepository.countFutureAppointmentsForDoctor(
                eq(1L),
                eq(LocalDate.now(clock)),
                any()
        )).thenReturn(0L);
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminDoctorDetailResponse response = adminDoctorService.updateDoctor(1L, request);

        assertThat(response.getIsActive()).isFalse();
        verify(doctorRepository, times(1)).save(doctor1);
    }

    @Test
    @DisplayName("Test 5: PATCH với updatedAt cũ -> lỗi STALE (khoá lạc quan)")
    void testUpdateDoctor_StaleUpdatedAt_ThrowsStaleConflict() {
        LocalDateTime oldUpdatedAt = LocalDateTime.of(2026, 11, 15, 8, 0);
        UpdateDoctorRequest request = UpdateDoctorRequest.builder()
                .fullName("Tên Mới")
                .updatedAt(oldUpdatedAt) // Khác với doctor1.updatedAt là 2026-11-20T08:00
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor1));

        assertThatThrownBy(() -> adminDoctorService.updateDoctor(1L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(ErrorCode.STALE.getCode());
                    assertThat(appEx.getStatusCode()).isEqualTo(409);
                });

        verify(doctorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 7: GET /api/v1/doctors công khai không trả bác sĩ isActive=false")
    void testPublicGetDoctors_ExcludesInactiveDoctors() {
        Doctor activeDoctor = Doctor.builder()
                .id(1L)
                .fullName("Bác sĩ Đang Hoạt Động")
                .specialty("YHCT")
                .sortOrder(1)
                .isActive(true)
                .build();

        when(doctorRepository.findByIsActiveTrueOrderBySortOrderAsc())
                .thenReturn(List.of(activeDoctor));

        List<DoctorResponse> responses = publicDoctorService.getActiveDoctors();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getFullName()).isEqualTo("Bác sĩ Đang Hoạt Động");
        verify(doctorRepository).findByIsActiveTrueOrderBySortOrderAsc();
    }

    @Test
    @DisplayName("Test Ticket 1: Lấy danh sách bác sĩ quản trị sắp theo sortOrder, phone được che")
    void testGetAllDoctors_AdminListMasksPhone() {
        when(doctorRepository.findAllByOrderBySortOrderAscFullNameAsc()).thenReturn(List.of(doctor1));

        List<AdminDoctorListResponse> list = adminDoctorService.getAllDoctors();

        assertThat(list).hasSize(1);
        AdminDoctorListResponse item = list.get(0);
        assertThat(item.getFullName()).isEqualTo("Nguyễn Thị Lan");
        assertThat(item.getPhone()).contains("***"); // Masked
        assertThat(item.getAccountId()).isEqualTo(10L);
        assertThat(item.getAccountUsername()).isEqualTo("bacsi_lan");
    }

    @Test
    @DisplayName("Test Ticket 3b: Lấy danh sách tài khoản DOCTOR chưa gán hoặc thuộc bác sĩ hiện tại (1 query tối ưu)")
    void testGetUnassignedDoctorAccounts_OptimizedSingleQuery() {
        User unassignedDocUser = User.builder()
                .id(11L)
                .username("bacsi_minh")
                .fullName("Bác sĩ Lê Văn Minh")
                .phone("0912345678")
                .role(Role.DOCTOR)
                .isActive(true)
                .build();

        when(userRepository.findByRole(Role.DOCTOR)).thenReturn(List.of(doctorUser, unassignedDocUser));
        when(doctorRepository.findAll()).thenReturn(List.of(doctor1));

        // 1. Khi tạo mới (currentDoctorId = null): chỉ trả về unassignedDocUser
        List<DoctorUserOptionResponse> forNew = adminDoctorService.getUnassignedDoctorAccounts(null);
        assertThat(forNew).hasSize(1);
        assertThat(forNew.get(0).getId()).isEqualTo(11L);

        // 2. Khi sửa bác sĩ 1 (currentDoctorId = 1L): trả về cả tài khoản của chính bác sĩ 1 và tài khoản chưa gán
        List<DoctorUserOptionResponse> forEdit = adminDoctorService.getUnassignedDoctorAccounts(1L);
        assertThat(forEdit).hasSize(2);
        assertThat(forEdit.stream().map(DoctorUserOptionResponse::getId).toList())
                .containsExactlyInAnyOrder(10L, 11L);
    }
}
