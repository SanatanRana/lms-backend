package com.lms.common.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * SYSTEM DESIGN: JPA Attribute Converter for Role Enum
 * ────────────────────────────────────────────────────
 * Protects the system from crashing if the database contains lowercase roles 
 * (e.g. 'admin', 'teacher', 'student') by converting them to uppercase before mapping.
 */
@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role role) {
        if (role == null) {
            return null;
        }
        return role.name(); // Stores as uppercase, e.g., "ADMIN"
    }

    @Override
    public Role convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(dbData.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown database value for Role enum: '" + dbData + "'");
            return null;
        }
    }
}
