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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProviderDataControllerTest {

    @Mock
    private DataService dataService;
    @Mock
    private ConsentService consentService;
    @Mock
    private Request request;
    @Mock
    private Response response;

    @InjectMocks
    private ProviderDataController providerDataController;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final Integer providerId = 1;
    private DataItem sampleDataItem;

    @BeforeEach
    void setUp() {
        // Register routes - not strictly necessary for unit testing methods directly,
        // but good practice if controller constructor does it.
        // providerDataController.registerRoutes(); // Controller constructor does not call this.

        sampleDataItem = new DataItem(providerId, "Test Data Item", "Description", "text", "Some data", null);
        sampleDataItem.setDataItemId(101);
        sampleDataItem.setCreatedAt(LocalDateTime.now());
        sampleDataItem.setUpdatedAt(LocalDateTime.now());

        // Common mock setup for providerId from auth middleware
        when(request.attribute("credentialId")).thenReturn(providerId);
    }

    @Test
    void createDataItem_success() throws Exception {
        String requestJson = "{\"name\":\"New Item\",\"description\":\"Desc\",\"type\":\"text\",\"data\":\"text data\"}";
        when(request.body()).thenReturn(requestJson);
        when(dataService.createDataItem(eq(providerId), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(sampleDataItem);

        Object result = providerDataController.createDataItem(request, response);

        assertNotNull(result);
        assertTrue(result instanceof DataItem);
        assertEquals(sampleDataItem.getName(), ((DataItem)result).getName());
        verify(response).status(201);
        verify(response).type("application/json");
    }

    @Test
    void createDataItem_serviceException_returnsError() throws Exception {
        String requestJson = "{\"name\":\"New Item\"}"; // Missing fields
        when(request.body()).thenReturn(requestJson);
        when(dataService.createDataItem(anyInt(), any(), any(), any(), any()))
            .thenThrow(new ServiceException("Missing required fields"));

        Object result = providerDataController.createDataItem(request, response);
        assertTrue(result instanceof ProviderDataController.ErrorResponse); // Using inner class
        verify(response).status(400);
    }


    @Test
    void getDataItemsByProvider_success() throws Exception {
        when(dataService.getDataItemsByProvider(providerId)).thenReturn(List.of(sampleDataItem));

        List<DataItem> result = (List<DataItem>) providerDataController.getDataItemsByProvider(request, response);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sampleDataItem.getName(), result.get(0).getName());
        verify(response).type("application/json");
    }

    @Test
    void updateDataItem_success() throws Exception {
        String updateJson = "{\"name\":\"Updated Item Name\"}";
        when(request.params(":id")).thenReturn("101");
        when(request.body()).thenReturn(updateJson);
        when(dataService.updateDataItem(eq(101), eq(providerId), anyString(), any(), any(), any(), any())).thenReturn(true);
        when(dataService.getDataItemByIdForProvider(101, providerId)).thenReturn(Optional.of(sampleDataItem)); // for returning updated item

        Object result = providerDataController.updateDataItem(request, response);
        assertTrue(result instanceof DataItem);
        assertEquals(sampleDataItem.getName(), ((DataItem)result).getName());
        verify(response).type("application/json");
    }

    @Test
    void updateDataItem_notFound_returnsError() throws Exception {
        String updateJson = "{\"name\":\"Updated Item Name\"}";
        when(request.params(":id")).thenReturn("101");
        when(request.body()).thenReturn(updateJson);
        when(dataService.updateDataItem(eq(101), eq(providerId), anyString(), any(), any(), any(), any())).thenReturn(false);

        Object result = providerDataController.updateDataItem(request, response);
        assertTrue(result instanceof ProviderDataController.ErrorResponse);
        verify(response).status(404);
    }


    @Test
    void deleteDataItem_success() throws Exception {
        when(request.params(":id")).thenReturn("101");
        when(dataService.deleteDataItem(101, providerId)).thenReturn(true);

        Object result = providerDataController.deleteDataItem(request, response);
        assertTrue(result instanceof ProviderDataController.SuccessResponse);
        verify(response).status(200);
    }

    @Test
    void deleteDataItem_notFound_returnsError() throws Exception {
        when(request.params(":id")).thenReturn("101");
        when(dataService.deleteDataItem(101, providerId)).thenReturn(false);

        Object result = providerDataController.deleteDataItem(request, response);
        assertTrue(result instanceof ProviderDataController.ErrorResponse);
        verify(response).status(404);
    }


    @Test
    void getPendingConsentRequests_success() throws Exception {
        Consent pendingConsent = new Consent(sampleDataItem.getDataItemId(), 50, providerId, "pending", null, LocalDateTime.now(), null, null,0,null);
        when(request.queryParams("status")).thenReturn("pending"); // or null for default
        when(consentService.getConsentsByProviderAndStatus(providerId, "pending")).thenReturn(List.of(pendingConsent));

        List<Consent> result = (List<Consent>) providerDataController.getPendingConsentRequests(request, response);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("pending", result.get(0).getStatus());
    }

    @Test
    void respondToConsentRequest_approveSuccess() throws Exception {
        String responseJson = "{\"status\":\"approved\",\"details\":\"Looks good\"}";
        when(request.params(":consentId")).thenReturn("201");
        when(request.body()).thenReturn(responseJson);
        when(consentService.respondToConsent(eq(providerId), eq(201), eq("approved"), anyString())).thenReturn(true);

        Object result = providerDataController.respondToConsentRequest(request, response);
        assertTrue(result instanceof ProviderDataController.SuccessResponse);
        assertEquals("Consent request approved successfully.", ((ProviderDataController.SuccessResponse)result).getMessage());
    }

    @Test
    void respondToConsentRequest_invalidStatus_returnsError() throws Exception {
        String responseJson = "{\"status\":\"invalid_action\",\"details\":\"...\"}";
        when(request.params(":consentId")).thenReturn("201");
        when(request.body()).thenReturn(responseJson);
        // No service call expected for invalid input prior to service logic

        Object result = providerDataController.respondToConsentRequest(request, response);
        assertTrue(result instanceof ProviderDataController.ErrorResponse);
        assertEquals("Invalid status. Must be 'approved' or 'rejected'.", ((ProviderDataController.ErrorResponse)result).getError());
        verify(response).status(400);
    }

    @Test
    void respondToConsentRequest_serviceException_returnsError() throws Exception {
        String responseJson = "{\"status\":\"approved\"}";
        when(request.params(":consentId")).thenReturn("201");
        when(request.body()).thenReturn(responseJson);
        when(consentService.respondToConsent(anyInt(), anyInt(), anyString(), any()))
            .thenThrow(new ServiceException("Consent not found"));

        Object result = providerDataController.respondToConsentRequest(request, response);
        assertTrue(result instanceof ProviderDataController.ErrorResponse);
        assertEquals("Consent not found", ((ProviderDataController.ErrorResponse)result).getError());
        verify(response).status(404); // Based on logic in controller for "not found"
    }
}
