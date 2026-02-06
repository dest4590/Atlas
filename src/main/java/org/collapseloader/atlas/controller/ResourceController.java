package org.collapseloader.atlas.controller;

import org.collapseloader.atlas.service.FileStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@RestController
public class ResourceController {

    private final FileStorageService storageService;

    public ResourceController(FileStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/uploads/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Path file = storageService.load(filename);

        FileSystemResource resource = new FileSystemResource(file);

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        try {

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (IOException e) {
            throw new RuntimeException("Error serving file: " + filename, e);
        }
    }
}
