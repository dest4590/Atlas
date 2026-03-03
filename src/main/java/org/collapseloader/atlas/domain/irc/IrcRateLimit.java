package org.collapseloader.atlas.domain.irc;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

final class IrcRateLimit {
    private static final int GUEST_MESSAGES_PER_SECOND = 1;
    private static final int GUEST_MESSAGES_PER_MINUTE = 30;
    private static final int GUEST_BURST_SIZE = 2;

    private static final int USER_MESSAGES_PER_SECOND = 2;
    private static final int USER_MESSAGES_PER_MINUTE = 60;
    private static final int USER_BURST_SIZE = 3;

    private static final int VIOLATION_THRESHOLD = 5;
    private static final Duration VIOLATION_RESET = Duration.ofMinutes(2);
    private static final Duration TEMP_MUTE = Duration.ofMinutes(5);

    private IrcRateLimit() {
    }

    static Result check(IrcSession session) {
        if (session.isAdminOrOwner()) {
            return Result.allow();
        }

        Instant now = Instant.now();
        if (session.getTempMutedUntil() != null && now.isBefore(session.getTempMutedUntil())) {
            long remaining = Duration.between(now, session.getTempMutedUntil()).toSeconds();
            return Result.deny("You are temporarily muted for " + remaining + " more seconds due to spam.");
        }

        if (session.getTempMutedUntil() != null && now.isAfter(session.getTempMutedUntil())) {
            session.setTempMutedUntil(null);
            session.resetViolations();
        }

        if (session.getLastViolationTime() != null
                && Duration.between(session.getLastViolationTime(), now).compareTo(VIOLATION_RESET) > 0) {
            session.resetViolations();
        }

        Config config = configFor(session.getRole());

        Instant minuteCutoff = now.minusSeconds(60);
        session.getMessageTimestamps().removeIf(instant -> instant.isBefore(minuteCutoff));

        long inSecond = session.getMessageTimestamps().stream().filter(ts -> ts.isAfter(now.minusSeconds(1))).count();
        long inMinute = session.getMessageTimestamps().size();
        long inBurst = session.getMessageTimestamps().stream().filter(ts -> ts.isAfter(now.minusSeconds(3))).count();

        if (inSecond >= config.messagesPerSecond) {
            return violation(session, "Rate limit: Maximum " + config.messagesPerSecond + " messages per second. Slow down!");
        }
        if (inMinute >= config.messagesPerMinute) {
            return violation(session, "Rate limit: Maximum " + config.messagesPerMinute + " messages per minute. Take a break!");
        }
        if (inBurst >= config.burstSize) {
            return violation(session, "Rate limit: Maximum " + config.burstSize + " messages in quick succession. Slow down!");
        }

        session.getMessageTimestamps().addLast(now);
        return Result.allow();
    }

    static String status(IrcSession session) {
        if (session.isAdminOrOwner()) {
            return "Rate limiting: Disabled (privileged user)";
        }

        Config config = configFor(session.getRole());
        Instant now = Instant.now();
        long inSecond = session.getMessageTimestamps().stream().filter(ts -> ts.isAfter(now.minusSeconds(1))).count();
        long inMinute = session.getMessageTimestamps().stream().filter(ts -> ts.isAfter(now.minusSeconds(60))).count();

        StringBuilder status = new StringBuilder();
        status.append("Rate limit status:\n")
                .append("  Last second: ").append(inSecond).append("/").append(config.messagesPerSecond).append(" messages\n")
                .append("  Last minute: ").append(inMinute).append("/").append(config.messagesPerMinute).append(" messages\n")
                .append("  Violations: ").append(session.getViolationCount()).append("/").append(VIOLATION_THRESHOLD);

        if (session.getTempMutedUntil() != null && now.isBefore(session.getTempMutedUntil())) {
            long remaining = Duration.between(now, session.getTempMutedUntil()).toSeconds();
            status.append("\n  Temporarily muted for: ").append(remaining).append(" seconds");
        }

        return status.toString();
    }

    private static Result violation(IrcSession session, String message) {
        int violations = session.incrementViolations();
        session.setLastViolationTime(Instant.now());
        if (violations >= VIOLATION_THRESHOLD) {
            session.setTempMutedUntil(Instant.now().plus(TEMP_MUTE));
            session.resetViolations();
        }
        return Result.deny(message);
    }

    private static Config configFor(String role) {
        String normalized = role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
        if ("guest".equals(normalized)) {
            return new Config(GUEST_MESSAGES_PER_SECOND, GUEST_MESSAGES_PER_MINUTE, GUEST_BURST_SIZE);
        }
        return new Config(USER_MESSAGES_PER_SECOND, USER_MESSAGES_PER_MINUTE, USER_BURST_SIZE);
    }

    record Result(boolean allowed, String message) {
        static Result allow() {
            return new Result(true, "");
        }

        static Result deny(String message) {
            return new Result(false, message);
        }
    }

    private record Config(int messagesPerSecond, int messagesPerMinute, int burstSize) {
    }
}
