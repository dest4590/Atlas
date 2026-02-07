package org.collapseloader.atlas.controller;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/upload")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UploadController {

    private final FileStorageService storageService;

    @PostMapping
    public ResponseEntity<FileStorageService.StoredFile> handleFileUpload(@RequestParam("file") MultipartFile file,
                                                                          @RequestParam(required = false) String target,
                                                                          @RequestParam(required = false, defaultValue = "") String path) {
        FileStorageService.UploadTarget uploadTarget = parseTarget(target);
        FileStorageService.StoredFile storedFile = uploadTarget != null
                ? storageService.store(file, uploadTarget)
                : storageService.store(file, path);
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
    public ResponseEntity<FileStorageService.StoredFile> mergeChunks(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("filename") String filename,
            @RequestParam(required = false) String target,
            @RequestParam(required = false, defaultValue = "") String path,
            @RequestParam("totalChunks") int totalChunks) {
        FileStorageService.UploadTarget uploadTarget = parseTarget(target);
        FileStorageService.StoredFile storedFile = uploadTarget != null
                ? storageService.mergeChunks(uploadId, filename, uploadTarget, totalChunks)
                : storageService.mergeChunks(uploadId, filename, path, totalChunks);
        return ResponseEntity.ok(storedFile);
    }

    private FileStorageService.UploadTarget parseTarget(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        FileStorageService.UploadTarget target = FileStorageService.UploadTarget.fromString(raw);
        if (target == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown upload target: " + raw);
        }
        return target;
    }
}
