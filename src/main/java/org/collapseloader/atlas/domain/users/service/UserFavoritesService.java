package org.collapseloader.atlas.domain.users.service;

import org.collapseloader.atlas.domain.users.dto.request.UserFavoriteRequest;
import org.collapseloader.atlas.domain.users.dto.response.UserFavoriteResponse;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.entity.UserFavorite;
import org.collapseloader.atlas.domain.users.repository.UserFavoriteRepository;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UserFavoritesService {
    private final UserRepository userRepository;
    private final UserFavoriteRepository userFavoriteRepository;

    public UserFavoritesService(UserRepository userRepository, UserFavoriteRepository userFavoriteRepository) {
        this.userRepository = userRepository;
        this.userFavoriteRepository = userFavoriteRepository;
    }

    @Transactional(readOnly = true)
    public List<UserFavoriteResponse> getFavorites(User principal) {
        return userFavoriteRepository.findByUserId(principal.getId()).stream()
                .map(this::mapFavorite)
                .toList();
    }

    @Transactional
    public UserFavoriteResponse addFavorite(User principal, UserFavoriteRequest request) {
        var user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (request == null) {
            throw new RuntimeException("Favorite payload is required");
        }
        String type = normalizeType(request.type());
        String reference = normalizeReference(request.reference());
        if (type == null || reference == null) {
            throw new RuntimeException("Favorite type and reference are required");
        }

        var favorite = UserFavorite.builder()
                .user(user)
                .type(type)
                .reference(reference)
                .metadata(request.metadata())
                .build();
        return mapFavorite(userFavoriteRepository.save(favorite));
    }

    @Transactional
    public void deleteFavorite(User principal, Long favoriteId) {
        var favorite = userFavoriteRepository.findByIdAndUserId(favoriteId, principal.getId())
                .orElseThrow(() -> new RuntimeException("Favorite not found"));
        userFavoriteRepository.delete(favorite);
    }

    private UserFavoriteResponse mapFavorite(UserFavorite favorite) {
        return new UserFavoriteResponse(
                favorite.getId(),
                favorite.getType(),
                favorite.getReference(),
                favorite.getMetadata(),
                favorite.getCreatedAt()
        );
    }

    private String normalizeType(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private String normalizeReference(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
