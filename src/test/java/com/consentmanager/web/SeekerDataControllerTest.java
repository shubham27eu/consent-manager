package com.consentmanager.web;

import com.consentmanager.models.Consent;
import com.consentmanager.models.DataItem;
import com.consentmanager.services.ConsentService;
import com.consentmanager.services.DataService;
import com.consentmanager.services.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spark.Request;
import spark.Response;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SeekerDataControllerTest {

    @Mock
    private DataService dataService;
    @Mock
    private ConsentService consentService;
    @Mock
    private Request request;
    @Mock
    private Response response;

    @InjectMocks
    private SeekerDataController seekerDataController;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final Integer seekerId = 1;
    private DataItem sampleDataItem;
    private Consent sampleConsent;

    @BeforeEach
    void setUp() {
        sampleDataItem = new DataItem(2, "Discoverable Item", "Desc", "text", "Data", null);
        sampleDataItem.setDataItemId(101);

        sampleConsent = new Consent(101, seekerId, 2, "pending", null, LocalDateTime.now(), null, null,0,10);
        sampleConsent.setConsentId(201);

        when(request.attribute("credentialId")).thenReturn(seekerId);
    }

    @Test
    void listDiscoverableDataItems_success() throws Exception {
        when(dataService.listDiscoverableDataItems(seekerId)).thenReturn(List.of(sampleDataItem));

        List<DataItem> result = (List<DataItem>) seekerDataController.listDiscoverableDataItems(request, response);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sampleDataItem.getName(), result.get(0).getName());
        verify(response).type("application/json");
    }

    @Test
    void listDiscoverableDataItems_serviceException() throws Exception {
        when(dataService.listDiscoverableDataItems(seekerId)).thenThrow(new ServiceException("DB error"));

        List<DataItem> result = (List<DataItem>) seekerDataController.listDiscoverableDataItems(request, response);
        assertTrue(result.get(0).getName().contains("DB error")); // ErrorResponse trick
        verify(response).status(400);
    }

    @Test
    void requestConsent_success() throws Exception {
        SeekerDataController.ConsentRequestPayload payload = new SeekerDataController.ConsentRequestPayload();
        payload.setDataItemId(sampleDataItem.getDataItemId());
        payload.setExpiresAt(LocalDateTime.now().plusDays(5));
        payload.setMaxAccessCount(5);

        when(request.body()).thenReturn(objectMapper.writeValueAsString(payload));
        when(consentService.requestConsent(seekerId, sampleDataItem.getDataItemId(), payload.getExpiresAt(), payload.getMaxAccessCount()))
            .thenReturn(sampleConsent);

        Consent result = (Consent) seekerDataController.requestConsent(request, response);

        assertNotNull(result);
        assertEquals(sampleConsent.getConsentId(), result.getConsentId());
        verify(response).status(201);
        verify(response).type("application/json");
    }

    @Test
    void requestConsent_missingDataItemId_throwsServiceException() throws Exception {
        SeekerDataController.ConsentRequestPayload payload = new SeekerDataController.ConsentRequestPayload(); // dataItemId is null
        when(request.body()).thenReturn(objectMapper.writeValueAsString(payload));

        Object result = seekerDataController.requestConsent(request, response);
        assertTrue(result instanceof SeekerDataController.ErrorResponse);
        assertEquals("Data Item ID is required.", ((SeekerDataController.ErrorResponse)result).getError());
        verify(response).status(400);
    }


    @Test
    void getSeekerConsents_success() throws Exception {
        when(consentService.getConsentsBySeeker(seekerId)).thenReturn(List.of(sampleConsent));

        List<Consent> result = (List<Consent>) seekerDataController.getSeekerConsents(request, response);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sampleConsent.getConsentId(), result.get(0).getConsentId());
        verify(response).type("application/json");
    }

    @Test
    void accessDataItem_success() throws Exception {
        when(request.params(":dataItemId")).thenReturn(String.valueOf(sampleDataItem.getDataItemId()));
        String mockAccessedData = "Decrypted: " + sampleDataItem.getData();
        when(dataService.accessDataItem(seekerId, sampleDataItem.getDataItemId())).thenReturn(mockAccessedData);

        SeekerDataController.DataAccessResponse result = (SeekerDataController.DataAccessResponse) seekerDataController.accessDataItem(request, response);

        assertNotNull(result);
        assertEquals(mockAccessedData, result.getData());
        verify(response).type("application/json");
    }

    @Test
    void accessDataItem_serviceException_forbidden() throws Exception {
        when(request.params(":dataItemId")).thenReturn(String.valueOf(sampleDataItem.getDataItemId()));
        when(dataService.accessDataItem(seekerId, sampleDataItem.getDataItemId()))
            .thenThrow(new ServiceException("Consent not approved"));

        Object result = seekerDataController.accessDataItem(request, response);
        assertTrue(result instanceof SeekerDataController.ErrorResponse);
        assertEquals("Consent not approved", ((SeekerDataController.ErrorResponse)result).getError());
        verify(response).status(403); // Forbidden
    }

    @Test
    void accessDataItem_serviceException_notFound() throws Exception {
        when(request.params(":dataItemId")).thenReturn(String.valueOf(sampleDataItem.getDataItemId()));
        when(dataService.accessDataItem(seekerId, sampleDataItem.getDataItemId()))
            .thenThrow(new ServiceException("Data item 101 not found"));

        Object result = seekerDataController.accessDataItem(request, response);
        assertTrue(result instanceof SeekerDataController.ErrorResponse);
        assertEquals("Data item 101 not found", ((SeekerDataController.ErrorResponse)result).getError());
        verify(response).status(404); // Not Found
    }
     @Test
    void accessDataItem_invalidDataItemIdFormat_returnsError() throws Exception {
        when(request.params(":dataItemId")).thenReturn("invalid-id");
        // No service call expected

        Object result = seekerDataController.accessDataItem(request, response);
        assertTrue(result instanceof SeekerDataController.ErrorResponse);
        assertEquals("Invalid Data Item ID format.", ((SeekerDataController.ErrorResponse)result).getError());
        verify(response).status(400);
    }

}
