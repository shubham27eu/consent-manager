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
import java.util.regex.Pattern;

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
                return haltWithError(res, 400, "Role is required for signup.");
            }
            role = role.toLowerCase();

            AuthService.SignupResponse<?> signupResponse;

            switch (role) {
                case "provider":
                    ProviderBacklog providerData = objectMapper.readValue(requestBody, ProviderBacklog.class);
                    validateProviderBacklog(providerData, res); // Throws HaltException on failure
                    signupResponse = authService.signupProvider(providerData);
                    break;
                case "seeker":
                    SeekerBacklog seekerData = objectMapper.readValue(requestBody, SeekerBacklog.class);
                    validateSeekerBacklog(seekerData, res); // Throws HaltException on failure
                    signupResponse = authService.signupSeeker(seekerData);
                    break;
                case "admin":
                    AdminSignupRequest adminSignupReq = objectMapper.readValue(requestBody, AdminSignupRequest.class);
                    validateAdminSignupRequest(adminSignupReq, res); // Throws HaltException on failure
                    signupResponse = authService.signupAdmin(adminSignupReq.adminDetails, adminSignupReq.username, adminSignupReq.password);
                    break;
                default:
                    return haltWithError(res, 400, "Invalid role specified.");
            }

            if (signupResponse.success) {
                res.status(201); // Created
            } else {
                res.status(signupResponse.message.contains("already exists") ? 409 : 400);
            }
            return signupResponse;

        } catch (spark.Spark.HaltException he) {
            throw he; // Re-throw halt exceptions to let Spark handle them
        }
        catch (Exception e) {
            logger.error("Error processing signup request", e);
            return haltWithError(res, 500, "Internal server error during signup.");
        }
    }

    private Object handleLogin(Request req, Response res) {
        res.type("application/json");
        String requestBody = req.body();
        logger.debug("Received login request: {}", requestBody);

        try {
            LoginRequest loginRequest = objectMapper.readValue(requestBody, LoginRequest.class);

            // Basic validation for login
            if (loginRequest.username == null || loginRequest.username.trim().isEmpty()) {
                return haltWithError(res, 400, "Username is required.");
            }
            if (loginRequest.password == null || loginRequest.password.isEmpty()) {
                return haltWithError(res, 400, "Password is required.");
            }
            if (loginRequest.role == null || loginRequest.role.trim().isEmpty()) {
                return haltWithError(res, 400, "Role is required.");
            }
            String role = loginRequest.role.toLowerCase();
            if (!List.of("provider", "seeker", "admin").contains(role)) {
                return haltWithError(res, 400, "Invalid role specified.");
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
            return haltWithError(res, 500, "Internal server error during login.");
        }
    }

    // --- Validation Helper Methods ---
    private void validateProviderBacklog(ProviderBacklog data, Response res) throws com.fasterxml.jackson.core.JsonProcessingException {
        if (isNullOrEmpty(data.getUsername()) || data.getUsername().length() < 3) haltWithError(res, 400, "Username must be at least 3 characters.");
        if (isNullOrEmpty(data.getPassword()) || data.getPassword().length() < 6) haltWithError(res, 400, "Password must be at least 6 characters.");
        if (isNullOrEmpty(data.getEmail()) || !isValidEmail(data.getEmail())) haltWithError(res, 400, "Valid email is required.");
        if (isNullOrEmpty(data.getFirstName())) haltWithError(res, 400, "First name is required.");
        if (isNullOrEmpty(data.getLastName())) haltWithError(res, 400, "Last name is required.");
        if (isNullOrEmpty(data.getMobileNo())) haltWithError(res, 400, "Mobile number is required.");
        if (data.getDateOfBirth() == null) haltWithError(res, 400, "Date of birth is required.");
        if (data.getAge() == null || data.getAge() <= 0) haltWithError(res, 400, "Valid age is required.");
        if (isNullOrEmpty(data.getGender())) haltWithError(res, 400, "Gender is required.");
        if (isNullOrEmpty(data.getPublicKey())) haltWithError(res, 400, "Public key is required.");
    }

    private void validateSeekerBacklog(SeekerBacklog data, Response res) throws com.fasterxml.jackson.core.JsonProcessingException {
        if (isNullOrEmpty(data.getUsername()) || data.getUsername().length() < 3) haltWithError(res, 400, "Username must be at least 3 characters.");
        if (isNullOrEmpty(data.getPassword()) || data.getPassword().length() < 6) haltWithError(res, 400, "Password must be at least 6 characters.");
        if (isNullOrEmpty(data.getEmail()) || !isValidEmail(data.getEmail())) haltWithError(res, 400, "Valid email is required.");
        if (isNullOrEmpty(data.getName())) haltWithError(res, 400, "Name is required.");
        if (isNullOrEmpty(data.getType())) haltWithError(res, 400, "Type is required.");
        if (isNullOrEmpty(data.getRegistrationNo())) haltWithError(res, 400, "Registration number is required.");
        if (isNullOrEmpty(data.getContactNo())) haltWithError(res, 400, "Contact number is required.");
        if (isNullOrEmpty(data.getAddress())) haltWithError(res, 400, "Address is required.");
        if (isNullOrEmpty(data.getPublicKey())) haltWithError(res, 400, "Public key is required.");
    }

    private void validateAdminSignupRequest(AdminSignupRequest data, Response res) throws com.fasterxml.jackson.core.JsonProcessingException {
        if (data.adminDetails == null) haltWithError(res, 400, "adminDetails are required.");
        if (isNullOrEmpty(data.username) || data.username.length() < 3) haltWithError(res, 400, "Username must be at least 3 characters.");
        if (isNullOrEmpty(data.password) || data.password.length() < 6) haltWithError(res, 400, "Password must be at least 6 characters.");
        if (isNullOrEmpty(data.adminDetails.getEmail()) || !isValidEmail(data.adminDetails.getEmail())) haltWithError(res, 400, "Valid email is required for admin.");
        if (isNullOrEmpty(data.adminDetails.getFirstName())) haltWithError(res, 400, "First name is required for admin.");
        if (isNullOrEmpty(data.adminDetails.getLastName())) haltWithError(res, 400, "Last name is required for admin.");
    }

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$");
    private boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    // Helper to standardize error responses and halt
    private String haltWithError(Response res, int statusCode, String message) {
        logger.warn("Validation/Request error in AuthController: {} - {}", statusCode, message);
        res.status(statusCode);
        res.type("application/json"); // Ensure content type for error
        try {
            halt(statusCode, objectMapper.writeValueAsString(java.util.Collections.singletonMap("error", message)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            logger.error("Error serializing error message for halt", e);
            // Fallback halt if JSON serialization itself fails
            halt(500, "{\"error\":\"Internal Server Error and failed to serialize error message.\"}");
        }
        return null; // Unreachable due to halt
    }
}
