package org.collapseloader.atlas.domain.clients.controller;

import org.collapseloader.atlas.dto.ApiResponse;
import org.collapseloader.atlas.domain.clients.dto.response.FabricClientResponse;
import org.collapseloader.atlas.domain.clients.service.FabricClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fabric-clients")
public class FabricClientController {
    private final FabricClientService fabricClientService;

    public FabricClientController(FabricClientService fabricClientService) {
        this.fabricClientService = fabricClientService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FabricClientResponse>>> getFabricClients() {
        var data = fabricClientService.getAll();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FabricClientResponse>> getFabricClient(@PathVariable Long id) {
        var data = fabricClientService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
