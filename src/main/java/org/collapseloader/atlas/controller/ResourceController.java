package org.collapseloader.atlas.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.collapseloader.atlas.titan.service.TitanFileStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
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
    public ResponseEntity<StreamingResponseBody> serveFile(
            HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String filename = new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path);

        if (filename.isBlank()) {
            StreamingResponseBody responseBody = outputStream -> outputStream.write("english or spanish?".getBytes());

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .contentLength(19)
                    .body(responseBody);
        }

        Path file = storageService.load(filename);

        FileSystemResource resource = new FileSystemResource(file);

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        try {
            long contentLength = resource.contentLength();

            StreamingResponseBody responseBody = outputStream -> {
                try (InputStream inputStream = resource.getInputStream()) {
                    byte[] buffer = new byte[64 * 1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                } catch (IOException ignored) {
                }
            };

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .contentLength(contentLength)
                    .body(responseBody);
        } catch (IOException e) {
            throw new RuntimeException("Error serving file: " + filename, e);
        }
    }
}
