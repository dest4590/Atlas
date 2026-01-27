package org.collapseloader.atlas.domain.presets.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PresetThemeResponse(
        @JsonProperty("customCSS")
        String customCSS,
        @JsonProperty("enableCustomCSS")
        boolean enableCustomCSS,

        String base100,
        String base200,
        String base300,
        String baseContent,

        String primary,
        String primaryContent,

        String secondary,
        String secondaryContent,

        String accent,
        String accentContent,

        String neutral,
        String neutralContent,

        String info,
        String infoContent,

        String success,
        String successContent,

        String warning,
        String warningContent,

        String error,
        String errorContent
) {
}
