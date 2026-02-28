package org.collapseloader.atlas.domain.users.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateSocialLinksRequest(
        @Size(max = 25, message = "links must contain at most 25 items")
        List<@Valid SocialLinkRequest> links) {
}
