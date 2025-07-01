package com.consentmanager.services;

import com.consentmanager.daos.*;
import com.consentmanager.models.*;
import com.consentmanager.utils.JwtUtil;
import com.consentmanager.utils.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;

public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final CredentialDAO credentialDAO;
    private final AdminDAO adminDAO;
    private final ProviderDAO providerDAO;
    private final SeekerDAO seekerDAO;
    private final ProviderBacklogDAO providerBacklogDAO;
    private final SeekerBacklogDAO seekerBacklogDAO;

    public AuthService(CredentialDAO credentialDAO, AdminDAO adminDAO, ProviderDAO providerDAO, SeekerDAO seekerDAO,
                       ProviderBacklogDAO providerBacklogDAO, SeekerBacklogDAO seekerBacklogDAO) {
        this.credentialDAO = credentialDAO;
        this.adminDAO = adminDAO;
        this.providerDAO = providerDAO;
        this.seekerDAO = seekerDAO;
        this.providerBacklogDAO = providerBacklogDAO;
        this.seekerBacklogDAO = seekerBacklogDAO;
    }

    // --- Signup Methods ---

    /**
     * Handles signup for a new provider.
     * Puts the provider into a backlog for admin approval.
     *
     * @param providerBacklogData Data for the provider to be signed up.
     * @return An Optional containing the created ProviderBacklog if successful, otherwise empty.
     *         Includes specific error messages for known issues (e.g., username/email exists).
     * @throws IllegalArgumentException if input data is invalid (e.g., null objects).
     */
    public SignupResponse<ProviderBacklog> signupProvider(ProviderBacklog providerBacklogData) {
        logger.info("Attempting to signup provider with username: {}", providerBacklogData.getUsername());
        if (providerBacklogData == null) {
            throw new IllegalArgumentException("ProviderBacklog data cannot be null.");
        }

        // Basic validation (more can be added based on model annotations or business rules)
        if (providerBacklogData.getUsername() == null || providerBacklogData.getUsername().trim().isEmpty() ||
            providerBacklogData.getPassword() == null || providerBacklogData.getPassword().trim().isEmpty() ||
            providerBacklogData.getEmail() == null || providerBacklogData.getEmail().trim().isEmpty()) {
            return new SignupResponse<>(null, "Username, password, and email are required.", false);
        }


        // Check if username already exists in Credential or ProviderBacklog
        if (credentialDAO.findByUsername(providerBacklogData.getUsername()).isPresent() ||
            providerBacklogDAO.findByUsername(providerBacklogData.getUsername()).isPresent()) {
            logger.warn("Username {} already exists.", providerBacklogData.getUsername());
            return new SignupResponse<>(null, "Username already exists.", false);
        }

        // Check if email already exists in Provider or ProviderBacklog
        if (providerDAO.findByEmail(providerBacklogData.getEmail()).isPresent() ||
            providerBacklogDAO.findByEmail(providerBacklogData.getEmail()).isPresent()) {
            logger.warn("Email {} already exists for a provider.", providerBacklogData.getEmail());
            return new SignupResponse<>(null, "Email already exists.", false);
        }

        try {
            // Hash the password
            String hashedPassword = PasswordUtil.hashPassword(providerBacklogData.getPassword());
            providerBacklogData.setPassword(hashedPassword);
            providerBacklogData.setRole("provider"); // Ensure role is set
            providerBacklogData.setStatus("pending"); // Default status
            providerBacklogData.setCreatedAt(LocalDateTime.now()); // Set creation time

            boolean saved = providerBacklogDAO.saveProviderBacklog(providerBacklogData);
            if (saved) {
                logger.info("Provider {} signed up successfully and added to backlog with ID: {}", providerBacklogData.getUsername(), providerBacklogData.getBacklogId());
                return new SignupResponse<>(providerBacklogData, "Provider signup successful. Pending admin approval.", true);
            } else {
                logger.error("Failed to save provider backlog for username: {}", providerBacklogData.getUsername());
                return new SignupResponse<>(null, "Failed to process provider signup. Please try again.", false);
            }
        } catch (Exception e) {
            logger.error("Exception during provider signup for username: " + providerBacklogData.getUsername(), e);
            return new SignupResponse<>(null, "An unexpected error occurred during provider signup.", false);
        }
    }

    public SignupResponse<SeekerBacklog> signupSeeker(SeekerBacklog seekerBacklogData) {
        logger.info("Attempting to signup seeker with username: {}", seekerBacklogData.getUsername());
         if (seekerBacklogData == null) {
            throw new IllegalArgumentException("SeekerBacklog data cannot be null.");
        }

        if (seekerBacklogData.getUsername() == null || seekerBacklogData.getUsername().trim().isEmpty() ||
            seekerBacklogData.getPassword() == null || seekerBacklogData.getPassword().trim().isEmpty() ||
            seekerBacklogData.getEmail() == null || seekerBacklogData.getEmail().trim().isEmpty()) {
            return new SignupResponse<>(null, "Username, password, and email are required.", false);
        }

        if (credentialDAO.findByUsername(seekerBacklogData.getUsername()).isPresent() ||
            seekerBacklogDAO.findByUsername(seekerBacklogData.getUsername()).isPresent()) {
            logger.warn("Username {} already exists.", seekerBacklogData.getUsername());
            return new SignupResponse<>(null, "Username already exists.", false);
        }

        if (seekerDAO.findByEmail(seekerBacklogData.getEmail()).isPresent() ||
            seekerBacklogDAO.findByEmail(seekerBacklogData.getEmail()).isPresent()) {
            logger.warn("Email {} already exists for a seeker.", seekerBacklogData.getEmail());
            return new SignupResponse<>(null, "Email already exists.", false);
        }

        try {
            String hashedPassword = PasswordUtil.hashPassword(seekerBacklogData.getPassword());
            seekerBacklogData.setPassword(hashedPassword);
            seekerBacklogData.setRole("seeker");
            seekerBacklogData.setStatus("pending");
            seekerBacklogData.setCreatedAt(LocalDateTime.now());

            boolean saved = seekerBacklogDAO.saveSeekerBacklog(seekerBacklogData);
            if (saved) {
                logger.info("Seeker {} signed up successfully and added to backlog with ID: {}", seekerBacklogData.getUsername(), seekerBacklogData.getBacklogId());
                return new SignupResponse<>(seekerBacklogData, "Seeker signup successful. Pending admin approval.", true);
            } else {
                logger.error("Failed to save seeker backlog for username: {}", seekerBacklogData.getUsername());
                return new SignupResponse<>(null, "Failed to process seeker signup. Please try again.", false);
            }
        } catch (Exception e) {
            logger.error("Exception during seeker signup for username: " + seekerBacklogData.getUsername(), e);
            return new SignupResponse<>(null, "An unexpected error occurred during seeker signup.", false);
        }
    }

    public SignupResponse<Admin> signupAdmin(Admin adminData, String username, String rawPassword) {
        logger.info("Attempting to signup admin with username: {}", username);
        if (adminData == null || username == null || username.trim().isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
             return new SignupResponse<>(null, "Admin data, username, and password are required.", false);
        }
        adminData.setEmail(adminData.getEmail().toLowerCase().trim()); // Normalize email


        if (credentialDAO.findByUsername(username).isPresent()) {
            logger.warn("Username {} already exists for admin signup.", username);
            return new SignupResponse<>(null, "Username already exists.", false);
        }
        if (adminDAO.findByEmail(adminData.getEmail()).isPresent()) {
            logger.warn("Email {} already exists for an admin.", adminData.getEmail());
            return new SignupResponse<>(null, "Email already exists.", false);
        }


        try {
            String hashedPassword = PasswordUtil.hashPassword(rawPassword);
            Credential credential = new Credential(username, hashedPassword, "admin");
            int credentialId = credentialDAO.saveCredential(credential);

            if (credentialId != -1) {
                adminData.setCredentialId(credentialId);
                boolean adminSaved = adminDAO.saveAdmin(adminData);
                if (adminSaved) {
                    logger.info("Admin {} signed up successfully with Credential ID: {} and Admin ID: {}", username, credentialId, adminData.getAdminId());
                    // adminData now contains the generated adminId
                    return new SignupResponse<>(adminData, "Admin signup successful.", true);
                } else {
                    // Rollback attempt or log inconsistency - credential created but admin profile failed
                    logger.error("Failed to save admin profile for {} after creating credential. Manual cleanup might be needed for credential ID: {}", username, credentialId);
                    // TODO: Consider a rollback mechanism for the credential if admin save fails.
                    return new SignupResponse<>(null, "Failed to create admin profile.", false);
                }
            } else {
                logger.error("Failed to save credential for admin username: {}", username);
                 return new SignupResponse<>(null, "Failed to create admin credential.", false);
            }
        } catch (Exception e) {
            logger.error("Exception during admin signup for username: " + username, e);
            return new SignupResponse<>(null, "An unexpected error occurred during admin signup.", false);
        }
    }

    // --- Login Method ---
    public LoginResponse login(String username, String password, String role) {
        logger.info("Attempting login for username: {}, role: {}", username, role);

        if (username == null || username.trim().isEmpty() ||
            password == null || password.isEmpty() ||
            role == null || role.trim().isEmpty()) {
            return new LoginResponse(null, null, null, "Username, password, and role are required.", false, null);
        }

        // Admins are checked directly against Credential and Admin table
        if ("admin".equalsIgnoreCase(role)) {
            Optional<Credential> credOpt = credentialDAO.findByUsername(username);
            if (credOpt.isPresent()) {
                Credential cred = credOpt.get();
                if (cred.getRole().equals("admin") && PasswordUtil.verifyPassword(password, cred.getPassword())) {
                    Optional<Admin> adminOpt = adminDAO.findByCredentialId(cred.getCredentialId());
                    if (adminOpt.isPresent()) {
                        String token = JwtUtil.generateToken(cred.getCredentialId(), cred.getRole());
                        logger.info("Admin {} logged in successfully.", username);
                        return new LoginResponse(token, cred.getCredentialId(), cred.getRole(), "Admin login successful.", true, null);
                    } else {
                         logger.warn("Admin profile not found for credential ID: {} (username: {})", cred.getCredentialId(), username);
                        return new LoginResponse(null, null, null, "Admin profile not found.", false, null);
                    }
                }
            }
        } else if ("provider".equalsIgnoreCase(role)) {
            // Check active providers first
            Optional<Credential> credOpt = credentialDAO.findByUsername(username);
            if (credOpt.isPresent()) {
                Credential cred = credOpt.get();
                if (cred.getRole().equals("provider") && PasswordUtil.verifyPassword(password, cred.getPassword())) {
                    Optional<Provider> providerOpt = providerDAO.findByCredentialId(cred.getCredentialId());
                    if (providerOpt.isPresent()) {
                        if (!providerOpt.get().getIsActive()) {
                            logger.warn("Provider account {} is inactive.", username);
                            return new LoginResponse(null, null, null, "Account is inactive.", false, "inactive");
                        }
                        String token = JwtUtil.generateToken(cred.getCredentialId(), cred.getRole());
                        logger.info("Provider {} logged in successfully.", username);
                        return new LoginResponse(token, cred.getCredentialId(), cred.getRole(), "Provider login successful.", true, null);
                    } else {
                        logger.warn("Provider profile not found for credential ID: {} (username: {})", cred.getCredentialId(), username);
                         // This case should ideally not happen if data is consistent
                        return new LoginResponse(null, null, null, "Provider profile not found.", false, null);
                    }
                }
            } else { // Check provider backlog if not in credentials
                Optional<ProviderBacklog> backlogOpt = providerBacklogDAO.findByUsername(username);
                if (backlogOpt.isPresent()) {
                    ProviderBacklog backlog = backlogOpt.get();
                    if (PasswordUtil.verifyPassword(password, backlog.getPassword())) {
                        logger.info("Provider {} found in backlog with status: {}", username, backlog.getStatus());
                        return new LoginResponse(null, null, null, "Login attempt for backlogged provider.", false, backlog.getStatus());
                    }
                }
            }
        } else if ("seeker".equalsIgnoreCase(role)) {
             Optional<Credential> credOpt = credentialDAO.findByUsername(username);
            if (credOpt.isPresent()) {
                Credential cred = credOpt.get();
                if (cred.getRole().equals("seeker") && PasswordUtil.verifyPassword(password, cred.getPassword())) {
                     Optional<Seeker> seekerOpt = seekerDAO.findByCredentialId(cred.getCredentialId());
                    if (seekerOpt.isPresent()) {
                        if (!seekerOpt.get().getIsActive()) {
                            logger.warn("Seeker account {} is inactive.", username);
                            return new LoginResponse(null, null, null, "Account is inactive.", false, "inactive");
                        }
                        String token = JwtUtil.generateToken(cred.getCredentialId(), cred.getRole());
                        logger.info("Seeker {} logged in successfully.", username);
                        return new LoginResponse(token, cred.getCredentialId(), cred.getRole(), "Seeker login successful.", true, null);
                    } else {
                        logger.warn("Seeker profile not found for credential ID: {} (username: {})", cred.getCredentialId(), username);
                        return new LoginResponse(null, null, null, "Seeker profile not found.", false, null);
                    }
                }
            } else { // Check seeker backlog
                Optional<SeekerBacklog> backlogOpt = seekerBacklogDAO.findByUsername(username);
                if (backlogOpt.isPresent()) {
                    SeekerBacklog backlog = backlogOpt.get();
                     if (PasswordUtil.verifyPassword(password, backlog.getPassword())) {
                        logger.info("Seeker {} found in backlog with status: {}", username, backlog.getStatus());
                        return new LoginResponse(null, null, null, "Login attempt for backlogged seeker.", false, backlog.getStatus());
                    }
                }
            }
        }

        logger.warn("Login failed for username: {}. Invalid credentials or role.", username);
        return new LoginResponse(null, null, null, "Invalid username, password, or role.", false, null);
    }


    // Helper DTOs for responses
    public static class SignupResponse<T> {
        public final T data;
        public final String message;
        public final boolean success;

        public SignupResponse(T data, String message, boolean success) {
            this.data = data;
            this.message = message;
            this.success = success;
        }
    }

    public static class LoginResponse {
        public final String token;
        public final Integer credentialId;
        public final String role;
        public final String message;
        public final boolean success;
        public final String status; // For backlog users: "pending", "rejected", or "inactive"

        public LoginResponse(String token, Integer credentialId, String role, String message, boolean success, String status) {
            this.token = token;
            this.credentialId = credentialId;
            this.role = role;
            this.message = message;
            this.success = success;
            this.status = status;
        }
    }
}
