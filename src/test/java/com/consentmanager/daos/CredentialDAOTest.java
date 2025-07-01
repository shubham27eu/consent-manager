package com.consentmanager.daos;

import com.consentmanager.models.Credential;
import com.consentmanager.utils.DatabaseUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CredentialDAOTest {

    private static final Logger logger = LoggerFactory.getLogger(CredentialDAOTest.class);
    private CredentialDAO credentialDAO;
    private Connection testConnection;

    @BeforeEach
    void setUp() throws SQLException {
        // Get a fresh, schema-initialized in-memory connection for each test
        testConnection = DatabaseUtil.getTestConnection();
        credentialDAO = new CredentialDAO(testConnection); // Inject connection
        logger.debug("Setup complete for test with connection: {}", testConnection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testConnection != null && !testConnection.isClosed()) {
            logger.debug("Closing test connection: {}", testConnection);
            testConnection.close();
        }
    }

    @Test
    void testSaveCredential_success() {
        Credential credential = new Credential("testUser", "hashedPassword", "provider");
        int generatedId = credentialDAO.saveCredential(credential);

        assertTrue(generatedId > 0, "Generated ID should be positive on successful save.");
        // The DAO's saveCredential method returns the ID but does not set it back on the input object.
        // Thus, credential.getCredentialId() would still be null or its initial value.
        // The check that the ID is correct is implicitly done by fetching using the generatedId.

        Optional<Credential> savedCredentialOpt = credentialDAO.findById(generatedId);
        assertTrue(savedCredentialOpt.isPresent(), "Saved credential should be findable by ID.");
        assertEquals("testUser", savedCredentialOpt.get().getUsername());
    }

    @Test
    void testSaveCredential_duplicateUsername_failsGracefully() {
        // This test relies on DB constraints. The DAO itself might not check for duplicates before insert.
        // The DB will throw an exception which should be caught and logged by DAO, returning -1.
        Credential credential1 = new Credential("duplicateUser", "pass1", "seeker");
        credentialDAO.saveCredential(credential1);

        Credential credential2 = new Credential("duplicateUser", "pass2", "admin");
        int generatedId2 = credentialDAO.saveCredential(credential2);

        assertEquals(-1, generatedId2, "Saving credential with duplicate username should fail and return -1.");
    }


    @Test
    void testFindByUsername_existingUser_returnsCredential() {
        Credential credential = new Credential("findMe", "password123", "admin");
        credentialDAO.saveCredential(credential);

        Optional<Credential> foundOpt = credentialDAO.findByUsername("findMe");
        assertTrue(foundOpt.isPresent(), "Credential should be found by username.");
        assertEquals("findMe", foundOpt.get().getUsername());
        assertEquals("password123", foundOpt.get().getPassword());
        assertEquals("admin", foundOpt.get().getRole());
    }

    @Test
    void testFindByUsername_nonExistingUser_returnsEmpty() {
        Optional<Credential> foundOpt = credentialDAO.findByUsername("nonExistentUser");
        assertFalse(foundOpt.isPresent(), "Should not find a non-existent user.");
    }

    @Test
    void testFindById_existingUser_returnsCredential() {
        Credential credential = new Credential("userById", "securePass", "provider");
        int id = credentialDAO.saveCredential(credential);
        assertTrue(id > 0);

        Optional<Credential> foundOpt = credentialDAO.findById(id);
        assertTrue(foundOpt.isPresent(), "Credential should be found by ID.");
        assertEquals(id, foundOpt.get().getCredentialId().intValue());
        assertEquals("userById", foundOpt.get().getUsername());
    }

    @Test
    void testFindById_nonExistingId_returnsEmpty() {
        Optional<Credential> foundOpt = credentialDAO.findById(99999); // Assuming this ID won't exist
        assertFalse(foundOpt.isPresent(), "Should not find a credential with a non-existent ID.");
    }
}
