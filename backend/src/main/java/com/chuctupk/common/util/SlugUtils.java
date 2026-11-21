package com.chuctupk.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s_]+");

    private SlugUtils() {
    }

    /**
     * Chuẩn hoá chuỗi sang định dạng slug:
     * - Chữ thường
     * - Bỏ dấu tiếng Việt (đ, Đ chuyển thành d)
     * - Khoảng trắng và gạch dưới chuyển thành gạch nối
     * - Loại bỏ ký tự đặc biệt
     * - Gom nhiều gạch nối liên tiếp thành 1 gạch nối
     */
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String s = input.replace('đ', 'd').replace('Đ', 'd').trim();
        String nowhitespace = WHITESPACE.matcher(s).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-+", "-").replaceAll("^-|-$", "");
    }
}
