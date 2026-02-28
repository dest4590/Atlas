package org.collapseloader.atlas.domain.presets.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PresetUpsertRequest(
        @JsonProperty("name") @JsonAlias("title")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,
        @Size(max = 2048, message = "description must be at most 2048 characters")
        String description,
        @JsonProperty("is_public") @JsonAlias("isPublic") Boolean isPublic,

        @JsonProperty("customCSS") @JsonAlias("custom_css")
        @Size(max = 20000, message = "customCSS must be at most 20000 characters")
        String customCSS,
        @JsonProperty("enableCustomCSS") @JsonAlias("enable_custom_css") Boolean enableCustomCSS,

        @JsonAlias({
                "base100", "base_100"}) String base100,
        @JsonAlias({"base200", "base_200"}) String base200,
        @JsonAlias({"base300", "base_300"}) String base300,
        @JsonProperty("baseContent") @JsonAlias({"baseContent", "base_content"}) String baseContent,

        @JsonAlias({"primary"}) String primary,
        @JsonProperty("primaryContent") @JsonAlias({"primaryContent",
                "primary_content"}) String primaryContent,

        @JsonAlias({"secondary"}) String secondary,
        @JsonProperty("secondaryContent") @JsonAlias({"secondaryContent",
                "secondary_content"}) String secondaryContent,

        @JsonAlias({"accent"}) String accent,
        @JsonProperty("accentContent") @JsonAlias({"accentContent", "accent_content"}) String accentContent,

        @JsonAlias({"neutral"}) String neutral,
        @JsonProperty("neutralContent") @JsonAlias({"neutralContent",
                "neutral_content"}) String neutralContent,

        @JsonAlias({"info"}) String info,
        @JsonProperty("infoContent") @JsonAlias({"infoContent", "info_content"}) String infoContent,

        @JsonAlias({"success"}) String success,
        @JsonProperty("successContent") @JsonAlias({"successContent",
                "success_content"}) String successContent,

        @JsonAlias({"warning"}) String warning,
        @JsonProperty("warningContent") @JsonAlias({"warningContent",
                "warning_content"}) String warningContent,

        @JsonAlias({"error"}) String error,
        @JsonProperty("errorContent") @JsonAlias({"errorContent", "error_content"}) String errorContent,

        @JsonProperty("backgroundImage") @JsonAlias({"backgroundImage",
                "background_image"}) String backgroundImage,

        @JsonProperty("backgroundBlur") @JsonAlias({"backgroundBlur",
                "background_blur"})
        @PositiveOrZero(message = "backgroundBlur must be greater than or equal to 0")
        Double backgroundBlur,

        @JsonProperty("backgroundOpacity") @JsonAlias({"backgroundOpacity",
                "background_opacity"})
        @DecimalMin(value = "0.0", message = "backgroundOpacity must be between 0 and 1")
        @DecimalMax(value = "1.0", message = "backgroundOpacity must be between 0 and 1")
        Double backgroundOpacity) {
}
