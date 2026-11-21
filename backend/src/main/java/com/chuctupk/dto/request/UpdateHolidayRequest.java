package com.chuctupk.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHolidayRequest {

    @Size(max = 150, message = "Tên ngày nghỉ không được vượt quá 150 ký tự")
    private String name;

    private Boolean isClosed;
}
