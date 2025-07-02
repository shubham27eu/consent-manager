package com.consentmanager.controllers;

import com.consentmanager.services.AdminService;
import com.consentmanager.utils.JwtUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Spark; // For Spark.before, Spark.halt

import java.util.Collections;
import java.util.Optional;

import static spark.Spark.*;

public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final AdminService adminService;
    private final ObjectMapper objectMapper;

    // DTOs for request bodies
    private static class ApprovalRequest {
        public int id; // backlogProviderId or backlogSeekerId
        public String action; // "approve" or "reject"
    }

    private static class UserStatusRequest {
        public int userId; // providerId or seekerId
        public boolean isActive;
    }

    private static class ReactivateRequest { // Used for /admin/reactivate
        public int userId;
        public String role; // "provider" or "seeker" to know which DAO to use for finding user by ID
    }


    public AdminController(AdminService adminService) {
        this.adminService = adminService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        setupRoutes();
    }

    private void setupRoutes() {
        // The authentication for /admin/* is now handled by AuthMiddleware in App.java
        // So, remove: before("/admin/*", this::authenticateAdmin);

        get("/admin/provider-backlog", this::getProviderBacklog, objectMapper::writeValueAsString);
        get("/admin/seeker-backlog", this::getSeekerBacklog, objectMapper::writeValueAsString);

        post("/admin/approve-provider", this::approveProvider, objectMapper::writeValueAsString);
        post("/admin/approve-seeker", this::approveSeeker, objectMapper::writeValueAsString);

        post("/admin/inactivate-provider", this::inactivateProvider, objectMapper::writeValueAsString);
        post("/admin/inactivate-seeker", this::inactivateSeeker, objectMapper::writeValueAsString);

        // This route was in the original server, let's adjust to take role for reactivation
        // Or have separate routes like /admin/reactivate-provider and /admin/reactivate-seeker
        // For now, using a single /reactivate with role in body.
        post("/admin/reactivate", this::reactivateUser, objectMapper::writeValueAsString);

        get("/admin/inactive-users", this::getInactiveUsers, objectMapper::writeValueAsString);
        get("/admin/providers", this::getActiveProviders, objectMapper::writeValueAsString);
        get("/admin/seekers", this::getActiveSeekers, objectMapper::writeValueAsString);
    }

    // The authenticateAdmin method is removed as middleware is handled in App.java

    private Object getProviderBacklog(Request req, Response res) {
        res.type("application/json");
        try {
            return adminService.getProviderBacklog();
        } catch (Exception e) {
            logger.error("Error fetching provider backlog", e);
            handleJsonException(e, "Internal server error fetching provider backlog.");
        }
        return null;
    }

    private Object getSeekerBacklog(Request req, Response res) {
        res.type("application/json");
        try {
            return adminService.getSeekerBacklog();
        } catch (Exception e) {
            logger.error("Error fetching seeker backlog", e);
            handleJsonException(e, "Internal server error fetching seeker backlog.");
        }
        return null;
    }

    private Object approveProvider(Request req, Response res) {
        res.type("application/json");
        try {
            ApprovalRequest approvalReq = objectMapper.readValue(req.body(), ApprovalRequest.class);
            // Validation
            if (approvalReq.id <= 0) {
                return haltWithError(res, 400, "Valid backlog ID is required.");
            }
            if (approvalReq.action == null || (!approvalReq.action.equalsIgnoreCase("approve") && !approvalReq.action.equalsIgnoreCase("reject"))) {
                return haltWithError(res, 400, "Action must be 'approve' or 'reject'.");
            }

            boolean success = adminService.processProviderApproval(approvalReq.id, "approve".equalsIgnoreCase(approvalReq.action));
            if (success) {
                return Collections.singletonMap("message", "Provider " + approvalReq.action + (approvalReq.action.endsWith("e") ? "d" : "ed") + " successfully.");
            } else {
                res.status(400);
                return Collections.singletonMap("error", "Failed to process provider approval.");
            }
        } catch (Exception e) {
            logger.error("Error processing provider approval request", e);
            handleJsonException(e, "Internal server error processing provider approval.");
        }
        return null;
    }

    private Object approveSeeker(Request req, Response res) {
        res.type("application/json");
        try {
            ApprovalRequest approvalReq = objectMapper.readValue(req.body(), ApprovalRequest.class);
             // Validation
            if (approvalReq.id <= 0) {
                return haltWithError(res, 400, "Valid backlog ID is required.");
            }
            if (approvalReq.action == null || (!approvalReq.action.equalsIgnoreCase("approve") && !approvalReq.action.equalsIgnoreCase("reject"))) {
                return haltWithError(res, 400, "Action must be 'approve' or 'reject'.");
            }

            boolean success = adminService.processSeekerApproval(approvalReq.id, "approve".equalsIgnoreCase(approvalReq.action));
            if (success) {
                return Collections.singletonMap("message", "Seeker " + approvalReq.action + (approvalReq.action.endsWith("e") ? "d" : "ed") + " successfully.");
            } else {
                res.status(400);
                return Collections.singletonMap("error", "Failed to process seeker approval.");
            }
        } catch (Exception e) {
            logger.error("Error processing seeker approval request", e);
            handleJsonException(e, "Internal server error processing seeker approval.");
        }
        return null;
    }

    private Object inactivateProvider(Request req, Response res) {
        res.type("application/json");
        try {
            UserStatusRequest statusRequest = objectMapper.readValue(req.body(), UserStatusRequest.class);
            // Validation
            if (statusRequest.userId <= 0) {
                return haltWithError(res, 400, "Valid userId is required.");
            }

            boolean success = adminService.setProviderActiveStatus(statusRequest.userId, false);
            if (success) {
                return Collections.singletonMap("message", "Provider inactivated successfully.");
            } else {
                res.status(404);
                return Collections.singletonMap("error", "Failed to inactivate provider (e.g., not found).");
            }
        } catch (Exception e) {
            logger.error("Error processing provider inactivation request", e);
            handleJsonException(e, "Internal server error during provider inactivation.");
        }
        return null;
    }

    private Object inactivateSeeker(Request req, Response res) {
        res.type("application/json");
        try {
            UserStatusRequest statusRequest = objectMapper.readValue(req.body(), UserStatusRequest.class);
            // Validation
             if (statusRequest.userId <= 0) {
                return haltWithError(res, 400, "Valid userId is required.");
            }

            boolean success = adminService.setSeekerActiveStatus(statusRequest.userId, false);
            if (success) {
                return Collections.singletonMap("message", "Seeker inactivated successfully.");
            } else {
                res.status(404);
                return Collections.singletonMap("error", "Failed to inactivate seeker (e.g., not found).");
            }
        } catch (Exception e) {
            logger.error("Error processing seeker inactivation request", e);
            handleJsonException(e, "Internal server error during seeker inactivation.");
        }
        return null;
    }

    private Object reactivateUser(Request req, Response res) {
        res.type("application/json");
        try {
            ReactivateRequest reactivateReq = objectMapper.readValue(req.body(), ReactivateRequest.class);
            // Validation
            if (reactivateReq.userId <= 0) {
                 return haltWithError(res, 400, "Valid userId is required.");
            }
            if (reactivateReq.role == null || (!reactivateReq.role.equalsIgnoreCase("provider") && !reactivateReq.role.equalsIgnoreCase("seeker"))) {
                return haltWithError(res, 400, "Role must be 'provider' or 'seeker'.");
            }

            boolean success = false;
            if ("provider".equalsIgnoreCase(reactivateReq.role)) {
                success = adminService.setProviderActiveStatus(reactivateReq.userId, true);
            } else if ("seeker".equalsIgnoreCase(reactivateReq.role)) { // This condition is now guaranteed by validation
                success = adminService.setSeekerActiveStatus(reactivateReq.userId, true);
            }
            // No 'else' needed due to prior validation ensuring role is one of the two

            if (success) {
                return Collections.singletonMap("message", reactivateReq.role + " reactivated successfully.");
            } else {
                res.status(404);
                return Collections.singletonMap("error", "Failed to reactivate " + reactivateReq.role + " (e.g., not found).");
            }
        } catch (Exception e) {
            logger.error("Error processing user reactivation request", e);
            handleJsonException(e, "Internal server error during user reactivation.");
        }
        return null;
    }


    private Object getInactiveUsers(Request req, Response res) {
        res.type("application/json");
        try {
            return adminService.getInactiveUsers();
        } catch (Exception e) {
            logger.error("Error fetching inactive users", e);
            handleJsonException(e, "Internal server error fetching inactive users.");
        }
        return null;
    }

    private Object getActiveProviders(Request req, Response res) {
        res.type("application/json");
        try {
            return adminService.getActiveProviders();
        } catch (Exception e) {
            logger.error("Error fetching active providers", e);
            handleJsonException(e, "Internal server error fetching active providers.");
        }
        return null;
    }

    private Object getActiveSeekers(Request req, Response res) {
        res.type("application/json");
        try {
            return adminService.getActiveSeekers();
        } catch (Exception e) {
            logger.error("Error fetching active seekers", e);
            handleJsonException(e, "Internal server error fetching active seekers.");
        }
        return null;
    }

    // Helper to handle JsonProcessingException in halt and standardize error response
    private String haltWithError(Response res, int statusCode, String message) throws com.fasterxml.jackson.core.JsonProcessingException {
        logger.warn("Validation/Request error in AdminController: {} - {}", statusCode, message);
        res.status(statusCode);
        halt(statusCode, objectMapper.writeValueAsString(Collections.singletonMap("error", message)));
        return null; // Unreachable
    }

    // Helper to handle JsonProcessingException in halt for general exceptions
    private void handleJsonException(Exception e, String defaultMessage) {
        try {
            halt(500, objectMapper.writeValueAsString(Collections.singletonMap("error", defaultMessage)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException jpe) {
            logger.error("Critical error: Failed to serialize error message for halt", jpe);
            halt(500, "{\"error\":\"Internal Server Error and failed to serialize error message.\"}");
        }
    }
}
