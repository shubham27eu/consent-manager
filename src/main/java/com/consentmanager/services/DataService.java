package com.consentmanager.services;

import com.consentmanager.daos.ConsentDAO;
import com.consentmanager.daos.DataItemDAO;
import com.consentmanager.models.Consent;
import com.consentmanager.models.DataItem;
// import com.consentmanager.utils.EncryptionUtil; // Will be needed later
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DataService {

    private static final Logger logger = LoggerFactory.getLogger(DataService.class);
    private DataItemDAO dataItemDAO;
    private ConsentDAO consentDAO; // Needed for access control and logging access

    // Constructor for dependency injection (testing and real use)
    public DataService(DataItemDAO dataItemDAO, ConsentDAO consentDAO) {
        this.dataItemDAO = dataItemDAO;
        this.consentDAO = consentDAO;
    }

    // Overloaded constructor for convenience when connection is managed externally for both DAOs
    public DataService(Connection connection) {
        this.dataItemDAO = new DataItemDAO(connection);
        this.consentDAO = new ConsentDAO(connection);
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

        String aesKeyEncrypted = null;
        String processedData = dataContentOrPath;

        // Simplified encryption placeholder:
        // In a real scenario:
        // 1. Generate actual AES key: String aesKey = EncryptionUtil.generateAesKeyString();
        // 2. If type is "text", encrypt dataContentOrPath with aesKey: processedData = EncryptionUtil.encryptAES(dataContentOrPath, aesKey);
        // 3. Encrypt aesKey with Provider's public key (or a master key): aesKeyEncrypted = EncryptionUtil.encryptRSA(aesKey, providerPublicKey);
        // For now, let's assume aesKeyEncrypted is either null or a placeholder if type is "file"

        if ("file".equalsIgnoreCase(type)) {
            try {
                // String rawAesKey = EncryptionUtil.generateAesKeyString(); // Placeholder
                // For now, just store a dummy placeholder for the encrypted key.
                // Real implementation would encrypt 'rawAesKey' with provider's public key.
                aesKeyEncrypted = "placeholder_encrypted_aes_key_for_" + name; // EncryptionUtil.encryptRSA(rawAesKey, providerPublicKey);
                logger.info("Generated (placeholder) encrypted AES key for file data item: {}", name);
            } catch (/*NoSuchAlgorithmException e*/ Exception e) { // General exception for placeholder
                logger.error("Error generating placeholder AES key for data item {}: {}", name, e.getMessage());
                throw new ServiceException("Failed to prepare data item due to key generation error.", e);
            }
        } else if ("text".equalsIgnoreCase(type)) {
            // Optionally encrypt text data here as well using a similar AES key mechanism
            // For simplicity now, we'll store text data as is, or a simple placeholder encryption
             try {
                // String rawAesKey = EncryptionUtil.generateAesKeyString();
                // processedData = EncryptionUtil.encryptAES(dataContentOrPath, rawAesKey);
                // aesKeyEncrypted = "placeholder_encrypted_aes_key_for_text_" + name; // Encrypt rawAesKey
                logger.info("Processing text data item (encryption placeholder): {}", name);
            } catch (/*NoSuchAlgorithmException e*/ Exception e) {
                 logger.error("Error during placeholder text encryption for data item {}: {}", name, e.getMessage());
                throw new ServiceException("Failed to prepare text data item.", e);
            }
        }


        DataItem newItem = new DataItem(providerId, name, description, type, processedData, aesKeyEncrypted);
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
        String accessedContent = "Accessed data (decryption placeholder): " + dataItem.getData();
        if ("file".equalsIgnoreCase(dataItem.getType())) {
             accessedContent += " (AES key for decryption: " + consent.getReEncryptedAesKey() + " - this would be decrypted by seeker)";
        }


        // Increment access count
        consentDAO.incrementAccessCount(consent.getConsentId());
        // Log access in ConsentHistory (to be done by ConsentService or here)

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
