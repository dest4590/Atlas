package org.collapseloader.atlas.domain.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.collapseloader.atlas.domain.admin.dto.request.BroadcastRequest;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/broadcast")
@RequiredArgsConstructor
@Slf4j
public class AdminBroadcastController {

    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> broadcastMessage(@AuthenticationPrincipal User user, @Valid @RequestBody BroadcastRequest request) {
        log.info("Admin broadcasting message: {}, admin: {}, id: {}", request, user.getUsername(), user.getId());

        if ("users".equalsIgnoreCase(request.getTarget())) {
            messagingTemplate.convertAndSend("/topic/broadcast/users", request);
        } else if ("guests".equalsIgnoreCase(request.getTarget())) {
            messagingTemplate.convertAndSend("/topic/broadcast/guests", request);
        } else {
            messagingTemplate.convertAndSend("/topic/broadcast", request);
        }

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
