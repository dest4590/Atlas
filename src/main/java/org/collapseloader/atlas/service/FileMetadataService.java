package org.collapseloader.atlas.service;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.storage.entity.FileMetadata;
import org.collapseloader.atlas.domain.storage.repository.FileMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileMetadataService {
    private static final Logger log = LoggerFactory.getLogger(FileMetadataService.class);
    private final FileMetadataRepository metadataRepository;

    @Transactional
    public String getOrCalculateMD5(Path path, Path rootLocation) throws IOException {
        String relativePath = rootLocation.relativize(path).toString().replace("\\", "/");

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
                byte[] buffer = new byte[65536];
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

    @Transactional
    public void deleteMetadata(String filePath) {
        metadataRepository.findByFilePath(filePath).ifPresent(metadataRepository::delete);
    }

    @Transactional
    public void updateMetadataPath(String oldPath, String newPath) {
        metadataRepository.findByFilePath(oldPath).ifPresent(meta -> {
            meta.setFilePath(newPath);
            metadataRepository.save(meta);
        });
    }

    @Transactional(readOnly = true)
    public Iterable<FileMetadata> findAll() {
        return metadataRepository.findAll();
    }

    @Transactional
    public void delete(FileMetadata metadata) {
        metadataRepository.delete(metadata);
    }

    @Async
    @Transactional
    public void calculateMd5Async(Path path, Path rootLocation) {
        try {
            getOrCalculateMD5(path, rootLocation);
            log.info("Asynchronously calculated MD5 for {}", path);
        } catch (IOException e) {
            log.error("Failed to calculate MD5 asynchronously for {}", path, e);
        }
    }
}
