package com.consentmanager.daos;

import com.consentmanager.models.Admin;
import com.consentmanager.models.Credential;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AdminDAOTest {

    private static final Logger logger = LoggerFactory.getLogger(AdminDAOTest.class);
    private AdminDAO adminDAO;
    private CredentialDAO credentialDAO;
    private Connection testConnection;

    @BeforeEach
    void setUp() throws SQLException {
        testConnection = DatabaseUtil.getTestConnection(); // Gets a fresh, schema-initialized DB connection
        adminDAO = new AdminDAO(testConnection);
        credentialDAO = new CredentialDAO(testConnection); // DAOs share the same test connection
        logger.debug("Setup complete for AdminDAOTest with connection: {}", testConnection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testConnection != null && !testConnection.isClosed()) {
            logger.debug("Closing test connection for AdminDAOTest: {}", testConnection);
            testConnection.close();
        }
    }

    private int createTestCredential(String username, String role) {
        // This method now uses the test-specific credentialDAO
        Credential credential = new Credential(username, "testPassword", role);
        return credentialDAO.saveCredential(credential);
    }

    @Test
    void testSaveAdmin_success() {
        int credId = createTestCredential("adminUser1", "admin");
        assertTrue(credId > 0);

        Admin admin = new Admin(credId, "John", "M", "Doe", "john.doe@example.com", "1234567890", LocalDate.of(1980, 1, 1));
        boolean saved = adminDAO.saveAdmin(admin);

        assertTrue(saved, "Admin should be saved successfully.");
        assertNotNull(admin.getAdminId(), "Admin ID should be set after saving.");
        assertTrue(admin.getAdminId() > 0, "Admin ID should be positive.");

        Optional<Admin> savedAdminOpt = adminDAO.findById(admin.getAdminId());
        assertTrue(savedAdminOpt.isPresent(), "Saved admin should be findable by ID.");
        assertEquals("john.doe@example.com", savedAdminOpt.get().getEmail());
    }

    @Test
    void testSaveAdmin_duplicateEmail_failsGracefully() {
        int credId1 = createTestCredential("adminUser2", "admin");
        Admin admin1 = new Admin(credId1, "Jane", null, "Doe", "jane.doe@example.com", "0987654321", LocalDate.of(1985, 5, 5));
        adminDAO.saveAdmin(admin1);

        int credId2 = createTestCredential("adminUser3", "admin");
        // Attempt to save another admin with the same email
        Admin admin2 = new Admin(credId2, "Janet", null, "Doe", "jane.doe@example.com", "1122334455", LocalDate.of(1990, 10, 10));
        boolean saved2 = adminDAO.saveAdmin(admin2);

        assertFalse(saved2, "Saving admin with duplicate email should fail.");
    }

    @Test
    void testSaveAdmin_duplicateCredentialId_failsGracefully() {
        int credId = createTestCredential("adminUser4", "admin");
        Admin admin1 = new Admin(credId, "First", null, "Admin", "first.admin@example.com", "123", LocalDate.now());
        adminDAO.saveAdmin(admin1);

        // Attempt to save another admin with the same credential_id
        Admin admin2 = new Admin(credId, "Second", null, "Admin", "second.admin@example.com", "456", LocalDate.now());
        boolean saved2 = adminDAO.saveAdmin(admin2);
        assertFalse(saved2, "Saving admin with duplicate credential_id should fail.");
    }


    @Test
    void testFindByCredentialId_existingAdmin_returnsAdmin() {
        int credId = createTestCredential("adminUser5", "admin");
        Admin admin = new Admin(credId, "Find", "By", "Cred", "find.by.cred@example.com", "555", LocalDate.of(1970, 3, 15));
        adminDAO.saveAdmin(admin);
        assertNotNull(admin.getAdminId());

        Optional<Admin> foundOpt = adminDAO.findByCredentialId(credId);
        assertTrue(foundOpt.isPresent(), "Admin should be found by credential ID.");
        assertEquals(admin.getAdminId(), foundOpt.get().getAdminId());
        assertEquals("find.by.cred@example.com", foundOpt.get().getEmail());
    }

    @Test
    void testFindByCredentialId_nonExisting_returnsEmpty() {
        Optional<Admin> foundOpt = adminDAO.findByCredentialId(99999); // Non-existent credential ID
        assertFalse(foundOpt.isPresent());
    }

    @Test
    void testFindById_existingAdmin_returnsAdmin() {
        int credId = createTestCredential("adminUser6", "admin");
        Admin admin = new Admin(credId, "Find", "By", "AdminId", "find.by.id@example.com", "666", LocalDate.of(1975, 7, 25));
        adminDAO.saveAdmin(admin);
        assertNotNull(admin.getAdminId());

        Optional<Admin> foundOpt = adminDAO.findById(admin.getAdminId());
        assertTrue(foundOpt.isPresent(), "Admin should be found by admin ID.");
        assertEquals(admin.getAdminId(), foundOpt.get().getAdminId());
    }

    @Test
    void testFindById_nonExisting_returnsEmpty() {
        Optional<Admin> foundOpt = adminDAO.findById(88888);
        assertFalse(foundOpt.isPresent());
    }

    @Test
    void testFindByEmail_existingAdmin_returnsAdmin() {
        int credId = createTestCredential("adminUser7", "admin");
        String email = "find.by.email@example.com";
        Admin admin = new Admin(credId, "Find", "By", "Email", email, "777", LocalDate.of(1995, 11, 11));
        adminDAO.saveAdmin(admin);
        assertNotNull(admin.getAdminId());

        Optional<Admin> foundOpt = adminDAO.findByEmail(email);
        assertTrue(foundOpt.isPresent(), "Admin should be found by email.");
        assertEquals(admin.getAdminId(), foundOpt.get().getAdminId());
    }

    @Test
    void testFindByEmail_nonExisting_returnsEmpty() {
        Optional<Admin> foundOpt = adminDAO.findByEmail("no.such.email@example.com");
        assertFalse(foundOpt.isPresent());
    }
}
