package com.chuctupk.entity.enums;

/**
 * Enum ngày trong tuần của phòng khám (khớp với ENUM('MON','TUE','WED','THU','FRI','SAT','SUN') trong MySQL).
 * Cung cấp hàm chuyển đổi tường minh với java.time.DayOfWeek, không import lẫn để tránh nhập nhằng tên lớp.
 */
public enum DayOfWeek {
    MON,
    TUE,
    WED,
    THU,
    FRI,
    SAT,
    SUN;

    public static DayOfWeek from(java.time.DayOfWeek javaDayOfWeek) {
        if (javaDayOfWeek == null) {
            return null;
        }
        return switch (javaDayOfWeek) {
            case MONDAY -> MON;
            case TUESDAY -> TUE;
            case WEDNESDAY -> WED;
            case THURSDAY -> THU;
            case FRIDAY -> FRI;
            case SATURDAY -> SAT;
            case SUNDAY -> SUN;
        };
    }

    public java.time.DayOfWeek toJavaDayOfWeek() {
        return switch (this) {
            case MON -> java.time.DayOfWeek.MONDAY;
            case TUE -> java.time.DayOfWeek.TUESDAY;
            case WED -> java.time.DayOfWeek.WEDNESDAY;
            case THU -> java.time.DayOfWeek.THURSDAY;
            case FRI -> java.time.DayOfWeek.FRIDAY;
            case SAT -> java.time.DayOfWeek.SATURDAY;
            case SUN -> java.time.DayOfWeek.SUNDAY;
        };
    }
}
