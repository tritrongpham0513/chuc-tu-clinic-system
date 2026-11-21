package com.chuctupk.service;

import com.chuctupk.config.DatabaseAutoMigrationListener;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.enums.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CapacityResolverTest {

    @Mock
    private DoctorAssignmentService doctorAssignmentService;

    @Mock
    private ClinicSettingService clinicSettingService;

    private CapacityResolver capacityResolver;

    private final LocalDate testDate = LocalDate.of(2026, 11, 20);
    private TimeSlot testSlot;

    @BeforeEach
    void setUp() {
        capacityResolver = new CapacityResolver(doctorAssignmentService, clinicSettingService);

        testSlot = new TimeSlot();
        testSlot.setId(1L);
        testSlot.setSlotLabel("08:00 - 09:00");
        testSlot.setPeriod(Period.MORNING);
        testSlot.setMaxPatients(5);
    }

    @Test
    @DisplayName("Test 1: effectiveCapacity với 0/1/2/3 bác sĩ trực → 0/3/5/5 theo seed (max_patients=5, patients_per_doctor=3)")
    void testEffectiveCapacity_WithDifferentOnDutyDoctors() {
        when(clinicSettingService.getPatientsPerDoctor()).thenReturn(3);

        // 0 bác sĩ trực -> 0
        when(doctorAssignmentService.findOnDutyDoctorIds(testDate, Period.MORNING)).thenReturn(List.of());
        assertEquals(0, capacityResolver.effectiveCapacity(testDate, testSlot));

        // 1 bác sĩ trực -> min(5, 1 * 3) = 3
        when(doctorAssignmentService.findOnDutyDoctorIds(testDate, Period.MORNING)).thenReturn(List.of(101L));
        assertEquals(3, capacityResolver.effectiveCapacity(testDate, testSlot));

        // 2 bác sĩ trực -> min(5, 2 * 3) = 5 (bị trần bởi slot maxPatients = 5)
        when(doctorAssignmentService.findOnDutyDoctorIds(testDate, Period.MORNING)).thenReturn(List.of(101L, 102L));
        assertEquals(5, capacityResolver.effectiveCapacity(testDate, testSlot));

        // 3 bác sĩ trực -> min(5, 3 * 3) = 5
        when(doctorAssignmentService.findOnDutyDoctorIds(testDate, Period.MORNING)).thenReturn(List.of(101L, 102L, 103L));
        assertEquals(5, capacityResolver.effectiveCapacity(testDate, testSlot));
    }

    @Test
    @DisplayName("Test 2: Đổi patients_per_doctor = 4, 1 bác sĩ trực → sức chứa là 4")
    void testEffectiveCapacity_WithCustomPatientsPerDoctor() {
        when(clinicSettingService.getPatientsPerDoctor()).thenReturn(4);
        when(doctorAssignmentService.findOnDutyDoctorIds(testDate, Period.MORNING)).thenReturn(List.of(101L));

        int capacity = capacityResolver.effectiveCapacity(testDate, testSlot);
        assertEquals(4, capacity);
    }

    @Test
    @DisplayName("Test 3: slot.maxPatients = 6, 2 bác sĩ trực, perDoctor = 3 → sức chứa là 6")
    void testEffectiveCapacity_WithHigherSlotMaxPatients() {
        testSlot.setMaxPatients(6);
        when(clinicSettingService.getPatientsPerDoctor()).thenReturn(3);
        when(doctorAssignmentService.findOnDutyDoctorIds(testDate, Period.MORNING)).thenReturn(List.of(101L, 102L));

        int capacity = capacityResolver.effectiveCapacity(testDate, testSlot);
        assertEquals(6, capacity);
    }

    @Test
    @DisplayName("Test 4: Restart (gọi lại DatabaseAutoMigrationListener) KHÔNG đổi giá trị slot_capacity hay ghi đè")
    void testDatabaseAutoMigrationListener_DoesNotOverwriteSlotCapacity() throws Exception {
        ApplicationContext mockContext = mock(ApplicationContext.class);
        DataSource mockDataSource = mock(DataSource.class);
        Connection mockConn = mock(Connection.class);
        Statement mockStmt = mock(Statement.class);
        ResultSet mockRsTables = mock(ResultSet.class);
        ResultSet mockRsCols = mock(ResultSet.class);

        when(mockContext.getBean(DataSource.class)).thenReturn(mockDataSource);
        when(mockDataSource.getConnection()).thenReturn(mockConn);
        when(mockConn.createStatement()).thenReturn(mockStmt);
        when(mockStmt.executeQuery("SHOW TABLES LIKE 'appointments'")).thenReturn(mockRsTables);
        when(mockRsTables.next()).thenReturn(true);
        when(mockStmt.executeQuery("SHOW COLUMNS FROM `appointments` LIKE 'reschedule_count'")).thenReturn(mockRsCols);
        when(mockRsCols.next()).thenReturn(true);

        DatabaseAutoMigrationListener listener = new DatabaseAutoMigrationListener();
        listener.setApplicationContext(mockContext);

        // Gọi method migration khởi động
        listener.executeAutoMigration();

        // Xác minh tuyệt đối: không có lệnh UPDATE nào lên clinic_settings hay time_slots
        verify(mockStmt, never()).execute(contains("clinic_settings"));
        verify(mockStmt, never()).execute(contains("time_slots"));
        verify(mockStmt, never()).execute(contains("UPDATE"));
    }
}
