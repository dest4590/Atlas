package org.collapseloader.atlas.domain.clients.service;

import org.collapseloader.atlas.domain.clients.dto.response.ForgeClientResponse;
import org.collapseloader.atlas.domain.clients.entity.ClientType;
import org.collapseloader.atlas.domain.clients.entity.forge.ForgeClient;
import org.collapseloader.atlas.domain.clients.repository.ForgeClientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ForgeClientService {
    private final ForgeClientRepository forgeClientRepository;

    public ForgeClientService(ForgeClientRepository forgeClientRepository) {
        this.forgeClientRepository = forgeClientRepository;
    }

    @Transactional(readOnly = true)
    public List<ForgeClientResponse> getAll() {
        return forgeClientRepository.findAllByType(ClientType.FORGE).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ForgeClientResponse getById(Long id) {
        var client = forgeClientRepository.findByIdAndType(id, ClientType.FORGE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Forge client not found: " + id));
        return toResponse(client);
    }

    private ForgeClientResponse toResponse(ForgeClient client) {
        return new ForgeClientResponse(
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
                client.getCreatedAt()
        );
    }
}
