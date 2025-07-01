package com.consentmanager.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilTest {

    @Test
    void testHashPassword_success() {
        String password = "testPassword123";
        String hashedPassword = PasswordUtil.hashPassword(password);
        assertNotNull(hashedPassword);
        assertNotEquals(password, hashedPassword);
        assertTrue(hashedPassword.startsWith("$2a$") || hashedPassword.startsWith("$2b$")); // BCrypt prefix
    }

    @Test
    void testHashPassword_nullPassword_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordUtil.hashPassword(null);
        });
    }

    @Test
    void testHashPassword_emptyPassword_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordUtil.hashPassword("");
        });
    }

    @Test
    void testVerifyPassword_correctPassword_returnsTrue() {
        String password = "mySecurePassword@2024";
        String hashedPassword = PasswordUtil.hashPassword(password);
        assertTrue(PasswordUtil.verifyPassword(password, hashedPassword));
    }

    @Test
    void testVerifyPassword_incorrectPassword_returnsFalse() {
        String password = "mySecurePassword@2024";
        String incorrectPassword = "wrongPassword";
        String hashedPassword = PasswordUtil.hashPassword(password);
        assertFalse(PasswordUtil.verifyPassword(incorrectPassword, hashedPassword));
    }

    @Test
    void testVerifyPassword_nullPlainPassword_returnsFalse() {
        String hashedPassword = PasswordUtil.hashPassword("somePassword");
        assertFalse(PasswordUtil.verifyPassword(null, hashedPassword));
    }

    @Test
    void testVerifyPassword_emptyPlainPassword_returnsFalse() {
        String hashedPassword = PasswordUtil.hashPassword("somePassword");
        assertFalse(PasswordUtil.verifyPassword("", hashedPassword));
    }

    @Test
    void testVerifyPassword_nullHashedPassword_returnsFalse() {
        assertFalse(PasswordUtil.verifyPassword("somePassword", null));
    }

    @Test
    void testVerifyPassword_emptyHashedPassword_returnsFalse() {
        assertFalse(PasswordUtil.verifyPassword("somePassword", ""));
    }

    @Test
    void testVerifyPassword_invalidHashFormat_returnsFalse() {
        String password = "myPassword";
        String invalidHash = "not-a-bcrypt-hash-obviously";
        // This test also implicitly checks the try-catch block in verifyPassword
        assertFalse(PasswordUtil.verifyPassword(password, invalidHash));
    }
}
