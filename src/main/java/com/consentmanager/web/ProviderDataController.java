package com.consentmanager.web;

import com.consentmanager.models.DataItem;
import com.consentmanager.models.Consent;
import com.consentmanager.services.DataService;
import com.consentmanager.services.ConsentService;
import com.consentmanager.services.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.util.List;
import java.util.Optional;

public class ProviderDataController {

    private static final Logger logger = LoggerFactory.getLogger(ProviderDataController.class);
    private final DataService dataService;
    private final ConsentService consentService;
    private final ObjectMapper objectMapper;

    public ProviderDataController(DataService dataService, ConsentService consentService) {
        this.dataService = dataService;
        this.consentService = consentService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); // For LocalDateTime serialization
    }

    public void registerRoutes() {
        // Assumes a path group like /api/provider is set up in App.java
        // And AuthMiddleware.requireProvider will be applied to these routes

        // --- DataItem Routes ---
        Spark.post("/data-items", this::createDataItem, objectMapper::writeValueAsString);
        Spark.get("/data-items", this::getDataItemsByProvider, objectMapper::writeValueAsString);
        Spark.put("/data-items/:id", this::updateDataItem, objectMapper::writeValueAsString);
        Spark.delete("/data-items/:id", this::deleteDataItem, objectMapper::writeValueAsString);

        // --- Consent Management Routes ---
        Spark.get("/consent-requests", this::getPendingConsentRequests, objectMapper::writeValueAsString);
        Spark.put("/consent-requests/:consentId", this::respondToConsentRequest, objectMapper::writeValueAsString);
    }

    private DataItem createDataItem(Request req, Response res) {
        res.type("application/json");
        try {
            Integer providerId = req.attribute("credentialId"); // Set by AuthMiddleware
            if (providerId == null) {
                throw new ServiceException("Provider ID not found in request attributes. Ensure authentication middleware is active.");
            }

            DataItemRequest dataItemRequest = objectMapper.readValue(req.body(), DataItemRequest.class);
            DataItem createdItem = dataService.createDataItem(
                    providerId,
                    dataItemRequest.getName(),
                    dataItemRequest.getDescription(),
                    dataItemRequest.getType(),
                    dataItemRequest.getData() // dataContentOrPath
            );
            res.status(201);
            return createdItem;
        } catch (ServiceException e) {
            logger.warn("ServiceException in createDataItem: {}", e.getMessage());
            res.status(e.getMessage().contains("not found") ? 404 : 400);
            return new ErrorResponse(e.getMessage()).toDataItem(); // Using a trick to satisfy return type
        } catch (Exception e) {
            logger.error("Error creating data item: {}", e.getMessage(), e);
            res.status(500);
            return new ErrorResponse("Internal server error while creating data item.").toDataItem();
        }
    }

    private List<DataItem> getDataItemsByProvider(Request req, Response res) {
        res.type("application/json");
        try {
            Integer providerId = req.attribute("credentialId");
             if (providerId == null) {
                throw new ServiceException("Provider ID not found in request attributes.");
            }
            return dataService.getDataItemsByProvider(providerId);
        } catch (ServiceException e) {
            logger.warn("ServiceException in getDataItemsByProvider: {}", e.getMessage());
            res.status(400);
            return List.of(new ErrorResponse(e.getMessage()).toDataItem());
        } catch (Exception e) {
            logger.error("Error fetching data items for provider: {}", e.getMessage(), e);
            res.status(500);
            return List.of(new ErrorResponse("Internal server error").toDataItem());
        }
    }

    private Object updateDataItem(Request req, Response res) {
        res.type("application/json");
        try {
            Integer providerId = req.attribute("credentialId");
            if (providerId == null) throw new ServiceException("Provider ID missing.");
            int dataItemId = Integer.parseInt(req.params(":id"));

            DataItemRequest dataItemUpdateRequest = objectMapper.readValue(req.body(), DataItemRequest.class);

            boolean updated = dataService.updateDataItem(
                dataItemId,
                providerId,
                dataItemUpdateRequest.getName(),
                dataItemUpdateRequest.getDescription(),
                dataItemUpdateRequest.getType(),
                dataItemUpdateRequest.getData(),
                dataItemUpdateRequest.getAesKeyEncrypted() // Provider might update this
            );

            if (updated) {
                Optional<DataItem> item = dataService.getDataItemByIdForProvider(dataItemId, providerId);
                if(item.isPresent()) return item.get();
                return new SuccessResponse("Data item updated successfully but could not be retrieved.");
            } else {
                res.status(404); // Or 403 if not owner
                return new ErrorResponse("Data item not found or update failed.");
            }
        } catch (ServiceException e) {
            logger.warn("ServiceException in updateDataItem: {}", e.getMessage());
            res.status(400);
            return new ErrorResponse(e.getMessage());
        } catch (NumberFormatException e) {
            logger.warn("Invalid data item ID format: {}", req.params(":id"));
            res.status(400);
            return new ErrorResponse("Invalid data item ID format.");
        } catch (Exception e) {
            logger.error("Error updating data item: {}", e.getMessage(), e);
            res.status(500);
            return new ErrorResponse("Internal server error while updating data item.");
        }
    }

    private Object deleteDataItem(Request req, Response res) {
        res.type("application/json");
        try {
            Integer providerId = req.attribute("credentialId");
            if (providerId == null) throw new ServiceException("Provider ID missing.");
            int dataItemId = Integer.parseInt(req.params(":id"));

            boolean deleted = dataService.deleteDataItem(dataItemId, providerId);
            if (deleted) {
                res.status(200); // OK or 204 No Content
                return new SuccessResponse("Data item deleted successfully.");
            } else {
                res.status(404); // Or 403 if not owner
                return new ErrorResponse("Data item not found or delete failed.");
            }
        } catch (ServiceException e) {
            logger.warn("ServiceException in deleteDataItem: {}", e.getMessage());
            res.status(400);
            return new ErrorResponse(e.getMessage());
        } catch (NumberFormatException e) {
            logger.warn("Invalid data item ID format: {}", req.params(":id"));
            res.status(400);
            return new ErrorResponse("Invalid data item ID format.");
        } catch (Exception e) {
            logger.error("Error deleting data item: {}", e.getMessage(), e);
            res.status(500);
            return new ErrorResponse("Internal server error while deleting data item.");
        }
    }

    private List<Consent> getPendingConsentRequests(Request req, Response res) {
        res.type("application/json");
        try {
            Integer providerId = req.attribute("credentialId");
            if (providerId == null) throw new ServiceException("Provider ID missing.");
            // Default to "pending" status if not specified
            String status = req.queryParams("status") != null ? req.queryParams("status") : "pending";
            return consentService.getConsentsByProviderAndStatus(providerId, status);
        } catch (ServiceException e) {
            logger.warn("ServiceException in getPendingConsentRequests: {}", e.getMessage());
            res.status(400);
            return List.of(new ErrorResponse(e.getMessage()).toConsent());
        } catch (Exception e) {
            logger.error("Error fetching consent requests: {}", e.getMessage(), e);
            res.status(500);
            return List.of(new ErrorResponse("Internal server error").toConsent());
        }
    }

    private Object respondToConsentRequest(Request req, Response res) {
        res.type("application/json");
        try {
            Integer providerId = req.attribute("credentialId");
            if (providerId == null) throw new ServiceException("Provider ID missing.");
            int consentId = Integer.parseInt(req.params(":consentId"));

            ConsentResponseRequest consentResponse = objectMapper.readValue(req.body(), ConsentResponseRequest.class);
            if (consentResponse.getStatus() == null ||
               (!consentResponse.getStatus().equalsIgnoreCase("approved") && !consentResponse.getStatus().equalsIgnoreCase("rejected"))) {
                res.status(400);
                return new ErrorResponse("Invalid status. Must be 'approved' or 'rejected'.");
            }

            boolean success = consentService.respondToConsent(
                    providerId,
                    consentId,
                    consentResponse.getStatus(),
                    consentResponse.getDetails()
            );

            if (success) {
                return new SuccessResponse("Consent request " + consentResponse.getStatus() + " successfully.");
            } else {
                // Service layer should throw exception for business rule violations (e.g., not found, not pending)
                // This path might be if DAO returns false unexpectedly.
                res.status(500);
                return new ErrorResponse("Failed to respond to consent request.");
            }
        } catch (ServiceException e) {
            logger.warn("ServiceException in respondToConsentRequest: {}", e.getMessage());
             if (e.getMessage().toLowerCase().contains("not found")) {
                res.status(404);
            } else if (e.getMessage().toLowerCase().contains("not in 'pending' state") || e.getMessage().toLowerCase().contains("does not own")) {
                res.status(403); // Forbidden or Bad Request
            }
             else {
                res.status(400);
            }
            return new ErrorResponse(e.getMessage());
        } catch (NumberFormatException e) {
            logger.warn("Invalid consent ID format: {}", req.params(":consentId"));
            res.status(400);
            return new ErrorResponse("Invalid consent ID format.");
        } catch (Exception e) {
            logger.error("Error responding to consent request: {}", e.getMessage(), e);
            res.status(500);
            return new ErrorResponse("Internal server error while responding to consent request.");
        }
    }

    // --- Request/Response DTOs (inner classes or separate files) ---
    private static class DataItemRequest {
        private String name;
        private String description;
        private String type;
        private String data; // content for text, path/url for file
        private String aesKeyEncrypted; // Optional, for provider to set/update

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
        public String getAesKeyEncrypted() { return aesKeyEncrypted; }
        public void setAesKeyEncrypted(String aesKeyEncrypted) { this.aesKeyEncrypted = aesKeyEncrypted; }
    }

    private static class ConsentResponseRequest {
        private String status;
        private String details; // e.g. reason for rejection

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
    }

    private static class ErrorResponse {
        private String error;
        public ErrorResponse(String error) { this.error = error; }
        public String getError() { return error; }
        // Trick to satisfy Spark's return type for lists/objects in error cases
        public DataItem toDataItem() { DataItem d = new DataItem(); d.setName(error); return d; }
        public Consent toConsent() { Consent c = new Consent(); c.setStatus(error); return c;}
    }

    private static class SuccessResponse {
        private String message;
        public SuccessResponse(String message) { this.message = message; }
        public String getMessage() { return message; }
    }
}
