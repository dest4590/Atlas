package org.collapseloader.atlas.domain.users.controller;

import org.collapseloader.atlas.domain.users.dto.request.UpdateSocialLinksRequest;
import org.collapseloader.atlas.domain.users.dto.response.SocialLinkResponse;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.service.UserSocialLinksService;
import org.collapseloader.atlas.dto.ApiResponse;
import org.collapseloader.atlas.exception.UnauthorizedException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserSocialLinksController {
    private final UserSocialLinksService userSocialLinksService;

    public UserSocialLinksController(UserSocialLinksService userSocialLinksService) {
        this.userSocialLinksService = userSocialLinksService;
    }

    @PutMapping("/me/social-links")
    public ResponseEntity<ApiResponse<List<SocialLinkResponse>>> replaceSocialLinks(
            Authentication authentication,
            @RequestBody UpdateSocialLinksRequest request) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(userSocialLinksService.replaceSocialLinks(user, request)));
    }

    @GetMapping("/me/social-links")
    public ResponseEntity<ApiResponse<List<SocialLinkResponse>>> getSocialLinks(Authentication authentication) {
        var user = requireUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(userSocialLinksService.getSocialLinks(user)));
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new UnauthorizedException("Unauthorized");
        }
        return user;
    }
}
