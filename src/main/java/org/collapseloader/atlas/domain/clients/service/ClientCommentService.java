package org.collapseloader.atlas.domain.clients.service;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.clients.dto.response.ClientCommentResponse;
import org.collapseloader.atlas.domain.clients.entity.ClientComment;
import org.collapseloader.atlas.domain.clients.repository.ClientCommentRepository;
import org.collapseloader.atlas.domain.clients.repository.ClientRepository;
import org.collapseloader.atlas.domain.users.entity.Role;
import org.collapseloader.atlas.domain.users.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientCommentService {
    private static final int MAX_COMMENT_LENGTH = 2000;

    private final ClientRepository clientRepository;
    private final ClientCommentRepository commentRepository;

    @Transactional(readOnly = true)
    public List<ClientCommentResponse> getComments(Long clientId) {
        var comments = commentRepository.findByClientIdWithAuthors(clientId);
        return comments.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ClientCommentResponse addComment(Long clientId, User user, String content) {
        if (!StringUtils.hasText(content)) {
            throw new RuntimeException("Comment content cannot be empty");
        }
        String normalized = content.trim();
        if (normalized.length() > MAX_COMMENT_LENGTH) {
            throw new RuntimeException("Comment content is too long");
        }

        var client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        var comment = new ClientComment();
        comment.setClient(client);
        comment.setUser(user);
        comment.setContent(normalized);

        var saved = commentRepository.saveAndFlush(comment);
        return toResponse(saved);
    }

    @Transactional
    public void deleteComment(Long clientId, Long commentId, User user) {
        var comment = commentRepository.findByIdAndClientId(commentId, clientId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        boolean isAuthor = comment.getUser() != null && comment.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;
        if (!isAuthor && !isAdmin) {
            throw new RuntimeException("Forbidden");
        }

        commentRepository.delete(comment);
    }

    private ClientCommentResponse toResponse(ClientComment comment) {
        var author = comment.getUser();
        var profile = author != null ? author.getProfile() : null;
        return new ClientCommentResponse(
                comment.getId(),
                comment.getClient() != null ? comment.getClient().getId() : null,
                author != null ? author.getId() : null,
                author != null ? author.getUsername() : null,
                profile != null ? profile.getAvatarUrl() : null,
                comment.getContent(),
                comment.getCreatedAt());
    }

}
