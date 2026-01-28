package org.collapseloader.atlas.domain.users.dto.response;

import org.collapseloader.atlas.domain.friends.dto.response.FriendsBatchResponse;

import java.util.List;

public record UserInitResponse(
        UserMeResponse user,
        List<UserPreferenceResponse> preferences,
        List<UserFavoriteResponse> favorites,
        List<UserExternalAccountResponse> accounts,
        FriendsBatchResponse friends) {
}
