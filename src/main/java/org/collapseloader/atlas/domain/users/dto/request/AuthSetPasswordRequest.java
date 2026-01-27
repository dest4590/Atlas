package org.collapseloader.atlas.domain.users.dto.request;

public record AuthSetPasswordRequest(String newPassword, String currentPassword) {
}
