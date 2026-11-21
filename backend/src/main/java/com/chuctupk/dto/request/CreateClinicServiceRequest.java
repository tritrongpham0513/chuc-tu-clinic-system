package com.chuctupk.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClinicServiceRequest {

    @NotBlank(message = "Tên dịch vụ không được để trống")
    @Size(max = 100, message = "Tên dịch vụ không vượt quá 100 ký tự")
    private String name;

    @Size(max = 120, message = "Slug không vượt quá 120 ký tự")
    private String slug;

    @Size(max = 1000, message = "Mô tả không vượt quá 1000 ký tự")
    private String description;

    @Builder.Default
    private Integer durationMin = 30;

    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Size(max = 255, message = "Đường dẫn biểu tượng không vượt quá 255 ký tự")
    private String iconUrl;

    @Builder.Default
    @Min(value = 0, message = "Thứ tự sắp xếp không được âm")
    private Integer sortOrder = 0;

    @Builder.Default
    private Boolean isActive = true;
}
