package org.collapseloader.atlas.domain.presets.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PresetCommentRequest(
        @JsonProperty("text")
        @JsonAlias("content")
        @NotBlank(message = "text is required")
        @Size(max = 2000, message = "text must be at most 2000 characters")
        String text
) {
}
