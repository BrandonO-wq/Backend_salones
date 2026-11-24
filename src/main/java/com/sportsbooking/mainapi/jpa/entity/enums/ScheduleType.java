package com.sportsbooking.mainapi.jpa.entity.enums;

public enum ScheduleType {
    DAY(1),
    NIGHT(2);

    private final int value;

    ScheduleType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ScheduleType fromValue(Integer value) {
        if (value == null) return null;
        for (ScheduleType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ScheduleType value: " + value);
    }
}
