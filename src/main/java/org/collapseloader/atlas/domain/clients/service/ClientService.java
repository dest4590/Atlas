package org.collapseloader.atlas.domain.clients.service;

import org.collapseloader.atlas.domain.clients.dto.request.ClientCreateRequest;
import org.collapseloader.atlas.domain.clients.dto.response.ClientResponse;
import org.collapseloader.atlas.domain.clients.entity.Client;
import org.collapseloader.atlas.domain.clients.entity.ClientType;
import org.collapseloader.atlas.domain.clients.entity.fabric.FabricClient;
import org.collapseloader.atlas.domain.clients.entity.forge.ForgeClient;
import org.collapseloader.atlas.domain.clients.repository.ClientRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ClientService {
    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @CacheEvict(value = "clients_list", allEntries = true)
    public ClientResponse create(ClientCreateRequest request) {
        ClientType type = request.clientType() == null ? ClientType.Vanilla : request.clientType();
        Client client = switch (type) {
            case FABRIC -> new FabricClient();
            case FORGE -> new ForgeClient();
            default -> new Client();
        };
        client.setName(request.name());
        client.setVersion(request.version());
        client.setType(type);
        client.setFilename(request.filename());
        client.setMd5Hash(request.md5Hash());
        client.setSize(request.size() == null ? 0L : request.size());
        client.setMainClass(request.mainClass());
        client.setShow(Boolean.TRUE.equals(request.show()));
        client.setWorking(Boolean.TRUE.equals(request.working()));
        client.setLaunches(request.launches() == null ? 0L : request.launches());
        client.setDownloads(request.downloads() == null ? 0L : request.downloads());

        var saved = clientRepository.save(client);
        return toResponse(saved);
    }

    @Cacheable("clients_list")
    public List<ClientResponse> getAll() {
        return clientRepository.findAllByType(ClientType.Vanilla).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ClientResponse incrementDownloads(Long id) {
        var client = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: " + id));
        client.setDownloads(client.getDownloads() + 1);
        return toResponse(client);
    }

    @Transactional
    public ClientResponse incrementLaunches(Long id) {
        var client = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: " + id));
        client.setLaunches(client.getLaunches() + 1);
        return toResponse(client);
    }

    private ClientResponse toResponse(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getVersion().getApiValue(),
                client.getFilename(),
                client.getMd5Hash(),
                client.getSize(),
                client.getMainClass(),
                client.isShow(),
                client.isWorking(),
                client.getLaunches(),
                client.getDownloads(),
                client.getType().getApiValue(),
                client.getCreatedAt()
        );
    }
}
