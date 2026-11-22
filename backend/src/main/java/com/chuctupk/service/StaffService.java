package com.chuctupk.service;

import com.chuctupk.dto.request.*;
import com.chuctupk.dto.response.*;
import com.chuctupk.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface StaffService {

    StaffAppointmentListResponse getAppointments(LocalDate date);

    StaffAppointmentListResponse getMyAppointments(LocalDate date, User currentUser);

    StaffAppointmentDatesResponse getAppointmentDates(String month);

    StaffAppointmentLookupResponse lookupAppointment(String code);

    StaffAppointmentDetailResponse getAppointmentDetail(Long id);

    default StaffAppointmentDetailResponse getAppointmentDetail(Long id, User currentUser) {
        return getAppointmentDetail(id);
    }

    AppointmentHistoryResponse getAppointmentHistory(Long appointmentId);

    StaffAvailabilityResponse getAvailability(LocalDate date);

    Map<String, Object> createCounterAppointment(User staff, StaffCreateAppointmentRequest request);

    StaffAppointmentLookupResponse rescheduleAppointment(User staff, Long id, RescheduleAppointmentRequest request);

    StaffAppointmentLookupResponse cancelAppointment(User staff, Long id, CancelAppointmentStaffRequest request);

    StaffAppointmentLookupResponse checkin(User staff, Long id);

    StaffAppointmentLookupResponse undoCheckin(User staff, Long id);

    StaffAppointmentLookupResponse enterRoom(User staff, Long id);

    List<NoShowPreviewItemDto> getNoShowPreview(LocalDate date);

    QueueStateDto getQueueState();

    QueueStateDto getQueueState(User currentUser, Integer mine);

    void requestDoctorChange(Long appointmentId, String note, User currentUser);

    Map<String, Object> getDashboardBadges();

    QueueStateDto inviteNext();

    QueueStateDto undoEnter(Long id);

    QueueStateDto setAutoInvite(boolean enabled);

    MyScheduleResponse getMySchedule(User currentUser);

    // =========================================================================
    // SPRINT 2: NHẮC LỊCH THỦ CÔNG 1 CLICK
    // =========================================================================

    ReminderPreviewDto getReminderPreview(Long id);

    ReminderMarkSentResponse markReminderSent(User staff, Long id, ReminderMarkSentRequest request);

    BulkReminderPreviewResponse bulkReminderPreview(BulkReminderPreviewRequest request);

    BulkReminderMarkSentResponse bulkReminderMarkSent(User staff, BulkReminderMarkSentRequest request);

    // =========================================================================
    // SPRINT 11: TÁI KHÁM & NHẮC TÁI KHÁM
    // =========================================================================

    com.chuctupk.dto.response.FollowUpListResponse getFollowUps(int days, User currentUser);

    com.chuctupk.dto.response.FollowUpReminderPreviewDto getFollowUpReminderPreview(Long recordId);

    Map<String, Object> markFollowUpReminderSent(User staff, Long recordId, ReminderMarkSentRequest request);
}
