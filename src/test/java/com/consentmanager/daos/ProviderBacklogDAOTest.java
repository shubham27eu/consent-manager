package com.consentmanager.daos;

import com.consentmanager.models.ProviderBacklog;
import com.consentmanager.utils.DatabaseUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ProviderBacklogDAOTest {

    private static final Logger logger = LoggerFactory.getLogger(ProviderBacklogDAOTest.class);
    private ProviderBacklogDAO providerBacklogDAO;
    private Connection testConnection;

    @BeforeEach
    void setUp() throws SQLException {
        testConnection = DatabaseUtil.getTestConnection();
        providerBacklogDAO = new ProviderBacklogDAO(testConnection);
        logger.debug("Setup complete for ProviderBacklogDAOTest with connection: {}", testConnection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testConnection != null && !testConnection.isClosed()) {
            logger.debug("Closing test connection for ProviderBacklogDAOTest: {}", testConnection);
            testConnection.close();
        }
    }

    private ProviderBacklog createTestProviderBacklog(String usernameSuffix, String status) {
        return new ProviderBacklog("provUser" + usernameSuffix, "hashedPass", "provider",
                "FirstName", "M", "LastName", "prov" + usernameSuffix + "@example.com",
                LocalDate.of(1988, 4, 4), "111222333", 35, "Female",
                "pubKey" + usernameSuffix, status);
    }

    @Test
    void testSaveProviderBacklog_success() {
        ProviderBacklog backlog = createTestProviderBacklog("Save", "pending");
        boolean saved = providerBacklogDAO.saveProviderBacklog(backlog);

        assertTrue(saved);
        assertNotNull(backlog.getBacklogId());
        assertTrue(backlog.getBacklogId() > 0);

        Optional<ProviderBacklog> retrievedOpt = providerBacklogDAO.findById(backlog.getBacklogId());
        assertTrue(retrievedOpt.isPresent());
        assertEquals(backlog.getEmail(), retrievedOpt.get().getEmail());
        assertNotNull(retrievedOpt.get().getCreatedAt(), "CreatedAt should be set by DB default or DAO");
    }

    @Test
    void testSaveProviderBacklog_duplicateUsername_fails() {
        ProviderBacklog backlog1 = createTestProviderBacklog("DupUser", "pending");
        providerBacklogDAO.saveProviderBacklog(backlog1);

        ProviderBacklog backlog2 = createTestProviderBacklog("DupUser", "pending");
        backlog2.setEmail("another.email@example.com"); // Different email, same username
        boolean saved2 = providerBacklogDAO.saveProviderBacklog(backlog2);
        assertFalse(saved2, "Saving with duplicate username should fail due to DB constraint");
    }


    @Test
    void testFindById_existing_returnsBacklog() {
        ProviderBacklog backlog = createTestProviderBacklog("FindById", "pending");
        providerBacklogDAO.saveProviderBacklog(backlog);

        Optional<ProviderBacklog> foundOpt = providerBacklogDAO.findById(backlog.getBacklogId());
        assertTrue(foundOpt.isPresent());
        assertEquals(backlog.getUsername(), foundOpt.get().getUsername());
    }

    @Test
    void testFindByUsername_existing_returnsBacklog() {
        ProviderBacklog backlog = createTestProviderBacklog("FindByUser", "approved");
        providerBacklogDAO.saveProviderBacklog(backlog);

        Optional<ProviderBacklog> foundOpt = providerBacklogDAO.findByUsername(backlog.getUsername());
        assertTrue(foundOpt.isPresent());
        assertEquals(backlog.getBacklogId(), foundOpt.get().getBacklogId());
    }

    @Test
    void testFindByEmail_existing_returnsBacklog() {
        ProviderBacklog backlog = createTestProviderBacklog("FindByEmailTest", "rejected");
        providerBacklogDAO.saveProviderBacklog(backlog);

        Optional<ProviderBacklog> foundOpt = providerBacklogDAO.findByEmail(backlog.getEmail());
        assertTrue(foundOpt.isPresent());
        assertEquals(backlog.getBacklogId(), foundOpt.get().getBacklogId());
    }


    @Test
    void testFindByStatus_returnsMatchingBacklogs() {
        providerBacklogDAO.saveProviderBacklog(createTestProviderBacklog("Status1", "pending"));
        providerBacklogDAO.saveProviderBacklog(createTestProviderBacklog("Status2", "approved"));
        providerBacklogDAO.saveProviderBacklog(createTestProviderBacklog("Status3", "pending"));

        List<ProviderBacklog> pendingList = providerBacklogDAO.findByStatus("pending");
        assertEquals(2, pendingList.size());
        assertTrue(pendingList.stream().allMatch(b -> b.getStatus().equals("pending")));
        // Check order by created_at (tricky to assert precisely without known timestamps, but check size)
         if (pendingList.size() == 2) {
            assertTrue(pendingList.get(0).getCreatedAt() == null || pendingList.get(1).getCreatedAt() == null ||
                       !pendingList.get(0).getCreatedAt().isAfter(pendingList.get(1).getCreatedAt()));
        }


        List<ProviderBacklog> approvedList = providerBacklogDAO.findByStatus("approved");
        assertEquals(1, approvedList.size());
    }

    @Test
    void testUpdateStatus_validId_updates() {
        ProviderBacklog backlog = createTestProviderBacklog("UpdateStatus", "pending");
        providerBacklogDAO.saveProviderBacklog(backlog);

        boolean updated = providerBacklogDAO.updateStatus(backlog.getBacklogId(), "approved");
        assertTrue(updated);

        Optional<ProviderBacklog> updatedOpt = providerBacklogDAO.findById(backlog.getBacklogId());
        assertTrue(updatedOpt.isPresent());
        assertEquals("approved", updatedOpt.get().getStatus());
    }

    @Test
    void testUpdateStatus_invalidId_returnsFalse() {
        boolean updated = providerBacklogDAO.updateStatus(999111, "approved"); // Non-existent ID
        assertFalse(updated);
    }

    @Test
    void testCreatedAt_isParsedCorrectly() {
        ProviderBacklog backlog = createTestProviderBacklog("TimeTest", "pending");
        // Manually set a known created_at for testing parsing, if DAO allowed it,
        // but here we rely on DB default. So we fetch and check it's not null.
        providerBacklogDAO.saveProviderBacklog(backlog);
        Optional<ProviderBacklog> retrieved = providerBacklogDAO.findById(backlog.getBacklogId());
        assertTrue(retrieved.isPresent());
        assertNotNull(retrieved.get().getCreatedAt());
        // Check if it's close to now, e.g., within the last few seconds
        assertTrue(retrieved.get().getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(5)));
        assertTrue(retrieved.get().getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(5)));
    }
}
