package com.consentmanager.utils;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auth0.jwt.JWT; // Added import
import java.util.Date;  // Added import


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class JwtUtilTest {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtilTest.class);
    private static final String TEST_SECRET = "test-secret-key-that-is-definitely-long-enough-for-testing-this";

    @BeforeAll
    static void setup() {
        // Attempt to set the environment variable for testing, though this might not always work
        // depending on when JwtUtil class is loaded.
        // A more robust way would be a setter or reinitialization method in JwtUtil for tests.
        System.setProperty("JWT_SECRET_KEY", TEST_SECRET);
        logger.info("Attempted to set JWT_SECRET_KEY system property to: {}", TEST_SECRET);


        // Force re-initialization of the Algorithm in JwtUtil using reflection
        // This is a common workaround for testing static initializers.
        // Commenting out reflection as it's causing issues (NoSuchFieldException: modifiers)
        // and might not be portable. Tests will run with whatever secret JwtUtil initializes with.
        /*
        try {
            Field algorithmField = JwtUtil.class.getDeclaredField("algorithm");
            algorithmField.setAccessible(true);

            // Remove final modifier if present (not typical for non-primitive static fields but good practice)
            // Field modifiersField = Field.class.getDeclaredField("modifiers"); // This line is problematic
            // modifiersField.setAccessible(true);
            // modifiersField.setInt(algorithmField, algorithmField.getModifiers() & ~Modifier.FINAL);

            Algorithm testAlgorithm = Algorithm.HMAC256(TEST_SECRET);
            algorithmField.set(null, testAlgorithm); // Set static field
            logger.info("Successfully re-initialized JwtUtil.algorithm with test secret via reflection.");

        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("Failed to re-initialize JwtUtil.algorithm via reflection. Tests might use default secret.", e);
        }
        */
         // To ensure tests are predictable with the secret, we might need a static setter in JwtUtil
         // For now, let's log what secret is being used by JwtUtil if possible (cannot directly access private static field here)
         logger.info("JwtUtilTest will run with the secret key JwtUtil has initialized with (either from ENV or fallback).");
    }


    @Test
    void testGenerateToken_success() {
        Integer credentialId = 1;
        String role = "admin";
        String token = JwtUtil.generateToken(credentialId, role);

        assertNotNull(token, "Token should not be null");
        assertFalse(token.isEmpty(), "Token should not be empty");
        logger.debug("Generated token for test: {}", token);
    }

    @Test
    void testGenerateToken_nullCredentialId_returnsNull() {
        String token = JwtUtil.generateToken(null, "admin");
        assertNull(token, "Token should be null for null credentialId");
    }

    @Test
    void testGenerateToken_nullRole_returnsNull() {
        String token = JwtUtil.generateToken(1, null);
        assertNull(token, "Token should be null for null role");
    }

    @Test
    void testGenerateToken_emptyRole_returnsNull() {
        String token = JwtUtil.generateToken(1, "");
        assertNull(token, "Token should be null for empty role");
    }

    @Test
    void testVerifyToken_validToken_returnsDecodedJWT() {
        Integer credentialId = 10;
        String role = "provider";
        String token = JwtUtil.generateToken(credentialId, role);
        assertNotNull(token);

        Optional<DecodedJWT> decodedOpt = JwtUtil.verifyToken(token);
        assertTrue(decodedOpt.isPresent(), "DecodedJWT should be present for a valid token");

        DecodedJWT decodedJWT = decodedOpt.get();
        assertEquals(String.valueOf(credentialId), decodedJWT.getSubject());
        assertEquals(credentialId, JwtUtil.getCredentialId(decodedJWT));
        assertEquals(role, JwtUtil.getRole(decodedJWT));
        assertEquals("com.consentmanager", decodedJWT.getIssuer());
    }

    @Test
    void testVerifyToken_invalidTokenSignature_returnsEmpty() {
        // Token generated with a different secret or tampered
        String otherSecret = "another-different-secret-key-that-is-also-long-enough";
        Algorithm otherAlgorithm = Algorithm.HMAC256(otherSecret);
        String tokenFromOtherSecret = JWT.create()
                .withIssuer("com.consentmanager")
                .withSubject("1")
                .withClaim("credentialId", 1)
                .withClaim("role", "user")
                .sign(otherAlgorithm);

        Optional<DecodedJWT> decodedOpt = JwtUtil.verifyToken(tokenFromOtherSecret);
        assertFalse(decodedOpt.isPresent(), "Verification should fail for token signed with a different secret");
    }

    @Test
    void testVerifyToken_expiredToken_returnsEmpty() throws InterruptedException {
        // Temporarily change expiration time for this test if possible, or generate an already expired one
        // For simplicity, let's generate a token that was valid very briefly.
        // This requires a more complex setup or a helper in JwtUtil to generate tokens with custom expiry.
        // As a proxy: if JwtUtil had a method to create token with custom expiry for testing:
        // String expiredToken = JwtUtil.generateTokenWithCustomExpiry(1, "user", -1000); // -1 second expiry
        // For now, this test is hard to do perfectly without modifying JwtUtil for testability.
        // We'll assume the library handles expiry correctly.
        // A manual way:
        Algorithm shortLivedAlgorithm;
        try {
             Field algorithmField = JwtUtil.class.getDeclaredField("algorithm");
             algorithmField.setAccessible(true);
             shortLivedAlgorithm = (Algorithm) algorithmField.get(null);
        } catch(Exception e) {
            shortLivedAlgorithm = Algorithm.HMAC256(TEST_SECRET); // fallback
        }

        String token = JWT.create()
                .withIssuer("com.consentmanager")
                .withSubject("123")
                .withClaim("credentialId", 123)
                .withClaim("role", "test")
                .withExpiresAt(new Date(System.currentTimeMillis() - 10000)) // Expires 10 seconds ago
                .sign(shortLivedAlgorithm);


        Thread.sleep(100); // Ensure time has passed

        Optional<DecodedJWT> decodedOpt = JwtUtil.verifyToken(token);
        assertFalse(decodedOpt.isPresent(), "Verification should fail for an expired token");
    }


    @Test
    void testVerifyToken_nullToken_returnsEmpty() {
        Optional<DecodedJWT> decodedOpt = JwtUtil.verifyToken(null);
        assertFalse(decodedOpt.isPresent(), "Verification should fail for null token");
    }

    @Test
    void testVerifyToken_emptyToken_returnsEmpty() {
        Optional<DecodedJWT> decodedOpt = JwtUtil.verifyToken("");
        assertFalse(decodedOpt.isPresent(), "Verification should fail for empty token");
    }

    @Test
    void testVerifyToken_malformedToken_returnsEmpty() {
        String malformedToken = "this.is.not.a.jwt";
        Optional<DecodedJWT> decodedOpt = JwtUtil.verifyToken(malformedToken);
        assertFalse(decodedOpt.isPresent(), "Verification should fail for malformed token");
    }

    @Test
    void testGetCredentialId_fromDecodedJWT() {
        String token = JwtUtil.generateToken(77, "seeker");
        Optional<DecodedJWT> decodedOpt = JwtUtil.verifyToken(token);
        assertTrue(decodedOpt.isPresent());
        assertEquals(77, JwtUtil.getCredentialId(decodedOpt.get()));
    }

    @Test
    void testGetRole_fromDecodedJWT() {
        String token = JwtUtil.generateToken(88, "provider");
        Optional<DecodedJWT> decodedOpt = JwtUtil.verifyToken(token);
        assertTrue(decodedOpt.isPresent());
        assertEquals("provider", JwtUtil.getRole(decodedOpt.get()));
    }

    @Test
    void testGetCredentialId_nullDecodedJWT_returnsNull() {
        assertNull(JwtUtil.getCredentialId(null));
    }

    @Test
    void testGetRole_nullDecodedJWT_returnsNull() {
        assertNull(JwtUtil.getRole(null));
    }
}
