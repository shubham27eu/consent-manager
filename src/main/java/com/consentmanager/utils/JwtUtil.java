package com.consentmanager.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Optional;

public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    // IMPORTANT: This secret should be externalized and not hardcoded in a real application!
    // For example, load from environment variables or a configuration file.
    private static final String JWT_SECRET = System.getenv("JWT_SECRET_KEY") != null ? System.getenv("JWT_SECRET_KEY") : "your-fallback-very-strong-secret-key-for-dev-must-be-at-least-32-bytes";
    private static final String ISSUER = "com.consentmanager";
    private static final long EXPIRATION_TIME_MS = 24 * 60 * 60 * 1000; // 24 hours

    private static Algorithm algorithm;

    static {
        if (JWT_SECRET == null || JWT_SECRET.length() < 32) {
             logger.warn("JWT_SECRET is null or too short. Using a default insecure key. THIS IS NOT SAFE FOR PRODUCTION.");
             algorithm = Algorithm.HMAC256("default_insecure_secret_key_replace_this_immediately_32_chars_long");
        } else {
            algorithm = Algorithm.HMAC256(JWT_SECRET);
        }
    }


    /**
     * Generates a JWT for a given credential ID and role.
     *
     * @param credentialId The ID of the credential.
     * @param role The role of the user.
     * @return The generated JWT string, or null if creation fails.
     */
    public static String generateToken(Integer credentialId, String role) {
        if (credentialId == null || role == null || role.isEmpty()) {
            logger.warn("Attempted to generate JWT with null or empty credentialId/role.");
            return null;
        }
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME_MS);

            String token = JWT.create()
                    .withIssuer(ISSUER)
                    .withSubject(String.valueOf(credentialId)) // Subject is typically user identifier
                    .withClaim("credentialId", credentialId)
                    .withClaim("role", role)
                    .withIssuedAt(now)
                    .withExpiresAt(expiryDate)
                    .sign(algorithm);
            logger.debug("JWT generated successfully for credential ID: {}", credentialId);
            return token;
        } catch (JWTCreationException exception) {
            logger.error("Error creating JWT for credential ID " + credentialId, exception);
            return null;
        }
    }

    /**
     * Verifies a JWT and returns the decoded token.
     *
     * @param token The JWT string to verify.
     * @return An Optional containing the DecodedJWT if valid, otherwise empty.
     */
    public static Optional<DecodedJWT> verifyToken(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("Attempted to verify a null or empty JWT.");
            return Optional.empty();
        }
        try {
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build();
            DecodedJWT decodedJWT = verifier.verify(token);
            logger.debug("JWT verified successfully for subject: {}", decodedJWT.getSubject());
            return Optional.of(decodedJWT);
        } catch (JWTVerificationException exception) {
            logger.warn("JWT verification failed: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Extracts the credential ID from a decoded JWT.
     *
     * @param decodedJWT The decoded JWT.
     * @return The credential ID, or null if not present.
     */
    public static Integer getCredentialId(DecodedJWT decodedJWT) {
        if (decodedJWT == null) return null;
        return decodedJWT.getClaim("credentialId").asInt();
    }

    /**
     * Extracts the role from a decoded JWT.
     *
     * @param decodedJWT The decoded JWT.
     * @return The role string, or null if not present.
     */
    public static String getRole(DecodedJWT decodedJWT) {
        if (decodedJWT == null) return null;
        return decodedJWT.getClaim("role").asString();
    }

    // Optional: Main method for testing
    public static void main(String[] args) {
        System.setProperty("JWT_SECRET_KEY", "test-secret-key-that-is-long-enough-for-testing-purpose");
        // Re-initialize algorithm if main is run directly after changing env var
        if (System.getenv("JWT_SECRET_KEY") != null && System.getenv("JWT_SECRET_KEY").length() >=32) {
             algorithm = Algorithm.HMAC256(System.getenv("JWT_SECRET_KEY"));
        } else {
             algorithm = Algorithm.HMAC256(JWT_SECRET); // fallback to potentially insecure one if env not set
        }


        Integer testCredentialId = 123;
        String testRole = "provider";

        String token = generateToken(testCredentialId, testRole);
        System.out.println("Generated Token: " + token);

        if (token != null) {
            Optional<DecodedJWT> decodedOpt = verifyToken(token);
            if (decodedOpt.isPresent()) {
                DecodedJWT decoded = decodedOpt.get();
                System.out.println("Token Verified Successfully!");
                System.out.println("Subject: " + decoded.getSubject());
                System.out.println("Credential ID Claim: " + getCredentialId(decoded));
                System.out.println("Role Claim: " + getRole(decoded));
                System.out.println("Issuer: " + decoded.getIssuer());
                System.out.println("Expires At: " + decoded.getExpiresAt());
            } else {
                System.out.println("Token Verification Failed.");
            }
        }

        // Test with an invalid token
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"; // Example invalid token
        Optional<DecodedJWT> decodedInvalidOpt = verifyToken(invalidToken);
        if (decodedInvalidOpt.isEmpty()) {
            System.out.println("Verification of invalid token correctly failed.");
        } else {
            System.out.println("Verification of invalid token unexpectedly passed.");
        }
    }
}
