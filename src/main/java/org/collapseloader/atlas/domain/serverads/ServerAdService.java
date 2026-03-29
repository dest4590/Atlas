package org.collapseloader.atlas.domain.serverads;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServerAdService {

    private final ServerAdRepository repository;

    public ServerAdService(ServerAdRepository repository) {
        this.repository = repository;
    }

    @Cacheable("server_ads_active")
    public List<ServerAd> findAllActive() {
        return repository.findAllByActiveTrue();
    }

    public List<ServerAd> findAll() {
        return repository.findAll();
    }

    @CacheEvict(value = "server_ads_active", allEntries = true)
    public ServerAd create(ServerAdRequest request) {
        ServerAd ad = new ServerAd();
        ad.setName(request.getName());
        ad.setIp(request.getIp());
        ad.setActive(request.getActive() != null ? request.getActive() : true);
        return repository.save(ad);
    }

    @CacheEvict(value = "server_ads_active", allEntries = true)
    public ServerAd update(Long id, ServerAdRequest request) throws NotFoundException {
        ServerAd ad = repository.findById(id).orElseThrow(NotFoundException::new);
        if (request.getName() != null) ad.setName(request.getName());
        if (request.getIp() != null) ad.setIp(request.getIp());
        if (request.getActive() != null) ad.setActive(request.getActive());
        return repository.save(ad);
    }

    @CacheEvict(value = "server_ads_active", allEntries = true)
    public void delete(Long id) throws NotFoundException {
        if (!repository.existsById(id)) throw new NotFoundException();
        repository.deleteById(id);
    }

    public ServerAdDto toDto(ServerAd ad) {
        return new ServerAdDto(
                ad.getId(),
                ad.getName(),
                ad.getIp(),
                ad.isActive(),
                ad.getCreatedAt(),
                ad.getUpdatedAt()
        );
    }

    public ServerAdDto findById(Long id) throws NotFoundException {
        return repository.findById(id).map(this::toDto).orElseThrow(NotFoundException::new);
    }

    public List<ServerAdDto> findAllActiveDtos() {
        return findAllActive().stream().map(this::toDto).toList();
    }

    public List<ServerAdDto> findAllDtos() {
        return findAll().stream().map(this::toDto).toList();
    }
}
