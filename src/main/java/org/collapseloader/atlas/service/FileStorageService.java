package org.collapseloader.atlas.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.storage.entity.FileMetadata;
import org.collapseloader.atlas.domain.storage.repository.FileMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private final Path rootLocation = Paths.get("uploads", "public").toAbsolutePath().normalize();
    private final FileMetadataRepository metadataRepository;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
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
                            calculateMD5(path);
                        } catch (IOException e) {
                            log.error("Failed to verify file: " + path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to walk files for integrity check", e);
        }

        metadataRepository.findAll().forEach(metadata -> {
            Path file = rootLocation.resolve(metadata.getFilePath()).normalize();
            if (!Files.exists(file)) {
                log.info("Removing missing file from database: {}", metadata.getFilePath());
                metadataRepository.delete(metadata);
            }
        });

        log.info("Integrity check completed.");
    }

    public StoredFile store(MultipartFile file, String subDir) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
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
                    Paths.get(file.getOriginalFilename()))
                    .normalize().toAbsolutePath();

            if (!destinationFile.getParent().startsWith(destinationDir.toAbsolutePath())) {
                throw new RuntimeException(
                        "Cannot store file outside current directory.");
            }

            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile,
                        StandardCopyOption.REPLACE_EXISTING);
            }

            String md5 = calculateMD5(destinationFile);
            long size = Files.size(destinationFile);

            String relativePath = this.rootLocation.relativize(destinationFile).toString().replace("\\", "/");

            return new StoredFile(file.getOriginalFilename(), relativePath, md5, size / (1024 * 1024));
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    public Resource loadAsResource(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new RuntimeException("Cannot access file outside current directory.");
            }
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException(
                        "Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + filename, e);
        }
    }

    public java.nio.file.Path load(String filename) {
        return rootLocation.resolve(filename).normalize();
    }

    public java.util.stream.Stream<Path> loadAll(String subDir) {
        try {
            Path startDir = this.rootLocation;
            if (subDir != null && !subDir.isEmpty()) {
                startDir = startDir.resolve(subDir).normalize();
                if (!startDir.startsWith(this.rootLocation)) {
                    throw new RuntimeException("Cannot list files outside root directory.");
                }
            }

            if (!Files.exists(startDir)) {
                return java.util.stream.Stream.empty();
            }

            Path finalStartDir = startDir;
            return Files.walk(finalStartDir, 1)
                    .filter(path -> !path.equals(finalStartDir))
                    .map(this.rootLocation::relativize);
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
            Files.deleteIfExists(file);
            metadataRepository.findByFilePath(filename).ifPresent(metadataRepository::delete);
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

            // Update database
            metadataRepository.findByFilePath(oldName).ifPresent(meta -> {
                meta.setFilePath(newName);
                metadataRepository.save(meta);
            });
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

    @Transactional
    public String calculateMD5(Path path) throws IOException {
        String relativePath = rootLocation.relativize(path).toString().replace("\\", "/");

        // Check database
        Optional<FileMetadata> existing = metadataRepository.findByFilePath(relativePath);
        long currentSize = Files.size(path);
        long currentLastModified = Files.getLastModifiedTime(path).toMillis();

        if (existing.isPresent()) {
            FileMetadata cached = existing.get();
            if (cached.getSize() == currentSize && cached.getLastModified() == currentLastModified) {
                return cached.getMd5();
            }
            log.info("File changed, recalculating hash: {}", relativePath);
        } else {
            log.info("New file detected or not in database, calculating hash: {}", relativePath);
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (var is = Files.newInputStream(path)) {
                byte[] buffer = new byte[65536]; // 64KB buffer
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                sb.append(String.format("%02x", b));
            }
            String md5 = sb.toString();

            // Update database
            FileMetadata metadata = existing.orElse(new FileMetadata());
            metadata.setFilePath(relativePath);
            metadata.setMd5(md5);
            metadata.setSize(currentSize);
            metadata.setLastModified(currentLastModified);
            metadataRepository.save(metadata);

            return md5;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
    }

    // These records are still useful for API compatibility
    public record CachedMetadata(String md5, long size, long lastModified) {
    }

    public record StoredFile(String originalFilename, String storedPath, String md5, long sizeMb) {
    }
}
