package org.collapseloader.atlas.domain.users.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.collapseloader.atlas.dto.ApiResponse;
import org.collapseloader.atlas.domain.users.dto.request.UserFavoriteRequest;
import org.collapseloader.atlas.domain.users.dto.response.UserFavoriteResponse;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.service.UserFavoritesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserFavoritesController {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final UserFavoritesService userFavoritesService;

    public UserFavoritesController(UserFavoritesService userFavoritesService) {
        this.userFavoritesService = userFavoritesService;
    }

    private static String stringValue(Map<String, Object> payload, String field) {
        var value = payload.get(field);
        if (value == null) {
            return null;
        }
        String stringValue = value instanceof String s ? s : value.toString();
        String trimmed = stringValue.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static JsonNode asJsonNode(Object value) {
        if (value == null) {
            return null;
        }
        return OBJECT_MAPPER.valueToTree(value);
    }

    @GetMapping("/me/favorites")
    public ResponseEntity<ApiResponse<List<UserFavoriteResponse>>> getFavorites(Authentication authentication) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(userFavoritesService.getFavorites(user)));
    }

    @PostMapping("/me/favorites")
    public ResponseEntity<ApiResponse<UserFavoriteResponse>> addFavorite(
            Authentication authentication,
            @RequestBody Map<String, Object> payload
    ) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                userFavoritesService.addFavorite(user, mapToRequest(payload))));
    }

    @DeleteMapping("/me/favorites/{favoriteId}")
    public ResponseEntity<ApiResponse<Void>> deleteFavorite(
            Authentication authentication,
            @PathVariable Long favoriteId
    ) {
        var user = requireUser(authentication);
        userFavoritesService.deleteFavorite(user, favoriteId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new RuntimeException("Unauthorized");
        }
        return user;
    }

    private UserFavoriteRequest mapToRequest(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        return new UserFavoriteRequest(
                stringValue(payload, "type"),
                stringValue(payload, "reference"),
                asJsonNode(payload.get("metadata"))
        );
    }
}
