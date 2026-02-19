package org.collapseloader.atlas.domain.clients.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.collapseloader.atlas.domain.clients.entity.Version;

@Converter(autoApply = true)
public class VersionConverter implements AttributeConverter<Version, String> {
    @Override
    public String convertToDatabaseColumn(Version version) {
        return version != null ? version.getApiValue() : null;
    }

    @Override
    public Version convertToEntityAttribute(String dbData) {
        return dbData != null ? Version.fromValue(dbData) : null;
    }
}
