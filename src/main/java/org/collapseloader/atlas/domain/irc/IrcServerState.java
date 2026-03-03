package org.collapseloader.atlas.domain.irc;

import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class IrcServerState {
    private final IrcSettings settings;

    private final Map<Channel, IrcSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, IrcSession> usernames = new ConcurrentHashMap<>();

    private final Set<String> bannedIps = ConcurrentHashMap.newKeySet();
    private final Set<String> mutedIps = ConcurrentHashMap.newKeySet();

    private final Deque<IrcPackets.OutgoingPacket> history = new ConcurrentLinkedDeque<>();

    private final AtomicLong guestCounter = new AtomicLong(1);
    private final AtomicLong packetCounter = new AtomicLong(1);

    private int lastOnlineUsers = -1;
    private int lastOnlineGuests = -1;

    public long nextGuestId() {
        return guestCounter.getAndIncrement();
    }

    public long nextPacketSequence() {
        return packetCounter.getAndIncrement();
    }

    public void register(IrcSession session) {
        sessions.put(session.getChannel(), session);
        usernames.put(session.getName().toLowerCase(Locale.ROOT), session);
    }

    public void unregister(Channel channel) {
        IrcSession removed = sessions.remove(channel);
        if (removed != null) {
            usernames.remove(removed.getName().toLowerCase(Locale.ROOT), removed);
        }
    }

    public List<IrcSession> snapshotUsers() {
        return new ArrayList<>(sessions.values());
    }

    public IrcSession findByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        for (IrcSession user : sessions.values()) {
            if (userId.equals(user.getUserId())) {
                return user;
            }
        }
        return null;
    }

    public List<IrcSession> findAllByPartialName(String partialName) {
        String needle = partialName == null ? "" : partialName.trim().toLowerCase(Locale.ROOT);
        if (needle.isBlank()) {
            return List.of();
        }

        IrcSession exact = usernames.get(needle);
        if (exact != null) {
            return List.of(exact);
        }

        List<IrcSession> matches = new ArrayList<>();
        for (IrcSession user : sessions.values()) {
            String current = user.getName().toLowerCase(Locale.ROOT);
            if (current.startsWith(needle)) {
                matches.add(user);
            }
        }

        matches.sort(Comparator.comparing(IrcSession::getName, String.CASE_INSENSITIVE_ORDER));
        return matches;
    }

    public void appendHistory(IrcPackets.OutgoingPacket packet) {
        history.addLast(packet);
        while (history.size() > Math.max(1, settings.getHistoryLimit())) {
            history.pollFirst();
        }
    }

    public List<IrcPackets.OutgoingPacket> historySnapshot() {
        return new ArrayList<>(history);
    }

    public boolean isIpBanned(String ip) {
        return bannedIps.contains(ip);
    }

    public boolean isIpMuted(String ip) {
        return mutedIps.contains(ip);
    }

    public int setIpBanned(String ip, boolean banned) {
        if (ip == null || ip.isBlank()) return 0;
        if (banned) bannedIps.add(ip);
        else bannedIps.remove(ip);

        int affected = 0;
        for (IrcSession session : snapshotUsers()) {
            if (!ip.equals(session.getIp())) continue;
            session.setBanned(banned);
            affected++;
            if (banned) {
                session.sendSystem("Your IP has been banned.");
                session.getChannel().close();
            }
        }
        return affected;
    }

    public int setIpMuted(String ip, boolean muted) {
        if (ip == null || ip.isBlank()) return 0;
        if (muted) mutedIps.add(ip);
        else mutedIps.remove(ip);

        int affected = 0;
        for (IrcSession session : snapshotUsers()) {
            if (!ip.equals(session.getIp())) continue;
            session.setMuted(muted);
            affected++;
            session.sendSystem(muted ? "Your IP has been muted." : "Your IP has been unmuted.");
        }
        return affected;
    }

    public void broadcast(IrcPackets.OutgoingPacket packet) {
        appendHistory(packet);
        for (IrcSession session : snapshotUsers()) {
            session.sendPacket(packet);
        }
    }

    public void broadcastSystemChat(String message) {
        broadcast(IrcPackets.OutgoingPacket.builder()
                .type("chat")
                .time(Instant.now().toString())
                .content(message)
                .build());
    }

    public void broadcastRoomState() {
        int usersCount = 0;
        int guestsCount = 0;
        List<IrcSession> loaderTargets = new ArrayList<>();

        for (IrcSession session : sessions.values()) {
            if ("guest".equalsIgnoreCase(session.getRole())) {
                guestsCount++;
            } else {
                usersCount++;
            }
            if ("loader".equalsIgnoreCase(session.getClientType())) {
                loaderTargets.add(session);
            }
        }

        if (usersCount == lastOnlineUsers && guestsCount == lastOnlineGuests) {
            return;
        }

        lastOnlineUsers = usersCount;
        lastOnlineGuests = guestsCount;

        IrcPackets.OutgoingPacket packet = IrcPackets.OutgoingPacket.builder()
                .type("room_state")
                .roomState(IrcPackets.RoomState.builder()
                        .onlineUsers(usersCount)
                        .onlineGuests(guestsCount)
                        .build())
                .build();

        for (IrcSession target : loaderTargets) {
            target.sendPacket(packet);
        }
    }
}
