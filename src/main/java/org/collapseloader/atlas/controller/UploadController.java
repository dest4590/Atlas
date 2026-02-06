package org.collapseloader.atlas.controller;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.service.FileStorageService;
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

    private final FileStorageService storageService;

    @PostMapping
    public ResponseEntity<FileStorageService.StoredFile> handleFileUpload(@RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "") String path) {
        FileStorageService.StoredFile storedFile = storageService.store(file, path);
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
            @RequestParam(required = false, defaultValue = "") String path,
            @RequestParam("totalChunks") int totalChunks) {
        FileStorageService.StoredFile storedFile = storageService.mergeChunks(uploadId, filename, path, totalChunks);
        return ResponseEntity.ok(storedFile);
    }
}
