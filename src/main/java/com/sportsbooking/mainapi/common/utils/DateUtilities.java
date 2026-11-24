package com.sportsbooking.mainapi.common.utils;

import java.time.*;

public abstract class DateUtilities {

    public static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");

    public static LocalDateTime currentLocalDateTime() {
        return ZonedDateTime.now(LIMA_ZONE).toLocalDateTime();
    }

    public static LocalDate currentLocalDate() {
        return LocalDate.now(LIMA_ZONE);
    }

    public static LocalTime currentLocalTime() {
        return LocalTime.now(LIMA_ZONE);
    }

    public static String formatMinutesToText(int minutes) {
        if (minutes == 60) return "1 hora";
        if (minutes == 120) return "2 horas";
        if (minutes < 60) return minutes + " minutos";
        
        int h = minutes / 60;
        int m = minutes % 60;
        if (m == 0) {
            return h + " horas";
        }
        return h + " hora" + (h > 1 ? "s" : "") + " y " + m + " minutos";
    }
}
