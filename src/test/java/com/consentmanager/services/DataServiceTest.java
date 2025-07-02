package com.consentmanager.services;

import com.consentmanager.daos.ConsentDAO;
import com.consentmanager.daos.DataItemDAO;
import com.consentmanager.models.Consent;
import com.consentmanager.models.DataItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DataServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(DataServiceTest.class);

    @Mock
    private DataItemDAO dataItemDAO;

    @Mock
    private ConsentDAO consentDAO;

    @InjectMocks
    private DataService dataService;

    private DataItem sampleDataItem;
    private Consent sampleConsent;

    @BeforeEach
    void setUp() {
        sampleDataItem = new DataItem(1, 1, "Test Item", "Description", "text", "Sample Data", null);
        sampleDataItem.setDataItemId(100);
        sampleDataItem.setCreatedAt(LocalDateTime.now().minusDays(1));
        sampleDataItem.setUpdatedAt(LocalDateTime.now().minusHours(12));

        sampleConsent = new Consent(100, 200, 1, "approved", "reEncryptedKey",
                                    LocalDateTime.now().minusDays(1), LocalDateTime.now().minusHours(10),
                                    LocalDateTime.now().plusDays(20), 0, 10);
        sampleConsent.setConsentId(300);
    }

    @Test
    void createDataItem_successText() throws ServiceException {
        when(dataItemDAO.saveDataItem(any(DataItem.class))).thenReturn(sampleDataItem);
        // Mock findById because saveDataItem in DAO calls it to populate createdAt/updatedAt
        when(dataItemDAO.findById(anyInt())).thenReturn(Optional.of(sampleDataItem));


        DataItem result = dataService.createDataItem(1, "New Item", "Desc", "text", "Some text data");
        assertNotNull(result);
        assertEquals("Test Item", result.getName()); // Assuming mock returns sampleDataItem
        verify(dataItemDAO).saveDataItem(any(DataItem.class));
    }

    @Test
    void createDataItem_successFile() throws ServiceException {
        DataItem fileItem = new DataItem(1, 1, "File Item", "File Desc", "file", "/path/file.txt", "placeholder_encrypted_aes_key_for_File Item");
        fileItem.setDataItemId(101);
        fileItem.setCreatedAt(LocalDateTime.now());
        fileItem.setUpdatedAt(LocalDateTime.now());

        when(dataItemDAO.saveDataItem(any(DataItem.class))).thenReturn(fileItem);
        when(dataItemDAO.findById(anyInt())).thenReturn(Optional.of(fileItem));


        DataItem result = dataService.createDataItem(1, "File Item", "File Desc", "file", "/path/file.txt");
        assertNotNull(result);
        assertEquals("File Item", result.getName());
        assertNotNull(result.getAesKeyEncrypted());
        assertTrue(result.getAesKeyEncrypted().startsWith("placeholder_encrypted_aes_key_for_"));
        verify(dataItemDAO).saveDataItem(any(DataItem.class));
    }


    @Test
    void createDataItem_missingFields_throwsServiceException() {
        assertThrows(ServiceException.class, () -> {
            dataService.createDataItem(null, "Name", "Desc", "type", "data");
        });
        assertThrows(ServiceException.class, () -> {
            dataService.createDataItem(1, " ", "Desc", "type", "data");
        });
    }

    @Test
    void createDataItem_daoSaveFails_throwsServiceException() {
        when(dataItemDAO.saveDataItem(any(DataItem.class))).thenReturn(null);
        assertThrows(ServiceException.class, () -> {
             dataService.createDataItem(1, "Fail Item", "Desc", "text", "data");
        });
    }


    @Test
    void getDataItemsByProvider_success() throws ServiceException {
        when(dataItemDAO.findByProviderId(1)).thenReturn(List.of(sampleDataItem));
        List<DataItem> result = dataService.getDataItemsByProvider(1);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(sampleDataItem.getName(), result.get(0).getName());
    }

    @Test
    void getDataItemsByProvider_nullProviderId_throwsServiceException() {
        assertThrows(ServiceException.class, () -> {
            dataService.getDataItemsByProvider(null);
        });
    }

    @Test
    void getDataItemByIdForProvider_success() throws ServiceException {
        when(dataItemDAO.findById(100)).thenReturn(Optional.of(sampleDataItem));
        Optional<DataItem> result = dataService.getDataItemByIdForProvider(100, 1); // providerId matches sampleDataItem
        assertTrue(result.isPresent());
        assertEquals(sampleDataItem.getName(), result.get().getName());
    }

    @Test
    void getDataItemByIdForProvider_wrongProvider_returnsEmpty() throws ServiceException {
        when(dataItemDAO.findById(100)).thenReturn(Optional.of(sampleDataItem));
        Optional<DataItem> result = dataService.getDataItemByIdForProvider(100, 2); // providerId does not match
        assertFalse(result.isPresent());
    }

    @Test
    void updateDataItem_success() throws ServiceException {
        when(dataItemDAO.findById(100)).thenReturn(Optional.of(sampleDataItem));
        when(dataItemDAO.updateDataItem(any(DataItem.class))).thenReturn(true);

        boolean updated = dataService.updateDataItem(100, 1, "Updated Name", null, null, null, null);
        assertTrue(updated);
        verify(dataItemDAO).updateDataItem(argThat(item -> item.getName().equals("Updated Name")));
    }

    @Test
    void updateDataItem_itemNotFoundOrNotOwner_returnsFalse() throws ServiceException {
        when(dataItemDAO.findById(100)).thenReturn(Optional.empty()); // Item not found
        boolean updated = dataService.updateDataItem(100, 1, "Updated Name", null, null, null, null);
        assertFalse(updated);

        when(dataItemDAO.findById(100)).thenReturn(Optional.of(sampleDataItem)); // Item found
        boolean updatedWrongOwner = dataService.updateDataItem(100, 2, "Updated Name", null, null, null, null); // Wrong owner
        assertFalse(updatedWrongOwner);
    }


    @Test
    void deleteDataItem_success() throws ServiceException {
        when(dataItemDAO.deleteDataItem(100, 1)).thenReturn(true);
        boolean deleted = dataService.deleteDataItem(100, 1);
        assertTrue(deleted);
    }

    @Test
    void deleteDataItem_fails_returnsFalse() throws ServiceException {
        when(dataItemDAO.deleteDataItem(100, 1)).thenReturn(false);
        boolean deleted = dataService.deleteDataItem(100, 1);
        assertFalse(deleted);
    }


    @Test
    void accessDataItem_success() throws ServiceException {
        when(consentDAO.findByDataItemAndSeeker(100, 200)).thenReturn(Optional.of(sampleConsent));
        when(dataItemDAO.findById(100)).thenReturn(Optional.of(sampleDataItem));
        when(consentDAO.incrementAccessCount(sampleConsent.getConsentId())).thenReturn(true);

        String result = dataService.accessDataItem(200, 100);
        assertNotNull(result);
        assertTrue(result.contains(sampleDataItem.getData()));
        verify(consentDAO).incrementAccessCount(sampleConsent.getConsentId());
    }

    @Test
    void accessDataItem_noConsent_throwsServiceException() {
        when(consentDAO.findByDataItemAndSeeker(100, 200)).thenReturn(Optional.empty());
        assertThrows(ServiceException.class, () -> {
            dataService.accessDataItem(200, 100);
        }, "No consent record found");
    }

    @Test
    void accessDataItem_consentNotApproved_throwsServiceException() {
        sampleConsent.setStatus("pending");
        when(consentDAO.findByDataItemAndSeeker(100, 200)).thenReturn(Optional.of(sampleConsent));
         assertThrows(ServiceException.class, () -> {
            dataService.accessDataItem(200, 100);
        }, "Consent not approved");
    }

    @Test
    void accessDataItem_consentExpired_throwsServiceException() {
        sampleConsent.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(consentDAO.findByDataItemAndSeeker(100, 200)).thenReturn(Optional.of(sampleConsent));
        assertThrows(ServiceException.class, () -> {
            dataService.accessDataItem(200, 100);
        }, "Consent has expired");
    }

    @Test
    void accessDataItem_accessCountExhausted_throwsServiceException() {
        sampleConsent.setAccessCount(10); // Max is 10
        sampleConsent.setMaxAccessCount(10);
        when(consentDAO.findByDataItemAndSeeker(100, 200)).thenReturn(Optional.of(sampleConsent));
         assertThrows(ServiceException.class, () -> {
            dataService.accessDataItem(200, 100);
        }, "Access count exhausted");
    }

    @Test
    void accessDataItem_dataItemNotFound_throwsServiceException() {
        when(consentDAO.findByDataItemAndSeeker(100, 200)).thenReturn(Optional.of(sampleConsent));
        when(dataItemDAO.findById(100)).thenReturn(Optional.empty());
         assertThrows(ServiceException.class, () -> {
            dataService.accessDataItem(200, 100);
        }, "Data item not found");
    }

    @Test
    void listDiscoverableDataItems_success() throws ServiceException {
        DataItem item2 = new DataItem(1, 1, "Item 2", "Desc 2", "text", "Data 2", null);
        item2.setDataItemId(102);

        List<DataItem> allItems = new ArrayList<>(List.of(sampleDataItem, item2));
        when(dataItemDAO.findAll()).thenReturn(allItems);

        // Seeker has consent for sampleDataItem (ID 100)
        Consent existingConsent = new Consent();
        existingConsent.setDataItemId(sampleDataItem.getDataItemId());
        existingConsent.setStatus("approved");
        when(consentDAO.findBySeekerId(200)).thenReturn(List.of(existingConsent));

        List<DataItem> discoverable = dataService.listDiscoverableDataItems(200);
        assertEquals(1, discoverable.size());
        assertEquals(item2.getDataItemId(), discoverable.get(0).getDataItemId());
    }

    @Test
    void listDiscoverableDataItems_noExistingConsents() throws ServiceException {
        DataItem item2 = new DataItem(1, 1, "Item 2", "Desc 2", "text", "Data 2", null);
        item2.setDataItemId(102);
        List<DataItem> allItems = new ArrayList<>(List.of(sampleDataItem, item2));

        when(dataItemDAO.findAll()).thenReturn(allItems);
        when(consentDAO.findBySeekerId(200)).thenReturn(new ArrayList<>()); // No consents for this seeker

        List<DataItem> discoverable = dataService.listDiscoverableDataItems(200);
        assertEquals(2, discoverable.size());
    }
}
