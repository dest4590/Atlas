package org.collapseloader.atlas.domain.clients.controller;

import org.collapseloader.atlas.ApiResponse;
import org.collapseloader.atlas.domain.clients.dto.request.ClientCommentRequest;
import org.collapseloader.atlas.domain.clients.dto.request.ClientCreateRequest;
import org.collapseloader.atlas.domain.clients.dto.request.ClientRatingRequest;
import org.collapseloader.atlas.domain.clients.dto.response.*;
import org.collapseloader.atlas.domain.clients.service.ClientCommentService;
import org.collapseloader.atlas.domain.clients.service.ClientDetailsService;
import org.collapseloader.atlas.domain.clients.service.ClientRatingService;
import org.collapseloader.atlas.domain.clients.service.ClientService;
import org.collapseloader.atlas.domain.users.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {
    private final ClientService clientService;
    private final ClientDetailsService detailsService;
    private final ClientRatingService ratingService;
    private final ClientCommentService commentService;

    public ClientController(
            ClientService clientService,
            ClientDetailsService detailsService,
            ClientRatingService ratingService,
            ClientCommentService commentService) {
        this.clientService = clientService;
        this.detailsService = detailsService;
        this.ratingService = ratingService;
        this.commentService = commentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ClientResponse>> createClient(@RequestBody ClientCreateRequest request) {
        var data = clientService.create(request);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClientResponse>>> getClients() {
        var data = clientService.getAll();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/download/{id}")
    public ResponseEntity<ApiResponse<ClientResponse>> incrementDownloads(@PathVariable Long id) {
        var data = clientService.incrementDownloads(id);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/launch/{id}")
    public ResponseEntity<ApiResponse<ClientResponse>> incrementLaunches(Authentication authentication,
            @PathVariable Long id) {
        User user = (authentication != null && authentication.getPrincipal() instanceof User u) ? u : null;
        var data = clientService.incrementLaunches(id, user);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{id}/detailed")
    public ResponseEntity<ApiResponse<ClientDetailedResponse>> getDetailed(@PathVariable Long id) {
        var data = detailsService.getDetailedInfo(id);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<List<ClientCommentResponse>>> getComments(@PathVariable Long id) {
        var data = commentService.getComments(id);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ClientCommentResponse>> addComment(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody ClientCommentRequest request) {
        var user = requireUser(authentication);
        var data = commentService.addComment(id, user, request != null ? request.content() : null);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @DeleteMapping("/{clientId}/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            Authentication authentication,
            @PathVariable Long clientId,
            @PathVariable Long commentId) {
        var user = requireUser(authentication);
        commentService.deleteComment(clientId, commentId, user);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/rating")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ClientRatingResponse>> submitRating(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody ClientRatingRequest request) {
        var user = requireUser(authentication);
        var data = ratingService.submitRating(id, user, request.rating());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{id}/rating")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ClientMyRatingResponse>> getMyRating(
            Authentication authentication,
            @PathVariable Long id) {
        var user = requireUser(authentication);
        var data = ratingService.getMyRating(id, user);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @DeleteMapping("/{id}/rating")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> removeRating(
            Authentication authentication,
            @PathVariable Long id) {
        var user = requireUser(authentication);
        ratingService.removeRating(id, user);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new RuntimeException("Unauthorized");
        }
        return user;
    }
}
