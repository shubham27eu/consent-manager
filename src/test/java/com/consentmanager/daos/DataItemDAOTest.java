package com.consentmanager.daos;

import com.consentmanager.models.Credential;
import com.consentmanager.models.DataItem;
import com.consentmanager.models.Provider;
import com.consentmanager.utils.DatabaseUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class DataItemDAOTest {

    private static final Logger logger = LoggerFactory.getLogger(DataItemDAOTest.class);
    private Connection testConnection;
    private DataItemDAO dataItemDAO;
    private ProviderDAO providerDAO; // For creating prerequisite providers
    private CredentialDAO credentialDAO; // For prerequisite credentials for providers

    private int testProviderId1;
    private int testProviderId2;

    @BeforeEach
    void setUp() throws SQLException {
        testConnection = DatabaseUtil.getTestConnection();
        dataItemDAO = new DataItemDAO(testConnection);
        providerDAO = new ProviderDAO(testConnection);
        credentialDAO = new CredentialDAO(testConnection);

        // Create prerequisite providers
        testProviderId1 = createTestProvider("providerUser1", "prov1@example.com");
        testProviderId2 = createTestProvider("providerUser2", "prov2@example.com");
        logger.debug("Setup complete for DataItemDAOTest.");
    }

    private int createTestProvider(String username, String email) {
        Credential cred = new Credential(username, "password", "provider");
        int credId = credentialDAO.saveCredential(cred);
        assertTrue(credId > 0, "Credential setup failed for provider.");

        Provider provider = new Provider(credId, "Test", "Prov", username, email,
                "1234567890", LocalDate.now(), 30, "publicKey", true);
        boolean saved = providerDAO.saveProvider(provider);
        assertTrue(saved, "Provider setup failed.");
        assertNotNull(provider.getProviderId(), "Provider ID should be set after save.");
        return provider.getProviderId();
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testConnection != null && !testConnection.isClosed()) {
            logger.debug("Closing test connection for DataItemDAOTest.");
            testConnection.close();
        }
    }

    @Test
    void testSaveAndFindById() {
        DataItem newItem = new DataItem(testProviderId1, "Health Record", "Annual Checkup", "file", "/path/to/record.dat", "encryptedKey123");
        DataItem savedItem = dataItemDAO.saveDataItem(newItem);

        assertNotNull(savedItem, "Saved item should not be null.");
        assertNotNull(savedItem.getDataItemId(), "Data Item ID should be set after saving.");
        assertTrue(savedItem.getDataItemId() > 0);
        assertNotNull(savedItem.getCreatedAt(), "CreatedAt should be set.");
        assertNotNull(savedItem.getUpdatedAt(), "UpdatedAt should be set.");
        assertEquals("Health Record", savedItem.getName());

        Optional<DataItem> foundItemOpt = dataItemDAO.findById(savedItem.getDataItemId());
        assertTrue(foundItemOpt.isPresent(), "Should find item by ID.");
        assertEquals(savedItem.getName(), foundItemOpt.get().getName());
    }

    @Test
    void testFindById_nonExistent() {
        Optional<DataItem> foundItemOpt = dataItemDAO.findById(99999);
        assertFalse(foundItemOpt.isPresent(), "Should not find non-existent item.");
    }

    @Test
    void testFindByProviderId() {
        dataItemDAO.saveDataItem(new DataItem(testProviderId1, "Item P1-1", "Desc1", "text", "data1", null));
        dataItemDAO.saveDataItem(new DataItem(testProviderId1, "Item P1-2", "Desc2", "file", "path2", "key2"));
        dataItemDAO.saveDataItem(new DataItem(testProviderId2, "Item P2-1", "Desc3", "text", "data3", null));

        List<DataItem> itemsForP1 = dataItemDAO.findByProviderId(testProviderId1);
        assertEquals(2, itemsForP1.size(), "Should find 2 items for provider 1.");
        assertTrue(itemsForP1.stream().allMatch(item -> item.getProviderId().equals(testProviderId1)));

        List<DataItem> itemsForP2 = dataItemDAO.findByProviderId(testProviderId2);
        assertEquals(1, itemsForP2.size(), "Should find 1 item for provider 2.");
    }

    @Test
    void testFindAll() {
        dataItemDAO.saveDataItem(new DataItem(testProviderId1, "Item A", "DescA", "text", "dataA", null));
        dataItemDAO.saveDataItem(new DataItem(testProviderId2, "Item B", "DescB", "file", "pathB", "keyB"));
        List<DataItem> allItems = dataItemDAO.findAll();
        assertEquals(2, allItems.size(), "Should find all created items.");
    }


    @Test
    void testUpdateDataItem() {
        DataItem item = new DataItem(testProviderId1, "Original Name", "Original Desc", "text", "original data", null);
        DataItem savedItem = dataItemDAO.saveDataItem(item);
        assertNotNull(savedItem);

        savedItem.setName("Updated Name");
        savedItem.setDescription("Updated Description");
        boolean updated = dataItemDAO.updateDataItem(savedItem);
        assertTrue(updated, "Update should be successful.");

        Optional<DataItem> updatedItemOpt = dataItemDAO.findById(savedItem.getDataItemId());
        assertTrue(updatedItemOpt.isPresent());
        assertEquals("Updated Name", updatedItemOpt.get().getName());
        assertEquals("Updated Description", updatedItemOpt.get().getDescription());
        assertNotNull(updatedItemOpt.get().getUpdatedAt());
        assertTrue(updatedItemOpt.get().getUpdatedAt().isAfter(updatedItemOpt.get().getCreatedAt()) ||
                   updatedItemOpt.get().getUpdatedAt().isEqual(updatedItemOpt.get().getCreatedAt()), // Possible if DB resolution is low and ops are fast
                   "UpdatedAt should be greater than or equal to CreatedAt after update.");
    }

    @Test
    void testUpdateDataItem_unauthorizedProvider() {
        DataItem item = new DataItem(testProviderId1, "Owned Item", "Desc", "text", "data", null);
        DataItem savedItem = dataItemDAO.saveDataItem(item);
        assertNotNull(savedItem);

        // Try to update using testProviderId2's context (simulated by passing it to DAO method)
        DataItem itemToUpdateByWrongProvider = new DataItem();
        itemToUpdateByWrongProvider.setDataItemId(savedItem.getDataItemId());
        itemToUpdateByWrongProvider.setProviderId(testProviderId2); // Set wrong provider ID
        itemToUpdateByWrongProvider.setName("Attempted Update Name");

        // The DAO's update method checks provider_id in WHERE clause
        boolean updated = dataItemDAO.updateDataItem(itemToUpdateByWrongProvider);
        assertFalse(updated, "Update should fail if provider ID does not match.");

        Optional<DataItem> originalItemOpt = dataItemDAO.findById(savedItem.getDataItemId());
        assertTrue(originalItemOpt.isPresent());
        assertEquals("Owned Item", originalItemOpt.get().getName(), "Name should not have changed.");
    }


    @Test
    void testDeleteDataItem() {
        DataItem item = new DataItem(testProviderId1, "To Be Deleted", "Desc", "text", "data", null);
        DataItem savedItem = dataItemDAO.saveDataItem(item);
        assertNotNull(savedItem);

        boolean deleted = dataItemDAO.deleteDataItem(savedItem.getDataItemId(), testProviderId1);
        assertTrue(deleted, "Delete should be successful.");

        Optional<DataItem> deletedItemOpt = dataItemDAO.findById(savedItem.getDataItemId());
        assertFalse(deletedItemOpt.isPresent(), "Item should not be found after deletion.");
    }

    @Test
    void testDeleteDataItem_unauthorizedProvider() {
        DataItem item = new DataItem(testProviderId1, "Another Item", "Desc", "text", "data", null);
        DataItem savedItem = dataItemDAO.saveDataItem(item);
        assertNotNull(savedItem);

        // Attempt to delete using wrong providerId
        boolean deleted = dataItemDAO.deleteDataItem(savedItem.getDataItemId(), testProviderId2);
        assertFalse(deleted, "Delete should fail if provider ID does not match.");

        Optional<DataItem> itemOpt = dataItemDAO.findById(savedItem.getDataItemId());
        assertTrue(itemOpt.isPresent(), "Item should still exist.");
    }
}
