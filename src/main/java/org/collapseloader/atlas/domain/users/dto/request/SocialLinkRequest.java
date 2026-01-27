package org.collapseloader.atlas.domain.users.dto.request;

import org.collapseloader.atlas.domain.users.entity.SocialPlatform;

public record SocialLinkRequest(SocialPlatform platform, String url) {
}
