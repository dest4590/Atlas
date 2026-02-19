package org.collapseloader.atlas.domain.clients.service;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.clients.dto.response.ClientDetailedResponse;
import org.collapseloader.atlas.domain.clients.entity.ClientScreenshot;
import org.collapseloader.atlas.domain.clients.repository.ClientCommentRepository;
import org.collapseloader.atlas.domain.clients.repository.ClientRatingRepository;
import org.collapseloader.atlas.domain.clients.repository.ClientRepository;
import org.collapseloader.atlas.domain.clients.repository.ClientScreenshotRepository;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientDetailsService {
    private final ClientRepository clientRepository;
    private final ClientRatingRepository ratingRepository;
    private final ClientCommentRepository commentRepository;
    private final ClientScreenshotRepository screenshotRepository;

    @Transactional(readOnly = true)
    public ClientDetailedResponse getDetailedInfo(Long clientId) throws NotFoundException {
        var client = clientRepository.findById(clientId)
                .orElseThrow(NotFoundException::new);

        Double avgRating = ratingRepository.getAverageRating(clientId);
        Integer ratingCount = ratingRepository.getRatingCount(clientId);
        Integer commentsCount = commentRepository.countByClientId(clientId);

        List<String> screenshots = screenshotRepository.findAllByClientIdOrderBySortOrderAsc(clientId)
                .stream()
                .map(ClientScreenshot::getImageUrl)
                .toList();

        return new ClientDetailedResponse(
                client.getId(),
                client.getName(),
                client.getVersion(),
                null,
                screenshots,
                avgRating != null ? avgRating : 0.0,
                ratingCount,
                commentsCount,
                client.getCreatedAt() != null
                        ? LocalDateTime.ofInstant(client.getCreatedAt(), ZoneId.systemDefault())
                        : null);
    }
}
