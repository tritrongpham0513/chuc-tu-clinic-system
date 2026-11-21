package com.chuctupk.controller;

import com.chuctupk.dto.response.AdminSettingsResponse;
import com.chuctupk.dto.response.SettingMetadataDto;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.exception.GlobalExceptionHandler;
import com.chuctupk.service.AdminSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Sprint 5 — Admin Settings Controller Slice Tests (Ticket 6)")
class AdminSettingsControllerSliceTest {

    private MockMvc mockMvc;

    @Mock
    private AdminSettingsService adminSettingsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User adminUser;
    private User staffUser;

    @BeforeEach
    void setUp() {
        AdminSettingsController controller = new AdminSettingsController(adminSettingsService);
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
                .fullName("Tiếp tân phòng khám")
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
    @DisplayName("Test 5a: PUT /admin/settings với khoá lạ ngoài whitelist -> 422 Unprocessable Entity kèm tên khoá")
    void testPutSettings_UnknownKey_Returns422() throws Exception {
        authenticateAs(adminUser);

        when(adminSettingsService.updateSettings(any(), anyString()))
                .thenThrow(AppException.unprocessable("Khoá cấu hình không hợp lệ: 'unknown_key'"));

        Map<String, String> body = Map.of("unknown_key", "123");

        mockMvc.perform(put("/api/v1/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("unknown_key")));
    }

    @Test
    @DisplayName("Test 5b: PUT /admin/settings với giá trị ngoài khoảng (slot_capacity=99) -> 422 Unprocessable Entity")
    void testPutSettings_OutOfRange_Returns422() throws Exception {
        authenticateAs(adminUser);

        when(adminSettingsService.updateSettings(any(), anyString()))
                .thenThrow(AppException.unprocessable("Khoá 'slot_capacity' phải là số nguyên trong khoảng 1–50"));

        Map<String, String> body = Map.of("slot_capacity", "99");

        mockMvc.perform(put("/api/v1/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("slot_capacity")));
    }

    @Test
    @DisplayName("Test 6: STAFF gọi PUT /admin/settings -> 403 Forbidden")
    void testPutSettings_StaffUser_Returns403() throws Exception {
        authenticateAs(staffUser);

        Map<String, String> body = Map.of("slot_capacity", "6");

        mockMvc.perform(put("/api/v1/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("ADMIN gọi PUT /admin/settings hợp lệ -> 200 OK")
    void testPutSettings_ValidAdmin_Returns200() throws Exception {
        authenticateAs(adminUser);

        AdminSettingsResponse response = AdminSettingsResponse.builder()
                .settings(Map.of("slot_capacity", "6", "patients_per_doctor", "3"))
                .metadata(Map.of())
                .maxFutureBookedPerSlot(0)
                .warnings(List.of())
                .build();

        when(adminSettingsService.updateSettings(any(), anyString())).thenReturn(response);

        Map<String, String> body = Map.of("slot_capacity", "6", "patients_per_doctor", "3");

        mockMvc.perform(put("/api/v1/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settings.slot_capacity").value("6"))
                .andExpect(jsonPath("$.data.settings.patients_per_doctor").value("3"));
    }

    @Test
    @DisplayName("ADMIN gọi GET /admin/settings -> 200 OK kèm settings và metadata")
    void testGetSettings_Admin_Returns200() throws Exception {
        authenticateAs(adminUser);

        AdminSettingsResponse response = AdminSettingsResponse.builder()
                .settings(Map.of("slot_capacity", "6", "patients_per_doctor", "3"))
                .metadata(Map.of("slot_capacity", SettingMetadataDto.builder()
                        .key("slot_capacity")
                        .label("Sức chứa tối đa")
                        .min(1)
                        .max(50)
                        .build()))
                .maxFutureBookedPerSlot(2)
                .warnings(List.of())
                .build();

        when(adminSettingsService.getSettings()).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settings.slot_capacity").value("6"))
                .andExpect(jsonPath("$.data.metadata.slot_capacity.label").value("Sức chứa tối đa"));
    }

    @Test
    @DisplayName("STAFF gọi GET /admin/settings -> 403 Forbidden")
    void testGetSettings_Staff_Returns403() throws Exception {
        authenticateAs(staffUser);

        mockMvc.perform(get("/api/v1/admin/settings"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}
