package org.collapseloader.atlas.domain.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.audit.AuditLogService;
import org.collapseloader.atlas.domain.serverads.ServerAdDto;
import org.collapseloader.atlas.domain.serverads.ServerAdRequest;
import org.collapseloader.atlas.domain.serverads.ServerAdService;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/server-ads")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminServerAdController {

    private final ServerAdService service;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<ServerAdDto>> getAll() {
        return ResponseEntity.ok(service.findAllDtos());
    }

    @PostMapping
    public ResponseEntity<ServerAdDto> create(@Valid @RequestBody ServerAdRequest request) {
        var ad = service.create(request);
        auditLogService.log("CREATE_SERVER_AD", "SERVER_AD", ad.getId().toString(),
                SecurityContextHolder.getContext().getAuthentication().getName(),
                "Created server ad: " + ad.getName() + " (" + ad.getIp() + ")");
        return ResponseEntity.ok(service.toDto(ad));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServerAdDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ServerAdRequest request) throws NotFoundException {
        var ad = service.update(id, request);
        auditLogService.log("UPDATE_SERVER_AD", "SERVER_AD", id.toString(),
                SecurityContextHolder.getContext().getAuthentication().getName(),
                "Updated server ad: " + ad.getName() + " (" + ad.getIp() + ")");
        return ResponseEntity.ok(service.toDto(ad));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws NotFoundException {
        service.delete(id);
        auditLogService.log("DELETE_SERVER_AD", "SERVER_AD", id.toString(),
                SecurityContextHolder.getContext().getAuthentication().getName(),
                "Deleted server ad #" + id);
        return ResponseEntity.noContent().build();
    }
}
