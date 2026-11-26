package com.chuctupk.service;

import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.*;
import com.chuctupk.event.AppointmentCancelledEvent;
import com.chuctupk.event.AppointmentCreatedEvent;
import com.chuctupk.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class StaffNotificationServiceTest {

    @Autowired
    private StaffNotificationService staffNotificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    private final List<Long> createdAppointmentIds = new ArrayList<>();
    private final List<Long> createdDoctorIds = new ArrayList<>();
    private final List<Long> createdPatientIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();

    private User adminUser;
    private User staffUser;
    private User doctorUser;
    private Doctor doctorWithUser;
    private Doctor doctorWithoutUser;
    private Patient patient;
    private TimeSlot slot;

    @BeforeEach
    void setUp() {
        long suffix = System.nanoTime();
        adminUser = saveTestUser("notif_admin_" + suffix, Role.ADMIN);
        staffUser = saveTestUser("notif_staff_" + suffix, Role.STAFF);
        doctorUser = saveTestUser("notif_doc_" + suffix, Role.DOCTOR);

        doctorWithUser = doctorRepository.save(Doctor.builder()
                .user(doctorUser)
                .fullName("Nguyễn Thanh Tùng")
                .specialty("Châm cứu")
                .phone("0911111111")
                .isActive(true)
                .build());
        createdDoctorIds.add(doctorWithUser.getId());

        doctorWithoutUser = doctorRepository.save(Doctor.builder()
                .user(null)
                .fullName("Trần Minh Quân")
                .specialty("Xoa bóp")
                .phone("0922222222")
                .isActive(true)
                .build());
        createdDoctorIds.add(doctorWithoutUser.getId());

        patient = patientRepository.save(Patient.builder()
                .patientCode("BNT-" + (suffix % 1_000_000_000L))
                .fullName("Lê Thị Hoa")
                .phone("0988777666")
                .yearOfBirth(1960)
                .gender(Gender.FEMALE)
                .build());
        createdPatientIds.add(patient.getId());

        slot = timeSlotRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> timeSlotRepository.save(TimeSlot.builder()
                        .slotLabel("08:30 - 09:00")
                        .startTime(LocalTime.of(8, 30))
                        .endTime(LocalTime.of(9, 0))
                        .period(Period.MORNING)
                        .maxPatients(5)
                        .isActive(true)
                        .build()));
    }

    @AfterEach
    void tearDown() {
        for (Long id : createdAppointmentIds) {
            appointmentRepository.deleteById(id);
        }
        for (Long id : createdDoctorIds) {
            doctorRepository.deleteById(id);
        }
        for (Long id : createdPatientIds) {
            patientRepository.deleteById(id);
        }
        for (Long id : createdUserIds) {
            userRepository.deleteById(id);
        }
    }

    private User saveTestUser(String username, Role role) {
        User u = userRepository.save(User.builder()
                .username(username)
                .passwordHash("$2a$12$DummyHashForTest12345678901234567890123456789012")
                .fullName(username)
                .phone("09" + String.valueOf(System.nanoTime()).substring(String.valueOf(System.nanoTime()).length() - 8))
                .role(role)
                .isActive(true)
                .mustChangePassword(false)
                .build());
        createdUserIds.add(u.getId());
        return u;
    }

    private Appointment createAppointment(Doctor doctor) {
        long suffix = System.nanoTime();
        Appointment appt = appointmentRepository.save(Appointment.builder()
                .bookingCode("PKCT-TEST-" + suffix)
                .patient(patient)
                .doctor(doctor)
                .appointmentDate(LocalDate.of(2026, 11, 27))
                .timeSlot(slot)
                .slotStartSnapshot(LocalTime.of(8, 30))
                .slotEndSnapshot(LocalTime.of(9, 0))
                .symptoms("Đau thắt lưng mãn tính 0988777666")
                .source(AppointmentSource.WEB)
                .status(AppointmentStatus.PENDING)
                .build());
        createdAppointmentIds.add(appt.getId());
        return appt;
    }

    private List<UserNotification> notificationsFor(Long userId, Long appointmentId, NotificationInboxType type) {
        return userNotificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 50))
                .getContent()
                .stream()
                .filter(n -> n.getAppointment() != null
                        && n.getAppointment().getId().equals(appointmentId)
                        && n.getType() == type)
                .toList();
    }

    @Test
    @DisplayName("Tạo lịch có bác sĩ có tài khoản: mỗi ADMIN/STAFF/DOCTOR nhận đúng 1 dòng, không trùng, không lộ SĐT/triệu chứng")
    void testAppointmentCreatedWithDoctorAccount() {
        Appointment appt = createAppointment(doctorWithUser);

        staffNotificationService.onAppointmentCreated(new AppointmentCreatedEvent(appt.getId()));
        // Gọi lần 2 phải idempotent
        staffNotificationService.onAppointmentCreated(new AppointmentCreatedEvent(appt.getId()));

        List<UserNotification> adminRows = notificationsFor(adminUser.getId(), appt.getId(), NotificationInboxType.APPOINTMENT_CREATED);
        List<UserNotification> staffRows = notificationsFor(staffUser.getId(), appt.getId(), NotificationInboxType.APPOINTMENT_CREATED);
        List<UserNotification> doctorRows = notificationsFor(doctorUser.getId(), appt.getId(), NotificationInboxType.APPOINTMENT_CREATED);

        assertThat(adminRows).hasSize(1);
        assertThat(staffRows).hasSize(1);
        assertThat(doctorRows).hasSize(1);

        UserNotification docNotif = doctorRows.get(0);
        assertThat(docNotif.getTitle()).isEqualTo("Lịch khám mới");
        assertThat(docNotif.getLink()).isEqualTo("/noi-bo/lich-cua-toi");
        assertThat(docNotif.getBody()).contains("Lê Thị Hoa").contains("08:30").contains("27/11/2026").contains("Nguyễn Thanh Tùng");
        assertThat(docNotif.getBody()).doesNotContain(patient.getPhone()).doesNotContain("Đau thắt lưng");

        assertThat(staffRows.get(0).getLink()).isEqualTo("/noi-bo/lich-trong-ngay?ngay=2026-11-27");
    }

    @Test
    @DisplayName("Tạo lịch chưa gán bác sĩ (doctor = null) hoặc bác sĩ chưa có tài khoản: không ném lỗi, chỉ gửi ADMIN/STAFF")
    void testAppointmentWithoutDoctorOrDoctorUser() {
        Appointment noDocAppt = createAppointment(null);
        staffNotificationService.onAppointmentCreated(new AppointmentCreatedEvent(noDocAppt.getId()));

        List<UserNotification> staffRows = notificationsFor(staffUser.getId(), noDocAppt.getId(), NotificationInboxType.APPOINTMENT_CREATED);
        assertThat(staffRows).hasSize(1);
        assertThat(staffRows.get(0).getBody()).contains("(chưa gán bác sĩ)");
        assertThat(notificationsFor(doctorUser.getId(), noDocAppt.getId(), NotificationInboxType.APPOINTMENT_CREATED)).isEmpty();

        Appointment docNoUserAppt = createAppointment(doctorWithoutUser);
        staffNotificationService.onAppointmentCreated(new AppointmentCreatedEvent(docNoUserAppt.getId()));

        assertThat(notificationsFor(adminUser.getId(), docNoUserAppt.getId(), NotificationInboxType.APPOINTMENT_CREATED)).hasSize(1);
        assertThat(notificationsFor(staffUser.getId(), docNoUserAppt.getId(), NotificationInboxType.APPOINTMENT_CREATED)).hasSize(1);
        assertThat(notificationsFor(doctorUser.getId(), docNoUserAppt.getId(), NotificationInboxType.APPOINTMENT_CREATED)).isEmpty();
    }

    @Test
    @DisplayName("Huỷ lịch: tạo thông báo APPOINTMENT_CANCELLED đúng tiêu đề và nội dung an toàn")
    void testAppointmentCancelledNotification() {
        Appointment appt = createAppointment(doctorWithUser);

        staffNotificationService.onAppointmentCancelled(new AppointmentCancelledEvent(appt.getId()));

        List<UserNotification> doctorRows = notificationsFor(doctorUser.getId(), appt.getId(), NotificationInboxType.APPOINTMENT_CANCELLED);
        assertThat(doctorRows).hasSize(1);
        assertThat(doctorRows.get(0).getTitle()).isEqualTo("Lịch khám đã huỷ");
        assertThat(doctorRows.get(0).getBody()).contains("đã huỷ lịch").doesNotContain(patient.getPhone()).doesNotContain("Đau thắt lưng");
    }
}
