package org.collapseloader.atlas.util.passwords;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
public class HybridPasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null)
            return false;
        String trimmedEncoded = encodedPassword.trim();

        if (trimmedEncoded.startsWith("pbkdf2_sha256$")) {
            return DjangoPasswordUtils.matches(rawPassword, trimmedEncoded);
        }

        if (trimmedEncoded.startsWith("$2")) {
            return bcrypt.matches(rawPassword, trimmedEncoded);
        }

        return false;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    public boolean isDjangoHash(String hash) {
        return hash != null && hash.trim().startsWith("pbkdf2_sha256$");
    }
}
