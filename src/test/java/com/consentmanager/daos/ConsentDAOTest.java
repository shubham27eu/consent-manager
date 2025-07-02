package com.consentmanager.daos;

import com.consentmanager.models.*;
import com.consentmanager.utils.DatabaseUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ConsentDAOTest {

    private static final Logger logger = LoggerFactory.getLogger(ConsentDAOTest.class);
    private Connection testConnection;
    private ConsentDAO consentDAO;
    private DataItemDAO dataItemDAO;
    private ProviderDAO providerDAO;
    private SeekerDAO seekerDAO;
    private CredentialDAO credentialDAO;

    private int testProviderCredentialId;
    private int testProviderId;
    private int testSeekerCredentialId;
    private int testSeekerId;
    private int testDataItemId;

    @BeforeEach
    void setUp() throws SQLException {
        testConnection = DatabaseUtil.getTestConnection();
        consentDAO = new ConsentDAO(testConnection);
        dataItemDAO = new DataItemDAO(testConnection);
        providerDAO = new ProviderDAO(testConnection);
        seekerDAO = new SeekerDAO(testConnection);
        credentialDAO = new CredentialDAO(testConnection);

        // Prerequisite: Credential for Provider
        Credential provCred = new Credential("provConsentTest", "pass", "provider");
        testProviderCredentialId = credentialDAO.saveCredential(provCred);
        assertTrue(testProviderCredentialId > 0);

        // Prerequisite: Provider
        Provider provider = new Provider(testProviderCredentialId, "Test", "ProvC", "User", "provc@example.com",
                "123", LocalDate.now(), 30, "pubKeyProvC", true);
        assertTrue(providerDAO.saveProvider(provider));
        testProviderId = provider.getProviderId();
        assertNotNull(testProviderId);

        // Prerequisite: Credential for Seeker
        Credential seekerCred = new Credential("seekerConsentTest", "pass", "seeker");
        testSeekerCredentialId = credentialDAO.saveCredential(seekerCred);
        assertTrue(testSeekerCredentialId > 0);

        // Prerequisite: Seeker
        Seeker seeker = new Seeker(testSeekerCredentialId, "SeekerOrgTestC", "Hospital", "REGSC123",
                "seekerc@example.com", "987", "Addr", "pubKeySeekerC", true);
        assertTrue(seekerDAO.saveSeeker(seeker));
        testSeekerId = seeker.getSeekerId();
        assertNotNull(testSeekerId);

        // Prerequisite: DataItem
        DataItem dataItem = new DataItem(testProviderId, "Medical Report", "X-Ray Result", "file", "/path/xray.jpg", "key-for-xray");
        DataItem savedDataItem = dataItemDAO.saveDataItem(dataItem);
        assertNotNull(savedDataItem);
        testDataItemId = savedDataItem.getDataItemId();
        assertNotNull(testDataItemId);

        logger.debug("Setup complete for ConsentDAOTest.");
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testConnection != null && !testConnection.isClosed()) {
            logger.debug("Closing test connection for ConsentDAOTest.");
            testConnection.close();
        }
    }

    @Test
    void testCreateAndFindById() {
        Consent consent = new Consent(testDataItemId, testSeekerId, testProviderId, "pending", null,
                                      null, null, LocalDateTime.now().plusDays(30), 0, 10);
        Consent createdConsent = consentDAO.createConsent(consent);

        assertNotNull(createdConsent, "Created consent should not be null.");
        assertNotNull(createdConsent.getConsentId(), "Consent ID should be set.");
        assertTrue(createdConsent.getConsentId() > 0);
        assertNotNull(createdConsent.getRequestedAt(), "RequestedAt should be set by DB/DAO.");
        assertEquals(0, createdConsent.getAccessCount());


        Optional<Consent> foundOpt = consentDAO.findById(createdConsent.getConsentId());
        assertTrue(foundOpt.isPresent(), "Should find consent by ID.");
        assertEquals(testDataItemId, foundOpt.get().getDataItemId());
        assertEquals(testSeekerId, foundOpt.get().getSeekerId());
    }

    @Test
    void testFindByDataItemAndSeeker() {
        Consent consent = new Consent(testDataItemId, testSeekerId, testProviderId, "pending", null,
                                      null, null, LocalDateTime.now().plusDays(30), 0, 10);
        consentDAO.createConsent(consent);

        Optional<Consent> foundOpt = consentDAO.findByDataItemAndSeeker(testDataItemId, testSeekerId);
        assertTrue(foundOpt.isPresent(), "Should find consent by data item and seeker ID.");
        assertEquals(consent.getConsentId(), foundOpt.get().getConsentId());

        Optional<Consent> notFoundOpt = consentDAO.findByDataItemAndSeeker(testDataItemId, testSeekerId + 1); // Different seeker
        assertFalse(notFoundOpt.isPresent(), "Should not find consent for different seeker.");
    }


    @Test
    void testUpdateConsentStatus_Approve() {
        Consent consent = new Consent(testDataItemId, testSeekerId, testProviderId, "pending", null, null, null, null, 0, null);
        Consent createdConsent = consentDAO.createConsent(consent);
        assertNotNull(createdConsent);

        LocalDateTime approvedAt = LocalDateTime.now();
        LocalDateTime expiresAt = LocalDateTime.now().plusMonths(1);
        String reEncryptedKey = "reEncryptedKeyForSeeker";

        boolean updated = consentDAO.updateConsentStatus(createdConsent.getConsentId(), "approved", approvedAt, expiresAt, reEncryptedKey);
        assertTrue(updated, "Consent status update to approved should succeed.");

        Optional<Consent> updatedConsentOpt = consentDAO.findById(createdConsent.getConsentId());
        assertTrue(updatedConsentOpt.isPresent());
        Consent updatedConsent = updatedConsentOpt.get();
        assertEquals("approved", updatedConsent.getStatus());
        assertNotNull(updatedConsent.getApprovedAt());
        assertEquals(reEncryptedKey, updatedConsent.getReEncryptedAesKey());
        assertNotNull(updatedConsent.getExpiresAt());
    }

    @Test
    void testUpdateConsentStatus_Reject() {
        Consent consent = new Consent(testDataItemId, testSeekerId, testProviderId, "pending", null, null, null, null, 0, null);
        Consent createdConsent = consentDAO.createConsent(consent);
        assertNotNull(createdConsent);

        boolean updated = consentDAO.updateConsentStatus(createdConsent.getConsentId(), "rejected", null, null, null);
        assertTrue(updated, "Consent status update to rejected should succeed.");

        Optional<Consent> updatedConsentOpt = consentDAO.findById(createdConsent.getConsentId());
        assertTrue(updatedConsentOpt.isPresent());
        assertEquals("rejected", updatedConsentOpt.get().getStatus());
        assertNull(updatedConsentOpt.get().getApprovedAt()); // Should remain null or not be set for rejection
    }


    @Test
    void testIncrementAccessCount() {
        Consent consent = new Consent(testDataItemId, testSeekerId, testProviderId, "approved", "key",
                                      LocalDateTime.now().minusDays(1), LocalDateTime.now().minusHours(1),
                                      LocalDateTime.now().plusDays(10), 0, 5);
        Consent createdConsent = consentDAO.createConsent(consent);
        assertNotNull(createdConsent);
        assertEquals(0, createdConsent.getAccessCount());

        boolean incremented = consentDAO.incrementAccessCount(createdConsent.getConsentId());
        assertTrue(incremented);

        Optional<Consent> accessedConsentOpt = consentDAO.findById(createdConsent.getConsentId());
        assertTrue(accessedConsentOpt.isPresent());
        assertEquals(1, accessedConsentOpt.get().getAccessCount());
    }

    @Test
    void testFindByProviderIdAndStatus() {
        consentDAO.createConsent(new Consent(testDataItemId, testSeekerId, testProviderId, "pending", null, null, null, null, 0, null));

        // Create another seeker and data item for variety
        Credential seekerCred2 = new Credential("seeker2ConsentTest", "pass", "seeker");
        int seekerCredId2 = credentialDAO.saveCredential(seekerCred2);
        Seeker seeker2 = new Seeker(seekerCredId2, "SeekerOrgTestC2", "Research", "REGSC456", "seeker2c@example.com", "988", "Addr2", "pubKeySeekerC2", true);
        providerDAO.saveProvider(new Provider(testProviderCredentialId, "P","P","P","p@p.com","1",LocalDate.now(),1,"pk",true)); // ensure provider exists
        seekerDAO.saveSeeker(seeker2); // Ensure seeker exists

        DataItem dataItem2 = new DataItem(testProviderId, "Lab Report", "Blood Test", "text", "AB+", null);
        dataItemDAO.saveDataItem(dataItem2);

        Consent approvedConsent = new Consent(dataItem2.getDataItemId(), seeker2.getSeekerId(), testProviderId, "approved", "key2", null, LocalDateTime.now(), null,0,null);
        consentDAO.createConsent(approvedConsent);

        List<Consent> pendingConsents = consentDAO.findByProviderIdAndStatus(testProviderId, "pending");
        assertEquals(1, pendingConsents.size());
        assertEquals("pending", pendingConsents.get(0).getStatus());

        List<Consent> approvedConsents = consentDAO.findByProviderIdAndStatus(testProviderId, "approved");
        assertEquals(1, approvedConsents.size());
        assertEquals("approved", approvedConsents.get(0).getStatus());
    }

    @Test
    void testFindBySeekerId() {
        consentDAO.createConsent(new Consent(testDataItemId, testSeekerId, testProviderId, "approved", "key1", null, LocalDateTime.now(), null,0,null));

        DataItem dataItem2 = new DataItem(testProviderId, "Consultation Notes", "Follow-up", "text", "Notes...", null);
        dataItemDAO.saveDataItem(dataItem2);
        consentDAO.createConsent(new Consent(dataItem2.getDataItemId(), testSeekerId, testProviderId, "pending", null, null, null, null,0,null));

        List<Consent> seekerConsents = consentDAO.findBySeekerId(testSeekerId);
        assertEquals(2, seekerConsents.size());
        assertTrue(seekerConsents.stream().allMatch(c -> c.getSeekerId().equals(testSeekerId)));
    }
}
