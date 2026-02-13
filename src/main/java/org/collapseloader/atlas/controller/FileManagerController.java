package org.collapseloader.atlas.controller;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.service.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/files")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FileManagerController {

    private final FileStorageService storageService;

    @GetMapping
    public ResponseEntity<List<FileResponse>> listFiles(
            @RequestParam(required = false, defaultValue = "") String path) {
        List<FileResponse> files = storageService.loadAll(path).map(filePath -> {
            String filename = filePath.toString().replace("\\", "/");
            String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(filename)
                    .toUriString();

            String md5 = "";
            long size = 0;
            long lastModified = 0;
            long created = 0;
            boolean isDir = false;
            try {
                Path file = storageService.load(filename);
                if (Files.exists(file)) {
                    BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                    isDir = attrs.isDirectory();
                    if (!isDir) {
                        size = Files.size(file);
                        md5 = storageService.calculateMD5(file);
                    }
                    lastModified = attrs.lastModifiedTime().toMillis();
                    created = attrs.creationTime().toMillis();
                }
            } catch (Exception ignored) {
            }
            return new FileResponse(filename, url, size, lastModified, created, isDir, md5);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(files);
    }

    @PostMapping("/create-directory")
    public ResponseEntity<Void> createDirectory(@RequestParam String name) {
        storageService.createDirectory(name);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rename")
    public ResponseEntity<Void> rename(@RequestParam String oldName, @RequestParam String newName) {
        storageService.rename(oldName, newName);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{*filename}")
    public ResponseEntity<Void> deleteFile(@PathVariable String filename) {
        String path = filename.startsWith("/") ? filename.substring(1) : filename;
        storageService.delete(path);
        return ResponseEntity.noContent().build();
    }

    public record FileResponse(String name, String url, long size, long lastModified, long created, boolean isDir,
                               String md5) {
    }
}
