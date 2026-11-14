package com.chuctupk.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Mã lỗi chuẩn hóa cho toàn hệ thống (máy đọc: SCREAMING_SNAKE, người đọc:
 * tiếng Việt thân thiện).
 */
@Getter
public enum ErrorCode {

        OK(HttpStatus.OK, "OK", "Thành công"),

        // Mã chuẩn HTTP
        BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Dữ liệu gửi lên không hợp lệ"),
        UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Yêu cầu đăng nhập để thực hiện thao tác này"),
        FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này"),
        NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy dữ liệu yêu cầu"),
        CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "Dữ liệu bị xung đột hoặc đã tồn tại"),
        UNPROCESSABLE(HttpStatus.UNPROCESSABLE_CONTENT, "UNPROCESSABLE", "Dữ liệu không thể xử lý"),
        TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS",
                        "Yêu cầu quá nhanh. Cô chú vui lòng chờ giây lát rồi thử lại"),
        INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                        "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau"),

    // Thao tác xác thực
    INVALID_CREDENTIALS(HttpStatus.BAD_REQUEST, "INVALID_CREDENTIALS", "Thông tin xác thực không chính xác"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu không hợp lệ"),

        // Mã lỗi nghiệp vụ FE phụ thuộc (BẮT BUỘC GIỮ NGUYÊN TÊN)
        SESSION_CHANGED(HttpStatus.CONFLICT, "SESSION_CHANGED",
                        "Phiên đăng nhập đã thay đổi ở thẻ khác. Trang sẽ tự động tải lại"),
        STALE(HttpStatus.CONFLICT, "STALE", "Dữ liệu đã được người khác cập nhật trước đó. Vui lòng tải lại trang"),
        SLOT_FULL(HttpStatus.CONFLICT, "SLOT_FULL",
                        "Khung giờ này đã đủ bệnh nhân. Cô chú chọn khung khác giúp phòng khám nhé"),

        CODE_WRONG(HttpStatus.BAD_REQUEST, "CODE_WRONG", "Mã xác nhận không đúng. Cô chú kiểm tra lại tin nhắn nhé"),
        CODE_EXPIRED(HttpStatus.BAD_REQUEST, "CODE_EXPIRED",
                        "Mã xác nhận đã hết hạn. Cô chú vui lòng yêu cầu gửi lại mã mới"),
        CODE_USED(HttpStatus.BAD_REQUEST, "CODE_USED", "Mã xác nhận này đã được sử dụng"),
        CODE_LOCKED(HttpStatus.FORBIDDEN, "CODE_LOCKED",
                        "Cô chú đã nhập sai mã quá số lần quy định. Vui lòng thử lại sau"),
        CODE_REQUIRED(HttpStatus.BAD_REQUEST, "CODE_REQUIRED", "Vui lòng nhập mã xác nhận 6 chữ số"),

        // Mã nghiệp vụ bổ sung
        PROFILE_LIMIT(HttpStatus.BAD_REQUEST, "PROFILE_LIMIT", "Tài khoản chỉ được tạo tối đa 10 hồ sơ người thân"),
        CANNOT_DELETE_PRIMARY(HttpStatus.BAD_REQUEST, "CANNOT_DELETE_PRIMARY",
                        "Không thể xóa hồ sơ chính chủ (Bản thân)"),
        NOT_CANCELLABLE(HttpStatus.BAD_REQUEST, "NOT_CANCELLABLE",
                        "Phiếu khám đã tiếp nhận hoặc đang khám, không thể hủy"),
        ROOM_FULL(HttpStatus.BAD_REQUEST, "ROOM_FULL", "Tất cả các ghế khám hiện đều đang có bệnh nhân"),
        RECORD_EXISTS(HttpStatus.CONFLICT, "RECORD_EXISTS", "Phiếu khám này đã được lập kết quả khám bệnh"),
        RECORD_EXPIRED(HttpStatus.FORBIDDEN, "RECORD_EXPIRED",
                        "Đã quá thời hạn 24 giờ cho phép chỉnh sửa phiếu kết quả"),
        WRONG_DAY(HttpStatus.BAD_REQUEST, "WRONG_DAY", "Chỉ có thể tiếp nhận cho lịch khám trong ngày hôm nay"),
        CLOSED(HttpStatus.BAD_REQUEST, "CLOSED", "Phòng khám đã hết giờ tiếp nhận bệnh nhân trong ngày"),
        DUPLICATE_PHONE(HttpStatus.CONFLICT, "DUPLICATE_PHONE", "Số điện thoại này đã có hồ sơ trong hệ thống"),
        USER_EXISTS(HttpStatus.CONFLICT, "USER_EXISTS", "Tài khoản với thông tin này đã tồn tại"),
        ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED",
                        "Tài khoản đang bị tạm khóa. Vui lòng liên hệ quản trị viên"),
        CANNOT_LOCK_SELF(HttpStatus.BAD_REQUEST, "CANNOT_LOCK_SELF",
                        "Quản trị viên không thể tự khóa tài khoản của chính mình");

        private final HttpStatus httpStatus;
        private final String code;
        private final String defaultMessage;

        ErrorCode(HttpStatus httpStatus, String code, String defaultMessage) {
                this.httpStatus = httpStatus;
                this.code = code;
                this.defaultMessage = defaultMessage;
        }

        public int getStatusCode() {
                return httpStatus.value();
        }
}
