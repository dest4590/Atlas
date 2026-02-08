package org.collapseloader.atlas.domain.clients.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Version {
    V_1_21_11("1.21.11"),
    V_1_21_8("1.21.8"),
    V_1_21_1("1.21.1"),
    V_1_21_4("1.21.4"),
    V_1_16_5("1.16.5"),
    V_1_12_2("1.12.2"),
    V_1_8_9("1.8.9");

    private final String apiValue;

    Version(String apiValue) {
        this.apiValue = apiValue;
    }

    @JsonCreator
    public static Version fromValue(String value) {
        return Arrays.stream(values())
                .filter(version -> version.apiValue.equalsIgnoreCase(value) || version.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported version: " + value));
    }

    @JsonValue
    public String getApiValue() {
        return apiValue;
    }
}
