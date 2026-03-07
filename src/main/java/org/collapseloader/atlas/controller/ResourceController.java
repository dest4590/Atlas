package org.collapseloader.atlas.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.collapseloader.atlas.titan.service.TitanFileStorageService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
public class ResourceController {

    private final TitanFileStorageService storageService;

    public ResourceController(TitanFileStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/uploads/**")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(
            HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String filename = new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path);

        if (filename.isBlank()) {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .contentLength(19)
                    .body(new ByteArrayResource("english or spanish?".getBytes()));
        }

        Path file = storageService.load(filename);

        FileSystemResource resource = new FileSystemResource(file);

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
