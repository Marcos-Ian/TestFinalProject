package security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Placeholder BCrypt-style hashing helper. Swap with an actual BCrypt implementation when available.
 */
public final class PasswordHasher {
    private PasswordHasher() {}

    public static String hash(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Hashing algorithm unavailable", e);
        }
    }

    public static boolean matches(String rawPassword, String hashed) {
        return hash(rawPassword).equals(hashed);
    }
}
