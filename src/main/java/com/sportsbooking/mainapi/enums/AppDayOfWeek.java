package com.sportsbooking.mainapi.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AppDayOfWeek {
    MONDAY(1),
    TUESDAY(2),
    WEDNESDAY(3),
    THURSDAY(4),
    FRIDAY(5),
    SATURDAY(6),
    SUNDAY(7);

    private final int value;

    AppDayOfWeek(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    public static AppDayOfWeek fromValue(int value) {
        for (AppDayOfWeek day : values()) {
            if (day.getValue() == value) {
                return day;
            }
        }
        throw new IllegalArgumentException("Unknown AppDayOfWeek value: " + value);
    }
}
