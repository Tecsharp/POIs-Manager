package com.tecsharp.apis.pois.models.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecsharp.apis.pois.models.BusinessHours;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;

@Converter
public class BusinessHoursConverter implements AttributeConverter<BusinessHours, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(BusinessHours attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting BusinessHours to JSON", e);
        }
    }

    @Override
    public BusinessHours convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, BusinessHours.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error converting JSON to BusinessHours", e);
        }
    }
}

