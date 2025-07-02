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


import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


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
        Spark.get("/data-items/:dataItemId/download", this::downloadDataItem); // No transform for raw response
    }

    private List<DataItem> listDiscoverableDataItems(Request req, Response res) {
        res.type("application/json");
        try {
            Integer seekerId = req.attribute("credentialId");
            if (seekerId == null) {
                 return haltWithError(res, 401, "Seeker authentication required.");
            }
            return dataService.listDiscoverableDataItems(seekerId);
        } catch (ServiceException e) {
            logger.warn("ServiceException in listDiscoverableDataItems: {}", e.getMessage());
            res.status(400); // Or map specific ServiceException types
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
            if (seekerId == null) return haltWithError(res, 401, "Seeker authentication required.");

            ConsentRequestPayload payload = objectMapper.readValue(req.body(), ConsentRequestPayload.class);

            // Validation
            if (payload.getDataItemId() == null || payload.getDataItemId() <= 0) {
                return haltWithError(res, 400, "Valid Data Item ID is required.");
            }
            if (payload.getExpiresAt() != null && payload.getExpiresAt().isBefore(LocalDateTime.now())) {
                return haltWithError(res, 400, "Expiration date, if provided, must be in the future.");
            }
            if (payload.getMaxAccessCount() != null && payload.getMaxAccessCount() <= 0) {
                return haltWithError(res, 400, "Max access count, if provided, must be positive.");
            }

            LocalDateTime expiresAt = payload.getExpiresAt();
            Integer maxAccessCount = payload.getMaxAccessCount();

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
            if (seekerId == null) return haltWithError(res, 401, "Seeker authentication required.");

            return consentService.getConsentsBySeeker(seekerId);
        } catch (ServiceException e) {
            logger.warn("ServiceException in getSeekerConsents: {}", e.getMessage());
            res.status(400); // Or map specific ServiceException types
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
            if (seekerId == null) return haltWithError(res, 401, "Seeker authentication required.");

            int dataItemId;
            try {
                dataItemId = Integer.parseInt(req.params(":dataItemId"));
                if (dataItemId <= 0) throw new NumberFormatException("ID must be positive.");
            } catch (NumberFormatException e) {
                return haltWithError(res, 400, "Invalid Data Item ID format in path.");
            }

            DataService.DataAccessDetails accessDetails = dataService.accessDataItem(seekerId, dataItemId);

            // The DataAccessDetails DTO is already suitable for JSON response.
            return accessDetails;

        } catch (ServiceException e) {
            logger.warn("ServiceException in accessDataItem: {}", e.getMessage());
            int statusCode = 400; // Default bad request
            if (e.getMessage().toLowerCase().contains("not found")) {
                statusCode = 404;
            } else if (e.getMessage().toLowerCase().contains("consent not approved") ||
                       e.getMessage().toLowerCase().contains("consent has expired") ||
                       e.getMessage().toLowerCase().contains("access count exhausted")) {
                statusCode = 403; // Forbidden
            }
            return haltWithError(res, statusCode, e.getMessage());
        } catch (Exception e) {
            logger.error("Error accessing data item: {}", e.getMessage(), e);
            res.status(500);
            return new ErrorResponse("Internal server error while accessing data.");
        }
    }

    private Object downloadDataItem(Request req, Response res) {
        try {
            Integer seekerId = req.attribute("credentialId");
            if (seekerId == null) {
                // Use haltWithError for consistency, though Spark.halt is also fine.
                return haltWithError(res, 401, "Seeker authentication required.");
            }

            int dataItemId;
            try {
                dataItemId = Integer.parseInt(req.params(":dataItemId"));
                 if (dataItemId <= 0) throw new NumberFormatException("ID must be positive.");
            } catch (NumberFormatException e) {
                 return haltWithError(res, 400, "Invalid Data Item ID format in path.");
            }

            // Use DataService to check consent, increment count, and get data details
            DataService.DataAccessDetails accessDetails = dataService.accessDataItem(seekerId, dataItemId);

            if (!"file".equalsIgnoreCase(accessDetails.getDataType())) {
                return haltWithError(res, 400, "Data item is not a file type and cannot be downloaded.");
            }

            String relativePath = accessDetails.getData(); // This is the file path
            String originalFileName = accessDetails.getOriginalFileName(); // Use this for Content-Disposition

            // Construct full path - BASE_STORAGE_PATH is defined in DataService, ideally pass it or get full path from service
            Path filePath = Paths.get(DataService.BASE_STORAGE_PATH, relativePath);
            logger.info("Attempting to download file: {} from path: {}", originalFileName, filePath);


            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                Spark.halt(404, objectMapper.writeValueAsString(new ErrorResponse("File not found on server.")));
                return null;
            }

            res.type(Files.probeContentType(filePath)); // Guess content type
            // Use originalFileName for Content-Disposition if available, otherwise fallback to path's filename part
            String downloadFileName = originalFileName != null ? originalFileName : filePath.getFileName().toString();
            res.header("Content-Disposition", "attachment; filename=\"" + downloadFileName + "\"");

            try (OutputStream outputStream = res.raw().getOutputStream();
                 FileInputStream fileInputStream = new FileInputStream(filePath.toFile())) {
                fileInputStream.transferTo(outputStream);
                outputStream.flush();
                return res.raw(); // Return the raw response after streaming
            } catch (IOException e) {
                logger.error("Error streaming file {}: {}", filePath, e.getMessage(), e);
                Spark.halt(500, objectMapper.writeValueAsString(new ErrorResponse("Error occurred while streaming file.")));
                return null;
            }

        } catch (ServiceException e) { // From accessDataItem call if it fails
            logger.warn("ServiceException in downloadDataItem (access check): {}", e.getMessage());
            int status = 400;
            if (e.getMessage().toLowerCase().contains("not found")) status = 404;
            else if (e.getMessage().toLowerCase().contains("consent not approved") ||
                     e.getMessage().toLowerCase().contains("consent has expired") ||
                     e.getMessage().toLowerCase().contains("access count exhausted")) status = 403;
            Spark.halt(status, objectMapper.writeValueAsString(new ErrorResponse(e.getMessage())));
        } catch (Exception e) { // Catch other exceptions like Jackson, IO
            logger.error("Error downloading data item: {}", e.getMessage(), e);
            Spark.halt(500, objectMapper.writeValueAsString(new ErrorResponse("Internal server error while downloading file.")));
        }
        return null; // Should be unreachable if Spark.halt is used
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
        public DataItem toDataItem() { DataItem d = new DataItem(); d.setName(error); return d; } // For List<DataItem> returns
        public Consent toConsent() { Consent c = new Consent(); c.setStatus(error); return c;} // For List<Consent> or single Consent returns
    }

    // Helper for null or empty string check
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    // Helper to standardize error responses and halt
    private Object haltWithError(Response res, int statusCode, String message) {
        logger.warn("Validation/Request error in SeekerDataController: {} - {}", statusCode, message);
        res.status(statusCode);
        try {
            // Ensure response type is JSON for error messages too
            res.type("application/json");
            Spark.halt(statusCode, objectMapper.writeValueAsString(new ErrorResponse(message)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            logger.error("Error serializing error message for halt", e);
            // Fallback halt if JSON serialization itself fails
            Spark.halt(500, "{\"error\":\"Internal Server Error and failed to serialize error message.\"}");
        }
        return null; // Unreachable due to halt
    }
}
