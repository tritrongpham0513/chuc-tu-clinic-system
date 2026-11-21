package com.chuctupk.dto.request;

import jakarta.validation.constraints.*;
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
public class UpdateClinicServiceRequest {

    @Size(max = 100, message = "Tên dịch vụ không vượt quá 100 ký tự")
    private String name;

    @Size(max = 120, message = "Slug không vượt quá 120 ký tự")
    private String slug;

    @Size(max = 1000, message = "Mô tả không vượt quá 1000 ký tự")
    private String description;

    private Integer durationMin;

    private BigDecimal price;

    @Size(max = 255, message = "Đường dẫn biểu tượng không vượt quá 255 ký tự")
    private String iconUrl;

    @Min(value = 0, message = "Thứ tự sắp xếp không được âm")
    private Integer sortOrder;

    private Boolean isActive;

    /** Khóa lạc quan (Optimistic Locking) */
    private LocalDateTime updatedAt;
}
