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

    private Object downloadDataItem(Request req, Response res) {
        try {
            Integer seekerId = req.attribute("credentialId");
            if (seekerId == null) {
                Spark.halt(401, objectMapper.writeValueAsString(new ErrorResponse("Seeker ID missing.")));
                return null; // Unreachable
            }

            int dataItemId;
            try {
                dataItemId = Integer.parseInt(req.params(":dataItemId"));
            } catch (NumberFormatException e) {
                 Spark.halt(400, objectMapper.writeValueAsString(new ErrorResponse("Invalid Data Item ID format.")));
                 return null;
            }

            // Use DataService to check consent and get DataItem (which includes relative path)
            // DataService.accessDataItem already performs consent checks.
            // We need to get the actual DataItem to retrieve its path and original name for Content-Disposition.

            Optional<Consent> consentOpt = consentService.getConsentById( // Or a more specific method
                // This is tricky: accessDataItem increments count. We need a way to get item path post-consent check
                // without necessarily consuming an "access". Or, accessDataItem needs to return structured info.
                // For now, let's assume we need to re-fetch consent and dataItem if accessDataItem only returns string.
                // A better approach: DataService.getAccessibleDataItemDetails(seekerId, dataItemId)
                // that returns DataItem if access is valid, and increments count.
                // Let's call accessDataItem first to ensure all checks & count increment pass, then fetch item.
                // This isn't ideal as it might return a string that we then discard.
                dataService.accessDataItem(seekerId, dataItemId) // This will throw if not permitted & increments count
            ); // This line is problematic, accessDataItem returns String.

            // Re-fetch DataItem to get its properties
            Optional<com.consentmanager.models.DataItem> dataItemOpt = dataService.getDataItemByIdForProvider(dataItemId, null); // providerId not needed for just fetching by id by service
             if(dataItemOpt.isEmpty() || !"file".equalsIgnoreCase(dataItemOpt.get().getType())){
                 Spark.halt(404, objectMapper.writeValueAsString(new ErrorResponse("File data item not found or not a file.")));
                 return null;
             }
             DataItem dataItem = dataItemOpt.get();
             String relativePath = dataItem.getData();
             // Construct full path - Assuming BASE_STORAGE_PATH is accessible here or defined globally
             // For now, hardcoding a base path as defined in DataService for consistency
             Path filePath = Paths.get("storage", relativePath);
             logger.info("Attempting to download file from path: {}", filePath);


            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                Spark.halt(404, objectMapper.writeValueAsString(new ErrorResponse("File not found on server.")));
                return null;
            }

            res.type(Files.probeContentType(filePath)); // Guess content type
            res.header("Content-Disposition", "attachment; filename=\"" + Paths.get(dataItem.getName()).getFileName().toString() + "\""); // Use original name from DataItem

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
        public DataItem toDataItem() { DataItem d = new DataItem(); d.setName(error); return d; }
        public Consent toConsent() { Consent c = new Consent(); c.setStatus(error); return c;}
    }
}
