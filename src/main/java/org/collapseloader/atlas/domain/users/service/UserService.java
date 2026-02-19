package org.collapseloader.atlas.domain.users.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    @Transactional
    public void deleteUser(User user) {
        Long userId = user.getId();
        
        entityManager.createNativeQuery("DELETE FROM friend_requests WHERE requester_id = ? OR addressee_id = ? OR blocked_by_id = ?")
                .setParameter(1, userId)
                .setParameter(2, userId)
                .setParameter(3, userId)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM preset_comments WHERE user_id = ?").setParameter(1, userId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM preset_likes WHERE user_id = ?").setParameter(1, userId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM preset_downloads WHERE user_id = ?").setParameter(1, userId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM presets WHERE owner_id = ?").setParameter(1, userId).executeUpdate();
        
        entityManager.createNativeQuery("DELETE FROM client_comments WHERE user_id = ?").setParameter(1, userId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM client_ratings WHERE user_id = ?").setParameter(1, userId).executeUpdate();
        
        entityManager.createNativeQuery("DELETE FROM user_achievements WHERE user_id = ?").setParameter(1, userId).executeUpdate();
        
        entityManager.createNativeQuery("DELETE FROM user_reports WHERE reporter_id = ? OR reported_user_id = ? OR resolved_by_id = ?")
                .setParameter(1, userId)
                .setParameter(2, userId)
                .setParameter(3, userId)
                .executeUpdate();
        
        entityManager.createNativeQuery("DELETE FROM user_favorites WHERE user_id = ?").setParameter(1, userId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM user_external_accounts WHERE user_id = ?").setParameter(1, userId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM user_preferences WHERE user_id = ?").setParameter(1, userId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM verification_tokens WHERE user_id = ?").setParameter(1, userId).executeUpdate();
        
        userRepository.delete(user);
    }
}
