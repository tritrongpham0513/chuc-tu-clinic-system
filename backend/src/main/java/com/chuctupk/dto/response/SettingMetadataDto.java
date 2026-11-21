package com.chuctupk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingMetadataDto {
    private String key;
    private String label;
    private String description;
    private String type; // "int", "time", "boolean"
    private Integer min;
    private Integer max;
    private String unit;
    private String group; // "BOOKING", "RECEPTION", "QUEUE"
}
