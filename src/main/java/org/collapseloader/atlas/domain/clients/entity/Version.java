package org.collapseloader.atlas.domain.clients.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Version implements Serializable {
    public static final Version V_1_21_11 = new Version("1.21.11");
    public static final Version V_1_21_8 = new Version("1.21.8");
    public static final Version V_1_21_1 = new Version("1.21.1");
    public static final Version V_1_21_4 = new Version("1.21.4");
    public static final Version V_1_16_5 = new Version("1.16.5");
    public static final Version V_1_12_2 = new Version("1.12.2");
    public static final Version V_1_8_9 = new Version("1.8.9");
    public static final List<Version> KNOWN_VERSIONS = List.of(
            V_1_21_11, V_1_21_8, V_1_21_1, V_1_21_4, V_1_16_5, V_1_12_2, V_1_8_9
    );
    private String apiValue;

    @JsonCreator
    public static Version fromValue(String value) {
        if (value == null) return null;
        return new Version(value);
    }

    @JsonValue
    public String getApiValue() {
        return apiValue;
    }

    @Override
    public String toString() {
        return apiValue;
    }
}
