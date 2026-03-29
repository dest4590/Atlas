package org.collapseloader.atlas.domain.serverads;

import org.collapseloader.atlas.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/server-ads")
public class ServerAdController {

    private final ServerAdService service;

    public ServerAdController(ServerAdService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServerAdDto>>> getActiveServerAds() {
        return ResponseEntity.ok(ApiResponse.success(service.findAllActiveDtos()));
    }
}
