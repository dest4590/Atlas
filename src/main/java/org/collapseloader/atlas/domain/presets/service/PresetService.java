package org.collapseloader.atlas.domain.presets.service;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.presets.dto.request.PresetCommentRequest;
import org.collapseloader.atlas.domain.presets.dto.request.PresetUpsertRequest;
import org.collapseloader.atlas.domain.presets.dto.response.PresetAuthorResponse;
import org.collapseloader.atlas.domain.presets.dto.response.PresetCommentResponse;
import org.collapseloader.atlas.domain.presets.dto.response.PresetResponse;
import org.collapseloader.atlas.domain.presets.dto.response.PresetThemeResponse;
import org.collapseloader.atlas.domain.presets.entity.*;
import org.collapseloader.atlas.domain.presets.repository.PresetCommentRepository;
import org.collapseloader.atlas.domain.presets.repository.PresetDownloadRepository;
import org.collapseloader.atlas.domain.presets.repository.PresetLikeRepository;
import org.collapseloader.atlas.domain.presets.repository.PresetRepository;
import org.collapseloader.atlas.domain.users.entity.Role;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.exception.EntityNotFoundException;
import org.collapseloader.atlas.exception.ForbiddenException;
import org.collapseloader.atlas.exception.UnauthorizedException;
import org.collapseloader.atlas.exception.ValidationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PresetService {
    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 2048;
    private static final int MAX_COMMENT_LENGTH = 2000;

    private final PresetRepository presetRepository;
    private final PresetLikeRepository likeRepository;
    private final PresetCommentRepository commentRepository;
    private final PresetDownloadRepository downloadRepository;
    private final org.collapseloader.atlas.domain.achievements.service.AchievementService achievementService;

    @Transactional(readOnly = true)
    public List<PresetResponse> listPresets(User principal, String query, Long ownerId, boolean mine, String sort,
                                            int limit) {
        int size = Math.min(Math.max(limit, 1), 100);
        boolean includePrivate = false;
        Long targetOwner = ownerId;

        if (mine) {
            if (principal == null) {
                throw new UnauthorizedException("Authentication required");
            }
            targetOwner = principal.getId();
            includePrivate = true;
        }
        if (targetOwner != null && principal != null) {
            if (targetOwner.equals(principal.getId()) || principal.getRole() == Role.ADMIN) {
                includePrivate = true;
            }
        }

        var sortOrder = resolveSort(sort);
        var pageable = PageRequest.of(0, size, sortOrder);
        var page = switch (resolveQueryMode(query, targetOwner, includePrivate)) {
            case OWNER_PRIVATE -> presetRepository.findByOwnerId(targetOwner, pageable);
            case OWNER_PUBLIC -> presetRepository.findByOwnerIdAndIsPublicTrue(targetOwner, pageable);
            case SEARCH_PUBLIC ->
                    presetRepository.findByIsPublicTrueAndNameContainingIgnoreCase(query.trim(), pageable);
            default -> presetRepository.findByIsPublicTrue(pageable);
        };

        Set<Long> liked = principal != null ? likeRepository.findPresetIdsLikedByUser(principal.getId()) : Set.of();
        return page.stream()
                .map(preset -> toResponse(preset, liked.contains(preset.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PresetResponse getPreset(Long id, User principal) {
        var preset = presetRepository.findWithOwnerById(id)
                .orElseThrow(() -> new EntityNotFoundException("Preset not found"));
        requireViewPermission(preset, principal);

        boolean liked = principal != null && likeRepository.existsByPresetIdAndUserId(id, principal.getId());
        return toResponse(preset, liked);
    }

    @Transactional
    public PresetResponse createPreset(User principal, PresetUpsertRequest request) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        var preset = new Preset();
        preset.setOwner(principal);
        preset.setName(requireName(request.name()));
        preset.setDescription(normalizeDescription(request.description()));
        preset.setPublic(request.isPublic() == null || request.isPublic());
        preset.setTheme(new PresetTheme());
        applyTheme(preset.getTheme(), request, false);

        var saved = presetRepository.save(preset);

        achievementService.unlockAchievement(principal.getId(), "PRESET_MAX");

        return toResponse(saved, false);
    }

    @Transactional
    public PresetResponse updatePreset(Long id, User principal, PresetUpsertRequest request) {
        var preset = presetRepository.findWithOwnerById(id)
                .orElseThrow(() -> new EntityNotFoundException("Preset not found"));
        requireOwnership(preset, principal);

        if (request.name() != null) {
            var normalizedName = requireName(request.name());
            preset.setName(normalizedName);
        }
        if (request.description() != null) {
            preset.setDescription(normalizeDescription(request.description()));
        }
        if (request.isPublic() != null) {
            preset.setPublic(request.isPublic());
        }
        if (preset.getTheme() == null) {
            preset.setTheme(new PresetTheme());
        }
        applyTheme(preset.getTheme(), request, true);

        var saved = presetRepository.save(preset);
        boolean liked = likeRepository.existsByPresetIdAndUserId(id, principal.getId());
        return toResponse(saved, liked);
    }

    @Transactional
    public void deletePreset(Long id, User principal) {
        var preset = presetRepository.findWithOwnerById(id)
                .orElseThrow(() -> new EntityNotFoundException("Preset not found"));
        requireOwnership(preset, principal);
        presetRepository.delete(preset);
    }

    @Transactional
    public PresetResponse likePreset(Long id, User principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        var preset = presetRepository.findWithOwnerById(id)
                .orElseThrow(() -> new EntityNotFoundException("Preset not found"));
        requireViewPermission(preset, principal);
        if (!likeRepository.existsByPresetIdAndUserId(id, principal.getId())) {
            var like = new PresetLike();
            like.setPreset(preset);
            like.setUser(principal);
            likeRepository.save(like);
            preset.setLikesCount(preset.getLikesCount() + 1);
        }
        return toResponse(preset, true);
    }

    @Transactional
    public PresetResponse unlikePreset(Long id, User principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        var preset = presetRepository.findWithOwnerById(id)
                .orElseThrow(() -> new EntityNotFoundException("Preset not found"));
        requireViewPermission(preset, principal);

        likeRepository.findByPresetIdAndUserId(id, principal.getId()).ifPresent(like -> {
            likeRepository.delete(like);
            if (preset.getLikesCount() > 0) {
                preset.setLikesCount(preset.getLikesCount() - 1);
            }
        });
        boolean liked = likeRepository.existsByPresetIdAndUserId(id, principal.getId());
        return toResponse(preset, liked);
    }

    @Transactional
    public PresetResponse incrementDownloads(Long id, User principal) {
        var preset = presetRepository.findWithOwnerById(id)
                .orElseThrow(() -> new EntityNotFoundException("Preset not found"));
        requireViewPermission(preset, principal);

        if (principal != null) {
            if (!downloadRepository.existsByPresetIdAndUserId(id, principal.getId())) {
                var download = PresetDownload.builder()
                        .preset(preset)
                        .user(principal)
                        .build();
                downloadRepository.save(download);
                preset.setDownloadsCount(preset.getDownloadsCount() + 1);
            }
        }

        boolean liked = principal != null && likeRepository.existsByPresetIdAndUserId(id, principal.getId());
        return toResponse(preset, liked);
    }

    @Transactional(readOnly = true)
    public List<PresetCommentResponse> listComments(Long presetId, User principal) {
        var preset = presetRepository.findWithOwnerById(presetId)
                .orElseThrow(() -> new EntityNotFoundException("Preset not found"));
        requireViewPermission(preset, principal);
        return commentRepository.findByPresetIdWithAuthors(presetId).stream()
                .map(this::toCommentResponse)
                .toList();
    }

    @Transactional
    public PresetCommentResponse addComment(Long presetId, User principal, PresetCommentRequest request) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        var preset = presetRepository.findWithOwnerById(presetId)
                .orElseThrow(() -> new EntityNotFoundException("Preset not found"));
        requireViewPermission(preset, principal);

        String text = request != null ? request.text() : null;
        if (!StringUtils.hasText(text)) {
            throw new ValidationException("Comment cannot be empty");
        }
        String normalized = text.trim();
        if (normalized.length() > MAX_COMMENT_LENGTH) {
            throw new ValidationException("Comment is too long");
        }

        var comment = new PresetComment();
        comment.setPreset(preset);
        comment.setUser(principal);
        comment.setContent(normalized);

        var saved = commentRepository.save(comment);
        preset.setCommentsCount(preset.getCommentsCount() + 1);
        return toCommentResponse(saved);
    }

    @Transactional
    public void deleteComment(Long presetId, Long commentId, User principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        var comment = commentRepository.findByIdAndPresetId(commentId, presetId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));
        var preset = comment.getPreset();
        boolean isOwner = preset != null && preset.getOwner() != null
                && preset.getOwner().getId().equals(principal.getId());
        boolean isAuthor = comment.getUser() != null && comment.getUser().getId().equals(principal.getId());
        boolean isAdmin = principal.getRole() == Role.ADMIN;
        if (!isAuthor && !isOwner && !isAdmin) {
            throw new ForbiddenException("Forbidden");
        }

        commentRepository.delete(comment);
        if (preset != null && preset.getCommentsCount() > 0) {
            preset.setCommentsCount(preset.getCommentsCount() - 1);
        }
    }

    private QueryMode resolveQueryMode(String query, Long ownerId, boolean includePrivate) {
        if (ownerId != null) {
            return includePrivate ? QueryMode.OWNER_PRIVATE : QueryMode.OWNER_PUBLIC;
        }
        if (StringUtils.hasText(query)) {
            return QueryMode.SEARCH_PUBLIC;
        }
        return QueryMode.PUBLIC;
    }

    private void requireViewPermission(Preset preset, User principal) {
        if (preset.isPublic()) {
            return;
        }
        boolean owner = principal != null && preset.getOwner() != null
                && preset.getOwner().getId().equals(principal.getId());
        boolean admin = principal != null && principal.getRole() == Role.ADMIN;
        if (!owner && !admin) {
            throw new ForbiddenException("Preset is private");
        }
    }

    private void requireOwnership(Preset preset, User principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        boolean owner = preset.getOwner() != null && preset.getOwner().getId().equals(principal.getId());
        boolean admin = principal.getRole() == Role.ADMIN;
        if (!owner && !admin) {
            throw new ForbiddenException("Forbidden");
        }
    }

    private String requireName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new ValidationException("Preset name is required");
        }
        String normalized = name.trim();
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new ValidationException("Preset name is too long");
        }
        return normalized;
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.trim();
        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("Description is too long");
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private void applyTheme(PresetTheme theme, PresetUpsertRequest request, boolean patch) {
        if (!patch || request.customCSS() != null) {
            theme.setCustomCss(trimToNull(request.customCSS()));
        }
        if (!patch || request.enableCustomCSS() != null) {
            theme.setEnableCustomCss(Boolean.TRUE.equals(request.enableCustomCSS()));
        }

        if (!patch || request.base100() != null)
            theme.setBase100(trimToNull(request.base100()));
        if (!patch || request.base200() != null)
            theme.setBase200(trimToNull(request.base200()));
        if (!patch || request.base300() != null)
            theme.setBase300(trimToNull(request.base300()));
        if (!patch || request.baseContent() != null)
            theme.setBaseContent(trimToNull(request.baseContent()));

        if (!patch || request.primary() != null)
            theme.setPrimary(trimToNull(request.primary()));
        if (!patch || request.primaryContent() != null)
            theme.setPrimaryContent(trimToNull(request.primaryContent()));

        if (!patch || request.secondary() != null)
            theme.setSecondary(trimToNull(request.secondary()));
        if (!patch || request.secondaryContent() != null)
            theme.setSecondaryContent(trimToNull(request.secondaryContent()));

        if (!patch || request.accent() != null)
            theme.setAccent(trimToNull(request.accent()));
        if (!patch || request.accentContent() != null)
            theme.setAccentContent(trimToNull(request.accentContent()));

        if (!patch || request.neutral() != null)
            theme.setNeutral(trimToNull(request.neutral()));
        if (!patch || request.neutralContent() != null)
            theme.setNeutralContent(trimToNull(request.neutralContent()));

        if (!patch || request.info() != null)
            theme.setInfo(trimToNull(request.info()));
        if (!patch || request.infoContent() != null)
            theme.setInfoContent(trimToNull(request.infoContent()));

        if (!patch || request.success() != null)
            theme.setSuccess(trimToNull(request.success()));
        if (!patch || request.successContent() != null)
            theme.setSuccessContent(trimToNull(request.successContent()));

        if (!patch || request.warning() != null)
            theme.setWarning(trimToNull(request.warning()));
        if (!patch || request.warningContent() != null)
            theme.setWarningContent(trimToNull(request.warningContent()));

        if (!patch || request.error() != null)
            theme.setError(trimToNull(request.error()));
        if (!patch || request.errorContent() != null)
            theme.setErrorContent(trimToNull(request.errorContent()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PresetResponse toResponse(Preset preset, boolean liked) {
        var theme = preset.getTheme();
        return new PresetResponse(
                preset.getId(),
                preset.getName(),
                preset.getName(),
                preset.getDescription(),
                preset.isPublic(),
                preset.getLikesCount(),
                preset.getDownloadsCount(),
                preset.getCommentsCount(),
                theme != null ? toTheme(theme) : null,
                toAuthor(preset.getOwner()),
                liked,
                preset.getCreatedAt(),
                preset.getUpdatedAt());
    }

    private PresetCommentResponse toCommentResponse(PresetComment comment) {
        var user = comment.getUser();
        var profile = user != null ? user.getProfile() : null;
        return new PresetCommentResponse(
                comment.getId(),
                comment.getPreset() != null ? comment.getPreset().getId() : null,
                user != null ? user.getId() : null,
                user != null ? user.getUsername() : null,
                profile != null ? profile.getNickname() : null,
                profile != null ? profile.getAvatarUrl() : null,
                comment.getContent(),
                comment.getCreatedAt());
    }

    private PresetThemeResponse toTheme(PresetTheme theme) {
        return new PresetThemeResponse(
                theme.getCustomCss(),
                theme.isEnableCustomCss(),
                theme.getBase100(),
                theme.getBase200(),
                theme.getBase300(),
                theme.getBaseContent(),
                theme.getPrimary(),
                theme.getPrimaryContent(),
                theme.getSecondary(),
                theme.getSecondaryContent(),
                theme.getAccent(),
                theme.getAccentContent(),
                theme.getNeutral(),
                theme.getNeutralContent(),
                theme.getInfo(),
                theme.getInfoContent(),
                theme.getSuccess(),
                theme.getSuccessContent(),
                theme.getWarning(),
                theme.getWarningContent(),
                theme.getError(),
                theme.getErrorContent());
    }

    private PresetAuthorResponse toAuthor(User owner) {
        if (owner == null) {
            return null;
        }
        var profile = owner.getProfile();
        return new PresetAuthorResponse(
                owner.getId(),
                owner.getUsername(),
                profile != null ? profile.getNickname() : null,
                profile != null ? profile.getAvatarUrl() : null);
    }

    private Sort resolveSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sort.toLowerCase()) {
            case "popular", "likes" -> Sort.by(Sort.Direction.DESC, "likesCount");
            case "downloads" -> Sort.by(Sort.Direction.DESC, "downloadsCount");
            case "comments" -> Sort.by(Sort.Direction.DESC, "commentsCount");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private enum QueryMode {
        PUBLIC,
        SEARCH_PUBLIC,
        OWNER_PUBLIC,
        OWNER_PRIVATE
    }
}
