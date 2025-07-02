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

import static org.junit.jupiter.api.Assertions.*;

public class ConsentHistoryDAOTest {

    private static final Logger logger = LoggerFactory.getLogger(ConsentHistoryDAOTest.class);
    private Connection testConnection;
    private ConsentHistoryDAO consentHistoryDAO;
    private ConsentDAO consentDAO;
    private DataItemDAO dataItemDAO;
    private ProviderDAO providerDAO;
    private SeekerDAO seekerDAO;
    private CredentialDAO credentialDAO;

    private int testConsentId;
    private int testSeekerCredentialId;
    private int testProviderCredentialId;


    @BeforeEach
    void setUp() throws SQLException {
        testConnection = DatabaseUtil.getTestConnection();
        consentHistoryDAO = new ConsentHistoryDAO(testConnection);
        consentDAO = new ConsentDAO(testConnection);
        dataItemDAO = new DataItemDAO(testConnection);
        providerDAO = new ProviderDAO(testConnection);
        seekerDAO = new SeekerDAO(testConnection);
        credentialDAO = new CredentialDAO(testConnection);

        // Prereqs: Provider
        Credential provCred = new Credential("provHistTest", "pass", "provider");
        testProviderCredentialId = credentialDAO.saveCredential(provCred);
        Provider provider = new Provider(testProviderCredentialId, "Test", "Hist", "User", "provh@example.com",
                "123", LocalDate.now(), 30, "pubKeyProvH", true);
        providerDAO.saveProvider(provider);

        // Prereqs: Seeker
        Credential seekerCred = new Credential("seekerHistTest", "pass", "seeker");
        testSeekerCredentialId = credentialDAO.saveCredential(seekerCred);
        Seeker seeker = new Seeker(testSeekerCredentialId, "SeekerOrgTestH", "Research", "REGSH123",
                "seekerh@example.com", "987", "AddrH", "pubKeySeekerH", true);
        seekerDAO.saveSeeker(seeker);

        // Prereqs: DataItem
        DataItem dataItem = new DataItem(provider.getProviderId(), "Patient Survey", "Health Questionnaire", "text", "data...", null);
        dataItemDAO.saveDataItem(dataItem);

        // Prereqs: Consent
        Consent consent = new Consent(dataItem.getDataItemId(), seeker.getSeekerId(), provider.getProviderId(),
                                      "pending", null, null, null, null,0,null);
        Consent createdConsent = consentDAO.createConsent(consent);
        assertNotNull(createdConsent);
        testConsentId = createdConsent.getConsentId();
        assertNotNull(testConsentId);

        logger.debug("Setup complete for ConsentHistoryDAOTest.");
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testConnection != null && !testConnection.isClosed()) {
            logger.debug("Closing test connection for ConsentHistoryDAOTest.");
            testConnection.close();
        }
    }

    @Test
    void testLogActionAndFindByConsentId() {
        ConsentHistory entry1 = new ConsentHistory(testConsentId, "requested", testSeekerCredentialId, "seeker", "Initial request by seeker.");
        boolean logged1 = consentHistoryDAO.logAction(entry1);
        assertTrue(logged1, "First action should be logged successfully.");

        // Add a small delay to ensure timestamps are different for ordering tests
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        ConsentHistory entry2 = new ConsentHistory(testConsentId, "approved", testProviderCredentialId, "provider", "Approved by provider.");
        boolean logged2 = consentHistoryDAO.logAction(entry2);
        assertTrue(logged2, "Second action should be logged successfully.");

        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        ConsentHistory entry3 = new ConsentHistory(testConsentId, "accessed", testSeekerCredentialId, "seeker", "Data accessed by seeker.");
        boolean logged3 = consentHistoryDAO.logAction(entry3);
        assertTrue(logged3, "Third action should be logged successfully.");


        List<ConsentHistory> historyList = consentHistoryDAO.findByConsentId(testConsentId);
        assertEquals(3, historyList.size(), "Should retrieve 3 history entries for the consent ID.");

        // Verify order (DESC by timestamp)
        assertNotNull(historyList.get(0).getTimestamp());
        assertNotNull(historyList.get(1).getTimestamp());
        assertNotNull(historyList.get(2).getTimestamp());

        assertTrue(historyList.get(0).getTimestamp().isAfter(historyList.get(1).getTimestamp()) || historyList.get(0).getTimestamp().isEqual(historyList.get(1).getTimestamp()), "Entry 0 should be newer or same as Entry 1");
        assertTrue(historyList.get(1).getTimestamp().isAfter(historyList.get(2).getTimestamp()) || historyList.get(1).getTimestamp().isEqual(historyList.get(2).getTimestamp()), "Entry 1 should be newer or same as Entry 2");


        assertEquals("accessed", historyList.get(0).getAction());
        assertEquals(testSeekerCredentialId, historyList.get(0).getActorId());

        assertEquals("approved", historyList.get(1).getAction());
        assertEquals(testProviderCredentialId, historyList.get(1).getActorId());

        assertEquals("requested", historyList.get(2).getAction());
        assertEquals(testSeekerCredentialId, historyList.get(2).getActorId());
    }

    @Test
    void testFindByConsentId_noHistory() {
        List<ConsentHistory> historyList = consentHistoryDAO.findByConsentId(testConsentId + 999); // Non-existent or no history consent
        assertTrue(historyList.isEmpty(), "Should return empty list for a consent ID with no history.");
    }
     @Test
    void testLogAction_nullActorId() {
        ConsentHistory entrySystem = new ConsentHistory(testConsentId, "expired", null, "system", "Consent expired due to policy.");
        boolean loggedSystem = consentHistoryDAO.logAction(entrySystem);
        assertTrue(loggedSystem, "System action with null actorId should be logged successfully.");

        List<ConsentHistory> historyList = consentHistoryDAO.findByConsentId(testConsentId);
        assertEquals(1, historyList.size());
        assertEquals("expired", historyList.get(0).getAction());
        assertNull(historyList.get(0).getActorId());
        assertEquals("system", historyList.get(0).getPerformedByRole());
    }
}
