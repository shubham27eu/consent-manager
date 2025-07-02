package com.consentmanager.services;

import com.consentmanager.daos.ConsentDAO;
import com.consentmanager.daos.ConsentHistoryDAO;
import com.consentmanager.daos.DataItemDAO;
import com.consentmanager.daos.SeekerDAO;
import com.consentmanager.models.Consent;
import com.consentmanager.models.DataItem;
import com.consentmanager.models.Seeker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConsentServiceTest {

    @Mock
    private ConsentDAO consentDAO;
    @Mock
    private DataItemDAO dataItemDAO;
    @Mock
    private ConsentHistoryDAO consentHistoryDAO;
    @Mock
    private SeekerDAO seekerDAO;

    @InjectMocks
    private ConsentService consentService;

    private DataItem sampleDataItem;
    private Seeker sampleSeeker;
    private Consent sampleConsent;

    private final Integer seekerId = 1;
    private final Integer providerId = 2;
    private final Integer dataItemId = 3;
    private final Integer consentId = 4;

    @BeforeEach
    void setUp() {
        sampleDataItem = new DataItem(providerId, "Test Item", "Description", "file", "/path/data.txt", "itemAesKeyEncrypted");
        sampleDataItem.setDataItemId(dataItemId);

        sampleSeeker = new Seeker(seekerId, "Seeker Name", "Type", "REG123", "seeker@example.com", "12345", "Address", "seekerPublicKey", true);
        sampleSeeker.setSeekerId(seekerId); // Seeker ID is distinct from credentialId

        sampleConsent = new Consent(dataItemId, seekerId, providerId, "pending", null, LocalDateTime.now(), null, null, 0, null);
        sampleConsent.setConsentId(consentId);
    }

    @Test
    void requestConsent_success() throws ServiceException {
        when(dataItemDAO.findById(dataItemId)).thenReturn(Optional.of(sampleDataItem));
        when(consentDAO.findByDataItemAndSeeker(dataItemId, seekerId)).thenReturn(Optional.empty());
        // The DAO's createConsent method is expected to set the ID and requestedAt
        // So, we'll have it return the input object after we manually set those, to simulate DAO behavior
        when(consentDAO.createConsent(any(Consent.class))).thenAnswer(invocation -> {
            Consent c = invocation.getArgument(0);
            c.setConsentId(consentId); // Simulate ID generation
            c.setRequestedAt(LocalDateTime.now()); // Simulate DB default timestamp
            c.setAccessCount(0); // Simulate DB default
            return c;
        });
        when(consentHistoryDAO.logAction(any(ConsentHistory.class))).thenReturn(true);

        Consent result = consentService.requestConsent(seekerId, dataItemId, LocalDateTime.now().plusDays(10), 100);

        assertNotNull(result);
        assertEquals("pending", result.getStatus());
        assertEquals(dataItemId, result.getDataItemId());
        assertEquals(seekerId, result.getSeekerId());
        assertEquals(providerId, result.getProviderId());
        assertNotNull(result.getRequestedAt());
        verify(consentHistoryDAO).logAction(argThat(h ->
            h.getConsentId().equals(consentId) && "requested".equals(h.getAction())
        ));
    }

    @Test
    void requestConsent_dataItemNotFound_throwsServiceException() {
        when(dataItemDAO.findById(dataItemId)).thenReturn(Optional.empty());
        assertThrows(ServiceException.class, () -> {
            consentService.requestConsent(seekerId, dataItemId, null, null);
        }, "Data item with ID " + dataItemId + " not found.");
    }

    @Test
    void requestConsent_alreadyExistsActive_returnsExisting() throws ServiceException {
        sampleConsent.setStatus("approved");
        when(dataItemDAO.findById(dataItemId)).thenReturn(Optional.of(sampleDataItem));
        when(consentDAO.findByDataItemAndSeeker(dataItemId, seekerId)).thenReturn(Optional.of(sampleConsent));

        Consent result = consentService.requestConsent(seekerId, dataItemId, null, null);
        assertNotNull(result);
        assertEquals(sampleConsent.getConsentId(), result.getConsentId());
        assertEquals("approved", result.getStatus()); // Should return existing
        verify(consentDAO, never()).createConsent(any(Consent.class)); // No new consent created
    }


    @Test
    void respondToConsent_approveSuccess() throws ServiceException {
        when(consentDAO.findById(consentId)).thenReturn(Optional.of(sampleConsent)); // status is "pending"
        when(dataItemDAO.findById(dataItemId)).thenReturn(Optional.of(sampleDataItem)); // providerId matches
        when(seekerDAO.findById(seekerId)).thenReturn(Optional.of(sampleSeeker)); // seeker has public key
        when(consentDAO.updateConsentStatus(anyInt(), eq("approved"), any(LocalDateTime.class), any(), anyString())).thenReturn(true);
        when(consentHistoryDAO.logAction(any(ConsentHistory.class))).thenReturn(true);

        boolean result = consentService.respondToConsent(providerId, consentId, "approved", null);
        assertTrue(result);
        verify(consentDAO).updateConsentStatus(eq(consentId), eq("approved"), any(LocalDateTime.class), any(), startsWith("placeholder_reencrypted_key_for_seeker_"));
        verify(consentHistoryDAO).logAction(argThat(h ->
            h.getConsentId().equals(consentId) && "approved".equals(h.getAction())
        ));
    }

    @Test
    void respondToConsent_approveFileItem_noSeekerPublicKey_throwsServiceException() {
        sampleSeeker.setPublicKey(null); // Seeker has no public key
        when(consentDAO.findById(consentId)).thenReturn(Optional.of(sampleConsent));
        when(dataItemDAO.findById(dataItemId)).thenReturn(Optional.of(sampleDataItem)); // type is "file"
        when(seekerDAO.findById(seekerId)).thenReturn(Optional.of(sampleSeeker));

        assertThrows(ServiceException.class, () -> {
            consentService.respondToConsent(providerId, consentId, "approved", null);
        }, "Seeker's public key not found");
        verify(consentDAO, never()).updateConsentStatus(anyInt(), anyString(), any(), any(), anyString());
    }


    @Test
    void respondToConsent_rejectSuccess() throws ServiceException {
        when(consentDAO.findById(consentId)).thenReturn(Optional.of(sampleConsent));
        when(dataItemDAO.findById(dataItemId)).thenReturn(Optional.of(sampleDataItem));
        when(consentDAO.updateConsentStatus(consentId, "rejected", null, null, null)).thenReturn(true);
        when(consentHistoryDAO.logAction(any(ConsentHistory.class))).thenReturn(true);

        boolean result = consentService.respondToConsent(providerId, consentId, "rejected", "Not suitable.");
        assertTrue(result);
        verify(consentDAO).updateConsentStatus(consentId, "rejected", null, null, null);
         verify(consentHistoryDAO).logAction(argThat(h ->
            h.getConsentId().equals(consentId) && "rejected".equals(h.getAction()) && h.getDetails().contains("Not suitable.")
        ));
    }

    @Test
    void respondToConsent_consentNotFound_throwsServiceException() {
        when(consentDAO.findById(consentId)).thenReturn(Optional.empty());
        assertThrows(ServiceException.class, () -> {
            consentService.respondToConsent(providerId, consentId, "approved", null);
        }, "Consent request with ID " + consentId + " not found.");
    }

    @Test
    void respondToConsent_providerNotOwner_throwsServiceException() {
        sampleDataItem.setProviderId(providerId + 1); // Different provider
        when(consentDAO.findById(consentId)).thenReturn(Optional.of(sampleConsent));
        when(dataItemDAO.findById(dataItemId)).thenReturn(Optional.of(sampleDataItem));

        assertThrows(ServiceException.class, () -> {
            consentService.respondToConsent(providerId, consentId, "approved", null);
        }, "Provider does not own the data item");
    }

    @Test
    void respondToConsent_notPending_throwsServiceException() {
        sampleConsent.setStatus("approved"); // Already approved
        when(consentDAO.findById(consentId)).thenReturn(Optional.of(sampleConsent));
        when(dataItemDAO.findById(dataItemId)).thenReturn(Optional.of(sampleDataItem));

        assertThrows(ServiceException.class, () -> {
            consentService.respondToConsent(providerId, consentId, "approved", null);
        }, "Consent request is not in 'pending' state.");
    }


    @Test
    void getConsentsByProviderAndStatus_success() throws ServiceException {
        when(consentDAO.findByProviderIdAndStatus(providerId, "pending")).thenReturn(List.of(sampleConsent));
        List<Consent> result = consentService.getConsentsByProviderAndStatus(providerId, "pending");
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(sampleConsent.getConsentId(), result.get(0).getConsentId());
    }

    @Test
    void getConsentsBySeeker_success() throws ServiceException {
        when(consentDAO.findBySeekerId(seekerId)).thenReturn(List.of(sampleConsent));
        List<Consent> result = consentService.getConsentsBySeeker(seekerId);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(sampleConsent.getConsentId(), result.get(0).getConsentId());
    }

    @Test
    void getConsentById_success() throws ServiceException {
        when(consentDAO.findById(consentId)).thenReturn(Optional.of(sampleConsent));
        Optional<Consent> result = consentService.getConsentById(consentId);
        assertTrue(result.isPresent());
        assertEquals(sampleConsent.getDataItemId(), result.get().getDataItemId());
    }
     @Test
    void getConsentById_notFound_returnsEmpty() throws ServiceException {
        when(consentDAO.findById(consentId)).thenReturn(Optional.empty());
        Optional<Consent> result = consentService.getConsentById(consentId);
        assertFalse(result.isPresent());
    }

}
