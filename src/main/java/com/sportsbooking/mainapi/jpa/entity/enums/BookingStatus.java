package com.sportsbooking.mainapi.jpa.entity.enums;

import lombok.Getter;

@Getter
public enum BookingStatus {
    PENDING(1),
    REJECTED(2),
    CONFIRMED(3),
    COMPLETED(4);

    private final int value;

    BookingStatus(int value) {
        this.value = value;
    }

    public static BookingStatus fromValue(int value) {
        for (BookingStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid BookingStatus value: " + value);
    }
}
