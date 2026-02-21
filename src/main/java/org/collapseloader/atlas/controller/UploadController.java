package org.collapseloader.atlas.controller;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.collapseloader.atlas.domain.clients.entity.ClientType;
import org.collapseloader.atlas.domain.clients.entity.fabric.FabricDependence;
import org.collapseloader.atlas.domain.clients.repository.FabricClientRepository;
import org.collapseloader.atlas.exception.EntityNotFoundException;
import org.collapseloader.atlas.titan.service.TitanFileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/upload")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UploadController {
    private static final Logger log = LoggerFactory.getLogger(UploadController.class);
    private final TitanFileStorageService storageService;
    private final FabricClientRepository fabricClientRepository;

    @PostMapping
    public ResponseEntity<TitanFileStorageService.StoredFile> handleFileUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String target,
            @RequestParam(required = false, defaultValue = "") String path,
            @RequestParam(required = false) Long clientId) throws BadRequestException {

        TitanFileStorageService.UploadTarget uploadTarget = parseTarget(target);
        TitanFileStorageService.StoredFile storedFile = uploadTarget != null
                ? storageService.store(file, uploadTarget)
                : storageService.store(file, path);

        if (uploadTarget == TitanFileStorageService.UploadTarget.FABRIC_DEPS && clientId != null) {
            registerFabricDep(storedFile, clientId);
        }

        return ResponseEntity.ok(storedFile);
    }

    @PostMapping("/chunk")
    public ResponseEntity<Void> uploadChunk(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") int chunkIndex) {
        storageService.storeChunk(uploadId, chunkIndex, file);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/merge")
    public ResponseEntity<TitanFileStorageService.StoredFile> mergeChunks(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("filename") String filename,
            @RequestParam(required = false) String target,
            @RequestParam(required = false, defaultValue = "") String path,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam(required = false) Long clientId) throws BadRequestException {

        TitanFileStorageService.UploadTarget uploadTarget = parseTarget(target);
        TitanFileStorageService.StoredFile storedFile = uploadTarget != null
                ? storageService.mergeChunks(uploadId, filename, uploadTarget, totalChunks)
                : storageService.mergeChunks(uploadId, filename, path, totalChunks);

        if (uploadTarget == TitanFileStorageService.UploadTarget.FABRIC_DEPS && clientId != null) {
            registerFabricDep(storedFile, clientId);
        }

        return ResponseEntity.ok(storedFile);
    }

    private void registerFabricDep(TitanFileStorageService.StoredFile storedFile, Long clientId) {
        try {
            var client = fabricClientRepository.findByIdAndType(clientId, ClientType.FABRIC)
                    .orElseThrow(() -> new EntityNotFoundException("Fabric client not found with ID: " + clientId));

            final String baseName = storedFile.originalFilename().endsWith(".jar")
                    ? storedFile.originalFilename().substring(0, storedFile.originalFilename().length() - 4)
                    : storedFile.originalFilename();

            boolean exists = client.getDependencies().stream()
                    .anyMatch(d -> d.getName().equalsIgnoreCase(baseName));

            if (!exists) {
                FabricDependence dep = new FabricDependence();
                dep.setClient(client);
                dep.setName(baseName);
                dep.setMd5Hash(storedFile.md5());
                dep.setSize(storedFile.sizeMb());
                client.getDependencies().add(dep);
                fabricClientRepository.save(client);
                log.info("[FABRIC] Automatically registered dependency {} for client {}", baseName, client.getName());
            }
        } catch (Exception e) {
            log.error("[FABRIC] Failed to automatically register dependency", e);
        }
    }

    private TitanFileStorageService.UploadTarget parseTarget(String raw) throws BadRequestException {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        TitanFileStorageService.UploadTarget target = TitanFileStorageService.UploadTarget.fromString(raw);
        if (target == null) {
            throw new BadRequestException("Unknown upload target: " + raw);
        }
        return target;
    }
}
