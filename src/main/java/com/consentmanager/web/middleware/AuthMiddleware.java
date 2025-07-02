package com.consentmanager.web.middleware;

import com.consentmanager.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Spark;

public class AuthMiddleware {

    private static final Logger logger = LoggerFactory.getLogger(AuthMiddleware.class);

    public static Filter requireAuth = (request, response) -> {
        logger.debug("AuthMiddleware: requireAuth filter triggered for path: {}", request.pathInfo());
        String authHeader = request.headers("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("AuthMiddleware: Missing or malformed Authorization Bearer header.");
            Spark.halt(401, "{\"error\":\"Unauthorized: Missing or malformed token\"}");
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        try {
            if (!JwtUtil.isValidToken(token)) { // This also checks expiration
                logger.warn("AuthMiddleware: Invalid token provided.");
                Spark.halt(401, "{\"error\":\"Unauthorized: Invalid token\"}");
                return;
            }

            Claims claims = JwtUtil.getClaims(token);
            if (claims == null) {
                 logger.warn("AuthMiddleware: Could not extract claims from a token that was previously deemed valid.");
                 Spark.halt(401, "{\"error\":\"Unauthorized: Invalid token claims\"}");
                 return;
            }

            Integer credentialId = claims.get("credentialId", Integer.class);
            String role = claims.get("role", String.class);

            if (credentialId == null || role == null) {
                logger.warn("AuthMiddleware: Token missing credentialId or role.");
                Spark.halt(401, "{\"error\":\"Unauthorized: Token missing required claims\"}");
                return;
            }

            logger.debug("AuthMiddleware: Token valid. CredentialId: {}, Role: {}", credentialId, role);
            request.attribute("credentialId", credentialId);
            request.attribute("role", role);

        } catch (ExpiredJwtException e) {
            logger.warn("AuthMiddleware: Token expired: {}", e.getMessage());
            Spark.halt(401, "{\"error\":\"Unauthorized: Token expired\"}");
        } catch (SignatureException | MalformedJwtException e) {
            logger.warn("AuthMiddleware: Token signature or format issue: {}", e.getMessage());
            Spark.halt(401, "{\"error\":\"Unauthorized: Invalid token signature or format\"}");
        } catch (Exception e) {
            logger.error("AuthMiddleware: Unexpected error during token validation: {}", e.getMessage(), e);
            Spark.halt(500, "{\"error\":\"Internal server error during authentication\"}");
        }
    };

    private static void requireRole(Request request, Response response, String requiredRole) {
        // This method assumes requireAuth has already run and set attributes
        String userRole = request.attribute("role");
        Integer credentialId = request.attribute("credentialId");

        logger.debug("AuthMiddleware: requireRole filter. Required: {}, User's Role: {} for CredentialId: {}",
            requiredRole, userRole, credentialId);

        if (userRole == null) {
            // This should ideally not happen if requireAuth is always applied first.
            // If requireAuth failed, it would have halted. If it passed, role should be set.
            logger.error("AuthMiddleware: Role attribute missing after requireAuth. This indicates a logic error in filter setup.");
            Spark.halt(500, "{\"error\":\"Internal Server Error: Auth context not properly set\"}");
            return;
        }

        if (!userRole.equalsIgnoreCase(requiredRole)) {
            logger.warn("AuthMiddleware: User role '{}' does not match required role '{}' for CredentialId: {}.",
                userRole, requiredRole, credentialId);
            Spark.halt(403, "{\"error\":\"Forbidden: Insufficient permissions\"}");
        }
         logger.debug("AuthMiddleware: Role '{}' authorized for CredentialId: {}.", requiredRole, credentialId);
    }

    public static Filter requireAdmin = (request, response) -> {
        requireRole(request, response, "admin");
    };

    public static Filter requireProvider = (request, response) -> {
        requireRole(request, response, "provider");
    };

    public static Filter requireSeeker = (request, response) -> {
        requireRole(request, response, "seeker");
    };
}
