package com.chuctupk.notification;

import com.chuctupk.common.mapping.ContractMapper;
import com.chuctupk.entity.Appointment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Provider soạn nội dung nhắc lịch thủ công (1 click) cho lễ tân/nhân viên phòng khám.
 * Không thực hiện cuộc gọi mạng. Trả về trạng thái SENT khi lễ tân xác nhận gửi.
 */
@Component("manual")
public class ManualProvider implements NotificationProvider {

    public static final String CHANNEL = "MANUAL";
    public static final String DEFAULT_HOTLINE = "0397 116 666";
    private static final DateTimeFormatter VN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String hotline;

    public ManualProvider(@Value("${app.clinic.hotline:0397 116 666}") String hotline) {
        this.hotline = (hotline != null && !hotline.isBlank()) ? hotline.trim() : DEFAULT_HOTLINE;
    }

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public DeliveryResult send(NotificationMessage message) {
        // Không gọi mạng. Trả về SENT khi lễ tân xác nhận đã gửi
        return new DeliveryResult(DeliveryResult.Status.SENT, null, "manual");
    }

    /**
     * Soạn thảo nội dung nhắc lịch chuẩn tiếng Việt có dấu theo mẫu:
     *
     * Kính gửi {tenBenhNhan},
     * Phòng khám Chúc Từ nhắc lịch khám: {gio} ngày {ngay}, mã phiếu {maPhieu}.
     * Cô/chú vui lòng đến trước 15 phút. Cần hỗ trợ xin gọi {hotline}.
     *
     * Tuyệt đối không đưa chẩn đoán, đơn thuốc, tên bệnh vào nội dung.
     */
    public String compose(String tenBenhNhan, String gio, String ngay, String maPhieu) {
        String patientName = (tenBenhNhan != null && !tenBenhNhan.isBlank()) ? tenBenhNhan.trim() : "Cô/Chú";
        String time = (gio != null && !gio.isBlank()) ? gio.trim() : "08:00";
        String date = (ngay != null && !ngay.isBlank()) ? ngay.trim() : "hôm nay";
        String code = (maPhieu != null && !maPhieu.isBlank()) ? maPhieu.trim() : "---";
        String phoneHotline = (this.hotline != null && !this.hotline.isBlank()) ? this.hotline.trim() : DEFAULT_HOTLINE;

        return String.format(
                "Kính gửi %s,\n" +
                "Phòng khám Chúc Từ nhắc lịch khám: %s ngày %s, mã phiếu %s.\n" +
                "Cô/chú vui lòng đến trước 15 phút. Cần hỗ trợ xin gọi %s.",
                patientName, time, date, code, phoneHotline
        );
    }

    /**
     * Soạn nội dung nhắc lịch từ Entity Appointment với đầy đủ fallback an toàn.
     */
    public String compose(Appointment appointment) {
        if (appointment == null) {
            return compose(null, null, null, null);
        }

        String patientName = null;
        if (appointment.getPatient() != null && appointment.getPatient().getFullName() != null) {
            patientName = appointment.getPatient().getFullName();
        } else if (appointment.getBookerName() != null) {
            patientName = appointment.getBookerName();
        }

        String time = null;
        if (appointment.getSlotStartSnapshot() != null) {
            time = ContractMapper.formatTime(appointment.getSlotStartSnapshot());
        } else if (appointment.getTimeSlot() != null && appointment.getTimeSlot().getStartTime() != null) {
            time = ContractMapper.formatTime(appointment.getTimeSlot().getStartTime());
        } else if (appointment.getExpectedTime() != null) {
            time = ContractMapper.formatTime(appointment.getExpectedTime());
        }

        String date = null;
        if (appointment.getAppointmentDate() != null) {
            date = appointment.getAppointmentDate().format(VN_DATE_FORMAT);
        }

        String code = appointment.getBookingCode();

        return compose(patientName, time, date, code);
    }
}
