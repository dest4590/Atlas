package org.collapseloader.atlas.domain.users.dto.response;

import org.collapseloader.atlas.domain.users.entity.SocialPlatform;

public record SocialLinkResponse(SocialPlatform platform, String url) {
}
