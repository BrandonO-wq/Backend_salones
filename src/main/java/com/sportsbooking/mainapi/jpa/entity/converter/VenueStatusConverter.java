package com.sportsbooking.mainapi.jpa.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.sportsbooking.mainapi.jpa.entity.enums.VenueStatus;

@Converter(autoApply = true)
public class VenueStatusConverter implements AttributeConverter<VenueStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(VenueStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public VenueStatus convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return VenueStatus.fromValue(dbData);
    }
}
