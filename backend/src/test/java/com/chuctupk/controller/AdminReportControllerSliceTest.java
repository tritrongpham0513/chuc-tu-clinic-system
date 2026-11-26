package com.chuctupk.controller;

import com.chuctupk.dto.response.ReportSummaryResponse;
import com.chuctupk.dto.response.ReportSummaryResponse.DayItem;
import com.chuctupk.dto.response.ReportSummaryResponse.DoctorItem;
import com.chuctupk.dto.response.ReportSummaryResponse.SlotItem;
import com.chuctupk.dto.response.ReportSummaryResponse.Totals;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.exception.GlobalExceptionHandler;
import com.chuctupk.service.AdminService;
import com.chuctupk.service.DoctorAssignmentService;
import com.chuctupk.service.ReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Sprint 13 — Admin Report Controller Slice Tests")
class AdminReportControllerSliceTest {

    private MockMvc mockMvc;

    @Mock
    private AdminService adminService;

    @Mock
    private DoctorAssignmentService doctorAssignmentService;

    @Mock
    private ReportService reportService;

    private User adminUser;
    private User staffUser;

    @BeforeEach
    void setUp() {
        AdminController controller = new AdminController(adminService, doctorAssignmentService, reportService);
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
                .fullName("Tiếp tân")
                .role(Role.STAFF)
                .isActive(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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
    @DisplayName("1. from > to -> 422 Unprocessable Entity")
    void testSummary_FromAfterTo_Returns422() throws Exception {
        authenticateAs(adminUser);

        when(reportService.getSummary(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 10)))
                .thenThrow(AppException.unprocessable("Ngày bắt đầu phải trước ngày kết thúc"));

        mockMvc.perform(get("/api/v1/admin/reports/summary")
                        .param("from", "2026-11-20")
                        .param("to", "2026-11-10"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Ngày bắt đầu phải trước ngày kết thúc")));
    }

    @Test
    @DisplayName("2. Khoảng 400 ngày -> 422 tối đa 366 ngày")
    void testSummary_RangeOver366Days_Returns422() throws Exception {
        authenticateAs(adminUser);

        when(reportService.getSummary(any(), any()))
                .thenThrow(AppException.unprocessable("Khoảng báo cáo tối đa 366 ngày"));

        mockMvc.perform(get("/api/v1/admin/reports/summary")
                        .param("from", "2025-01-01")
                        .param("to", "2026-02-05"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("tối đa 366 ngày")));
    }

    @Test
    @DisplayName("ADMIN gọi GET /admin/reports/summary -> 200 OK")
    void testSummary_Admin_Returns200() throws Exception {
        authenticateAs(adminUser);

        ReportSummaryResponse payload = new ReportSummaryResponse(
                "2026-11-09",
                "2026-11-15",
                "2026-11-18T07:30:00",
                new Totals(120, 98, 12, 10, 15, 83),
                List.of(new DayItem("2026-11-09", 22, 18, 2, 2, 55.0)),
                List.of(new DoctorItem(1L, "Nguyễn Văn A", 62, 51, 6, 5)),
                List.of(new SlotItem(3L, "08:00 - 09:00", 40, 33, 66.7))
        );

        when(reportService.getSummary(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 15)))
                .thenReturn(payload);

        mockMvc.perform(get("/api/v1/admin/reports/summary")
                        .param("from", "2026-11-09")
                        .param("to", "2026-11-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.from", is("2026-11-09")))
                .andExpect(jsonPath("$.data.to", is("2026-11-15")))
                .andExpect(jsonPath("$.data.generatedAt", is("2026-11-18T07:30:00")))
                .andExpect(jsonPath("$.data.totals.booked", is(120)))
                .andExpect(jsonPath("$.data.totals.completed", is(98)))
                .andExpect(jsonPath("$.data.totals.cancelled", is(12)))
                .andExpect(jsonPath("$.data.totals.noShow", is(10)))
                .andExpect(jsonPath("$.data.byDay", hasSize(1)))
                .andExpect(jsonPath("$.data.byDoctor", hasSize(1)))
                .andExpect(jsonPath("$.data.bySlot[0].fillRatePct", is(66.7)));
    }

    @Test
    @DisplayName("7. STAFF gọi GET /admin/reports/summary và /export -> 403 Forbidden")
    void testReports_StaffForbidden_Returns403() throws Exception {
        authenticateAs(staffUser);

        mockMvc.perform(get("/api/v1/admin/reports/summary"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(get("/api/v1/admin/reports/export").param("type", "summary"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("8. CSV bắt đầu bằng UTF-8 BOM, dòng đầu chứa ';', không chứa cột tiền/CCCD")
    void testExportCsv_ValidBomHeadersAndNoForbiddenColumns() throws Exception {
        authenticateAs(adminUser);

        String csvString = "\uFEFFNgày;Lượt đặt;Lượt khám;Đã huỷ;Không đến\r\n09/11/2026;22;18;2;2\r\n";
        when(reportService.exportCsv(any(), any(), eq("summary")))
                .thenReturn(new ReportService.CsvExportResult(
                        "bao-cao-2026-11-09_2026-11-15.csv",
                        csvString.getBytes(StandardCharsets.UTF_8)
                ));

        MvcResult result = mockMvc.perform(get("/api/v1/admin/reports/export")
                        .param("from", "2026-11-09")
                        .param("to", "2026-11-15")
                        .param("type", "summary"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("bao-cao-2026-11-09_2026-11-15.csv")))
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertThat(bytes.length).isGreaterThan(3);
        // UTF-8 BOM: EF BB BF
        assertThat(bytes[0]).isEqualTo((byte) 0xEF);
        assertThat(bytes[1]).isEqualTo((byte) 0xBB);
        assertThat(bytes[2]).isEqualTo((byte) 0xBF);

        String body = new String(bytes, StandardCharsets.UTF_8);
        assertThat(body).startsWith("\uFEFF");
        String firstLine = body.split("\r\n")[0];
        assertThat(firstLine).contains(";");
        assertThat(body.toLowerCase()).doesNotContain("doanh thu");
        assertThat(body.toLowerCase()).doesNotContain("tiền");
        assertThat(body).doesNotContain("CCCD");
    }
}
