package org.collapseloader.atlas.util.passwords;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public final class DjangoPasswordUtils {
    private static byte[] pbkdf2(
            char[] password,
            byte[] salt,
            int iterations
    ) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    public static boolean matches(CharSequence raw, String encoded) {
        try {
            String[] p = encoded.split("\\$");
            int iterations = Integer.parseInt(p[1]);
            String salt = p[2];
            String expected = p[3];

            byte[] hash = pbkdf2(
                    raw.toString().toCharArray(),
                    salt.getBytes(StandardCharsets.UTF_8),
                    iterations
            );

            String actual = Base64.getEncoder().encodeToString(hash);
            return MessageDigest.isEqual(
                    actual.getBytes(StandardCharsets.UTF_8),
                    expected.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }
}
