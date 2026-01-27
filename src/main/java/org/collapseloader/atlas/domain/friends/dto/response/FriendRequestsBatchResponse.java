package org.collapseloader.atlas.domain.friends.dto.response;

import java.util.List;

public record FriendRequestsBatchResponse(
                List<FriendRequestResponse> sent,
                List<FriendRequestResponse> received,
                List<FriendRequestResponse> blocked) {
}
