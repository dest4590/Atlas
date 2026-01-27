package org.collapseloader.atlas.domain.clients.service;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.clients.dto.response.ClientMyRatingResponse;
import org.collapseloader.atlas.domain.clients.dto.response.ClientRatingResponse;
import org.collapseloader.atlas.domain.clients.entity.ClientRating;
import org.collapseloader.atlas.domain.clients.repository.ClientRatingRepository;
import org.collapseloader.atlas.domain.clients.repository.ClientRepository;
import org.collapseloader.atlas.domain.users.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientRatingService {
    private final ClientRepository clientRepository;
    private final ClientRatingRepository ratingRepository;

    @Transactional
    public ClientRatingResponse submitRating(Long clientId, User user, short rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        var client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        var existing = ratingRepository.findByClientIdAndUserId(clientId, user.getId());
        ClientRating savedRating = existing.orElseGet(ClientRating::new);

        savedRating.setClient(client);
        savedRating.setUser(user);
        savedRating.setRating(rating);

        ratingRepository.save(savedRating);

        Double avgRating = ratingRepository.getAverageRating(clientId);
        Integer ratingCount = ratingRepository.getRatingCount(clientId);

        return new ClientRatingResponse(
                avgRating,
                ratingCount,
                (int) rating
        );
    }

    @Transactional(readOnly = true)
    public ClientMyRatingResponse getMyRating(Long clientId, User user) {
        var myRating = ratingRepository.findByClientIdAndUserId(clientId, user.getId())
                .map(ClientRating::getRating)
                .map(Short::intValue)
                .orElse(null);

        return new ClientMyRatingResponse(myRating);
    }

    @Transactional
    public void removeRating(Long clientId, User user) {
        ratingRepository.deleteByClientIdAndUserId(clientId, user.getId());
    }
}
