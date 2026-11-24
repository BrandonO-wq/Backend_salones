package com.sportsbooking.mainapi.jpa.entity.converter;

import com.sportsbooking.mainapi.jpa.entity.enums.HostDebtStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class HostDebtStatusConverter implements AttributeConverter<HostDebtStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(HostDebtStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public HostDebtStatus convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return HostDebtStatus.fromValue(dbData);
    }
}
