package org.collapseloader.atlas.domain.users.dto.request;

import jakarta.validation.constraints.Size;
import org.collapseloader.atlas.domain.users.entity.SocialPlatform;

public record SocialLinkRequest(
        SocialPlatform platform,
        @Size(max = 2048, message = "url must be at most 2048 characters")
        String url) {
}
