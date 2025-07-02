package com.consentmanager.services;

import com.consentmanager.daos.ConsentDAO;
import com.consentmanager.daos.ConsentHistoryDAO;
import com.consentmanager.daos.DataItemDAO;
import com.consentmanager.daos.SeekerDAO; // To get seeker's public key
import com.consentmanager.models.Consent;
import com.consentmanager.models.ConsentHistory;
import com.consentmanager.models.DataItem;
import com.consentmanager.models.Seeker;
import com.consentmanager.utils.EncryptionUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ConsentService {

    private static final Logger logger = LoggerFactory.getLogger(ConsentService.class);
    private ConsentDAO consentDAO;
    private DataItemDAO dataItemDAO;
    private ConsentHistoryDAO consentHistoryDAO;
    private SeekerDAO seekerDAO; // To fetch seeker details like public key

    public ConsentService(Connection connection) {
        this.consentDAO = new ConsentDAO(connection);
        this.dataItemDAO = new DataItemDAO(connection);
        this.consentHistoryDAO = new ConsentHistoryDAO(connection);
        this.seekerDAO = new SeekerDAO(connection);
    }

    public ConsentService(ConsentDAO consentDAO, DataItemDAO dataItemDAO, ConsentHistoryDAO consentHistoryDAO, SeekerDAO seekerDAO) {
        this.consentDAO = consentDAO;
        this.dataItemDAO = dataItemDAO;
        this.consentHistoryDAO = consentHistoryDAO;
        this.seekerDAO = seekerDAO;
    }

    /**
     * Seeker requests consent for a data item.
     */
    public Consent requestConsent(Integer seekerId, Integer dataItemId, LocalDateTime expiresAt, Integer maxAccessCount) throws ServiceException {
        if (seekerId == null || dataItemId == null) {
            throw new ServiceException("Seeker ID and Data Item ID are required to request consent.");
        }

        Optional<DataItem> dataItemOpt = dataItemDAO.findById(dataItemId);
        if (dataItemOpt.isEmpty()) {
            throw new ServiceException("Data item with ID " + dataItemId + " not found.");
        }
        DataItem dataItem = dataItemOpt.get();

        // Check if consent already exists (pending or approved)
        Optional<Consent> existingConsentOpt = consentDAO.findByDataItemAndSeeker(dataItemId, seekerId);
        if (existingConsentOpt.isPresent()) {
            String status = existingConsentOpt.get().getStatus();
            if ("pending".equalsIgnoreCase(status) || "approved".equalsIgnoreCase(status)) {
                 logger.warn("Seeker {} already has an active or pending consent for data item {}", seekerId, dataItemId);
                return existingConsentOpt.get(); // Or throw exception
            }
        }

        Consent newConsent = new Consent();
        newConsent.setDataItemId(dataItemId);
        newConsent.setSeekerId(seekerId);
        newConsent.setProviderId(dataItem.getProviderId()); // Get provider from data item
        newConsent.setStatus("pending");
        newConsent.setRequestedAt(LocalDateTime.now()); // DAO should handle this based on schema too
        newConsent.setExpiresAt(expiresAt);
        newConsent.setMaxAccessCount(maxAccessCount);
        newConsent.setAccessCount(0);


        Consent createdConsent = consentDAO.createConsent(newConsent);
        if (createdConsent == null || createdConsent.getConsentId() == null) {
            throw new ServiceException("Failed to create consent request in database.");
        }

        // Log action
        consentHistoryDAO.logAction(new ConsentHistory(
            createdConsent.getConsentId(),
            "requested",
            seekerId,
            "seeker", // Assuming role is known or can be derived
            "Seeker requested consent."
        ));

        return createdConsent;
    }

    /**
     * Provider approves or rejects a consent request.
     */
    public boolean respondToConsent(Integer providerId, Integer consentId, String responseStatus, String rejectionDetails) throws ServiceException {
        if (providerId == null || consentId == null || responseStatus == null) {
            throw new ServiceException("Provider ID, Consent ID, and Response Status are required.");
        }
        if (!("approved".equalsIgnoreCase(responseStatus) || "rejected".equalsIgnoreCase(responseStatus))) {
            throw new ServiceException("Invalid response status. Must be 'approved' or 'rejected'.");
        }

        Optional<Consent> consentOpt = consentDAO.findById(consentId);
        if (consentOpt.isEmpty()) {
            throw new ServiceException("Consent request with ID " + consentId + " not found.");
        }
        Consent consent = consentOpt.get();

        // Verify provider owns the data item associated with the consent
        Optional<DataItem> dataItemOpt = dataItemDAO.findById(consent.getDataItemId());
        if (dataItemOpt.isEmpty() || !dataItemOpt.get().getProviderId().equals(providerId)) {
            throw new ServiceException("Provider does not own the data item for this consent request or data item not found.");
        }

        if (!"pending".equalsIgnoreCase(consent.getStatus())) {
            throw new ServiceException("Consent request is not in 'pending' state. Current status: " + consent.getStatus());
        }

        LocalDateTime approvedAt = null;
        LocalDateTime currentExpiresAt = consent.getExpiresAt(); // Preserve if already set by seeker
        String reEncryptedAesKey = consent.getReEncryptedAesKey(); // Preserve if already set (e.g. if re-requesting)

        if ("approved".equalsIgnoreCase(responseStatus)) {
            approvedAt = LocalDateTime.now();
            DataItem dataItem = dataItemOpt.get();

            if (dataItem.getAesKeyEncrypted() != null && !dataItem.getAesKeyEncrypted().isEmpty()) {
                Optional<Seeker> seekerOpt = seekerDAO.findById(consent.getSeekerId());
                if (seekerOpt.isEmpty() || seekerOpt.get().getPublicKey() == null || seekerOpt.get().getPublicKey().isBlank()) {
                    logger.error("Seeker ID {} or Seeker's public key not found/empty for consent approval of item ID {}.", consent.getSeekerId(), dataItem.getDataItemId());
                    throw new ServiceException("Seeker's public key not found or empty, cannot approve consent for this encrypted data item.");
                }
                String seekerPublicKeyStr = seekerOpt.get().getPublicKey();

                try {
                    // 1. Decrypt DataItem's AES key (which was encrypted with System/Provider's public key)
                    // For this, we need the System/Provider's private key.
                    // Using the static system private key from EncryptionUtil for now.
                    String dataItemAesKey = EncryptionUtil.decryptRSA(dataItem.getAesKeyEncrypted(), EncryptionUtil.getSystemRsaPrivateKeyString());

                    // 2. Re-encrypt this AES key with the Seeker's public key
                    reEncryptedAesKey = EncryptionUtil.encryptRSA(dataItemAesKey, seekerPublicKeyStr);
                    logger.info("Successfully re-encrypted AES key for consent ID {} using seeker's public key.", consentId);

                } catch (Exception e) {
                    logger.error("Error during AES key re-encryption for consent ID {}: {}", consentId, e.getMessage(), e);
                    throw new ServiceException("Failed to process encryption keys for consent approval.", e);
                }
            } else {
                 logger.warn("Data item {} for consent {} has no encrypted AES key; re-encryption step skipped.", dataItem.getDataItemId(), consentId);
                 reEncryptedAesKey = null; // Ensure it's null if not applicable
            }
        }

        boolean success = consentDAO.updateConsentStatus(consentId, responseStatus, approvedAt, currentExpiresAt, reEncryptedAesKey);
        if (success) {
            consentHistoryDAO.logAction(new ConsentHistory(
                consentId,
                responseStatus,
                providerId,
                "provider",
                "approved".equalsIgnoreCase(responseStatus) ? "Consent approved by provider." : "Consent rejected by provider. Details: " + rejectionDetails
            ));
        } else {
            throw new ServiceException("Failed to update consent status in database.");
        }
        return success;
    }

    public List<Consent> getConsentsByProviderAndStatus(Integer providerId, String status) throws ServiceException {
        if (providerId == null || status == null) {
            throw new ServiceException("Provider ID and status are required.");
        }
        return consentDAO.findByProviderIdAndStatus(providerId, status);
    }

    public List<Consent> getConsentsBySeeker(Integer seekerId) throws ServiceException {
        if (seekerId == null) {
            throw new ServiceException("Seeker ID is required.");
        }
        return consentDAO.findBySeekerId(seekerId);
    }

    public Optional<Consent> getConsentById(Integer consentId) throws ServiceException {
        if (consentId == null) {
            throw new ServiceException("Consent ID is required.");
        }
        return consentDAO.findById(consentId);
    }
}
