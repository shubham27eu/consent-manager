package com.consentmanager.daos;

import com.consentmanager.models.Credential;
import com.consentmanager.models.Seeker;
import com.consentmanager.utils.DatabaseUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class SeekerDAOTest {

    private static final Logger logger = LoggerFactory.getLogger(SeekerDAOTest.class);
    private SeekerDAO seekerDAO;
    private CredentialDAO credentialDAO;
    private Connection testConnection;

    @BeforeEach
    void setUp() throws SQLException {
        testConnection = DatabaseUtil.getTestConnection();
        seekerDAO = new SeekerDAO(testConnection);
        credentialDAO = new CredentialDAO(testConnection);
        logger.debug("Setup complete for SeekerDAOTest with connection: {}", testConnection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testConnection != null && !testConnection.isClosed()) {
            logger.debug("Closing test connection for SeekerDAOTest: {}", testConnection);
            testConnection.close();
        }
    }

    private int createTestCredential(String username) {
        // Uses the test-specific credentialDAO
        Credential credential = new Credential(username, "testPassword", "seeker");
        return credentialDAO.saveCredential(credential);
    }

    private Seeker createAndSaveTestSeeker(String usernameSuffix, boolean isActive) {
        int credId = createTestCredential("seekerUser" + usernameSuffix);
        assertTrue(credId > 0);
        Seeker seeker = new Seeker(credId, "SeekerOrg" + usernameSuffix, "Bank",
                "REG123" + usernameSuffix, "seeker" + usernameSuffix + "@example.com",
                "987654321", "123 Seeker St", "seekerPublicKey" + usernameSuffix, isActive);
        boolean saved = seekerDAO.saveSeeker(seeker);
        assertTrue(saved);
        assertNotNull(seeker.getSeekerId());
        return seeker;
    }

    @Test
    void testSaveSeeker_success() {
        Seeker seeker = createAndSaveTestSeeker("1", true);
        Optional<Seeker> savedSeekerOpt = seekerDAO.findById(seeker.getSeekerId());
        assertTrue(savedSeekerOpt.isPresent());
        assertEquals(seeker.getEmail(), savedSeekerOpt.get().getEmail());
        assertEquals(seeker.getIsActive(), savedSeekerOpt.get().getIsActive());
    }

    @Test
    void testFindByEmail_existing_returnsSeeker() {
        Seeker seeker = createAndSaveTestSeeker("FindByEmail", true);
        Optional<Seeker> foundOpt = seekerDAO.findByEmail(seeker.getEmail());
        assertTrue(foundOpt.isPresent());
        assertEquals(seeker.getSeekerId(), foundOpt.get().getSeekerId());
    }


    @Test
    void testFindByCredentialId_existing_returnsSeeker() {
        Seeker seeker = createAndSaveTestSeeker("2", true);
        Optional<Seeker> foundOpt = seekerDAO.findByCredentialId(seeker.getCredentialId());
        assertTrue(foundOpt.isPresent());
        assertEquals(seeker.getSeekerId(), foundOpt.get().getSeekerId());
    }

    @Test
    void testFindById_nonExisting_returnsEmpty() {
        Optional<Seeker> foundOpt = seekerDAO.findById(99999);
        assertFalse(foundOpt.isPresent());
    }

    @Test
    void testUpdateActiveStatus_validId_updatesStatus() {
        Seeker seeker = createAndSaveTestSeeker("3", true);
        boolean updated = seekerDAO.updateActiveStatus(seeker.getSeekerId(), false);
        assertTrue(updated);

        Optional<Seeker> updatedSeekerOpt = seekerDAO.findById(seeker.getSeekerId());
        assertTrue(updatedSeekerOpt.isPresent());
        assertFalse(updatedSeekerOpt.get().getIsActive());
    }

    @Test
    void testUpdateActiveStatus_invalidId_returnsFalse() {
        boolean updated = seekerDAO.updateActiveStatus(123456, true); // Non-existent ID
        assertFalse(updated);
    }

    @Test
    void testFindAllByStatus_findsActiveAndInactive() {
        createAndSaveTestSeeker("Active1", true);
        createAndSaveTestSeeker("Inactive1", false);
        createAndSaveTestSeeker("Active2", true);

        List<Seeker> activeSeekers = seekerDAO.findAllByStatus(true);
        assertEquals(2, activeSeekers.size());

        List<Seeker> inactiveSeekers = seekerDAO.findAllByStatus(false);
        assertEquals(1, inactiveSeekers.size());
    }
}
