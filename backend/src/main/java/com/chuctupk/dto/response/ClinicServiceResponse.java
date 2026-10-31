package com.chuctupk.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicServiceResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private Integer durationMin;
    private BigDecimal price;
    private String iconUrl;
}

