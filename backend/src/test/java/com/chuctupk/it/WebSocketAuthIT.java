package com.chuctupk.it;

import com.chuctupk.config.JwtTokenProvider;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.*;
import com.chuctupk.event.AppointmentCreatedEvent;
import com.chuctupk.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WebSocketAuthIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final List<Long> createdAppointmentIds = new ArrayList<>();
    private final List<Long> createdDoctorIds = new ArrayList<>();
    private final List<Long> createdPatientIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();

    private User doctorUser;
    private Doctor doctor;
    private Patient patient;
    private TimeSlot slot;

    @BeforeEach
    void setUp() {
        long suffix = System.nanoTime();
        doctorUser = userRepository.save(User.builder()
                .username("ws_doc_" + suffix)
                .passwordHash("$2a$12$DummyHashForTest12345678901234567890123456789012")
                .fullName("BS. Lê Minh Tuấn")
                .phone("09" + String.valueOf(suffix).substring(String.valueOf(suffix).length() - 8))
                .role(Role.DOCTOR)
                .isActive(true)
                .mustChangePassword(false)
                .build());
        createdUserIds.add(doctorUser.getId());

        doctor = doctorRepository.save(Doctor.builder()
                .user(doctorUser)
                .fullName("Lê Minh Tuấn")
                .specialty("Y học cổ truyền")
                .phone("0912345670")
                .isActive(true)
                .build());
        createdDoctorIds.add(doctor.getId());

        patient = patientRepository.save(Patient.builder()
                .patientCode("BNW-" + (suffix % 1_000_000_000L))
                .fullName("Phạm Văn Hùng")
                .phone("0977888999")
                .yearOfBirth(1958)
                .gender(Gender.MALE)
                .build());
        createdPatientIds.add(patient.getId());

        slot = timeSlotRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> timeSlotRepository.save(TimeSlot.builder()
                        .slotLabel("09:00 - 09:30")
                        .startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(9, 30))
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

    private WebSocketStompClient newStompClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MessageConverter() {
            @Override
            public Object fromMessage(Message<?> message, Class<?> targetClass) {
                Object payload = message.getPayload();
                if (payload instanceof byte[] bytes) {
                    return new String(bytes, StandardCharsets.UTF_8);
                }
                return payload != null ? payload.toString() : null;
            }

            @Override
            public Message<?> toMessage(Object payload, MessageHeaders headers) {
                return null;
            }
        });
        return client;
    }

    @Test
    @DisplayName("CONNECT không có Authorization -> thất bại, không nhận CONNECTED")
    void testConnectWithoutAuthorizationFails() {
        WebSocketStompClient stompClient = newStompClient();
        String wsUrl = "ws://localhost:" + port + "/ws";

        CompletableFuture<StompSession> future = stompClient.connectAsync(
                wsUrl,
                new WebSocketHttpHeaders(),
                new StompHeaders(),
                new StompSessionHandlerAdapter() {}
        );

        assertThatThrownBy(() -> future.get(3, TimeUnit.SECONDS))
                .isInstanceOf(Exception.class);
        stompClient.stop();
    }

    @Test
    @DisplayName("CONNECT với JWT hợp lệ -> nhận CONNECTED, subscribe /user/queue/appointments và nhận message trong 3s")
    void testConnectWithJwtAndReceiveDoctorQueueNotification() throws Exception {
        String token = jwtTokenProvider.generateToken(doctorUser);
        WebSocketStompClient stompClient = newStompClient();
        String wsUrl = "ws://localhost:" + port + "/ws";

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        CompletableFuture<String> receivedPayload = new CompletableFuture<>();
        StompSession session = stompClient.connectAsync(
                wsUrl,
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                    @Override
                    public void handleException(StompSession s, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                        receivedPayload.completeExceptionally(exception);
                    }

                    @Override
                    public void handleTransportError(StompSession s, Throwable exception) {
                        receivedPayload.completeExceptionally(exception);
                    }
                }
        ).get(3, TimeUnit.SECONDS);

        try {
            assertThat(session.isConnected()).isTrue();

            session.subscribe("/user/queue/appointments", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return String.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    if (payload != null) {
                        receivedPayload.complete(payload.toString());
                    }
                }
            });

            // Cho broker 150ms đăng ký subscription trước khi phát event
            Thread.sleep(150);

            Appointment savedAppt = transactionTemplate.execute(status -> {
                Appointment appt = appointmentRepository.save(Appointment.builder()
                        .bookingCode("PKCT-WS-" + System.nanoTime())
                        .patient(patient)
                        .doctor(doctor)
                        .appointmentDate(LocalDate.of(2026, 11, 28))
                        .timeSlot(slot)
                        .slotStartSnapshot(LocalTime.of(9, 0))
                        .slotEndSnapshot(LocalTime.of(9, 30))
                        .symptoms("Đau vai gáy 0977888999")
                        .source(AppointmentSource.WEB)
                        .status(AppointmentStatus.PENDING)
                        .build());
                createdAppointmentIds.add(appt.getId());
                eventPublisher.publishEvent(new AppointmentCreatedEvent(appt.getId()));
                return appt;
            });

            String json = receivedPayload.get(3, TimeUnit.SECONDS);
            assertThat(json)
                    .contains("Lịch khám mới")
                    .contains(savedAppt.getBookingCode())
                    .contains("Phạm Văn Hùng")
                    .doesNotContain("0977888999")
                    .doesNotContain("Đau vai gáy");
        } finally {
            if (session.isConnected()) {
                session.disconnect();
            }
            stompClient.stop();
        }
    }
}
