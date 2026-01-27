package org.collapseloader.atlas.domain.presets.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record PresetCommentRequest(
        @JsonProperty("text")
        @JsonAlias("content")
        String text
) {
}
