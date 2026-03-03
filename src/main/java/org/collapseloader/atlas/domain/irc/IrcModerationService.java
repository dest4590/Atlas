package org.collapseloader.atlas.domain.irc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.collapseloader.atlas.domain.irc.entity.IrcBan;
import org.collapseloader.atlas.domain.irc.entity.IrcMute;
import org.collapseloader.atlas.domain.irc.repository.IrcBanRepository;
import org.collapseloader.atlas.domain.irc.repository.IrcMuteRepository;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IrcModerationService {
    private final IrcBanRepository banRepository;
    private final IrcMuteRepository muteRepository;
    private final UserRepository userRepository;
    private final IrcServerState state;

    @Transactional(readOnly = true)
    public boolean isUserBanned(String userId) {
        if (userId == null || userId.startsWith("guest-")) return false;
        try {
            return banRepository.existsByUserId(Long.parseLong(userId));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean isUserMuted(String userId) {
        if (userId == null || userId.startsWith("guest-")) return false;
        try {
            return muteRepository.existsByUserId(Long.parseLong(userId));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Transactional
    public int setUserBanned(String userId, boolean banned) {
        if (userId == null || userId.startsWith("guest-")) return 0;
        long uid = Long.parseLong(userId);

        if (banned) {
            if (!banRepository.existsByUserId(uid)) {
                User user = userRepository.findById(uid).orElse(null);
                if (user == null) return 0;
                banRepository.save(IrcBan.builder().user(user).reason("Banned via IRC").build());
            }
        } else {
            banRepository.deleteByUserId(uid);
        }

        int affected = 0;
        for (IrcSession session : state.snapshotUsers()) {
            if (userId.equals(session.getUserId())) {
                session.setBanned(banned);
                affected++;
                if (banned) {
                    session.sendSystem("You have been banned.");
                    session.getChannel().close();
                } else {
                    session.sendSystem("You have been unbanned.");
                }
            }
        }
        return affected;
    }

    @Transactional
    public int setUserMuted(String userId, boolean muted) {
        if (userId == null || userId.startsWith("guest-")) return 0;
        long uid = Long.parseLong(userId);

        if (muted) {
            if (!muteRepository.existsByUserId(uid)) {
                User user = userRepository.findById(uid).orElse(null);
                if (user == null) return 0;
                muteRepository.save(IrcMute.builder().user(user).reason("Muted via IRC").build());
            }
        } else {
            muteRepository.deleteByUserId(uid);
        }

        int affected = 0;
        for (IrcSession session : state.snapshotUsers()) {
            if (userId.equals(session.getUserId())) {
                session.setMuted(muted);
                affected++;
                session.sendSystem(muted ? "You have been muted." : "You have been unmuted.");
            }
        }
        return affected;
    }
}
