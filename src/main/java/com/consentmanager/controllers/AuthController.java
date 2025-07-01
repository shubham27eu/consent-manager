package com.consentmanager.controllers;

import com.consentmanager.models.*;
import com.consentmanager.services.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import static spark.Spark.halt;
import static spark.Spark.post;

public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    // Inner DTO for Login Request
    private static class LoginRequest {
        public String username;
        public String password;
        public String role;
    }

    // Inner DTO for Admin Signup Request (since Admin model doesn't include username/password directly for credential)
    private static class AdminSignupRequest {
        public Admin adminDetails; // This will contain firstName, lastName, email etc.
        public String username;
        public String password;
    }


    public AuthController(AuthService authService) {
        this.authService = authService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); // For LocalDate/LocalDateTime

        setupRoutes();
    }

    private void setupRoutes() {
        post("/api/auth/signup", this::handleSignup, objectMapper::writeValueAsString);
        post("/api/auth/login", this::handleLogin, objectMapper::writeValueAsString);
    }

    private Object handleSignup(Request req, Response res) {
        res.type("application/json");
        String requestBody = req.body();
        logger.debug("Received signup request: {}", requestBody);

        try {
            // Determine role from a common field or a specific endpoint if preferred.
            // For a generic /signup, we might need a 'role' field in the JSON.
            // Let's assume the JSON includes a 'role' field to distinguish.

            // A simple way to get role before parsing to specific type
            String role = objectMapper.readTree(requestBody).get("role").asText();

            if (role == null || role.trim().isEmpty()) {
                logger.warn("Role not provided in signup request.");
                halt(400, objectMapper.writeValueAsString(new AuthService.SignupResponse<>(null, "Role is required for signup.", false)));
            }

            AuthService.SignupResponse<?> signupResponse;

            switch (role.toLowerCase()) {
                case "provider":
                    ProviderBacklog providerData = objectMapper.readValue(requestBody, ProviderBacklog.class);
                    signupResponse = authService.signupProvider(providerData);
                    break;
                case "seeker":
                    SeekerBacklog seekerData = objectMapper.readValue(requestBody, SeekerBacklog.class);
                    signupResponse = authService.signupSeeker(seekerData);
                    break;
                case "admin":
                    // For admin, we expect a nested structure or a specific DTO
                    AdminSignupRequest adminSignupReq = objectMapper.readValue(requestBody, AdminSignupRequest.class);
                     if (adminSignupReq.adminDetails == null) {
                        logger.warn("adminDetails missing in admin signup request.");
                        halt(400, objectMapper.writeValueAsString(new AuthService.SignupResponse<>(null, "adminDetails are required for admin signup.", false)));
                    }
                    signupResponse = authService.signupAdmin(adminSignupReq.adminDetails, adminSignupReq.username, adminSignupReq.password);
                    break;
                default:
                    logger.warn("Invalid role specified: {}", role);
                    halt(400, objectMapper.writeValueAsString(new AuthService.SignupResponse<>(null, "Invalid role specified.", false)));
                    return null; // Unreachable due to halt
            }

            if (signupResponse.success) {
                res.status(201); // Created
            } else {
                // Determine appropriate status code based on message if needed, e.g., 409 for conflict
                if (signupResponse.message.contains("already exists")) {
                    res.status(409); // Conflict
                } else {
                    res.status(400); // Bad Request
                }
            }
            return signupResponse;

        } catch (Exception e) {
            logger.error("Error processing signup request", e);
            try {
                halt(500, objectMapper.writeValueAsString(new AuthService.SignupResponse<>(null, "Internal server error during signup.", false)));
            } catch (Exception ex) {
                logger.error("Error serializing error response for signup", ex);
                halt(500, "{\"success\":false, \"message\":\"Internal server error and failed to serialize error.\"}");
            }
        }
        return null; // Unreachable
    }

    private Object handleLogin(Request req, Response res) {
        res.type("application/json");
        String requestBody = req.body();
        logger.debug("Received login request: {}", requestBody);

        try {
            LoginRequest loginRequest = objectMapper.readValue(requestBody, LoginRequest.class);
            if (loginRequest.username == null || loginRequest.password == null || loginRequest.role == null) {
                 logger.warn("Login request missing username, password, or role.");
                halt(400, objectMapper.writeValueAsString(new AuthService.LoginResponse(null, null, null, "Username, password, and role are required.", false, null)));
            }

            AuthService.LoginResponse loginResponse = authService.login(
                    loginRequest.username,
                    loginRequest.password,
                    loginRequest.role
            );

            if (loginResponse.success) {
                res.status(200); // OK
            } else {
                // Check for specific statuses to set HTTP response code
                if ("pending".equals(loginResponse.status) || "rejected".equals(loginResponse.status)) {
                    res.status(403); // Forbidden, but with a specific status message
                } else if ("Account is inactive.".equals(loginResponse.message)) {
                     res.status(403); // Forbidden
                }
                else {
                    res.status(401); // Unauthorized for bad credentials
                }
            }
            return loginResponse;

        } catch (Exception e) {
            logger.error("Error processing login request", e);
             try {
                halt(500, objectMapper.writeValueAsString(new AuthService.LoginResponse(null, null, null, "Internal server error during login.", false, null)));
            } catch (Exception ex) {
                logger.error("Error serializing error response for login", ex);
                halt(500, "{\"success\":false, \"message\":\"Internal server error and failed to serialize error.\"}");
            }
        }
        return null; // Unreachable
    }
}
