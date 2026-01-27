package org.collapseloader.atlas.domain.users.dto.request;

import java.util.List;

public record UpdateSocialLinksRequest(List<SocialLinkRequest> links) {
}
