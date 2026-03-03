package org.collapseloader.atlas.domain.irc;

import java.util.List;
import java.util.regex.Pattern;

final class IrcSanitizer {
    private static final int MAX_USERNAME_CHARS = 32;
    private static final int MAX_MESSAGE_CHARS = 256;

    private static final Pattern USERNAME_PATTERN = Pattern
            .compile("^[\\p{L}\\p{N}\\p{P}\\p{Z}§<>?{}\\[\\]\"';]{1,256}$");
    private static final Pattern MESSAGE_PATTERN = Pattern
            .compile("^[\\p{L}\\p{N}\\p{P}\\p{Z}§<>?{}\\[\\]\"';]{1,256}$");
    private static final Pattern INVALID_MC_COLOR = Pattern.compile("§[^0-9a-fklmnor]", Pattern.CASE_INSENSITIVE);
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private static final List<Pattern> DANGEROUS_PATTERNS = List.of(
            Pattern.compile("[\\x00\\x01\\x02\\x03\\x04\\x05\\x06\\x07\\x08\\x0E\\x0F]"),
            Pattern.compile("\\\\x[0-9a-fA-F]{2}"));

    private IrcSanitizer() {
    }

    static String sanitizeUsername(String username) {
        String value = sanitizeString(username, MAX_USERNAME_CHARS);
        if (value.isBlank()) {
            throw new IllegalArgumentException("username cannot be empty");
        }
        if (!USERNAME_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("username contains invalid characters");
        }
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                throw new IllegalArgumentException("username contains prohibited content");
            }
        }
        return value;
    }

    static String sanitizeMessage(String message) {
        String value = sanitizeString(message, MAX_MESSAGE_CHARS);
        if (value.isBlank()) {
            throw new IllegalArgumentException("message cannot be empty");
        }
        if (!MESSAGE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("message contains invalid characters");
        }
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                throw new IllegalArgumentException("message contains prohibited content");
            }
        }
        return sanitizeMinecraftColors(value);
    }

    static String sanitizePlain(String input) {
        return sanitizeString(input, MAX_MESSAGE_CHARS);
    }

    private static String sanitizeString(String input, int maxChars) {
        if (input == null) {
            return "";
        }

        String value = removeControlChars(input.trim());
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            value = pattern.matcher(value).replaceAll("");
        }
        value = MULTI_SPACE.matcher(value).replaceAll(" ");
        if (value.length() > maxChars) {
            value = value.substring(0, maxChars);
        }
        return sanitizeMinecraftColors(value);
    }

    private static String removeControlChars(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isISOControl(c) && c != '\n' && c != '\t') {
                continue;
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private static String sanitizeMinecraftColors(String text) {
        return INVALID_MC_COLOR.matcher(text).replaceAll("§f");
    }
}
