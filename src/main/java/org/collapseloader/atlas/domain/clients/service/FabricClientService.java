package org.collapseloader.atlas.domain.clients.service;

import org.collapseloader.atlas.domain.clients.dto.response.FabricClientResponse;
import org.collapseloader.atlas.domain.clients.dto.response.FabricDependenceResponse;
import org.collapseloader.atlas.domain.clients.entity.ClientType;
import org.collapseloader.atlas.domain.clients.entity.fabric.FabricClient;
import org.collapseloader.atlas.domain.clients.repository.FabricClientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FabricClientService {
    private final FabricClientRepository fabricClientRepository;

    public FabricClientService(FabricClientRepository fabricClientRepository) {
        this.fabricClientRepository = fabricClientRepository;
    }

    @Transactional(readOnly = true)
    public List<FabricClientResponse> getAll() {
        return fabricClientRepository.findAllByType(ClientType.FABRIC).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FabricClientResponse getById(Long id) {
        var client = fabricClientRepository.findByIdAndType(id, ClientType.FABRIC)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fabric client not found: " + id));
        return toResponse(client);
    }

    private FabricClientResponse toResponse(FabricClient client) {
        var dependencies = client.getDependencies().stream()
                .map(dep -> new FabricDependenceResponse(
                        dep.getName(),
                        dep.getMd5Hash(),
                        dep.getSize()
                ))
                .toList();

        return new FabricClientResponse(
                client.getId(),
                client.getName(),
                client.getVersion(),
                client.getFilename(),
                client.getMd5Hash(),
                client.getSize(),
                client.getMainClass(),
                client.isShow(),
                client.isWorking(),
                client.getLaunches(),
                client.getDownloads(),
                client.getType() != null ? client.getType().getApiValue() : null,
                client.getCreatedAt(),
                dependencies
        );
    }
}
