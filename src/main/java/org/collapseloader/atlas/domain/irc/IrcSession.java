package org.collapseloader.atlas.domain.irc;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Getter
public class IrcSession {
    private final Channel channel;
    private final Consumer<IrcPackets.OutgoingPacket> sender;
    private final String ip;
    private final String userId;
    private final String token;
    private final String clientType;
    private final String clientName;
    private final String role;
    private final boolean authenticated;
    private final Deque<Instant> messageTimestamps = new ArrayDeque<>();
    private final AtomicInteger rateLimitViolations = new AtomicInteger(0);
    @Setter
    private String name;
    @Setter
    private String lastPrivatePartner = "";
    @Setter
    private boolean banned;
    @Setter
    private boolean muted;
    @Setter
    private Instant lastViolationTime;
    @Setter
    private Instant tempMutedUntil;

    public IrcSession(
            Channel channel,
            Consumer<IrcPackets.OutgoingPacket> sender,
            String ip,
            String userId,
            String token,
            String clientType,
            String clientName,
            String role,
            boolean authenticated,
            String name,
            boolean banned,
            boolean muted
    ) {
        this.channel = channel;
        this.sender = sender;
        this.ip = ip;
        this.userId = userId;
        this.token = token;
        this.clientType = clientType;
        this.clientName = clientName;
        this.role = role;
        this.authenticated = authenticated;
        this.name = name;
        this.banned = banned;
        this.muted = muted;
    }

    public void sendPacket(IrcPackets.OutgoingPacket packet) {
        sender.accept(packet);
    }

    public void sendSystem(String message) {
        sendPacket(IrcPackets.OutgoingPacket.builder()
                .type("system")
                .time(Instant.now().toString())
                .content(message)
                .build());
    }

    public boolean isAdminOrOwner() {
        String lowered = role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
        return lowered.equals("admin") || lowered.equals("owner") || lowered.equals("developer");
    }

    public int incrementViolations() {
        return rateLimitViolations.incrementAndGet();
    }

    public void resetViolations() {
        rateLimitViolations.set(0);
    }

    public int getViolationCount() {
        return rateLimitViolations.get();
    }
}
