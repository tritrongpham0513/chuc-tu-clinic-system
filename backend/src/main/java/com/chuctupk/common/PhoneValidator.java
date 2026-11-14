package com.chuctupk.common;

import com.chuctupk.exception.AppException;

import java.util.regex.Pattern;

/**
 * Kiểm tra và chuẩn hóa số điện thoại Việt Nam (10 chữ số, bắt đầu bằng 03, 05, 07, 08, 09).
 */
public final class PhoneValidator {

    private static final Pattern VN_PHONE_PATTERN = Pattern.compile("^(03|05|07|08|09)\\d{8}$");

    private PhoneValidator() {}

    public static String clean(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("\\D", "");
    }

    public static boolean isValid(String phone) {
        if (phone == null || phone.isBlank()) return false;
        String digits = clean(phone);
        if (digits.startsWith("84") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        }
        return VN_PHONE_PATTERN.matcher(digits).matches();
    }

    public static String validate(String phone) {
        if (phone == null || phone.isBlank()) {
            throw AppException.badRequest("Cô chú vui lòng nhập số điện thoại");
        }
        String digits = clean(phone);
        if (digits.startsWith("84") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        }
        if (!VN_PHONE_PATTERN.matcher(digits).matches()) {
            throw AppException.badRequest("Số điện thoại không đúng định dạng Việt Nam (10 chữ số, bắt đầu bằng 03, 05, 07, 08, 09)");
        }
        return digits;
    }

    public static String mask(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        String digits = clean(phone);
        if (digits.length() >= 8) {
            return digits.substring(0, 4) + " *** " + digits.substring(digits.length() - 3);
        }
        return phone;
    }
}
