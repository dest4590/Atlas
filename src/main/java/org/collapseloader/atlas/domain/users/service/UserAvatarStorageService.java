package org.collapseloader.atlas.domain.users.service;

import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UserAvatarStorageService {
    private static final long MAX_AVATAR_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final String URL_ROOT = "uploads";
    private static final String AVATAR_SUBDIR = "avatars";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif");
    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final Path uploadRoot;
    private final Path avatarsRoot;

    public UserAvatarStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.avatarsRoot = uploadRoot.resolve(AVATAR_SUBDIR).normalize();
        try {
            Files.createDirectories(avatarsRoot);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create avatar upload directory", e);
        }
    }

    public String storeAvatar(Long userId, MultipartFile file) throws BadRequestException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Avatar file is required");
        }
        if (file.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw new BadRequestException("Avatar file is too large");
        }
        String contentType = normalize(file.getContentType());
        String extension = extractExtension(file, contentType);
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Unsupported avatar content type");
        }
        String fileName = "user-" + userId + "-" + Instant.now().getEpochSecond() + "-" + UUID.randomUUID()
                + "." + extension;
        Path target = avatarsRoot.resolve(fileName).normalize();
        if (!target.startsWith(avatarsRoot)) {
            throw new BadRequestException("Invalid avatar file path");
        }
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store avatar", e);
        }
        return URL_ROOT + "/" + AVATAR_SUBDIR + "/" + fileName;
    }

    public void deleteAvatar(String avatarPath) {
        if (avatarPath == null || avatarPath.isBlank()) {
            return;
        }
        String normalized = avatarPath.replace("\\", "/");
        String expectedPrefix = URL_ROOT + "/" + AVATAR_SUBDIR + "/";
        if (!normalized.startsWith(expectedPrefix)) {
            return;
        }
        String relative = normalized.substring(URL_ROOT.length() + 1);
        Path target = uploadRoot.resolve(relative).normalize();
        if (!target.startsWith(uploadRoot)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
        }
    }

    private String extractExtension(MultipartFile file, String contentType) {
        String originalName = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalName);
        if (extension != null) {
            extension = extension.toLowerCase(Locale.ROOT);
        }
        if (extension != null && ALLOWED_EXTENSIONS.contains(extension)) {
            return extension.equals("jpeg") ? "jpg" : extension;
        }
        if (contentType != null) {
            String mapped = CONTENT_TYPE_TO_EXTENSION.get(contentType);
            if (mapped != null) {
                return mapped;
            }
        }
        throw new RuntimeException("Unsupported avatar format");
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
