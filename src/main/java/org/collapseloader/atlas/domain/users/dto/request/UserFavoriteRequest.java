package org.collapseloader.atlas.domain.users.dto.request;

import com.fasterxml.jackson.databind.JsonNode;

public record UserFavoriteRequest(String type, String reference, JsonNode metadata) {
}
