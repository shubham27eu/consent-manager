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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;


public class AuthMiddleware {

    private static final Logger logger = LoggerFactory.getLogger(AuthMiddleware.class);
    private static final ObjectMapper objectMapper = new ObjectMapper(); // For JSON error responses

    private static void haltWithJsonError(Response response, int statusCode, String message) {
        response.type("application/json");
        try {
            Spark.halt(statusCode, objectMapper.writeValueAsString(Collections.singletonMap("error", message)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            logger.error("Critical error: Failed to serialize error message for halt in AuthMiddleware", e);
            // Fallback to plain text if JSON serialization fails
            Spark.halt(500, "Internal Server Error (serialization failed)");
        }
    }

    public static Filter requireAuth = (request, response) -> {
        logger.debug("AuthMiddleware: requireAuth filter triggered for path: {}", request.pathInfo());
        String authHeader = request.headers("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("AuthMiddleware: Missing or malformed Authorization Bearer header.");
            haltWithJsonError(response, 401, "Unauthorized: Missing or malformed token");
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        try {
            if (!JwtUtil.isValidToken(token)) { // This also checks expiration
                logger.warn("AuthMiddleware: Invalid token provided.");
                haltWithJsonError(response, 401, "Unauthorized: Invalid token");
                return;
            }

            Claims claims = JwtUtil.getClaims(token);
            if (claims == null) {
                 logger.warn("AuthMiddleware: Could not extract claims from a token that was previously deemed valid.");
                 haltWithJsonError(response, 401, "Unauthorized: Invalid token claims");
                 return;
            }

            Integer credentialId = claims.get("credentialId", Integer.class);
            String role = claims.get("role", String.class);

            if (credentialId == null || role == null) {
                logger.warn("AuthMiddleware: Token missing credentialId or role.");
                haltWithJsonError(response, 401, "Unauthorized: Token missing required claims");
                return;
            }

            logger.debug("AuthMiddleware: Token valid. CredentialId: {}, Role: {}", credentialId, role);
            request.attribute("credentialId", credentialId);
            request.attribute("role", role);

        } catch (ExpiredJwtException e) {
            logger.warn("AuthMiddleware: Token expired: {}", e.getMessage());
            haltWithJsonError(response, 401, "Unauthorized: Token expired");
        } catch (SignatureException | MalformedJwtException e) {
            logger.warn("AuthMiddleware: Token signature or format issue: {}", e.getMessage());
            haltWithJsonError(response, 401, "Unauthorized: Invalid token signature or format");
        } catch (Exception e) {
            logger.error("AuthMiddleware: Unexpected error during token validation: {}", e.getMessage(), e);
            haltWithJsonError(response, 500, "Internal server error during authentication");
        }
    };

    private static void requireRole(Request request, Response response, String requiredRole) {
        String userRole = request.attribute("role");
        Integer credentialId = request.attribute("credentialId");

        logger.debug("AuthMiddleware: requireRole filter. Required: {}, User's Role: {} for CredentialId: {}",
            requiredRole, userRole, credentialId);

        if (userRole == null) {
            logger.error("AuthMiddleware: Role attribute missing after requireAuth. This indicates a logic error in filter setup.");
            haltWithJsonError(response, 500, "Internal Server Error: Auth context not properly set");
            return;
        }

        if (!userRole.equalsIgnoreCase(requiredRole)) {
            logger.warn("AuthMiddleware: User role '{}' does not match required role '{}' for CredentialId: {}.",
                userRole, requiredRole, credentialId);
            haltWithJsonError(response, 403, "Forbidden: Insufficient permissions");
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
