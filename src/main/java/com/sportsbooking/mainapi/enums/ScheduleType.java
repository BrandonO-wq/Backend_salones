package com.sportsbooking.mainapi.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ScheduleType {
    DAY(1),
    NIGHT(2);

    private final int value;

    ScheduleType(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    public static ScheduleType fromValue(int value) {
        for (ScheduleType type : values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ScheduleType value: " + value);
    }
}
