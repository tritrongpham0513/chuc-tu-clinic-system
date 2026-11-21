package com.chuctupk.dto.response;

import com.chuctupk.entity.ClinicService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminServiceResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private Integer durationMin;
    private BigDecimal price;
    private String iconUrl;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long appointmentCount;

    public static AdminServiceResponse from(ClinicService s, Long appointmentCount) {
        return AdminServiceResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .slug(s.getSlug())
                .description(s.getDescription())
                .durationMin(s.getDurationMin())
                .price(s.getPrice())
                .iconUrl(s.getIconUrl())
                .sortOrder(s.getSortOrder())
                .isActive(s.getIsActive())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .appointmentCount(appointmentCount != null ? appointmentCount : 0L)
                .build();
    }
}
