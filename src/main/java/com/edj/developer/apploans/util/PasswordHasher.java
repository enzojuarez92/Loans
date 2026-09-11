package com.edj.developer.apploans.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/** Contraseñas con PBKDF2, sin agregar dependencias externas al instalador. */
public final class PasswordHasher {
    private static final String PREFIX = "pbkdf2-sha256";
    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() { }

    public static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(password.toCharArray(), salt, ITERATIONS);
        return PREFIX + "$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derived);
    }

    public static boolean matches(String password, String storedValue) {
        if (storedValue == null || !storedValue.startsWith(PREFIX + "$")) return false;
        try {
            String[] parts = storedValue.split("\\$", -1);
            if (parts.length != 4) return false;
            int iterations = Integer.parseInt(parts[1]);
            byte[] actual = Base64.getDecoder().decode(parts[3]);
            byte[] candidate = derive(password.toCharArray(), Base64.getDecoder().decode(parts[2]), iterations);
            return constantTimeEquals(actual, candidate);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public static boolean isLegacyPlainText(String storedValue) {
        return storedValue != null && !storedValue.startsWith(PREFIX + "$");
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS);
            try {
                return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo proteger la contraseña.", ex);
        }
    }

    private static boolean constantTimeEquals(byte[] left, byte[] right) {
        if (left.length != right.length) return false;
        int difference = 0;
        for (int i = 0; i < left.length; i++) difference |= left[i] ^ right[i];
        return difference == 0;
    }
}
