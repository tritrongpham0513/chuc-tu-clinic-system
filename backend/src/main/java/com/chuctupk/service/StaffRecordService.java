package com.chuctupk.service;

import com.chuctupk.dto.request.CreateRecordRequest;
import com.chuctupk.dto.request.UpdateRecordRequest;
import com.chuctupk.dto.response.RecordDetailResponse;
import com.chuctupk.entity.User;

public interface StaffRecordService {

    RecordDetailResponse createRecord(User staff, Long appointmentId, CreateRecordRequest request);

    RecordDetailResponse getRecord(Long id);

    RecordDetailResponse updateRecord(User staff, Long id, UpdateRecordRequest request);
}

