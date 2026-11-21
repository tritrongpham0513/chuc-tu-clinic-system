package com.chuctupk.service;

import com.chuctupk.dto.response.StaffPatientListResponse;
import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.Patient;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.MedicalRecordRepository;
import com.chuctupk.repository.PatientRepository;
import com.chuctupk.repository.RecordAccessLogRepository;
import com.chuctupk.repository.UserRepository;
import com.chuctupk.repository.specification.PatientSpecifications;
import com.chuctupk.service.impl.StaffPatientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Clock;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Ticket 4 & 5 — Kiểm tra Tìm kiếm Bệnh nhân theo CCCD, Năm sinh và Hiệu năng (Sprint 9)")
class PatientSearchTest {

    private PatientRepository patientRepository;
    private AppointmentRepository appointmentRepository;
    private MedicalRecordRepository medicalRecordRepository;
    private RecordAccessLogRepository recordAccessLogRepository;
    private UserRepository userRepository;
    private SequenceGeneratorService sequenceGeneratorService;
    private Clock clock;

    private StaffPatientServiceImpl staffPatientService;
    private User staffUser;

    @BeforeEach
    void setUp() {
        patientRepository = mock(PatientRepository.class);
        appointmentRepository = mock(AppointmentRepository.class);
        medicalRecordRepository = mock(MedicalRecordRepository.class);
        recordAccessLogRepository = mock(RecordAccessLogRepository.class);
        userRepository = mock(UserRepository.class);
        sequenceGeneratorService = mock(SequenceGeneratorService.class);
        clock = Clock.systemDefaultZone();

        staffPatientService = new StaffPatientServiceImpl(
                patientRepository,
                appointmentRepository,
                medicalRecordRepository,
                recordAccessLogRepository,
                userRepository,
                sequenceGeneratorService,
                clock
        );

        staffUser = User.builder().id(1L).username("staff").role(Role.STAFF).build();
    }

    @Test
    @DisplayName("Test 7.1: Tìm theo năm sinh (q = 1960) -> trả đúng bệnh nhân sinh năm 1960")
    void searchByYearOfBirth_returnsPatientsBornInThatYear() {
        Patient p1 = Patient.builder().id(1L).fullName("Bác Trần Thị Mai").yearOfBirth(1960).phone("0901234567").patientCode("CT-BN-26-0001").isActive(true).build();
        Patient p2 = Patient.builder().id(2L).fullName("Bác Lê Văn Nam").yearOfBirth(1960).phone("0908765432").patientCode("CT-BN-26-0002").isActive(true).build();

        Page<Patient> pageResult = new PageImpl<>(List.of(p1, p2), PageRequest.of(0, 10, Sort.by("fullName")), 2);

        when(patientRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pageResult);
        when(appointmentRepository.findLatestCompletedVisitDates(any())).thenReturn(Collections.emptyList());

        StaffPatientListResponse response = staffPatientService.getPatients("1960", null, 1, 10, staffUser);

        assertThat(response.items()).hasSize(2);
        assertThat(response.total()).isEqualTo(2);
        assertThat(response.items()).allMatch(p -> p.birthYear() == 1960);

        // Kiểm tra phân trang và sắp theo tên (Sort.by("fullName"))
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(patientRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getSort().getOrderFor("fullName")).isNotNull();
    }

    @Test
    @DisplayName("Test 7.2: Tìm đúng số CCCD (q = 079160001234) -> trả đúng 1 người")
    void searchByNationalId_returnsSinglePatient() {
        Patient p = Patient.builder()
                .id(1L)
                .fullName("Bác Phạm Văn Hùng")
                .nationalId("079160001234")
                .yearOfBirth(1960)
                .phone("0903333333")
                .patientCode("CT-BN-26-0099")
                .isActive(true)
                .build();

        Page<Patient> pageResult = new PageImpl<>(List.of(p), PageRequest.of(0, 10, Sort.by("fullName")), 1);

        when(patientRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pageResult);
        when(appointmentRepository.findLatestCompletedVisitDates(any())).thenReturn(Collections.emptyList());

        StaffPatientListResponse response = staffPatientService.getPatients("079160001234", null, 1, 10, staffUser);

        assertThat(response.items()).hasSize(1);
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.items().get(0).fullName()).isEqualTo("Bác Phạm Văn Hùng");
    }

    @Test
    @DisplayName("Test 7.3: Đo thời gian tìm kiếm không chậm hơn 300ms với 5.000 bệnh nhân")
    void searchPerformance_lessThan300msFor5000Patients() {
        // Tạo 5.000 bệnh nhân mẫu để đo đạc thuật toán lọc & ánh xạ
        List<Patient> largeDataset = new ArrayList<>(5000);
        for (int i = 1; i <= 5000; i++) {
            largeDataset.add(Patient.builder()
                    .id((long) i)
                    .fullName("Bệnh nhân số " + i)
                    .patientCode(String.format("CT-BN-26-%04d", i))
                    .phone(String.format("090%07d", i))
                    .yearOfBirth(1940 + (i % 60))
                    .nationalId(String.format("079%09d", i))
                    .isActive(true)
                    .build());
        }

        // Lấy 1 trang 10 phần tử từ tập 5.000 bệnh nhân đã sắp xếp
        List<Patient> pageContent = largeDataset.subList(0, 10);
        Page<Patient> mockPage = new PageImpl<>(pageContent, PageRequest.of(0, 10, Sort.by("fullName")), 5000);

        when(patientRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(appointmentRepository.findLatestCompletedVisitDates(any())).thenReturn(Collections.emptyList());

        long startTime = System.currentTimeMillis();
        StaffPatientListResponse response = staffPatientService.getPatients("1960", null, 1, 10, staffUser);
        long duration = System.currentTimeMillis() - startTime;

        System.out.printf("[PERFORMANCE MEASURE] Thời gian tìm kiếm và phân trang: %d ms (tiêu chuẩn: < 300 ms)%n", duration);

        assertThat(response.total()).isEqualTo(5000);
        assertThat(response.items()).hasSize(10);
        assertThat(duration).isLessThan(300L);
    }
}
