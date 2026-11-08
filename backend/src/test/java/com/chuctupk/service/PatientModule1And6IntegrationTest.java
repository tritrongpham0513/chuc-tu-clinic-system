package com.chuctupk.service;

import com.chuctupk.dto.request.AppointmentCancelByPatientRequest;
import com.chuctupk.dto.request.BookingRequest;
import com.chuctupk.dto.request.PatientProfileRequest;
import com.chuctupk.dto.response.AppointmentDetailResponse;
import com.chuctupk.dto.response.PatientProfileResponse;
import com.chuctupk.dto.response.SlotAvailabilityResponse;
import com.chuctupk.entity.Patient;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.PatientRepository;
import com.chuctupk.repository.TimeSlotRepository;
import com.chuctupk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PatientModule1And6IntegrationTest {

    @Autowired
    private PatientProfileService patientProfileService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    private User testUser;
    private Patient primaryPatient;

    @BeforeEach
    void setUp() {
        // Mock Security Context
        testUser = userRepository.findByUsername("patient_test_user").orElseGet(() -> {
            User u = User.builder()
                    .username("patient_test_user")
                    .passwordHash("$2a$10$abcdefghijklmnopqrstuvwxyz123456")
                    .fullName("Nguyễn Văn A")
                    .phone("0901234567")
                    .role(Role.PATIENT)
                    .isActive(true)
                    .mustChangePassword(false)
                    .build();
            return userRepository.save(u);
        });

        var auth = new UsernamePasswordAuthenticationToken(
                testUser.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Tạo hồ sơ chính chủ
        primaryPatient = patientRepository.findByPhone("0901234567").stream().findFirst().orElseGet(() -> {
            Patient p = Patient.builder()
                    .user(testUser)
                    .patientCode("BN-2026-9999")
                    .fullName("Nguyễn Văn A")
                    .phone("0901234567")
                    .yearOfBirth(1965)
                    .gender(Gender.MALE)
                    .relationship("Bản thân")
                    .isPrimary(true)
                    .isActive(true)
                    .build();
            return patientRepository.save(p);
        });
    }

    @Test
    @DisplayName("F.1.6: Lấy danh sách hồ sơ của tôi bao gồm hồ sơ chính chủ")
    void testGetMyProfiles() {
        List<PatientProfileResponse> profiles = patientProfileService.getMyProfiles();
        assertThat(profiles).isNotEmpty();
        assertThat(profiles.stream().anyMatch(p -> Boolean.TRUE.equals(p.getIsPrimary()))).isTrue();
    }

    @Test
    @DisplayName("F.1.7: Thêm hồ sơ người thân (Mẹ) thành công")
    void testAddRelativeProfile() {
        PatientProfileRequest req = new PatientProfileRequest();
        req.setFullName("Trần Thị Mẹ");
        req.setPhone("0909888777");
        req.setYearOfBirth(1945);
        req.setGender(Gender.FEMALE);
        req.setRelationship("Mẹ");
        req.setAddress("TP.HCM");
        req.setMedicalHistory("Cao huyết áp");

        PatientProfileResponse added = patientProfileService.addProfile(req);
        assertThat(added.getId()).isNotNull();
        assertThat(added.getFullName()).isEqualTo("Trần Thị Mẹ");
        assertThat(added.getIsPrimary()).isFalse();

        // Kiểm tra xem đã xuất hiện trong danh sách
        List<PatientProfileResponse> list = patientProfileService.getMyProfiles();
        assertThat(list.stream().anyMatch(p -> p.getId().equals(added.getId()))).isTrue();
    }

    @Test
    @DisplayName("F.1.8: Không được phép xóa hồ sơ chính chủ")
    void testCannotDeletePrimaryProfile() {
        assertThatThrownBy(() -> patientProfileService.deleteOrUnlinkProfile(primaryPatient.getId()))
                .hasMessageContaining("Không thể xóa hồ sơ chính chủ");
    }

    @Test
    @DisplayName("F.6.1: Lấy danh sách khung giờ khả dụng theo ngày")
    void testGetAvailableSlots() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<SlotAvailabilityResponse> slots = bookingService.getAvailableSlots(tomorrow);
        assertThat(slots).isNotEmpty();
        assertThat(slots.get(0).getAvailable()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("F.6.3: Đặt lịch khám và hủy phiếu khám thành công")
    void testBookAndCancelAppointment() {
        LocalDate bookingDate = LocalDate.now().plusDays(2);
        Long slotId = timeSlotRepository.findAll().get(0).getId();

        BookingRequest req = new BookingRequest();
        req.setAppointmentDate(bookingDate);
        req.setTimeSlotId(slotId);
        req.setPatientId(primaryPatient.getId());
        req.setSymptoms("Đau nhức khớp gối khi trời lạnh");

        AppointmentDetailResponse booked = bookingService.bookAppointment(req);
        assertThat(booked.getBookingCode()).startsWith("PKCT-");
        assertThat(booked.getStatus()).isEqualTo(AppointmentStatus.PENDING);

        // Kiểm tra trong danh sách sắp tới
        List<AppointmentDetailResponse> upcoming = bookingService.getMyUpcoming();
        assertThat(upcoming.stream().anyMatch(a -> a.getId().equals(booked.getId()))).isTrue();

        // Hủy phiếu khám
        AppointmentCancelByPatientRequest cancelReq = new AppointmentCancelByPatientRequest();
        cancelReq.setCancelReason("Bận việc đột xuất");
        AppointmentDetailResponse cancelled = bookingService.cancelMyAppointment(booked.getId(), cancelReq);
        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(cancelled.getCancelReason()).isEqualTo("Bận việc đột xuất");
    }
}

