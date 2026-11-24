package com.sportsbooking.mainapi.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ScheduleStatus {
    INACTIVE(0),
    ACTIVE(1);

    private final int value;

    ScheduleStatus(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    public static ScheduleStatus fromValue(int value) {
        for (ScheduleStatus status : values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ScheduleStatus value: " + value);
    }
}
