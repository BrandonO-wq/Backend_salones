package com.sportsbooking.mainapi.jpa.entity.converter;

import com.sportsbooking.mainapi.jpa.entity.enums.HostDebtStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HostDebtStatusConverterTest {

    private final HostDebtStatusConverter converter = new HostDebtStatusConverter();

    @Test
    void convertsLegacyZeroStatusToPending() {
        assertEquals(HostDebtStatus.PENDING, converter.convertToEntityAttribute(0));
    }

    @Test
    void convertsKnownValuesToEnum() {
        assertEquals(HostDebtStatus.PENDING, converter.convertToEntityAttribute(1));
        assertEquals(HostDebtStatus.PAID, converter.convertToEntityAttribute(2));
        assertEquals(HostDebtStatus.OVERDUE, converter.convertToEntityAttribute(3));
    }
}
