package com.consentmanager.daos;

import com.consentmanager.models.Credential;
import com.consentmanager.models.Provider;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ProviderDAOTest {

    private static final Logger logger = LoggerFactory.getLogger(ProviderDAOTest.class);
    private ProviderDAO providerDAO;
    private CredentialDAO credentialDAO;
    private Connection testConnection;

    @BeforeEach
    void setUp() throws SQLException {
        testConnection = DatabaseUtil.getTestConnection();
        providerDAO = new ProviderDAO(testConnection);
        credentialDAO = new CredentialDAO(testConnection); // Both DAOs use the same test connection
        logger.debug("Setup complete for ProviderDAOTest with connection: {}", testConnection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testConnection != null && !testConnection.isClosed()) {
            logger.debug("Closing test connection for ProviderDAOTest: {}", testConnection);
            testConnection.close();
        }
    }

    private int createTestCredential(String username) {
        // Uses the test-specific credentialDAO
        Credential credential = new Credential(username, "testPassword", "provider");
        return credentialDAO.saveCredential(credential);
    }

    private Provider createAndSaveTestProvider(String usernameSuffix, boolean isActive) {
        int credId = createTestCredential("providerUser" + usernameSuffix);
        assertTrue(credId > 0);
        Provider provider = new Provider(credId, "Test", "Prov", "User" + usernameSuffix,
                "provider" + usernameSuffix + "@example.com", "123456789",
                LocalDate.of(1990, 1, 1), 30, "publicKey" + usernameSuffix, isActive);
        boolean saved = providerDAO.saveProvider(provider);
        assertTrue(saved);
        assertNotNull(provider.getProviderId());
        return provider;
    }

    @Test
    void testSaveProvider_success() {
        Provider provider = createAndSaveTestProvider("1", true);
        Optional<Provider> savedProviderOpt = providerDAO.findById(provider.getProviderId());
        assertTrue(savedProviderOpt.isPresent());
        assertEquals(provider.getEmail(), savedProviderOpt.get().getEmail());
        assertEquals(provider.getIsActive(), savedProviderOpt.get().getIsActive());
    }

    @Test
    void testFindByCredentialId_existing_returnsProvider() {
        Provider provider = createAndSaveTestProvider("2", true);
        Optional<Provider> foundOpt = providerDAO.findByCredentialId(provider.getCredentialId());
        assertTrue(foundOpt.isPresent());
        assertEquals(provider.getProviderId(), foundOpt.get().getProviderId());
    }

    @Test
    void testFindByEmail_existing_returnsProvider() {
        Provider provider = createAndSaveTestProvider("FindByEmail", true);
        Optional<Provider> foundOpt = providerDAO.findByEmail(provider.getEmail());
        assertTrue(foundOpt.isPresent());
        assertEquals(provider.getProviderId(), foundOpt.get().getProviderId());
    }

    @Test
    void testFindById_nonExisting_returnsEmpty() {
        Optional<Provider> foundOpt = providerDAO.findById(99999);
        assertFalse(foundOpt.isPresent());
    }

    @Test
    void testUpdateActiveStatus_validId_updatesStatus() {
        Provider provider = createAndSaveTestProvider("3", true);
        boolean updated = providerDAO.updateActiveStatus(provider.getProviderId(), false);
        assertTrue(updated);

        Optional<Provider> updatedProviderOpt = providerDAO.findById(provider.getProviderId());
        assertTrue(updatedProviderOpt.isPresent());
        assertFalse(updatedProviderOpt.get().getIsActive());

        boolean updatedAgain = providerDAO.updateActiveStatus(provider.getProviderId(), true);
        assertTrue(updatedAgain);
        updatedProviderOpt = providerDAO.findById(provider.getProviderId());
        assertTrue(updatedProviderOpt.isPresent());
        assertTrue(updatedProviderOpt.get().getIsActive());
    }

    @Test
    void testUpdateActiveStatus_invalidId_returnsFalse() {
        boolean updated = providerDAO.updateActiveStatus(888777, false); // Non-existent ID
        assertFalse(updated);
    }


    @Test
    void testFindAllByStatus_findsActiveAndInactive() {
        createAndSaveTestProvider("Active1", true);
        createAndSaveTestProvider("Active2", true);
        createAndSaveTestProvider("Inactive1", false);

        List<Provider> activeProviders = providerDAO.findAllByStatus(true);
        assertEquals(2, activeProviders.size());
        assertTrue(activeProviders.stream().allMatch(Provider::getIsActive));

        List<Provider> inactiveProviders = providerDAO.findAllByStatus(false);
        assertEquals(1, inactiveProviders.size());
        assertFalse(inactiveProviders.get(0).getIsActive());
    }
}
