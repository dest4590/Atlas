package org.collapseloader.atlas.domain.users.service;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.achievements.repository.UserAchievementRepository;
import org.collapseloader.atlas.domain.clients.repository.ClientCommentRepository;
import org.collapseloader.atlas.domain.clients.repository.ClientRatingRepository;
import org.collapseloader.atlas.domain.friends.repository.FriendRequestRepository;
import org.collapseloader.atlas.domain.presets.repository.PresetCommentRepository;
import org.collapseloader.atlas.domain.presets.repository.PresetDownloadRepository;
import org.collapseloader.atlas.domain.presets.repository.PresetLikeRepository;
import org.collapseloader.atlas.domain.presets.repository.PresetRepository;
import org.collapseloader.atlas.domain.reports.repository.UserReportRepository;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final PresetCommentRepository presetCommentRepository;
    private final PresetLikeRepository presetLikeRepository;
    private final PresetDownloadRepository presetDownloadRepository;
    private final PresetRepository presetRepository;
    private final ClientCommentRepository clientCommentRepository;
    private final ClientRatingRepository clientRatingRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserReportRepository userReportRepository;
    private final UserFavoriteRepository userFavoriteRepository;
    private final UserExternalAccountRepository userExternalAccountRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final VerificationTokenRepository verificationTokenRepository;

    @Transactional
    public void deleteUser(User user) {
        Long userId = user.getId();

        friendRequestRepository.deleteAllByUserId(userId);
        presetCommentRepository.deleteAllByUserId(userId);
        presetLikeRepository.deleteAllByUserId(userId);
        presetDownloadRepository.deleteAllByUserId(userId);
        presetRepository.deleteAllByOwnerId(userId);

        clientCommentRepository.deleteAllByUserId(userId);
        clientRatingRepository.deleteAllByUserId(userId);

        userAchievementRepository.deleteAllByUserId(userId);
        userReportRepository.deleteAllByUserId(userId);
        userFavoriteRepository.deleteAllByUserId(userId);
        userExternalAccountRepository.deleteAllByUserId(userId);
        userPreferenceRepository.deleteAllByUserId(userId);
        verificationTokenRepository.deleteByUser(user);

        userRepository.delete(user);
    }
}
