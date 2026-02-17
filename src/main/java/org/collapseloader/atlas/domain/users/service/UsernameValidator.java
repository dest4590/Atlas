package org.collapseloader.atlas.domain.users.service;

import org.collapseloader.atlas.exception.ValidationException;

import java.util.Set;
import java.util.regex.Pattern;

public final class UsernameValidator {
    private static final Pattern ALLOWED = Pattern.compile("^[A-Za-z][A-Za-z0-9._-]{2,29}$");
    private static final Pattern CONSECUTIVE = Pattern.compile("[._-]{2,}");
    private static final Set<String> RESERVED = Set.of(
            "admin",
            "administrator",
            "root",
            "system",
            "support",
            "staff",
            "null",
            "undefined",
            "api",
            "user",
            "users",
            "moderator",
            "mod",
            "owner"
    );

    private UsernameValidator() {
    }

    public static void validate(String username) {
        if (username == null) {
            throw new ValidationException("Username is required");
        }

        String u = username.trim();
        if (u.isEmpty()) {
            throw new ValidationException("Username is required");
        }

        if (u.length() < 3 || u.length() > 30) {
            throw new ValidationException("Username must be between 3 and 30 characters");
        }

        if (!ALLOWED.matcher(u).matches()) {
            throw new ValidationException("Username must start with a letter and contain only letters, numbers, '.', '_' or '-'");
        }

        if (CONSECUTIVE.matcher(u).find()) {
            throw new ValidationException("Username cannot contain consecutive '.', '_' or '-' characters");
        }

        char last = u.charAt(u.length() - 1);

        if (last == '.' || last == '_' || last == '-') {
            throw new ValidationException("Username cannot end with '.', '_' or '-'");
        }

        if (u.matches("\\d+")) {
            throw new ValidationException("Username cannot be entirely numeric");
        }

        if (RESERVED.contains(u.toLowerCase())) {
            throw new ValidationException("Username is reserved");
        }
    }
}
