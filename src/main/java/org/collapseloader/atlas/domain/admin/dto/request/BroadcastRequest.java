package org.collapseloader.atlas.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BroadcastRequest {
    @NotBlank
    private String message;
    private String type; // "info", "warning", "error"
    private boolean sticky;
    private String target; // "all", "users", "guests"
}
