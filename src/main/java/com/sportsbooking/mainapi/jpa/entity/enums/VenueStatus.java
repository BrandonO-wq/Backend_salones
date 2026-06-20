package com.sportsbooking.mainapi.jpa.entity.enums;

/**
 * Estado lógico de Venue en sistema.
 * <ul>
 *     <li>INACTIVE (0) - Equivalente a eliminado (Soft Delete) o sin publicar.</li>
 *     <li>ACTIVE (1) - Venue operativo y visible.</li>
 * </ul>
 */
public enum VenueStatus {
    INACTIVE(0),
    ACTIVE(1);

    private final int value;

    VenueStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static VenueStatus fromValue(Integer value) {
        if (value == null) return null;
        for (VenueStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown VenueStatus value: " + value);
    }
}
