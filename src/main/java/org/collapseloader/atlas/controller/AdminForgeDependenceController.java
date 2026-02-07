package org.collapseloader.atlas.controller;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.ApiResponse;
import org.collapseloader.atlas.domain.clients.dto.request.ForgeDependenceRequest;
import org.collapseloader.atlas.domain.clients.entity.ClientType;
import org.collapseloader.atlas.domain.clients.entity.forge.ForgeDependence;
import org.collapseloader.atlas.domain.clients.repository.ForgeClientRepository;
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
public class AdminForgeDependenceController {
    private final ForgeClientRepository forgeClientRepository;

    @GetMapping("/{clientId}/forge-deps")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listDependencies(@PathVariable Long clientId) {
        var client = forgeClientRepository.findByIdAndType(clientId, ClientType.FORGE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Forge client not found"));

        var items = client.getDependencies().stream()
                .map(dep -> Map.<String, Object>of(
                        "id", dep.getId(),
                        "name", dep.getName(),
                        "md5Hash", dep.getMd5Hash(),
                        "size", dep.getSize()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @PostMapping("/{clientId}/forge-deps")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addDependency(
            @PathVariable Long clientId,
            @RequestBody ForgeDependenceRequest request) {
        var client = forgeClientRepository.findByIdAndType(clientId, ClientType.FORGE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Forge client not found"));

        boolean exists = client.getDependencies().stream()
                .anyMatch(dep -> dep.getName() != null
                        && dep.getName().equalsIgnoreCase(request.name()));
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dependency already exists");
        }

        ForgeDependence dep = new ForgeDependence();
        dep.setClient(client);
        dep.setName(request.name());
        dep.setMd5Hash(request.md5Hash());
        dep.setSize(request.size());
        client.getDependencies().add(dep);

        var savedClient = forgeClientRepository.save(client);

        var savedDep = savedClient.getDependencies().stream()
                .filter(d -> d.getName().equalsIgnoreCase(request.name()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to save dependency"));

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", savedDep.getId());
        response.put("name", savedDep.getName());
        response.put("md5Hash", savedDep.getMd5Hash());
        response.put("size", savedDep.getSize());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{clientId}/forge-deps/{depId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateDependency(
            @PathVariable Long clientId,
            @PathVariable Long depId,
            @RequestBody ForgeDependenceRequest request) {
        var client = forgeClientRepository.findByIdAndType(clientId, ClientType.FORGE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Forge client not found"));

        var dep = client.getDependencies().stream()
                .filter(d -> d.getId().equals(depId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Dependency not found"));

        dep.setName(request.name());
        dep.setMd5Hash(request.md5Hash());
        dep.setSize(request.size());
        forgeClientRepository.save(client);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", dep.getId());
        response.put("name", dep.getName());
        response.put("md5Hash", dep.getMd5Hash());
        response.put("size", dep.getSize());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{clientId}/forge-deps/{depId}")
    public ResponseEntity<ApiResponse<Void>> deleteDependency(
            @PathVariable Long clientId,
            @PathVariable Long depId) {
        var client = forgeClientRepository.findByIdAndType(clientId, ClientType.FORGE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Forge client not found"));

        boolean removed = client.getDependencies().removeIf(dep -> dep.getId().equals(depId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dependency not found");
        }
        forgeClientRepository.save(client);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
