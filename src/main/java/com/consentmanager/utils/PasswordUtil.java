package com.consentmanager.utils;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordUtil {

    private static final Logger logger = LoggerFactory.getLogger(PasswordUtil.class);
    private static final int LOG_ROUNDS = 12; // Standard work factor for BCrypt

    /**
     * Hashes a plain text password using BCrypt.
     *
     * @param plainPassword The password to hash.
     * @return The hashed password.
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            logger.warn("Attempted to hash an empty or null password.");
            throw new IllegalArgumentException("Password cannot be empty or null.");
        }
        String salt = BCrypt.gensalt(LOG_ROUNDS);
        String hashedPassword = BCrypt.hashpw(plainPassword, salt);
        logger.debug("Password hashed successfully.");
        return hashedPassword;
    }

    /**
     * Verifies a plain text password against a stored hashed password.
     *
     * @param plainPassword    The plain text password to verify.
     * @param hashedPassword The stored hashed password.
     * @return true if the password matches, false otherwise.
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || plainPassword.isEmpty() || hashedPassword == null || hashedPassword.isEmpty()) {
            logger.warn("Attempted to verify with empty or null password/hash.");
            return false;
        }
        try {
            boolean matches = BCrypt.checkpw(plainPassword, hashedPassword);
            logger.debug("Password verification result: {}", matches);
            return matches;
        } catch (IllegalArgumentException e) {
            // BCrypt.checkpw can throw this if the hash is not a valid BCrypt hash
            logger.warn("Password verification failed due to invalid hash format: {}", hashedPassword, e);
            return false;
        }
    }

    // Optional: Main method for testing
    public static void main(String[] args) {
        String myPassword = "testPassword123!";
        String hashedPassword = hashPassword(myPassword);
        System.out.println("Original Password: " + myPassword);
        System.out.println("Hashed Password: " + hashedPassword);

        boolean isMatch = verifyPassword(myPassword, hashedPassword);
        System.out.println("Password matches: " + isMatch); // Should be true

        boolean isNotMatch = verifyPassword("wrongPassword", hashedPassword);
        System.out.println("Password matches (wrong): " + isNotMatch); // Should be false

        // Test with an invalid hash
        boolean invalidHashTest = verifyPassword(myPassword, "not-a-bcrypt-hash");
        System.out.println("Password matches (invalid hash): " + invalidHashTest); // Should be false
    }
}
