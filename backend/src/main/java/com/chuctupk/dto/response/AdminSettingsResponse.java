package com.chuctupk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSettingsResponse {
    private Map<String, String> settings;
    private Map<String, SettingMetadataDto> metadata;
    private int maxFutureBookedPerSlot;
    private List<String> warnings;
}
