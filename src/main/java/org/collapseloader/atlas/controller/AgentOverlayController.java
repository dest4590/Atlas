package org.collapseloader.atlas.controller;

import org.collapseloader.atlas.dto.ApiResponse;
import org.collapseloader.atlas.titan.service.FileMetadataService;
import org.collapseloader.atlas.titan.service.TitanFileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController()
@RequestMapping("/api/v1")
public class AgentOverlayController {
    private static final String AGENT_JAR = "agent/CollapseAgent.jar";
    private static final String OVERLAY_DLL = "agent/CollapseOverlay.dll";
    private static final String OVERLAY_SO = "agent/libCollapseOverlay.so";
    private static final OverlayAsset WINDOWS_OVERLAY = new OverlayAsset(OVERLAY_DLL, "CollapseOverlay.dll",
            MediaType.parseMediaType("application/x-msdownload"), ".dll");
    private static final OverlayAsset LINUX_OVERLAY = new OverlayAsset(OVERLAY_SO, "libCollapseOverlay.so",
            MediaType.APPLICATION_OCTET_STREAM, ".so");
    private static final MediaType JAR_MEDIA_TYPE = MediaType.parseMediaType("application/java-archive");

    private final TitanFileStorageService storageService;
    private final FileMetadataService metadataService;

    public AgentOverlayController(TitanFileStorageService storageService, FileMetadataService metadataService) {
        this.storageService = storageService;
        this.metadataService = metadataService;
    }

    @GetMapping({"/agent/download", "/agent/download/windows", "/agent/download/linux"})
    public ResponseEntity<Resource> downloadAgent() {
        return serveFile(AGENT_JAR, "CollapseAgent.jar", JAR_MEDIA_TYPE);
    }

    @GetMapping("/overlay/download/{os:windows|linux}")
    public ResponseEntity<Resource> downloadOverlay(@PathVariable String os) {
        OverlayAsset overlay = resolveOverlay(os);
        return serveFile(overlay.path(), overlay.fileName(), overlay.mediaType());
    }

    @GetMapping("/agent-overlay/checksums")
    public ResponseEntity<ApiResponse<Map<String, String>>> checksum(
            @RequestParam(value = "os", required = false) String os) {
        Map<String, String> hashes = new LinkedHashMap<>();
        hashes.put("agent_hash", safeGetMd5(AGENT_JAR));

        if (os == null || os.isBlank()) {
            hashes.put("windows_overlay_hash", safeGetMd5(WINDOWS_OVERLAY.path()));
            hashes.put("linux_overlay_hash", safeGetMd5(LINUX_OVERLAY.path()));
        } else {
            OverlayAsset overlay = resolveOverlay(os);
            hashes.put("overlay_hash", safeGetMd5(overlay.path()));
        }

        return ResponseEntity.ok(ApiResponse.success(hashes));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping({"/agent/upload"})
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAgent(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(storeAgent(file)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/overlay/upload/{os:windows|linux}")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadOverlay(@PathVariable String os,
                                                                          @RequestParam("file") MultipartFile file) {
        OverlayAsset overlay = resolveOverlay(os);
        return ResponseEntity.ok(ApiResponse.success(storeOverlay(file, overlay)));
    }

    private ResponseEntity<Resource> serveFile(String relativePath, String fileName, MediaType mediaType) {
        try {
            Path path = storageService.load(relativePath);
            if (!Files.exists(path)) {
                throw new ResponseStatusException(NOT_FOUND, "Requested artifact is missing");
            }
            Resource resource = new org.springframework.core.io.UrlResource(path.toUri());
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(mediaType)
                    .body(resource);
        } catch (java.net.MalformedURLException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to resolve artifact location", e);
        }
    }

    private Map<String, String> storeAgent(MultipartFile file) {
        validateFile(file, ".jar");
        TitanFileStorageService.StoredFile stored = storageService.store(file, "agent", "CollapseAgent.jar");
        return Map.of(
                "path", "/uploads/" + stored.storedPath(),
                "agent_hash", stored.md5());
    }

    private Map<String, String> storeOverlay(MultipartFile file, OverlayAsset overlay) {
        validateFile(file, overlay.expectedExtension());
        TitanFileStorageService.StoredFile stored = storageService.store(file, "agent", overlay.fileName());
        return Map.of(
                "path", "/uploads/" + stored.storedPath(),
                "overlay_hash", stored.md5());
    }

    private OverlayAsset resolveOverlay(String os) {
        String normalized = os == null ? "" : os.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "windows" -> WINDOWS_OVERLAY;
            case "linux" -> LINUX_OVERLAY;
            default -> throw new ResponseStatusException(BAD_REQUEST, "Unsupported os parameter");
        };
    }

    private String safeGetMd5(String relativePath) {
        try {
            Path path = storageService.load(relativePath);
            if (!Files.exists(path)) {
                return null;
            }
            return metadataService.getOrCalculateMD5(path, storageService.getRootLocation());
        } catch (IOException e) {
            return null;
        }
    }

    private void validateFile(MultipartFile file, String expectedExtension) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "File is required");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(expectedExtension)) {
            throw new ResponseStatusException(BAD_REQUEST, "File must end with " + expectedExtension);
        }
    }

    private record OverlayAsset(String path, String fileName, MediaType mediaType, String expectedExtension) {
    }
}
