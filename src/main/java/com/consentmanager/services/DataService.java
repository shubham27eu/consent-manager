package com.consentmanager.services;

import com.consentmanager.daos.ConsentDAO;
import com.consentmanager.daos.DataItemDAO;
import com.consentmanager.models.Consent;
import com.consentmanager.models.DataItem;
import com.consentmanager.utils.EncryptionUtil; // Will be needed later
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DataService {

    private static final Logger logger = LoggerFactory.getLogger(DataService.class);
    private static final String BASE_STORAGE_PATH = "storage/"; // Base directory for storing files
    private DataItemDAO dataItemDAO;
    private ConsentDAO consentDAO;
    private ConsentHistoryDAO consentHistoryDAO; // For logging access

    // Constructor for dependency injection (testing and real use)
    public DataService(DataItemDAO dataItemDAO, ConsentDAO consentDAO, ConsentHistoryDAO consentHistoryDAO) {
        this.dataItemDAO = dataItemDAO;
        this.consentDAO = consentDAO;
        this.consentHistoryDAO = consentHistoryDAO;
    }

    // Overloaded constructor for convenience when connection is managed externally for all DAOs
    public DataService(Connection connection) {
        this.dataItemDAO = new DataItemDAO(connection);
        this.consentDAO = new ConsentDAO(connection);
        this.consentHistoryDAO = new ConsentHistoryDAO(connection);
    }


    /**
     * Creates a new data item for a provider.
     * For "file" types, it's assumed an AES key is generated, encrypted, and stored.
     * For "text" types, the data itself might be encrypted.
     * (Full encryption logic with provider/seeker keys will be more complex involving EncryptionUtil)
     */
    public DataItem createDataItem(Integer providerId, String name, String description, String type, String dataContentOrPath) throws ServiceException {
        // Basic validation
        if (providerId == null || name == null || name.trim().isEmpty() || type == null || type.trim().isEmpty()) {
            throw new ServiceException("Provider ID, name, and type are required to create a data item.");
        }

        String actualAesKey;
        String aesKeyEncryptedWithProviderKey;
        String finalDataToStore = dataContentOrPath;

        try {
            actualAesKey = EncryptionUtil.generateAesKeyString();

            if ("file".equalsIgnoreCase(type)) {
                Path providerDir = Paths.get(BASE_STORAGE_PATH, "provider_" + providerId);
                Files.createDirectories(providerDir);
                String uniqueFileName = name.replaceAll("[^a-zA-Z0-9.-]", "_") + "_" + System.currentTimeMillis();
                Path filePath = providerDir.resolve(uniqueFileName);

                byte[] rawFileBytes;
                try {
                    rawFileBytes = java.util.Base64.getDecoder().decode(dataContentOrPath);
                } catch (IllegalArgumentException e) {
                    logger.error("Invalid Base64 content for file item {}: {}", name, e.getMessage());
                    throw new ServiceException("Invalid Base64 file content.", e);
                }

                // Encrypt the file content before saving
                // For simplicity, encrypting bytes as if it were a string. Real file encryption might need streaming.
                // This is a placeholder for actual byte[] encryption.
                // Consider if encryptAES should take byte[] and return byte[]
                String encryptedFileContentBase64 = EncryptionUtil.encryptAES(Base64.getEncoder().encodeToString(rawFileBytes), actualAesKey);
                Files.write(filePath, Base64.getDecoder().decode(encryptedFileContentBase64), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                finalDataToStore = providerDir.relativize(filePath).toString(); // Store relative path
                logger.info("Encrypted file data item {} stored at: {}", name, filePath.toString());

            } else if ("text".equalsIgnoreCase(type)) {
                finalDataToStore = EncryptionUtil.encryptAES(dataContentOrPath, actualAesKey);
                logger.info("Encrypted text data for item: {}", name);
            }
            // Encrypt the AES key with the system/provider's public RSA key
            aesKeyEncryptedWithProviderKey = EncryptionUtil.encryptRSA(actualAesKey, EncryptionUtil.SYSTEM_RSA_PUBLIC_KEY_STRING);

        } catch (NoSuchAlgorithmException e) {
            logger.error("AES algorithm not found during key generation for {}: {}", name, e.getMessage(), e);
            throw new ServiceException("Failed to generate encryption key.", e);
        } catch (IOException e) {
            logger.error("Error saving encrypted file for data item {}: {}", name, e.getMessage(), e);
            throw new ServiceException("Failed to save encrypted file data.", e);
        } catch (Exception e) {
            logger.error("Error during encryption preparation for data item {}: {}", name, e.getMessage(), e);
            throw new ServiceException("Failed to prepare encrypted data item.", e);
        }


        DataItem newItem = new DataItem(providerId, name, description, type, finalDataToStore, aesKeyEncryptedWithProviderKey);
        DataItem savedItem = dataItemDAO.saveDataItem(newItem);
        if (savedItem == null) {
            throw new ServiceException("Failed to save data item to database.");
        }
        return savedItem;
    }

    /**
     * Retrieves all data items for a specific provider.
     */
    public List<DataItem> getDataItemsByProvider(Integer providerId) throws ServiceException {
        if (providerId == null) {
            throw new ServiceException("Provider ID is required to fetch data items.");
        }
        try {
            return dataItemDAO.findByProviderId(providerId);
        } catch (Exception e) {
            logger.error("Error retrieving data items for provider {}: {}", providerId, e.getMessage(), e);
            throw new ServiceException("Could not retrieve data items.", e);
        }
    }

    /**
     * Retrieves a specific data item by its ID, ensuring it belongs to the provider.
     */
    public Optional<DataItem> getDataItemByIdForProvider(Integer dataItemId, Integer providerId) throws ServiceException {
         if (dataItemId == null || providerId == null) {
            throw new ServiceException("Data Item ID and Provider ID are required.");
        }
        Optional<DataItem> dataItemOpt = dataItemDAO.findById(dataItemId);
        if (dataItemOpt.isPresent() && dataItemOpt.get().getProviderId().equals(providerId)) {
            return dataItemOpt;
        }
        return Optional.empty(); // Or throw NotAuthorized/NotFound
    }

    /**
     * Updates an existing data item. Only the owner (provider) can update.
     */
    public boolean updateDataItem(Integer dataItemId, Integer providerId, String name, String description, String type, String dataContentOrPath, String aesKeyEncrypted) throws ServiceException {
        if (dataItemId == null || providerId == null) {
            throw new ServiceException("Data Item ID and Provider ID are required for update.");
        }
        // Fetch existing to ensure it's owned by the provider
        Optional<DataItem> existingItemOpt = dataItemDAO.findById(dataItemId);
        if (existingItemOpt.isEmpty() || !existingItemOpt.get().getProviderId().equals(providerId)) {
            logger.warn("Attempt to update data item {} not owned by provider {} or item not found.", dataItemId, providerId);
            return false; // Or throw specific exception
        }

        DataItem itemToUpdate = existingItemOpt.get();
        if (name != null) itemToUpdate.setName(name);
        if (description != null) itemToUpdate.setDescription(description);
        if (type != null) itemToUpdate.setType(type);
        if (dataContentOrPath != null) itemToUpdate.setData(dataContentOrPath);
        if (aesKeyEncrypted != null) itemToUpdate.setAesKeyEncrypted(aesKeyEncrypted); // Provider might re-key

        try {
            return dataItemDAO.updateDataItem(itemToUpdate);
        } catch (Exception e) {
            logger.error("Error updating data item {}: {}", dataItemId, e.getMessage(), e);
            throw new ServiceException("Could not update data item.", e);
        }
    }

    /**
     * Deletes a data item. Only the owner (provider) can delete.
     */
    public boolean deleteDataItem(Integer dataItemId, Integer providerId) throws ServiceException {
        if (dataItemId == null || providerId == null) {
            throw new ServiceException("Data Item ID and Provider ID are required for deletion.");
        }
        // Optional: Check ownership first if DAO doesn't, but DAO delete includes providerId.
        try {
            // TODO: Need to handle cascading deletes for consents and consent history related to this data item.
            // This should ideally be handled at the database level with ON DELETE CASCADE or by the service.
            // For now, just deleting the item.
            return dataItemDAO.deleteDataItem(dataItemId, providerId);
        } catch (Exception e) {
            logger.error("Error deleting data item {}: {}", dataItemId, e.getMessage(), e);
            throw new ServiceException("Could not delete data item.", e);
        }
    }

    /**
     * Allows a seeker to access a data item they have approved consent for.
     * This is a simplified version. Real decryption would involve seeker's private key.
     * @return The decrypted data content (for text) or info for file access.
     */
    public String accessDataItem(Integer seekerId, Integer dataItemId) throws ServiceException {
        if (seekerId == null || dataItemId == null) {
            throw new ServiceException("Seeker ID and Data Item ID are required.");
        }

        Optional<Consent> consentOpt = consentDAO.findByDataItemAndSeeker(dataItemId, seekerId);
        if (consentOpt.isEmpty()) {
            throw new ServiceException("No consent record found for this seeker and data item.");
        }

        Consent consent = consentOpt.get();
        if (!"approved".equalsIgnoreCase(consent.getStatus())) {
            throw new ServiceException("Consent not approved for data item " + dataItemId);
        }

        if (consent.getExpiresAt() != null && LocalDateTime.now().isAfter(consent.getExpiresAt())) {
            // Optionally update status to "expired"
            // consentDAO.updateConsentStatus(consent.getConsentId(), "expired", consent.getApprovedAt(), consent.getExpiresAt(), consent.getReEncryptedAesKey());
            throw new ServiceException("Consent has expired for data item " + dataItemId);
        }

        if (consent.getMaxAccessCount() != null && consent.getAccessCount() >= consent.getMaxAccessCount()) {
            // Optionally update status to "exhausted"
             // consentDAO.updateConsentStatus(consent.getConsentId(), "exhausted", consent.getApprovedAt(), consent.getExpiresAt(), consent.getReEncryptedAesKey());
            throw new ServiceException("Access count exhausted for data item " + dataItemId);
        }

        Optional<DataItem> dataItemOpt = dataItemDAO.findById(dataItemId);
        if (dataItemOpt.isEmpty()) {
            throw new ServiceException("Data item " + dataItemId + " not found.");
        }
        DataItem dataItem = dataItemOpt.get();

        // --- Placeholder for decryption ---
        // String decryptedData;
        // String reEncryptedItemAesKey = consent.getReEncryptedAesKey(); // Key for DataItem's AES key, encrypted with Seeker's public key
        // String itemAesKey; // This would be decrypted by Seeker using their private key from reEncryptedItemAesKey

        // Assume seeker has decrypted reEncryptedItemAesKey to get itemAesKey
        // String itemAesKey = EncryptionUtil.decryptRSA(reEncryptedItemAesKey, seekerPrivateKeyString);
        // String dataItemEncryptedAesKey = dataItem.getAesKeyEncrypted(); // This is the AES key of the data, encrypted with provider's key
        // String actualDataAesKey = EncryptionUtil.decryptRSA(dataItemEncryptedAesKey, providerSystemPrivateKey); // or however this is managed

        // if ("text".equalsIgnoreCase(dataItem.getType())) {
        //     decryptedData = EncryptionUtil.decryptAES(dataItem.getData(), actualDataAesKey);
        // } else if ("file".equalsIgnoreCase(dataItem.getType())) {
        //     decryptedData = "File: " + dataItem.getData() + " (Access with key: " + actualDataAesKey + ")";
        // } else {
        //     throw new ServiceException("Unsupported data type for access: " + dataItem.getType());
        // }
        String accessedContent;
        if ("file".equalsIgnoreCase(dataItem.getType())) {
            // For file type, data stores the relative path
            accessedContent = "File path: " + dataItem.getData() +
                              " (AES key for decryption: " + consent.getReEncryptedAesKey() +
                              " - this would be decrypted by seeker to get the actual AES key for the file)";
        } else { // text or other types
            accessedContent = "Accessed data (decryption placeholder): " + dataItem.getData();
        }


        // Increment access count
        boolean countIncremented = consentDAO.incrementAccessCount(consent.getConsentId());
        if(countIncremented){
            // Log access in ConsentHistory
            consentHistoryDAO.logAction(new com.consentmanager.models.ConsentHistory(
                consent.getConsentId(),
                "accessed",
                seekerId, // actorId is the seeker's credentialId
                "seeker", // actorRole
                "Data item accessed by seeker."
            ));
        } else {
            logger.warn("Failed to increment access count for consent ID {}, but access was granted based on checks.", consent.getConsentId());
            // Depending on policy, this might still be an error or just a warning
        }


        return accessedContent;
    }

    /**
     * Lists all data items available for a seeker to request consent for.
     * This is a simplified version; a real system might have more complex discovery/filtering.
     */
    public List<DataItem> listDiscoverableDataItems(Integer seekerId) throws ServiceException {
        try {
            // For now, return all data items.
            // Later, this could be filtered based on seeker preferences, provider visibility settings, etc.
            // Also, filter out items the seeker already has an active/pending consent for.
            List<DataItem> allItems = dataItemDAO.findAll();
            List<Consent> seekerConsents = consentDAO.findBySeekerId(seekerId);

            List<Integer> consentedItemIds = seekerConsents.stream()
                .filter(c -> "approved".equalsIgnoreCase(c.getStatus()) || "pending".equalsIgnoreCase(c.getStatus()))
                .map(Consent::getDataItemId)
                .collect(Collectors.toList());

            return allItems.stream()
                .filter(item -> !consentedItemIds.contains(item.getDataItemId()))
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error listing discoverable data items for seeker {}: {}", seekerId, e.getMessage(), e);
            throw new ServiceException("Could not list discoverable data items.", e);
        }
    }
}
