package com.consentmanager.daos;

import com.consentmanager.models.SeekerBacklog;
import com.consentmanager.utils.DatabaseUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class SeekerBacklogDAOTest {

    private static final Logger logger = LoggerFactory.getLogger(SeekerBacklogDAOTest.class);
    private SeekerBacklogDAO seekerBacklogDAO;
    private Connection testConnection;

    @BeforeEach
    void setUp() throws SQLException {
        testConnection = DatabaseUtil.getTestConnection();
        seekerBacklogDAO = new SeekerBacklogDAO(testConnection);
        logger.debug("Setup complete for SeekerBacklogDAOTest with connection: {}", testConnection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testConnection != null && !testConnection.isClosed()) {
            logger.debug("Closing test connection for SeekerBacklogDAOTest: {}", testConnection);
            testConnection.close();
        }
    }

    private SeekerBacklog createTestSeekerBacklog(String usernameSuffix, String status) {
        return new SeekerBacklog("seekUser" + usernameSuffix, "hashedPass", "seeker",
                "SeekerOrg" + usernameSuffix, "Bank", "REG SEEK" + usernameSuffix,
                "seek" + usernameSuffix + "@example.com", "777888999", "456 Seeker Ave",
                "pubKeySeeker" + usernameSuffix, status);
    }

    @Test
    void testSaveSeekerBacklog_success() {
        SeekerBacklog backlog = createTestSeekerBacklog("Save", "pending");
        boolean saved = seekerBacklogDAO.saveSeekerBacklog(backlog);

        assertTrue(saved);
        assertNotNull(backlog.getBacklogId());
        assertTrue(backlog.getBacklogId() > 0);

        Optional<SeekerBacklog> retrievedOpt = seekerBacklogDAO.findById(backlog.getBacklogId());
        assertTrue(retrievedOpt.isPresent());
        assertEquals(backlog.getEmail(), retrievedOpt.get().getEmail());
        assertNotNull(retrievedOpt.get().getCreatedAt());
    }

    @Test
    void testSaveSeekerBacklog_duplicateUsername_fails() {
        SeekerBacklog backlog1 = createTestSeekerBacklog("DupUserS", "pending");
        seekerBacklogDAO.saveSeekerBacklog(backlog1);

        SeekerBacklog backlog2 = createTestSeekerBacklog("DupUserS", "pending");
        backlog2.setEmail("another.seeker.email@example.com");
        boolean saved2 = seekerBacklogDAO.saveSeekerBacklog(backlog2);
        assertFalse(saved2, "Saving with duplicate username should fail");
    }


    @Test
    void testFindById_existing_returnsBacklog() {
        SeekerBacklog backlog = createTestSeekerBacklog("FindByIdS", "pending");
        seekerBacklogDAO.saveSeekerBacklog(backlog);

        Optional<SeekerBacklog> foundOpt = seekerBacklogDAO.findById(backlog.getBacklogId());
        assertTrue(foundOpt.isPresent());
        assertEquals(backlog.getUsername(), foundOpt.get().getUsername());
    }

    @Test
    void testFindByUsername_existing_returnsBacklog() {
        SeekerBacklog backlog = createTestSeekerBacklog("FindByUserS", "approved");
        seekerBacklogDAO.saveSeekerBacklog(backlog);

        Optional<SeekerBacklog> foundOpt = seekerBacklogDAO.findByUsername(backlog.getUsername());
        assertTrue(foundOpt.isPresent());
        assertEquals(backlog.getBacklogId(), foundOpt.get().getBacklogId());
    }

    @Test
    void testFindByEmail_existing_returnsBacklog() {
        SeekerBacklog backlog = createTestSeekerBacklog("FindByEmailTestS", "rejected");
        seekerBacklogDAO.saveSeekerBacklog(backlog);

        Optional<SeekerBacklog> foundOpt = seekerBacklogDAO.findByEmail(backlog.getEmail());
        assertTrue(foundOpt.isPresent());
        assertEquals(backlog.getBacklogId(), foundOpt.get().getBacklogId());
    }


    @Test
    void testFindByStatus_returnsMatchingBacklogs() {
        seekerBacklogDAO.saveSeekerBacklog(createTestSeekerBacklog("StatusS1", "pending"));
        seekerBacklogDAO.saveSeekerBacklog(createTestSeekerBacklog("StatusS2", "approved"));
        seekerBacklogDAO.saveSeekerBacklog(createTestSeekerBacklog("StatusS3", "pending"));

        List<SeekerBacklog> pendingList = seekerBacklogDAO.findByStatus("pending");
        assertEquals(2, pendingList.size());

        List<SeekerBacklog> approvedList = seekerBacklogDAO.findByStatus("approved");
        assertEquals(1, approvedList.size());
    }

    @Test
    void testUpdateStatus_validId_updates() {
        SeekerBacklog backlog = createTestSeekerBacklog("UpdateStatusS", "pending");
        seekerBacklogDAO.saveSeekerBacklog(backlog);

        boolean updated = seekerBacklogDAO.updateStatus(backlog.getBacklogId(), "approved");
        assertTrue(updated);

        Optional<SeekerBacklog> updatedOpt = seekerBacklogDAO.findById(backlog.getBacklogId());
        assertTrue(updatedOpt.isPresent());
        assertEquals("approved", updatedOpt.get().getStatus());
    }

    @Test
    void testUpdateStatus_invalidId_returnsFalse() {
        boolean updated = seekerBacklogDAO.updateStatus(888222, "approved"); // Non-existent ID
        assertFalse(updated);
    }

     @Test
    void testCreatedAt_isParsedCorrectly() {
        SeekerBacklog backlog = createTestSeekerBacklog("TimeTestSeeker", "pending");
        seekerBacklogDAO.saveSeekerBacklog(backlog);
        Optional<SeekerBacklog> retrieved = seekerBacklogDAO.findById(backlog.getBacklogId());
        assertTrue(retrieved.isPresent());
        assertNotNull(retrieved.get().getCreatedAt());
        assertTrue(retrieved.get().getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(5)));
        assertTrue(retrieved.get().getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(5)));
    }
}
