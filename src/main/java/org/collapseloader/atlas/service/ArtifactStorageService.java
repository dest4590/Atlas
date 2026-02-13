package org.collapseloader.atlas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ArtifactStorageService {
    private final Path uploadRoot;
    private final Map<String, String> hashCache = new ConcurrentHashMap<>();

    public ArtifactStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare artifact upload root", e);
        }
    }

    public Resource load(String relativePath) {
        Path target = resolve(relativePath);
        try {
            Resource resource = new UrlResource(target.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(NOT_FOUND, "Requested artifact is missing");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to resolve artifact location", e);
        }
    }

    public String store(String relativePath, MultipartFile file) {
        Path target = resolve(relativePath);
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            MessageDigest digest = createDigest();
            try (var output = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                 InputStream inputStream = file.getInputStream();
                 DigestInputStream digestStream = new DigestInputStream(inputStream, digest)) {
                digestStream.transferTo(output);
            }
            String hash = HexFormat.of().formatHex(digest.digest());
            hashCache.put(relativePath, hash);
            writeHashFile(target, hash);
            return hash;
        } catch (IOException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to persist artifact", e);
        }
    }

    public String getMd5(String relativePath) {
        return hashCache.computeIfAbsent(relativePath, this::readMd5);
    }

    private Path resolve(String relativePath) {
        Path resolved = uploadRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(uploadRoot)) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Invalid artifact path");
        }
        return resolved;
    }

    private String readMd5(String relativePath) {
        Path target = resolve(relativePath);
        if (!Files.exists(target)) {
            throw new ResponseStatusException(NOT_FOUND, "Requested artifact is missing");
        }
        Path hashFile = hashFilePath(target);
        if (Files.exists(hashFile)) {
            try {
                return Files.readString(hashFile, StandardCharsets.UTF_8).trim();
            } catch (IOException e) {
                throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to read checksum cache", e);
            }
        }

        String hash = computeMd5(target);
        writeHashFile(target, hash);
        return hash;
    }

    private MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    private String computeMd5(Path target) {
        try {
            MessageDigest digest = createDigest();
            try (InputStream inputStream = Files.newInputStream(target)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to read artifact for checksum", e);
        }
    }

    private void writeHashFile(Path target, String hash) {
        try {
            Files.writeString(hashFilePath(target), hash, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to persist checksum cache", e);
        }
    }

    private Path hashFilePath(Path target) {
        return target.resolveSibling(target.getFileName().toString() + ".md5");
    }
}
