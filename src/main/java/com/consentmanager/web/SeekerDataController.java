package com.consentmanager.web;

import com.consentmanager.models.Consent;
import com.consentmanager.models.DataItem;
import com.consentmanager.services.ConsentService;
import com.consentmanager.services.DataService;
import com.consentmanager.services.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.time.LocalDateTime;
import java.util.List;

public class SeekerDataController {

    private static final Logger logger = LoggerFactory.getLogger(SeekerDataController.class);
    private final DataService dataService;
    private final ConsentService consentService;
    private final ObjectMapper objectMapper;

    public SeekerDataController(DataService dataService, ConsentService consentService) {
        this.dataService = dataService;
        this.consentService = consentService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void registerRoutes() {
        // Assumes a path group like /api/seeker is set up in App.java
        // And AuthMiddleware.requireSeeker will be applied

        Spark.get("/data-items", this::listDiscoverableDataItems, objectMapper::writeValueAsString);
        Spark.post("/consents", this::requestConsent, objectMapper::writeValueAsString);
        Spark.get("/consents", this::getSeekerConsents, objectMapper::writeValueAsString);
        Spark.get("/data-items/:dataItemId/access", this::accessDataItem, objectMapper::writeValueAsString);
    }

    private List<DataItem> listDiscoverableDataItems(Request req, Response res) {
        res.type("application/json");
        try {
            Integer seekerId = req.attribute("credentialId");
            if (seekerId == null) throw new ServiceException("Seeker ID missing from request.");
            return dataService.listDiscoverableDataItems(seekerId);
        } catch (ServiceException e) {
            logger.warn("ServiceException in listDiscoverableDataItems: {}", e.getMessage());
            res.status(400);
            return List.of(new ErrorResponse(e.getMessage()).toDataItem());
        } catch (Exception e) {
            logger.error("Error listing discoverable data items: {}", e.getMessage(), e);
            res.status(500);
            return List.of(new ErrorResponse("Internal server error").toDataItem());
        }
    }

    private Consent requestConsent(Request req, Response res) {
        res.type("application/json");
        try {
            Integer seekerId = req.attribute("credentialId");
            if (seekerId == null) throw new ServiceException("Seeker ID missing.");

            ConsentRequestPayload payload = objectMapper.readValue(req.body(), ConsentRequestPayload.class);
            if (payload.getDataItemId() == null) {
                throw new ServiceException("Data Item ID is required.");
            }

            LocalDateTime expiresAt = payload.getExpiresAt(); // Can be null
            Integer maxAccessCount = payload.getMaxAccessCount(); // Can be null

            Consent consent = consentService.requestConsent(seekerId, payload.getDataItemId(), expiresAt, maxAccessCount);
            res.status(201); // Created
            return consent;
        } catch (ServiceException e) {
            logger.warn("ServiceException in requestConsent: {}", e.getMessage());
            res.status(e.getMessage().contains("not found") ? 404 : 400);
            return new ErrorResponse(e.getMessage()).toConsent();
        } catch (Exception e) {
            logger.error("Error requesting consent: {}", e.getMessage(), e);
            res.status(500);
            return new ErrorResponse("Internal server error while requesting consent.").toConsent();
        }
    }

    private List<Consent> getSeekerConsents(Request req, Response res) {
        res.type("application/json");
        try {
            Integer seekerId = req.attribute("credentialId");
            if (seekerId == null) throw new ServiceException("Seeker ID missing.");
            return consentService.getConsentsBySeeker(seekerId);
        } catch (ServiceException e) {
            logger.warn("ServiceException in getSeekerConsents: {}", e.getMessage());
            res.status(400);
            return List.of(new ErrorResponse(e.getMessage()).toConsent());
        } catch (Exception e) {
            logger.error("Error fetching seeker consents: {}", e.getMessage(), e);
            res.status(500);
            return List.of(new ErrorResponse("Internal server error").toConsent());
        }
    }

    private Object accessDataItem(Request req, Response res) {
        res.type("application/json");
        try {
            Integer seekerId = req.attribute("credentialId");
            if (seekerId == null) throw new ServiceException("Seeker ID missing.");

            int dataItemId;
            try {
                dataItemId = Integer.parseInt(req.params(":dataItemId"));
            } catch (NumberFormatException e) {
                throw new ServiceException("Invalid Data Item ID format.");
            }

            String accessedData = dataService.accessDataItem(seekerId, dataItemId);
            // For now, just return the string. Could wrap in a JSON object.
            // Consider if the raw data or a structured response is better.
            // If it's file metadata, a JSON object would be better.
            // If it's decrypted text, it could be returned as plain text or JSON.
            // For simplicity, wrapping in a simple JSON object.
            return new DataAccessResponse(accessedData);

        } catch (ServiceException e) {
            logger.warn("ServiceException in accessDataItem: {}", e.getMessage());
            if (e.getMessage().toLowerCase().contains("not found")) {
                res.status(404);
            } else if (e.getMessage().toLowerCase().contains("consent not approved") ||
                       e.getMessage().toLowerCase().contains("consent has expired") ||
                       e.getMessage().toLowerCase().contains("access count exhausted")) {
                res.status(403); // Forbidden
            } else {
                res.status(400);
            }
            return new ErrorResponse(e.getMessage());
        } catch (Exception e) {
            logger.error("Error accessing data item: {}", e.getMessage(), e);
            res.status(500);
            return new ErrorResponse("Internal server error while accessing data.");
        }
    }

    // --- Request/Response DTOs ---
    private static class ConsentRequestPayload {
        private Integer dataItemId;
        private LocalDateTime expiresAt; // Optional: Seeker might suggest an expiry
        private Integer maxAccessCount;  // Optional: Seeker might suggest an access limit

        public Integer getDataItemId() { return dataItemId; }
        public void setDataItemId(Integer dataItemId) { this.dataItemId = dataItemId; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
        public Integer getMaxAccessCount() { return maxAccessCount; }
        public void setMaxAccessCount(Integer maxAccessCount) { this.maxAccessCount = maxAccessCount; }
    }

    private static class DataAccessResponse {
        private String data;
        public DataAccessResponse(String data) { this.data = data; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
    }

    // Helper for consistent error responses, similar to ProviderDataController
    private static class ErrorResponse {
        private String error;
        public ErrorResponse(String error) { this.error = error; }
        public String getError() { return error; }
        public DataItem toDataItem() { DataItem d = new DataItem(); d.setName(error); return d; }
        public Consent toConsent() { Consent c = new Consent(); c.setStatus(error); return c;}
    }
}
