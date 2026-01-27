package org.collapseloader.atlas.controller;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.ApiResponse;
import org.collapseloader.atlas.domain.clients.entity.ClientScreenshot;
import org.collapseloader.atlas.domain.clients.repository.ClientRepository;
import org.collapseloader.atlas.domain.clients.repository.ClientScreenshotRepository;
import org.collapseloader.atlas.service.ArtifactStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin")
public class AdminClientScreenshotController {

    private final ArtifactStorageService storageService;
    private final ClientRepository clientRepository;
    private final ClientScreenshotRepository screenshotRepository;

    @GetMapping("/clients/{clientId}/screenshots")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listScreenshots(
            @PathVariable Long clientId
    ) {
        var items = screenshotRepository.findAllByClientIdOrderBySortOrderAsc(clientId)
                .stream()
                .map(screenshot -> Map.of(
                        "id", screenshot.getId(),
                        "imageUrl", screenshot.getImageUrl(),
                        "sortOrder", screenshot.getSortOrder()
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(Map.of("items", items)));
    }

    @PostMapping("/clients/{clientId}/screenshots")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadScreenshot(
            @PathVariable Long clientId,
            @RequestParam("file") List<MultipartFile> files
    ) {
        var client = clientRepository.findById(clientId).orElseThrow(() -> new RuntimeException("Client not found"));
        if (files == null || files.isEmpty()) {
            throw new RuntimeException("No files provided");
        }

        var existing = screenshotRepository.findAllByClientIdOrderBySortOrderAsc(clientId);
        int startOrder = existing.size();
        List<Map<String, Object>> items = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "screenshot";
            String filename = System.currentTimeMillis() + "-" + originalName;
            String relative = "clients/" + clientId + "/screenshots/" + filename;
            storageService.store(relative, file);
            String url = "/uploads/" + relative;

            ClientScreenshot screenshot = new ClientScreenshot();
            screenshot.setClient(client);
            screenshot.setImageUrl(url);
            screenshot.setSortOrder(startOrder + i);
            screenshotRepository.save(screenshot);

            items.add(Map.of("id", screenshot.getId(), "imageUrl", screenshot.getImageUrl()));
        }

        return ResponseEntity.ok(ApiResponse.success(Map.of("items", items)));
    }

    @DeleteMapping("/clients/{clientId}/screenshots/{screenshotId}")
    public ResponseEntity<ApiResponse<Void>> deleteScreenshot(
            @PathVariable Long clientId,
            @PathVariable Long screenshotId
    ) {
        var screenshot = screenshotRepository.findById(screenshotId).orElseThrow(() -> new RuntimeException("Screenshot not found"));
        if (screenshot.getClient() == null || !screenshot.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Mismatched client id");
        }
        screenshotRepository.deleteById(screenshotId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/clients/{clientId}/screenshots/order")
    public ResponseEntity<ApiResponse<Void>> updateOrder(
            @PathVariable Long clientId,
            @RequestBody List<Long> orderedIds
    ) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        var screenshots = screenshotRepository.findAllByClientIdOrderBySortOrderAsc(clientId);
        var map = screenshots.stream().collect(Collectors.toMap(ClientScreenshot::getId, s -> s));

        for (int i = 0; i < orderedIds.size(); i++) {
            var id = orderedIds.get(i);
            var screenshot = map.get(id);
            if (screenshot != null) {
                screenshot.setSortOrder(i);
            }
        }
        screenshotRepository.saveAll(map.values());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
