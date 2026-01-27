package org.collapseloader.atlas.controller;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.ApiResponse;
import org.collapseloader.atlas.domain.clients.dto.request.FabricDependenceRequest;
import org.collapseloader.atlas.domain.clients.entity.ClientType;
import org.collapseloader.atlas.domain.clients.entity.fabric.FabricDependence;
import org.collapseloader.atlas.domain.clients.repository.FabricClientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/clients")
public class AdminFabricDependenceController {
    private final FabricClientRepository fabricClientRepository;

    @GetMapping("/{clientId}/fabric-deps")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listDependencies(@PathVariable Long clientId) {
        var client = fabricClientRepository.findByIdAndType(clientId, ClientType.FABRIC)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fabric client not found"));

        var items = client.getDependencies().stream()
                .map(dep -> Map.<String, Object>of(
                        "id", dep.getId(),
                        "name", dep.getName(),
                        "md5Hash", dep.getMd5Hash(),
                        "size", dep.getSize()
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @PostMapping("/{clientId}/fabric-deps")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addDependency(
            @PathVariable Long clientId,
            @RequestBody FabricDependenceRequest request
    ) {
        var client = fabricClientRepository.findByIdAndType(clientId, ClientType.FABRIC)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fabric client not found"));

        boolean exists = client.getDependencies().stream()
                .anyMatch(dep -> dep.getName() != null && dep.getName().equalsIgnoreCase(request.name()));
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dependency already exists");
        }

        FabricDependence dep = new FabricDependence();
        dep.setClient(client);
        dep.setName(request.name());
        dep.setMd5Hash(request.md5Hash());
        dep.setSize(request.size());
        client.getDependencies().add(dep);

        fabricClientRepository.save(client);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "id", dep.getId(),
                "name", dep.getName(),
                "md5Hash", dep.getMd5Hash(),
                "size", dep.getSize()
        )));
    }

    @PutMapping("/{clientId}/fabric-deps/{depId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateDependency(
            @PathVariable Long clientId,
            @PathVariable Long depId,
            @RequestBody FabricDependenceRequest request
    ) {
        var client = fabricClientRepository.findByIdAndType(clientId, ClientType.FABRIC)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fabric client not found"));

        var dep = client.getDependencies().stream()
                .filter(d -> d.getId().equals(depId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dependency not found"));

        dep.setName(request.name());
        dep.setMd5Hash(request.md5Hash());
        dep.setSize(request.size());
        fabricClientRepository.save(client);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "id", dep.getId(),
                "name", dep.getName(),
                "md5Hash", dep.getMd5Hash(),
                "size", dep.getSize()
        )));
    }

    @DeleteMapping("/{clientId}/fabric-deps/{depId}")
    public ResponseEntity<ApiResponse<Void>> deleteDependency(
            @PathVariable Long clientId,
            @PathVariable Long depId
    ) {
        var client = fabricClientRepository.findByIdAndType(clientId, ClientType.FABRIC)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fabric client not found"));

        boolean removed = client.getDependencies().removeIf(dep -> dep.getId().equals(depId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dependency not found");
        }
        fabricClientRepository.save(client);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
