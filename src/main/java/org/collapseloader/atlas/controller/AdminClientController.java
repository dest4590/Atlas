package org.collapseloader.atlas.controller;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.clients.dto.request.AdminClientRequest;
import org.collapseloader.atlas.domain.clients.dto.response.ClientResponse;
import org.collapseloader.atlas.domain.clients.entity.Client;
import org.collapseloader.atlas.domain.clients.repository.ClientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/admin/clients")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminClientController {

    private final ClientRepository clientRepository;
    private final org.collapseloader.atlas.domain.audit.AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<ClientResponse>> getAllClients() {
        var list = clientRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClient(
            @PathVariable Long id) {
        return clientRepository.findById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Client> createClient(@RequestBody AdminClientRequest request) {
        Client client = new Client();
        mapRequestToClient(request, client);
        var saved = clientRepository.save(client);

        auditLogService.log("CREATE_CLIENT", "CLIENT", saved.getId().toString(),
                Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication())
                        .getName(),
                "Created client: " + saved.getName());

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Client> updateClient(@PathVariable Long id, @RequestBody AdminClientRequest request) {
        return clientRepository.findById(id)
                .map(client -> {
                    mapRequestToClient(request, client);
                    var saved = clientRepository.save(client);

                    auditLogService.log("UPDATE_CLIENT", "CLIENT", saved.getId().toString(),
                            Objects.requireNonNull(SecurityContextHolder.getContext()
                                    .getAuthentication()).getName(),
                            "Updated client: " + saved.getName());

                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        if (clientRepository.existsById(id)) {
            var client = clientRepository.findById(id).orElse(null);
            String clientName = client != null ? client.getName() : "Unknown";

            clientRepository.deleteById(id);

            auditLogService.log("DELETE_CLIENT", "CLIENT", id.toString(),
                    Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication())
                            .getName(),
                    "Deleted client: " + clientName);

            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private void mapRequestToClient(AdminClientRequest request, Client client) {
        client.setName(request.name());
        client.setVersion(request.version());
        client.setFilename(request.filename());
        client.setMd5Hash(request.md5Hash());
        if (request.size() != null) {
            client.setSize(request.size());
        }
        client.setMainClass(request.mainClass());
        if (request.show() != null) {
            client.setShow(request.show());
        }
        if (request.working() != null) {
            client.setWorking(request.working());
        }

        if (request.launches() != null) {
            client.setLaunches(request.launches());
        }
        if (request.downloads() != null) {
            client.setDownloads(request.downloads());
        }
        if (request.type() != null) {
            client.setType(request.type());
        }
    }

    private ClientResponse toResponse(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getVersion() != null ? client.getVersion().getApiValue() : null,
                client.getFilename(),
                client.getMd5Hash(),
                client.getSize(),
                client.getMainClass(),
                client.isShow(),
                client.isWorking(),
                client.getLaunches(),
                client.getDownloads(),
                client.getType() != null ? client.getType().getApiValue() : null,
                client.getCreatedAt());
    }
}
