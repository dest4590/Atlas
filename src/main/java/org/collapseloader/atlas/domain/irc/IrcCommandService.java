package org.collapseloader.atlas.domain.irc;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.entity.UserStatus;
import org.collapseloader.atlas.domain.users.repository.UserProfileRepository;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.collapseloader.atlas.domain.users.service.UserStatusService;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IrcCommandService {
    private static final Set<String> ADMIN_COMMANDS = Set.of(
            "ban", "unban", "mute", "unmute", "banip", "unbanip", "muteip", "unmuteip", "sysmsg"
    );
    private static final Map<String, String> ROLE_COLORS = Map.of(
            "tester", "a",
            "admin", "c",
            "developer", "6",
            "owner", "5"
    );
    private static final Map<String, String> ROLE_LABELS = Map.of(
            "tester", "Tester",
            "admin", "Admin",
            "developer", "Developer",
            "owner", "Owner"
    );

    private final IrcServerState state;
    private final IrcModerationService moderationService;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserStatusService userStatusService;

    public boolean handleUserCommand(IrcSession user, String command) {
        String[] parts = split(command);
        if (parts.length == 0) {
            return false;
        }

        String cmd = parts[0].toLowerCase(Locale.ROOT);
        switch (cmd) {
            case "@ping" -> {
                user.sendSystem("PONG");
                return true;
            }
            case "@online" -> {
                List<IrcSession> users = state.snapshotUsers();
                int totalCount = users.size();
                int guestCount = (int) users.stream().filter(u -> "guest".equalsIgnoreCase(u.getRole())).count();
                if (guestCount > 0) {
                    user.sendSystem("Channel info: " + totalCount + " users online (" + guestCount + " guests)");
                } else {
                    user.sendSystem("Channel info: " + totalCount + " users online");
                }
                return true;
            }
            case "@who", "@list" -> {
                List<String> usersList = new ArrayList<>();
                List<String> guestsList = new ArrayList<>();
                for (IrcSession session : state.snapshotUsers()) {
                    String display = userDisplayName(session);
                    if ("guest".equalsIgnoreCase(session.getRole())) {
                        guestsList.add(display);
                    } else {
                        usersList.add(display);
                    }
                }

                StringBuilder text = new StringBuilder();
                text.append("Online users (").append(usersList.size() + guestsList.size()).append("):\n");
                if (!usersList.isEmpty()) {
                    text.append("Users:\n").append(String.join("\n", usersList)).append("\n");
                } else {
                    text.append("Users: none\n");
                }
                if (!guestsList.isEmpty()) {
                    text.append("Guests:\n").append(String.join("\n", guestsList));
                } else {
                    text.append("Guests: none");
                }

                user.sendSystem(text.toString());
                return true;
            }
            case "@ratelimit" -> {
                user.sendSystem(IrcRateLimit.status(user));
                return true;
            }
            case "@help" -> {
                user.sendSystem(buildHelpText(user));
                return true;
            }
            case "@profile" -> {
                if (!user.isAdminOrOwner()) {
                    user.sendSystem("ERROR: You are not authorized");
                    return true;
                }

                String target = parts.length < 2 ? user.getUserId() : parts[1];
                showProfile(user, target);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public void handleAdminCommand(IrcSession user, String command) {
        if (!user.isAdminOrOwner()) {
            user.sendSystem("ERROR: You are not authorized");
            return;
        }

        String[] parts = split(command);
        if (parts.length == 0 || !parts[0].startsWith("@")) {
            return;
        }

        String action = parts[0].substring(1).toLowerCase(Locale.ROOT);
        if (!ADMIN_COMMANDS.contains(action)) {
            user.sendSystem("Unknown admin command");
            return;
        }

        if (!action.equals("sysmsg") && parts.length < 2) {
            user.sendSystem("ERROR: Admin command requires a target");
            return;
        }

        switch (action) {
            case "ban" -> applyUserModeration(user, parts[1], true, true);
            case "unban" -> applyUserModeration(user, parts[1], false, true);
            case "mute" -> applyUserModeration(user, parts[1], true, false);
            case "unmute" -> applyUserModeration(user, parts[1], false, false);
            case "banip" -> applyIpModeration(user, parts[1], true, true);
            case "unbanip" -> applyIpModeration(user, parts[1], false, true);
            case "muteip" -> applyIpModeration(user, parts[1], true, false);
            case "unmuteip" -> applyIpModeration(user, parts[1], false, false);

            case "sysmsg" -> {
                if (parts.length < 2) {
                    user.sendSystem("Usage: @sysmsg <message>");
                    return;
                }
                String text = command.substring(command.indexOf(' ') + 1).trim();
                String full = "§c§lSystem§r: §l" + text + "§r";
                state.broadcastSystemChat(full);
                user.sendSystem("System message sent");
            }
            default -> {
            }
        }
    }

    public void handlePrivateMessage(IrcSession user, String message) {
        if (user.isMuted() && !user.isAdminOrOwner()) {
            user.sendSystem("You are muted.");
            return;
        }

        String[] parts = split(message);
        if (parts.length < 3) {
            user.sendSystem("Usage: @msg <nickname> <message>");
            return;
        }

        String targetName = parts[1];
        String privateMessage = message.substring(message.indexOf(targetName) + targetName.length()).trim();

        List<IrcSession> matches = state.findAllByPartialName(targetName);
        if (matches.isEmpty()) {
            user.sendSystem("User '" + targetName + "' not found");
            return;
        }
        if (matches.size() > 1) {
            user.sendSystem("Multiple users match '" + targetName + "': "
                    + String.join(", ", matches.stream().map(IrcSession::getName).toList()) + ". Be more specific.");
            return;
        }

        IrcSession target = matches.getFirst();
        if (target.getUserId().equals(user.getUserId())) {
            user.sendSystem("You cannot send a message to yourself");
            return;
        }

        user.setLastPrivatePartner(target.getName());
        target.setLastPrivatePartner(user.getName());

        target.sendPacket(IrcPackets.OutgoingPacket.builder()
                .type("private")
                .content("[PM from " + formatNameWithRole(user) + "]: " + privateMessage)
                .build());
        user.sendPacket(IrcPackets.OutgoingPacket.builder()
                .type("private")
                .content("[PM to " + formatNameWithRole(target) + "]: " + privateMessage)
                .build());
    }

    public void handleQuickReply(IrcSession user, String message) {
        if (user.isMuted() && !user.isAdminOrOwner()) {
            user.sendSystem("You are muted.");
            return;
        }
        if (user.getLastPrivatePartner() == null || user.getLastPrivatePartner().isBlank()) {
            user.sendSystem("No previous private conversation found");
            return;
        }

        String[] parts = split(message);
        if (parts.length < 2) {
            user.sendSystem("Usage: @r <message>");
            return;
        }

        String reply = message.substring(message.indexOf(' ') + 1).trim();
        List<IrcSession> matches = state.findAllByPartialName(user.getLastPrivatePartner());
        if (matches.isEmpty()) {
            user.sendSystem("User '" + user.getLastPrivatePartner() + "' is no longer online");
            return;
        }

        IrcSession target = matches.getFirst();
        target.setLastPrivatePartner(user.getName());
        target.sendPacket(IrcPackets.OutgoingPacket.builder()
                .type("private")
                .content("[PM from " + formatNameWithRole(user) + "]: " + reply)
                .build());

        user.sendPacket(IrcPackets.OutgoingPacket.builder()
                .type("private")
                .content("[PM to " + formatNameWithRole(target) + "]: " + reply)
                .build());
    }

    public String formatNameWithRole(IrcSession session) {
        String role = session.getRole() == null ? "user" : session.getRole().trim().toLowerCase(Locale.ROOT);
        String color = ROLE_COLORS.getOrDefault(role, "f");
        String roleLabel = ROLE_LABELS.getOrDefault(role, "User");

        String clientPart = session.getClientName() == null || session.getClientName().isBlank()
                ? ""
                : " §7(" + session.getClientName() + ")§r";

        return "§" + color + session.getName() + "§r" + clientPart + " [§" + color + roleLabel + "§r]";
    }

    public String userDisplayName(IrcSession session) {
        String name = session.getName();
        String client = session.getClientName();
        if ((client == null || client.isBlank()) && session.getClientType() != null) {
            client = session.getClientType();
        }
        if (client != null && !client.isBlank()) {
            return name + " §7(" + client + ")§r";
        }
        return name;
    }

    private void showProfile(IrcSession requester, String targetValue) {
        String target = targetValue == null ? "" : targetValue.trim();
        if (target.isBlank()) {
            target = requester.getUserId();
        }

        IrcSession onlineByName = state.findAllByPartialName(target).stream().findFirst().orElse(null);
        String targetUserId = onlineByName != null ? onlineByName.getUserId() : target;
        if (targetUserId.startsWith("guest-")) {
            IrcSession guest = state.findByUserId(targetUserId);
            if (guest == null) {
                requester.sendSystem("Guest not found online.");
                return;
            }
            requester.sendSystem("Guest Profile:\nName: " + guest.getName() + "\nID: " + guest.getUserId() + "\nIP: " + guest.getIp() + "\nConnected: Yes");
            return;
        }

        Long id;
        try {
            id = Long.parseLong(targetUserId);
        } catch (NumberFormatException ex) {
            requester.sendSystem("User '" + target + "' not found online.");
            return;
        }

        User dbUser = userRepository.findById(id).orElse(null);
        if (dbUser == null) {
            requester.sendSystem("Failed to fetch profile: user not found");
            return;
        }

        var profile = userProfileRepository.findByUserId(id).orElse(null);
        var status = userStatusService.getStatus(id);

        StringBuilder out = new StringBuilder();
        out.append("Profile for ").append(dbUser.getUsername()).append(" (ID: ").append(dbUser.getId()).append("):\n");

        IrcSession online = state.findByUserId(String.valueOf(dbUser.getId()));
        if (online != null && online.getIp() != null && !online.getIp().isBlank()) {
            out.append("IP: ").append(online.getIp()).append("\n");
        }

        if (profile != null) {
            if (profile.getNickname() != null) {
                out.append("Nickname: ").append(profile.getNickname()).append("\n");
            }
            if (profile.getRole() != null) {
                out.append("Role: ").append(profile.getRole().name().toLowerCase(Locale.ROOT)).append("\n");
            }
            if (profile.getCreatedAt() != null) {
                out.append("Member Since: ").append(profile.getCreatedAt()).append("\n");
            }
            if (profile.getSocialLinks() != null && !profile.getSocialLinks().isEmpty()) {
                out.append("Social Links:\n");
                profile.getSocialLinks().forEach(link ->
                        out.append("- ").append(link.getPlatform().name().toLowerCase(Locale.ROOT)).append(": ").append(link.getUrl()).append("\n"));
            }
        }

        out.append("Status: ").append(status.status() == UserStatus.ONLINE ? "Online" : "Offline");
        if (status.status() == UserStatus.ONLINE
                && status.clientName() != null
                && !status.clientName().isBlank()) {
            out.append(" using ").append(status.clientName());
        }

        requester.sendSystem(out.toString().trim());
    }

    private ResolvedTarget resolveUserTarget(String input) {
        if (input == null || input.trim().isBlank()) {
            return new ResolvedTarget(null, null, null, "missing target");
        }

        String target = input.trim();
        List<IrcSession> matches = state.findAllByPartialName(target);
        if (matches.size() == 1) {
            IrcSession online = matches.getFirst();
            return new ResolvedTarget(online.getUserId(), online.getName(), online.getIp(), null);
        }
        if (matches.size() > 1) {
            return new ResolvedTarget(null, null, null,
                    "multiple users match '" + target + "': " + String.join(", ", matches.stream().map(IrcSession::getName).toList()));
        }

        try {
            Long.parseLong(target);
            return new ResolvedTarget(target, null, null, null);
        } catch (NumberFormatException ignored) {
        }

        if (target.toLowerCase(Locale.ROOT).startsWith("guest-")) {
            return new ResolvedTarget(target, null, null, null);
        }

        return new ResolvedTarget(null, target, null, null);
    }

    private void applyUserModeration(IrcSession actor, String rawTarget, boolean enabled, boolean ban) {
        ResolvedTarget target = resolveUserTarget(rawTarget);
        if (target.error != null) {
            actor.sendSystem("ERROR: " + target.error);
            return;
        }

        if (target.userId == null || target.userId.startsWith("guest-")) {
            actor.sendSystem("ERROR: cannot " + (ban ? "ban" : "mute") + " a guest by ID or username. Logged in users only.");
            return;
        }

        int affected = ban
                ? moderationService.setUserBanned(target.userId, enabled)
                : moderationService.setUserMuted(target.userId, enabled);

        String verb = moderationVerb(ban, enabled);
        actor.sendSystem(verb + " " + target.userId + " (affected " + affected + " connections)");
    }

    private void applyIpModeration(IrcSession actor, String input, boolean enabled, boolean ban) {
        String ip = parseIp(input);
        if (ip == null && enabled) {
            ip = resolveUserTarget(input).ip;
        }
        if (ip == null || ip.isBlank()) {
            actor.sendSystem(enabled ? "ERROR: provide an IP or an online user" : "ERROR: invalid IP");
            return;
        }

        int affected = ban ? state.setIpBanned(ip, enabled) : state.setIpMuted(ip, enabled);
        String verb = moderationVerb(ban, enabled);
        actor.sendSystem(verb + " IP " + ip + " (affected " + affected + " connections)");
    }

    private String moderationVerb(boolean ban, boolean enabled) {
        if (ban) {
            return enabled ? "Banned" : "Unbanned";
        }
        return enabled ? "Muted" : "Unmuted";
    }

    private String buildHelpText(IrcSession user) {
        String prefix = "loader".equalsIgnoreCase(user.getClientType()) ? "@" : "@@";
        List<String> userCommands = List.of(
                "ping - Test server connection",
                "online - Show number of online users",
                "who / " + prefix + "list - List online users",
                "ratelimit - Check your rate limit status",
                "help - Show this help message",
                "msg <nickname> <message> - Send private message (supports partial names)",
                "r <message> - Reply to last private message"
        );

        StringBuilder help = new StringBuilder("Available commands:\n");
        userCommands.forEach(line -> help.append(prefix).append(line).append("\n"));

        if (!user.isAdminOrOwner()) {
            return help.toString().trim();
        }

        List<String> adminCommands = List.of(
                "profile [nickname] - View user profile",
                "ban <user_id> - Ban a user",
                "unban <user_id> - Unban a user",
                "banip <user_id|ip> - Ban an IP address",
                "unbanip <ip> - Unban an IP address",
                "mute <user_id> - Mute a user",
                "unmute <user_id> - Unmute a user",
                "muteip <user_id|ip> - Mute an IP address",
                "unmuteip <ip> - Unmute an IP address",
                "sysmsg <message> - Send system message to all users"
        );

        help.append("Admin commands (require authentication):\n");
        adminCommands.forEach(line -> help.append(prefix).append(line).append("\n"));
        return help.toString().trim();
    }

    private String[] split(String text) {
        String trimmed = text == null ? "" : text.trim();
        return trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
    }

    private String parseIp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String input = value.trim();
        try {
            return InetAddress.getByName(input).getHostAddress();
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    private record ResolvedTarget(String userId, String username, String ip, String error) {
    }
}
