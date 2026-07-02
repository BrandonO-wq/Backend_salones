package com.sportsbooking.mainapi.jpa.entity.enums;

public enum HostDebtStatus {
    PENDING(1),
    PAID(2),
    OVERDUE(3),
    LEGACY_PENDING(0);

    private final int value;

    HostDebtStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static HostDebtStatus fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        if (value == 0) {
            return PENDING;
        }
        for (HostDebtStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid HostDebtStatus value: " + value);
    }
}
