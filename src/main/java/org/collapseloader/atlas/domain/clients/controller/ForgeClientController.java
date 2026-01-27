package org.collapseloader.atlas.domain.clients.controller;

import org.collapseloader.atlas.ApiResponse;
import org.collapseloader.atlas.domain.clients.dto.response.ForgeClientResponse;
import org.collapseloader.atlas.domain.clients.service.ForgeClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/forge-clients")
public class ForgeClientController {
    private final ForgeClientService forgeClientService;

    public ForgeClientController(ForgeClientService forgeClientService) {
        this.forgeClientService = forgeClientService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ForgeClientResponse>>> getForgeClients() {
        var data = forgeClientService.getAll();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ForgeClientResponse>> getForgeClient(@PathVariable Long id) {
        var data = forgeClientService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
