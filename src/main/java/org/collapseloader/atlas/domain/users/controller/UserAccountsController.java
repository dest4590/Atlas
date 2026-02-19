package org.collapseloader.atlas.domain.users.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.coyote.BadRequestException;
import org.collapseloader.atlas.domain.users.dto.request.UserExternalAccountRequest;
import org.collapseloader.atlas.domain.users.dto.response.UserExternalAccountResponse;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.service.UserExternalAccountsService;
import org.collapseloader.atlas.dto.ApiResponse;
import org.collapseloader.atlas.exception.UnauthorizedException;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserAccountsController {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final UserExternalAccountsService userExternalAccountsService;

    public UserAccountsController(UserExternalAccountsService userExternalAccountsService) {
        this.userExternalAccountsService = userExternalAccountsService;
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

    @GetMapping("/me/accounts")
    public ResponseEntity<ApiResponse<List<UserExternalAccountResponse>>> getAccounts(Authentication authentication) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(userExternalAccountsService.getExternalAccounts(user)));
    }

    @PostMapping("/me/accounts")
    public ResponseEntity<ApiResponse<UserExternalAccountResponse>> addAccount(
            Authentication authentication,
            @RequestBody Map<String, Object> payload) throws BadRequestException, NotFoundException {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                userExternalAccountsService.addExternalAccount(user, mapToRequest(payload))));
    }

    @DeleteMapping("/me/accounts/{accountId}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            Authentication authentication,
            @PathVariable Long accountId) throws BadRequestException, NotFoundException {
        var user = requireUser(authentication);
        userExternalAccountsService.deleteExternalAccount(user, accountId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new UnauthorizedException("Unauthorized");
        }
        return user;
    }

    private UserExternalAccountRequest mapToRequest(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        return new UserExternalAccountRequest(
                stringValue(payload, "display_name"),
                asJsonNode(payload.get("metadata")));
    }
}
