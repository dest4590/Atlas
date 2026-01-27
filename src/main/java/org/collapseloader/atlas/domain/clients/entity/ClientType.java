package org.collapseloader.atlas.domain.clients.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ClientType {
    Vanilla("default"),
    FORGE("forge"),
    FABRIC("fabric");

    private final String apiValue;

    ClientType(String apiValue) {
        this.apiValue = apiValue;
    }

    @JsonCreator
    public static ClientType fromValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.apiValue.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported client type: " + value));
    }

    @JsonValue
    public String getApiValue() {
        return apiValue;
    }
}
