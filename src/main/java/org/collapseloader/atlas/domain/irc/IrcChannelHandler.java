package org.collapseloader.atlas.domain.irc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class IrcChannelHandler extends SimpleChannelInboundHandler<String> {

    private static final String AUTH_REQUIRED = "Authentication required";
    private static final Set<String> PUBLIC_BAN_ALLOWED = Set.of("@help", "@ping", "@online");
    private static final Set<String> ADMIN_COMMAND_PREFIXES = Set.of(
            "@ban ", "@unban ", "@sysmsg ", "@mute ", "@unmute ", "@banip ", "@unbanip ", "@muteip ", "@unmuteip ");

    private static final AttributeKey<IrcSession> SESSION_KEY = AttributeKey.valueOf("irc.session");
    private static final AttributeKey<ScheduledFuture<?>> AUTH_TIMEOUT_KEY = AttributeKey.valueOf("irc.auth.timeout");

    private final ObjectMapper objectMapper;
    private final IrcSettings settings;
    private final IrcServerState state;
    private final IrcAuthService authService;
    private final IrcCommandService commandService;
    private final IrcModerationService moderationService;
    private final IrcMetrics metrics;

    public IrcChannelHandler(
            ObjectMapper objectMapper,
            IrcSettings settings,
            IrcServerState state,
            IrcAuthService authService,
            IrcCommandService commandService,
            IrcModerationService moderationService,
            IrcMetrics metrics) {
        this.objectMapper = objectMapper;
        this.settings = settings;
        this.state = state;
        this.authService = authService;
        this.commandService = commandService;
        this.moderationService = moderationService;
        this.metrics = metrics;
    }

    @Override
    @SuppressWarnings("resource")
    public void channelActive(ChannelHandlerContext ctx) {
        ScheduledFuture<?> timeout = ctx.executor().schedule(() -> {
            if (ctx.channel().attr(SESSION_KEY).get() == null) {
                writePacket(ctx.channel(), IrcPackets.OutgoingPacket.builder()
                        .type("error")
                        .content(AUTH_REQUIRED)
                        .build());
                ctx.close();
            }
        }, settings.authTimeout().toSeconds(), TimeUnit.SECONDS);
        ctx.channel().attr(AUTH_TIMEOUT_KEY).set(timeout);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        IrcSession session = ctx.channel().attr(SESSION_KEY).get();
        if (session == null) {
            handleAuth(ctx, msg);
            return;
        }

        IrcPackets.IncomingPacket packet = parsePacket(msg);
        if (packet == null) {
            log.debug("Invalid IRC JSON packet from {}", ctx.channel().remoteAddress());
            return;
        }

        switch (safe(packet.getOp())) {
            case "ping" -> session.sendPacket(IrcPackets.OutgoingPacket.builder().type("pong").content("PONG").build());
            case "chat" -> handleChat(session, safe(packet.getContent()).trim());
            default -> {
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        IrcSession session = ctx.channel().attr(SESSION_KEY).get();
        cancelAuthTimeout(ctx.channel());

        if (session != null) {
            state.unregister(ctx.channel());
            // log.info("[UNREGISTER] User '{}' (ID: {}, role: {}, client: {}, type: {})
            // disconnected from {}",
            // session.getName(), session.getUserId(), session.getRole(),
            // session.getClientName(),
            // session.getClientType(), session.getIp());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.debug("IRC channel exception from {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }

    private void handleChat(IrcSession session, String incomingRaw) {
        String incoming = incomingRaw;

        if (!session.isAdminOrOwner()) {
            try {
                incoming = IrcSanitizer.sanitizeMessage(incoming);
            } catch (IllegalArgumentException ignored) {
                session.sendSystem("Invalid message format");
                return;
            }

            IrcRateLimit.Result result = IrcRateLimit.check(session);
            if (!result.allowed()) {
                session.sendSystem(result.message());
                return;
            }

            if (session.isBanned()) {
                session.sendSystem("You are banned.");
                return;
            }
            if (session.isMuted()) {
                session.sendSystem("You are muted.");
                return;
            }
        }

        if (incoming.startsWith("@")) {
            handleCommandMessage(session, incoming);
            return;
        }

        String sender = commandService.formatNameWithRole(session);
        String fullMessage = sender + ": " + incoming;

        IrcPackets.OutgoingPacket out = IrcPackets.OutgoingPacket.builder()
                .type("chat")
                .id(Instant.now().toEpochMilli() + "-" + state.nextPacketSequence())
                .time(Instant.now().toString())
                .content(fullMessage)
                .sender(IrcPackets.SenderInfo.builder()
                        .username(session.getName())
                        .role(session.getRole())
                        .userId(session.getUserId())
                        .build())
                .build();

        // record metrics && maybe in future add filter of system and user messages
        if (metrics != null) {
            metrics.recordChatMessage(session.getRole(), incoming.length());
        }

        state.broadcast(out);
    }

    private void handleCommandMessage(IrcSession session, String incoming) {
        if (!session.isAuthenticated()) {
            session.sendSystem("Login required to use commands.");
            return;
        }

        if (isBlockedByBan(session, incoming)) {
            session.sendSystem("You are banned.");
            return;
        }

        if (startsWithAny(incoming)) {
            commandService.handleAdminCommand(session, incoming);
            return;
        }
        if (incoming.startsWith("@msg ")) {
            commandService.handlePrivateMessage(session, incoming);
            return;
        }
        if (incoming.startsWith("@r ")) {
            commandService.handleQuickReply(session, incoming);
            return;
        }
        if (commandService.handleUserCommand(session, incoming)) {
            return;
        }

        String commandPrefix = "loader".equalsIgnoreCase(session.getClientType()) ? "@" : "@@";
        session.sendSystem("Unknown command. Use " + commandPrefix + "help");
    }

    private boolean isBlockedByBan(IrcSession session, String incoming) {
        if (!session.isBanned() || session.isAdminOrOwner()) {
            return false;
        }
        return PUBLIC_BAN_ALLOWED.stream().noneMatch(incoming::startsWith);
    }

    private void handleAuth(ChannelHandlerContext ctx, String raw) {
        IrcPackets.IncomingPacket authPacket = parsePacket(raw);
        if (authPacket == null) {
            log.debug("Failed to decode auth packet from {}", ctx.channel().remoteAddress());
            ctx.close();
            return;
        }

        if (!"auth".equals(safe(authPacket.getOp()))) {
            writePacket(ctx.channel(), IrcPackets.OutgoingPacket.builder()
                    .type("error")
                    .content(AUTH_REQUIRED)
                    .build());
            ctx.close();
            return;
        }

        String ip = resolveIp(ctx.channel());
        if (state.isIpBanned(ip)) {
            writePacket(ctx.channel(), IrcPackets.OutgoingPacket.builder()
                    .type("error")
                    .content("Your IP is banned")
                    .build());
            ctx.close();
            return;
        }

        String token = safe(authPacket.getToken());
        String clientType = safe(authPacket.getType());
        if (clientType.isBlank()) {
            clientType = "client";
        }

        String clientName = IrcSanitizer.sanitizePlain(authPacket.getClient());
        if ("loader".equalsIgnoreCase(clientType) && clientName.isBlank()) {
            clientName = "CollapseLoader";
        }

        IrcAuthService.AuthResult auth = authService.authenticate(token);

        String userId;
        String username;
        String role;
        boolean authenticated = auth.authenticated();

        if (!authenticated) {
            long guestId = state.nextGuestId();
            userId = "guest-" + guestId;
            username = "Guest-" + guestId;
            role = "guest";
        } else {
            userId = String.valueOf(auth.userId());
            username = auth.username();
            role = auth.role();
            if (!List.of("admin", "owner").contains(role.toLowerCase(Locale.ROOT))) {
                try {
                    username = IrcSanitizer.sanitizeUsername(username);
                } catch (IllegalArgumentException ex) {
                    username = IrcSanitizer.sanitizePlain(username);
                }
                if (username.isBlank()) {
                    username = "User-" + userId;
                }
            }
        }

        boolean muted = state.isIpMuted(ip) || moderationService.isUserMuted(userId);
        boolean banned = moderationService.isUserBanned(userId);

        if (banned) {
            writePacket(ctx.channel(), IrcPackets.OutgoingPacket.builder()
                    .type("error")
                    .content("You are banned")
                    .build());
            ctx.close();
            return;
        }

        IrcSession session = new IrcSession(
                ctx.channel(),
                packet -> writePacket(ctx.channel(), packet),
                ip,
                userId,
                token,
                clientType,
                clientName,
                role,
                authenticated,
                username,
                banned,
                muted);

        ctx.channel().attr(SESSION_KEY).set(session);
        state.register(session);
        cancelAuthTimeout(ctx.channel());

        if (!authenticated) {
            session.sendSystem("Connected as guest. Commands are disabled, but you can chat.");
        } else if (muted) {
            session.sendSystem("You are muted.");
        }

        replayHistoryIfNeeded(clientType, session);

        // log.info("[REGISTER] User '{}' (ID: {}, role: {}, client: {}, type: {})
        // connected from {}",
        // session.getName(), session.getUserId(), session.getRole(),
        // session.getClientName(),
        // session.getClientType(), session.getIp());
    }

    private void replayHistoryIfNeeded(String clientType, IrcSession session) {
        if ("client".equalsIgnoreCase(clientType)) {
            return;
        }
        for (IrcPackets.OutgoingPacket item : state.historySnapshot()) {
            session.sendPacket(IrcPackets.OutgoingPacket.builder()
                    .type(item.getType())
                    .id(item.getId())
                    .time(item.getTime())
                    .sender(item.getSender())
                    .content(item.getContent())
                    .history(true)
                    .roomState(item.getRoomState())
                    .build());
        }
    }

    private boolean startsWithAny(String text) {
        return IrcChannelHandler.ADMIN_COMMAND_PREFIXES.stream().anyMatch(text::startsWith);
    }

    private IrcPackets.IncomingPacket parsePacket(String raw) {
        try {
            return objectMapper.readValue(raw, IrcPackets.IncomingPacket.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void cancelAuthTimeout(Channel channel) {
        ScheduledFuture<?> timeout = channel.attr(AUTH_TIMEOUT_KEY).get();
        if (timeout != null) {
            timeout.cancel(false);
        }
    }

    private void writePacket(Channel channel, IrcPackets.OutgoingPacket packet) {
        try {
            channel.writeAndFlush(objectMapper.writeValueAsString(packet) + "\n");
        } catch (Exception ex) {
            log.debug("Failed to write packet to {}", channel.remoteAddress(), ex);
        }
    }

    private String resolveIp(Channel channel) {
        if (channel.remoteAddress() instanceof InetSocketAddress address) {
            return address.getAddress().getHostAddress();
        }
        return safe(String.valueOf(channel.remoteAddress()));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
