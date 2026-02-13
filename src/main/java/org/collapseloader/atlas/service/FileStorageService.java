package org.collapseloader.atlas.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final long ASYNC_MD5_THRESHOLD_BYTES = 5 * 1024 * 1024;
    private final Path rootLocation = Paths.get("uploads").toAbsolutePath().normalize();
    private final Path tempLocation = Paths.get("uploads", "temp").toAbsolutePath().normalize();
    private final FileMetadataService metadataService;

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            Files.createDirectories(tempLocation);
            verifyIntegrity();
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    private void verifyIntegrity() {
        log.info("Verifying file integrity in {}...", rootLocation);
        try (Stream<Path> stream = Files.walk(rootLocation)) {
            stream.parallel()
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            metadataService.getOrCalculateMD5(path, rootLocation);
                        } catch (IOException e) {
                            log.error("Failed to verify file: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to walk files for integrity check", e);
        }

        metadataService.findAll().forEach(metadata -> {
            Path file = rootLocation.resolve(metadata.getFilePath()).normalize();
            if (!Files.exists(file)) {
                log.info("Removing missing file from database: {}", metadata.getFilePath());
                metadataService.delete(metadata);
            }
        });

        log.info("Integrity check completed.");
    }

    public StoredFile store(MultipartFile file, String subDir) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new RuntimeException("Failed to store file with empty name.");
            }
            Path destinationDir = this.rootLocation;
            if (subDir != null && !subDir.isEmpty()) {
                Path resolvedSubDir = destinationDir.resolve(subDir).normalize();
                if (!resolvedSubDir.startsWith(destinationDir)) {
                    throw new RuntimeException("Cannot store file outside current directory.");
                }
                destinationDir = resolvedSubDir;
                Files.createDirectories(destinationDir);
            }

            Path destinationFile = destinationDir.resolve(
                            Paths.get(originalFilename))
                    .normalize().toAbsolutePath();

            if (!destinationFile.getParent().startsWith(destinationDir.toAbsolutePath())) {
                throw new RuntimeException(
                        "Cannot store file outside current directory.");
            }

            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile,
                        StandardCopyOption.REPLACE_EXISTING);
            }

            long size = Files.size(destinationFile);

            String md5 = null;

            if (size <= ASYNC_MD5_THRESHOLD_BYTES) {
                md5 = metadataService.getOrCalculateMD5(destinationFile, rootLocation);
            } else {
                metadataService.calculateMd5Async(destinationFile, rootLocation);
            }

            String relativePath = this.rootLocation.relativize(destinationFile).toString().replace("\\", "/");

            return new StoredFile(originalFilename, relativePath, md5, size / (1024 * 1024));
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    public StoredFile store(MultipartFile file, UploadTarget target) {
        String subDir = target != null ? target.getSubDir() : "";
        return store(file, subDir);
    }

    public Path load(String filename) {
        return rootLocation.resolve(filename).normalize();
    }

    public Stream<Path> loadAll(String subDir) {
        Path startDir = this.rootLocation;
        if (subDir != null && !subDir.isEmpty()) {
            startDir = startDir.resolve(subDir).normalize();
            if (!startDir.startsWith(this.rootLocation)) {
                throw new RuntimeException("Cannot list files outside root directory.");
            }
        }

        if (!Files.exists(startDir)) {
            return Stream.empty();
        }

        Path finalStartDir = startDir;

        try (Stream<Path> walk = Files.walk(finalStartDir, 1)) {
            var files = walk
                    .filter(path -> !path.equals(finalStartDir))
                    .map(this.rootLocation::relativize)
                    .toList();
            return files.stream();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read stored files", e);
        }
    }

    public void delete(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new RuntimeException("Cannot delete file outside current directory.");
            }
            if (Files.exists(file) && Files.isDirectory(file)) {
                deleteRecursively(file);
                String relative = this.rootLocation.relativize(file).toString().replace("\\", "/");
                metadataService.deleteMetadataPrefix(relative);
                return;
            }

            try {
                Files.deleteIfExists(file);
            } catch (java.nio.file.DirectoryNotEmptyException e) {
                deleteRecursively(file);
                String relative = this.rootLocation.relativize(file).toString().replace("\\", "/");
                metadataService.deleteMetadataPrefix(relative);
                return;
            }

            metadataService.deleteMetadata(filename);
        } catch (IOException e) {
            throw new RuntimeException("Could not delete file: " + filename, e);
        }
    }

    public void rename(String oldName, String newName) {
        try {
            Path source = rootLocation.resolve(oldName).normalize();
            Path target = rootLocation.resolve(newName).normalize();

            if (!source.startsWith(rootLocation) || !target.startsWith(rootLocation)) {
                throw new RuntimeException("Cannot rename outside current directory.");
            }

            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

            metadataService.updateMetadataPath(oldName, newName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to rename file", e);
        }
    }

    public void createDirectory(String dirName) {
        try {
            Path dir = rootLocation.resolve(dirName).normalize();
            if (!dir.startsWith(rootLocation)) {
                throw new RuntimeException("Cannot create directory outside root.");
            }
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory", e);
        }
    }

    public void storeChunk(String uploadId, int chunkIndex, MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty chunk.");
            }
            Path uploadTempDir = tempLocation.resolve(uploadId);
            Files.createDirectories(uploadTempDir);

            Path chunkFile = uploadTempDir.resolve(String.valueOf(chunkIndex));
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, chunkFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to store chunk.", e);
        }
    }

    public StoredFile mergeChunks(String uploadId, String filename, String subDir, int totalChunks) {
        try {
            Path uploadTempDir = tempLocation.resolve(uploadId);
            if (!Files.exists(uploadTempDir)) {
                throw new RuntimeException("Upload session not found.");
            }

            Path destinationDir = this.rootLocation;
            if (subDir != null && !subDir.isEmpty()) {
                destinationDir = destinationDir.resolve(subDir).normalize();
                Files.createDirectories(destinationDir);
            }

            Path destinationFile = destinationDir.resolve(Paths.get(filename)).normalize().toAbsolutePath();
            if (!destinationFile.getParent().startsWith(destinationDir.toAbsolutePath())) {
                throw new RuntimeException("Cannot store file outside current directory.");
            }

            try (var outputStream = Files.newOutputStream(destinationFile, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                for (int i = 0; i < totalChunks; i++) {
                    Path chunkFile = uploadTempDir.resolve(String.valueOf(i));
                    if (!Files.exists(chunkFile)) {
                        throw new RuntimeException("Missing chunk " + i);
                    }
                    Files.copy(chunkFile, outputStream);
                }
            }

            deleteRecursively(uploadTempDir);

            long size = Files.size(destinationFile);
            String md5 = null;
            if (size <= ASYNC_MD5_THRESHOLD_BYTES) {
                md5 = metadataService.getOrCalculateMD5(destinationFile, rootLocation);
            } else {
                metadataService.calculateMd5Async(destinationFile, rootLocation);
            }
            String relativePath = this.rootLocation.relativize(destinationFile).toString().replace("\\", "/");

            return new StoredFile(filename, relativePath, md5, size / (1024 * 1024));
        } catch (IOException e) {
            throw new RuntimeException("Failed to merge chunks.", e);
        }
    }

    public StoredFile mergeChunks(String uploadId, String filename, UploadTarget target, int totalChunks) {
        String subDir = target != null ? target.getSubDir() : "";
        return mergeChunks(uploadId, filename, subDir, totalChunks);
    }

    public String calculateMD5(Path file) throws IOException {
        return metadataService.getOrCalculateMD5(file, rootLocation);
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupTempFiles() {
        log.info("Cleaning up stale temp upload files...");
        try (Stream<Path> stream = Files.list(tempLocation)) {
            stream.filter(Files::isDirectory)
                    .forEach(path -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                            long age = System.currentTimeMillis() - attrs.lastModifiedTime().toMillis();
                            if (age > 24 * 60 * 60 * 1000) {
                                log.info("Deleting stale upload session: {}", path);
                                deleteRecursively(path);
                            }
                        } catch (IOException e) {
                            log.error("Failed to cleanup temp path: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to list temp files for cleanup", e);
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path))
            return;
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    public enum UploadTarget {
        CLIENTS("clients"),
        FABRIC_CLIENTS("fabric-clients"),
        FABRIC_DEPS("fabric-deps"),
        FORGE_CLIENTS("forge-clients"),
        FORGE_DEPS("forge-deps");

        private final String subDir;

        UploadTarget(String subDir) {
            this.subDir = subDir;
        }

        public static UploadTarget fromString(String raw) {
            if (raw == null) {
                return null;
            }
            String normalized = raw.trim().toLowerCase();
            for (UploadTarget target : values()) {
                if (target.subDir.equals(normalized) || target.name().equalsIgnoreCase(normalized)) {
                    return target;
                }
            }
            return null;
        }

        public String getSubDir() {
            return subDir;
        }
    }

    public record StoredFile(String originalFilename, String storedPath, String md5, long sizeMb) {
    }
}
