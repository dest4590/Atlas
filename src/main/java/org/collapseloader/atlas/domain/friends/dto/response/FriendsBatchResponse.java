package org.collapseloader.atlas.domain.friends.dto.response;

import java.util.List;

public record FriendsBatchResponse(
        List<FriendResponse> friends,
        FriendRequestsBatchResponse requests
) {
}
