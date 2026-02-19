package org.collapseloader.atlas.titan.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.config.StorageProperties;
import org.collapseloader.atlas.exception.TitanException;
import org.collapseloader.atlas.titan.model.FileMetadata;
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
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final long ASYNC_MD5_THRESHOLD_BYTES = 512 * 1024 * 1024;

    private final StorageProperties properties;
    private final FileMetadataService metadataService;

    @Getter
    private Path rootLocation;
    private Path tempLocation;
    private Path trashLocation;

    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        this.rootLocation = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        this.tempLocation = this.rootLocation.resolve(properties.getTempDir()).normalize();
        this.trashLocation = this.rootLocation.resolve(properties.getTrashDir()).normalize();

        try {
            Files.createDirectories(rootLocation);
            Files.createDirectories(tempLocation);
            Files.createDirectories(trashLocation);
            log.info("[STORAGE] Initialized. Root: {}, Trash: {}", rootLocation, trashLocation);
            verifyIntegrity();
        } catch (IOException e) {
            throw new TitanException("Could not initialize storage: " + e);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void scheduledIntegrityCheck() {
        verifyIntegrity();
    }

    public void verifyIntegrity() {
        log.info("[SECURITY] Starting file integrity verification...");

        try (Stream<Path> stream = Files.walk(rootLocation)) {
            stream.filter(path -> !path.startsWith(trashLocation) && !path.startsWith(tempLocation))
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            metadataService.getOrCalculateMD5(path, rootLocation);
                        } catch (IOException e) {
                            log.error("[SECURITY] Failed to verify file integrity for: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("[SECURITY] Failed to walk files for integrity check", e);
        }

        metadataService.findAll().forEach(metadata -> {
            Path file = rootLocation.resolve(metadata.getFilePath()).normalize();
            if (!Files.exists(file)) {
                log.warn("[SECURITY] Active file missing from disk, purging from DB: {}", metadata.getFilePath());
                metadataService.purgeMetadata(metadata.getFilePath());
            }
        });

        metadataService.findTrash().forEach(metadata -> {
            Path trashFile = trashLocation.resolve(metadata.getFilePath()).normalize();
            if (!Files.exists(trashFile)) {
                log.warn("[SECURITY] Deleted file missing from trash disk, purging from DB: {}",
                        metadata.getFilePath());
                metadataService.purgeMetadata(metadata.getFilePath());
            }
        });

        log.info("[SECURITY] Integrity check completed.");
    }

    public StoredFile store(MultipartFile file, String subDir) {
        return store(file, subDir, file.getOriginalFilename());
    }

    public StoredFile store(MultipartFile file, String subDir, String customFilename) {
        log.info("[SECURITY] Attempting to store file: {} as {} in subDir: {}", file.getOriginalFilename(),
                customFilename, subDir);
        try {
            if (file.isEmpty()) {
                throw new TitanException("Failed to store empty file.");
            }
            if (customFilename == null || customFilename.isEmpty()) {
                throw new TitanException("Failed to store file with empty name.");
            }
            Path destinationDir = this.rootLocation;
            if (subDir != null && !subDir.isEmpty()) {
                Path resolvedSubDir = destinationDir.resolve(subDir).normalize();
                if (!resolvedSubDir.startsWith(destinationDir)) {
                    log.warn("[SECURITY] Path traversal attempt detected while storing file: {}", subDir);
                    throw new TitanException("Cannot store file outside current directory.");
                }
                destinationDir = resolvedSubDir;
                Files.createDirectories(destinationDir);
            }

            Path destinationFile = destinationDir.resolve(
                            Paths.get(customFilename))
                    .normalize().toAbsolutePath();

            if (!destinationFile.startsWith(rootLocation.toAbsolutePath())) {
                log.warn("[SECURITY] Path traversal attempt detected at file level: {}", destinationFile);
                throw new TitanException("Cannot store file outside current directory.");
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
            log.info("[SECURITY] File stored successfully: {}", relativePath);

            return new StoredFile(customFilename, relativePath, md5, size / (1024 * 1024));
        } catch (IOException e) {
            log.error("[SECURITY] Failed to store file: {}", file.getOriginalFilename(), e);
            throw new TitanException("Failed to store file: '" + file.getOriginalFilename() + "', " + e);
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
                throw new TitanException("Cannot list files outside root directory.");
            }
        }

        if (!Files.exists(startDir)) {
            return Stream.empty();
        }

        Path finalStartDir = startDir;
        try {
            try (Stream<Path> walk = Files.walk(finalStartDir, 1)) {
                return walk
                        .filter(path -> !path.equals(finalStartDir))
                        .filter(path -> !path.startsWith(trashLocation) && !path.startsWith(tempLocation))
                        .map(this.rootLocation::relativize)
                        .toList()
                        .stream();
            }
        } catch (IOException e) {
            throw new TitanException("Failed to read stored files: " + e);
        }
    }

    public void delete(String filename) {
        log.info("[SECURITY] Attempting to soft-delete file: {}", filename);
        try {
            Path file = rootLocation.resolve(filename).normalize();
            if (!file.startsWith(rootLocation)) {
                log.warn("[SECURITY] Attempted to delete outside root: {}", filename);
                throw new TitanException("Cannot delete file outside current directory.");
            }
            if (file.equals(rootLocation)) {
                throw new TitanException("Cannot delete root directory.");
            }

            Optional<FileMetadata> metadata = metadataService.findByFilePath(filename);
            boolean isAlreadyInTrash = (metadata.isPresent() && metadata.get().isDeleted())
                    || file.startsWith(trashLocation);

            if (isAlreadyInTrash) {
                log.info("[SECURITY] Permanent delete requested for file already in trash: {}", filename);
                Path trashFile = file.startsWith(trashLocation) ? file : trashLocation.resolve(filename).normalize();
                permanentDelete(trashFile);
                return;
            }

            if (!Files.exists(file)) {
                Path trashFile = trashLocation.resolve(filename).normalize();
                if (Files.exists(trashFile)) {
                    log.info("[SECURITY] Permanent delete requested for file found only in trash disk: {}", filename);
                    permanentDelete(trashFile);
                    return;
                }
                log.warn("[SECURITY] Attempted to delete non-existent file: {}", filename);
                return;
            }

            Path trashFile = trashLocation.resolve(rootLocation.relativize(file)).normalize();
            Files.createDirectories(trashFile.getParent());

            Files.move(file, trashFile, StandardCopyOption.REPLACE_EXISTING);
            metadataService.deleteMetadata(filename);
            log.info("[SECURITY] File moved to trash: {}", filename);
        } catch (IOException e) {
            log.error("[SECURITY] Failed to soft-delete file: {}", filename, e);
            throw new TitanException("Could not delete file: '" + filename + "', " + e);
        }
    }

    public void restore(String filename) {
        log.info("[SECURITY] Attempting to restore file: {}", filename);
        try {
            Path trashFile = trashLocation.resolve(filename).normalize();
            if (!trashFile.startsWith(trashLocation)) {
                throw new TitanException("Cannot restore file from outside trash.");
            }
            if (!Files.exists(trashFile)) {
                throw new TitanException("File not found in trash.");
            }

            Path originalFile = rootLocation.resolve(filename).normalize();
            Files.createDirectories(originalFile.getParent());

            Files.move(trashFile, originalFile, StandardCopyOption.REPLACE_EXISTING);
            metadataService.restore(filename);
            log.info("[SECURITY] File restored from trash: {}", filename);
        } catch (IOException e) {
            log.error("[SECURITY] Failed to restore file: {}", filename, e);
            throw new TitanException("Could not restore file: '" + filename + "', " + e);
        }
    }

    private void permanentDelete(Path file) throws IOException {
        String relativePath = rootLocation.relativize(file).toString().replace("\\", "/");

        String trashDirPrefix = properties.getTrashDir() + "/";
        String purgePath = relativePath.startsWith(trashDirPrefix)
                ? relativePath.substring(trashDirPrefix.length())
                : relativePath;

        if (Files.isDirectory(file)) {
            deleteRecursively(file);
            metadataService.purgeMetadataPrefix(purgePath);
        } else {
            Files.deleteIfExists(file);
            metadataService.purgeMetadata(purgePath);
        }
        log.info("[SECURITY] Permanently deleted: {}", file);
    }

    public void rename(String oldName, String newName) {
        log.info("[SECURITY] Renaming file from {} to {}", oldName, newName);
        try {
            Path source = rootLocation.resolve(oldName).normalize();
            Path target = rootLocation.resolve(newName).normalize();

            if (!source.startsWith(rootLocation) || !target.startsWith(rootLocation)) {
                throw new TitanException("Cannot rename outside current directory.");
            }

            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            metadataService.updateMetadataPath(oldName, newName);
        } catch (IOException e) {
            log.error("[SECURITY] Failed to rename file from {} to {}", oldName, newName, e);
            throw new TitanException("Failed to rename file from '" + oldName + "' to '" + newName + "'" + ", " + e);
        }
    }

    public void createDirectory(String dirName) {
        log.info("[SECURITY] Creating directory: {}", dirName);
        try {
            Path dir = rootLocation.resolve(dirName).normalize();
            if (!dir.startsWith(rootLocation)) {
                throw new TitanException("Cannot create directory outside root.");
            }
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.error("[SECURITY] Failed to create directory: {}", dirName, e);
            throw new TitanException("Failed to create directory: '" + dirName + "'" + ", " + e);
        }
    }

    public void storeChunk(String uploadId, int chunkIndex, MultipartFile file) {
        try {
            Path uploadTempDir = tempLocation.resolve(uploadId);

            if (!Files.exists(uploadTempDir)) {
                Files.createDirectories(uploadTempDir);
            }

            Path chunkFile = uploadTempDir.resolve(String.valueOf(chunkIndex));
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, chunkFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new TitanException(
                    "Failed to store chunk for uploadId: '" + uploadId + "', chunkIndex: " + chunkIndex + ", " + e);
        }
    }

    public StoredFile mergeChunks(String uploadId, String filename, String subDir, int totalChunks) {
        log.info("[SECURITY] Merging chunks for file: {} in subDir: {}", filename, subDir);
        try {
            Path uploadTempDir = tempLocation.resolve(uploadId);
            Path destinationDir = subDir != null && !subDir.isEmpty() ? rootLocation.resolve(subDir).normalize()
                    : rootLocation;
            Files.createDirectories(destinationDir);

            Path destinationFile = destinationDir.resolve(Paths.get(filename)).normalize().toAbsolutePath();
            if (!destinationFile.startsWith(rootLocation.toAbsolutePath())) {
                throw new TitanException("Cannot store file outside root.");
            }

            try (var outputStream = Files.newOutputStream(destinationFile, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                for (int i = 0; i < totalChunks; i++) {
                    Path chunkFile = uploadTempDir.resolve(String.valueOf(i));
                    Files.copy(chunkFile, outputStream);
                }
            }

            deleteRecursively(uploadTempDir);
            long size = Files.size(destinationFile);
            String md5 = size <= ASYNC_MD5_THRESHOLD_BYTES
                    ? metadataService.getOrCalculateMD5(destinationFile, rootLocation)
                    : null;
            if (md5 == null)
                metadataService.calculateMd5Async(destinationFile, rootLocation);

            String relativePath = this.rootLocation.relativize(destinationFile).toString().replace("\\", "/");
            return new StoredFile(filename, relativePath, md5, size / (1024 * 1024));
        } catch (IOException e) {
            log.error("[SECURITY] Failed to merge chunks for: {}", filename, e);
            throw new TitanException("Failed to merge chunks for file: '" + filename + "'" + ", " + e);
        }
    }

    public StoredFile mergeChunks(String uploadId, String filename, UploadTarget target, int totalChunks) {
        String subDir = target != null ? target.getSubDir() : "";
        return mergeChunks(uploadId, filename, subDir, totalChunks);
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void purgeOldTrash() {
        log.info("[SECURITY] Starting daily trash purge...");
        try (Stream<Path> stream = Files.walk(trashLocation)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                            long age = System.currentTimeMillis() - attrs.lastModifiedTime().toMillis();
                            if (age > (long) properties.getTrashRetentionDays() * 24 * 60 * 60 * 1000) {
                                log.info("[SECURITY] Purging old trash file: {}", path);
                                permanentDelete(path);
                            }
                        } catch (IOException e) {
                            log.error("[SECURITY] Failed to purge trash file: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("[SECURITY] Failed to walk trash for purge", e);
        }
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
                                deleteRecursively(path);
                            }
                        } catch (IOException e) {
                            log.error("Failed to cleanup temp path: '{}'", path, e);
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

    @Getter
    public enum UploadTarget {
        CLIENTS("clients", "clients/vanilla/jars"),
        FABRIC_CLIENTS("fabric-clients", "clients/fabric/jars"),
        FABRIC_DEPS("fabric-deps", "clients/fabric/deps/jars"),
        FORGE_CLIENTS("forge-clients", "clients/forge/jars"),
        FORGE_DEPS("forge-deps", "clients/forge/deps/jars"),
        AGENT("agent", "agent");

        private final String label;
        private final String subDir;

        UploadTarget(String label, String subDir) {
            this.label = label;
            this.subDir = subDir;
        }

        public static UploadTarget fromString(String raw) {
            if (raw == null)
                return null;
            String normalized = raw.trim().toLowerCase();
            for (UploadTarget target : values()) {
                if (target.label.equals(normalized) || target.name().equalsIgnoreCase(normalized.replace("-", "_")))
                    return target;
            }
            return null;
        }
    }

    public record StoredFile(String originalFilename, String storedPath, String md5, long sizeMb) {
    }
}
