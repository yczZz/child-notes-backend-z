package com.ycz.childnotesbackend.util;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class AdminPasswordUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final int SALT_BYTES = 16;

    private static final int ITERATIONS = 120000;

    private static final int KEY_LENGTH = 256;

    private AdminPasswordUtil() {
    }

    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String password, String salt) {
        try {
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return Base64.getEncoder().encodeToString(factory.generateSecret(spec).getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash admin password", e);
        }
    }

    public static boolean matches(String rawPassword, String salt, String expectedHash) {
        if (rawPassword == null || salt == null || expectedHash == null) {
            return false;
        }
        return constantTimeEquals(hash(rawPassword, salt), expectedHash);
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        int max = Math.max(left.length(), right.length());
        int result = left.length() ^ right.length();
        for (int i = 0; i < max; i++) {
            char l = i < left.length() ? left.charAt(i) : 0;
            char r = i < right.length() ? right.charAt(i) : 0;
            result |= l ^ r;
        }
        return result == 0;
    }
}
