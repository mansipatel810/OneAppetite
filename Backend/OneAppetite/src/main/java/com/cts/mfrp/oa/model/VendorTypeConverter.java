package com.cts.mfrp.oa.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class VendorTypeConverter implements AttributeConverter<VendorType, String> {

    @Override
    public String convertToDatabaseColumn(VendorType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public VendorType convertToEntityAttribute(String dbData) {
        return VendorType.fromString(dbData);
    }
}
