package com.consentmanager.services;

import com.consentmanager.daos.*;
import com.consentmanager.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collections;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    @Mock
    private ProviderBacklogDAO providerBacklogDAO;
    @Mock
    private SeekerBacklogDAO seekerBacklogDAO;
    @Mock
    private CredentialDAO credentialDAO;
    @Mock
    private ProviderDAO providerDAO;
    @Mock
    private SeekerDAO seekerDAO;

    @InjectMocks
    private AdminService adminService;

    private ProviderBacklog sampleProviderBacklog;
    private SeekerBacklog sampleSeekerBacklog;
    private Credential sampleCredential;
    private Provider sampleProvider;
    private Seeker sampleSeeker;


    @BeforeEach
    void setUp() {
        sampleProviderBacklog = new ProviderBacklog(1, "provUser", "hashedPass", "provider", "Test", "P", "User",
                "prov@example.com", LocalDate.now().minusYears(30), "123", 30, "Male", "pubKey", "pending", LocalDateTime.now());

        sampleSeekerBacklog = new SeekerBacklog(1, "seekUser", "hashedPass", "seeker", "SeekerOrg", "Bank",
                "REG123", "seek@example.com", "456", "Addr", "pubKeyS", "pending", LocalDateTime.now());

        sampleCredential = new Credential(1, "testUser", "hashedPassword", "provider");
        sampleProvider = new Provider(1,1, "Prov", "T", "User", "testprov@example.com", "mob", LocalDate.now(), 30, "pk", true);
        sampleSeeker = new Seeker(1,1, "Seek", "Bank", "reg", "testseek@example.com", "mob", "addr", "pk", true);
    }

    // --- Get Backlog Tests ---
    @Test
    void getProviderBacklog_returnsPendingProviders() {
        when(providerBacklogDAO.findByStatus("pending")).thenReturn(List.of(sampleProviderBacklog));
        List<ProviderBacklog> result = adminService.getProviderBacklog();
        assertEquals(1, result.size());
        assertEquals("pending", result.get(0).getStatus());
        verify(providerBacklogDAO).findByStatus("pending");
    }

    @Test
    void getSeekerBacklog_returnsPendingSeekers() {
        when(seekerBacklogDAO.findByStatus("pending")).thenReturn(List.of(sampleSeekerBacklog));
        List<SeekerBacklog> result = adminService.getSeekerBacklog();
        assertEquals(1, result.size());
        assertEquals("pending", result.get(0).getStatus());
        verify(seekerBacklogDAO).findByStatus("pending");
    }

    // --- Process Provider Approval Tests ---
    @Test
    void processProviderApproval_approveSuccess() {
        when(providerBacklogDAO.findById(1)).thenReturn(Optional.of(sampleProviderBacklog));
        when(credentialDAO.findByUsername("provUser")).thenReturn(Optional.empty());
        when(providerDAO.findByEmail("prov@example.com")).thenReturn(Optional.empty());
        when(credentialDAO.saveCredential(any(Credential.class))).thenReturn(10); // New credential ID
        when(providerDAO.saveProvider(any(Provider.class))).thenReturn(true);
        when(providerBacklogDAO.updateStatus(1, "approved")).thenReturn(true);

        boolean result = adminService.processProviderApproval(1, true);
        assertTrue(result);
        verify(credentialDAO).saveCredential(argThat(c -> c.getUsername().equals("provUser")));
        verify(providerDAO).saveProvider(argThat(p -> p.getCredentialId() == 10));
        verify(providerBacklogDAO).updateStatus(1, "approved");
    }

    @Test
    void processProviderApproval_rejectSuccess() {
        when(providerBacklogDAO.findById(1)).thenReturn(Optional.of(sampleProviderBacklog));
        when(providerBacklogDAO.updateStatus(1, "rejected")).thenReturn(true);

        boolean result = adminService.processProviderApproval(1, false); // false for reject
        assertTrue(result);
        verify(providerBacklogDAO).updateStatus(1, "rejected");
        verify(credentialDAO, never()).saveCredential(any());
        verify(providerDAO, never()).saveProvider(any());
    }

    @Test
    void processProviderApproval_backlogNotFound_returnsFalse() {
        when(providerBacklogDAO.findById(1)).thenReturn(Optional.empty());
        boolean result = adminService.processProviderApproval(1, true);
        assertFalse(result);
    }

    @Test
    void processProviderApproval_notPending_returnsFalse() {
        sampleProviderBacklog.setStatus("approved");
        when(providerBacklogDAO.findById(1)).thenReturn(Optional.of(sampleProviderBacklog));
        boolean result = adminService.processProviderApproval(1, true);
        assertFalse(result);
    }

    @Test
    void processProviderApproval_usernameExistsInCredentials_rejectsAndReturnsFalse() {
        when(providerBacklogDAO.findById(1)).thenReturn(Optional.of(sampleProviderBacklog));
        when(credentialDAO.findByUsername(sampleProviderBacklog.getUsername())).thenReturn(Optional.of(new Credential()));
        when(providerBacklogDAO.updateStatus(1, "rejected")).thenReturn(true); // for the automatic rejection

        boolean result = adminService.processProviderApproval(1, true);

        assertFalse(result);
        verify(providerBacklogDAO).updateStatus(1, "rejected");
    }

    @Test
    void processProviderApproval_emailExistsInProviders_rejectsAndReturnsFalse() {
        when(providerBacklogDAO.findById(1)).thenReturn(Optional.of(sampleProviderBacklog));
        when(credentialDAO.findByUsername(anyString())).thenReturn(Optional.empty()); // Username is fine
        when(providerDAO.findByEmail(sampleProviderBacklog.getEmail())).thenReturn(Optional.of(new Provider()));
        when(providerBacklogDAO.updateStatus(1, "rejected")).thenReturn(true);

        boolean result = adminService.processProviderApproval(1, true);

        assertFalse(result);
        verify(providerBacklogDAO).updateStatus(1, "rejected");
    }


    // --- Process Seeker Approval Tests (similar structure to provider) ---
    @Test
    void processSeekerApproval_approveSuccess() {
        when(seekerBacklogDAO.findById(1)).thenReturn(Optional.of(sampleSeekerBacklog));
        when(credentialDAO.findByUsername("seekUser")).thenReturn(Optional.empty());
        when(seekerDAO.findByEmail("seek@example.com")).thenReturn(Optional.empty());
        when(seekerDAO.findByRegistrationNumber("REG123")).thenReturn(Optional.empty());
        when(credentialDAO.saveCredential(any(Credential.class))).thenReturn(11); // New credential ID
        when(seekerDAO.saveSeeker(any(Seeker.class))).thenReturn(true);
        when(seekerBacklogDAO.updateStatus(1, "approved")).thenReturn(true);

        boolean result = adminService.processSeekerApproval(1, true);
        assertTrue(result);
    }

    @Test
    void processSeekerApproval_regNoExists_rejectsAndReturnsFalse() {
        when(seekerBacklogDAO.findById(1)).thenReturn(Optional.of(sampleSeekerBacklog));
        when(credentialDAO.findByUsername(anyString())).thenReturn(Optional.empty());
        when(seekerDAO.findByEmail(anyString())).thenReturn(Optional.empty());
        when(seekerDAO.findByRegistrationNumber(sampleSeekerBacklog.getRegistrationNo())).thenReturn(Optional.of(new Seeker()));
        when(seekerBacklogDAO.updateStatus(1, "rejected")).thenReturn(true);

        boolean result = adminService.processSeekerApproval(1, true);
        assertFalse(result);
        verify(seekerBacklogDAO).updateStatus(1, "rejected");
    }


    // --- Set Active Status Tests ---
    @Test
    void setProviderActiveStatus_success() {
        when(providerDAO.findById(1)).thenReturn(Optional.of(sampleProvider));
        when(providerDAO.updateActiveStatus(1, false)).thenReturn(true);
        // TODO: Mock DataItemDAO and ConsentDAO when they are part of AdminService
        boolean result = adminService.setProviderActiveStatus(1, false);
        assertTrue(result);
        verify(providerDAO).updateActiveStatus(1, false);
    }

    @Test
    void setProviderActiveStatus_providerNotFound_returnsFalse() {
        when(providerDAO.findById(1)).thenReturn(Optional.empty());
        boolean result = adminService.setProviderActiveStatus(1, false);
        assertFalse(result);
    }


    @Test
    void setSeekerActiveStatus_success() {
        when(seekerDAO.findById(1)).thenReturn(Optional.of(sampleSeeker));
        when(seekerDAO.updateActiveStatus(1, true)).thenReturn(true);
        boolean result = adminService.setSeekerActiveStatus(1, true);
        assertTrue(result);
    }

    // --- Get User Lists ---
    @Test
    void getInactiveUsers_combinesProvidersAndSeekers() {
        Provider inactiveProvider = new Provider(1, 10, "Inactive", "P", "User", "ip@example.com", "1", LocalDate.now(), 30, "pkP", false);
        Seeker inactiveSeeker = new Seeker(2, 11, "InactiveSeeker", "Other", "regS", "is@example.com", "2", "addrS", "pkS", false);
        Credential credP = new Credential(10, "inactiveProv", "pass", "provider");
        Credential credS = new Credential(11, "inactiveSeek", "pass", "seeker");

        when(providerDAO.findAllByStatus(false)).thenReturn(List.of(inactiveProvider));
        when(seekerDAO.findAllByStatus(false)).thenReturn(List.of(inactiveSeeker));
        when(credentialDAO.findById(10)).thenReturn(Optional.of(credP));
        when(credentialDAO.findById(11)).thenReturn(Optional.of(credS));

        List<AdminService.UserSummary> result = adminService.getInactiveUsers();
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(u -> u.username.equals("inactiveProv") && u.role.equals("provider")));
        assertTrue(result.stream().anyMatch(u -> u.username.equals("inactiveSeek") && u.role.equals("seeker")));
    }

    @Test
    void getActiveProviders_returnsActiveProviderSummaries() {
        Provider activeProvider = new Provider(1, 10, "Active", "P", "User", "ap@example.com", "1", LocalDate.now(), 30, "pkP", true);
        Credential credP = new Credential(10, "activeProv", "pass", "provider");
        when(providerDAO.findAllByStatus(true)).thenReturn(List.of(activeProvider));
        when(credentialDAO.findById(10)).thenReturn(Optional.of(credP));

        List<AdminService.UserSummary> result = adminService.getActiveProviders();
        assertEquals(1, result.size());
        assertEquals("activeProv", result.get(0).username);
    }

    @Test
    void getActiveSeekers_returnsActiveSeekerSummaries() {
        Seeker activeSeeker = new Seeker(2, 11, "ActiveSeeker", "Bank", "regS", "as@example.com", "2", "addrS", "pkS", true);
        Credential credS = new Credential(11, "activeSeek", "pass", "seeker");
        when(seekerDAO.findAllByStatus(true)).thenReturn(List.of(activeSeeker));
        when(credentialDAO.findById(11)).thenReturn(Optional.of(credS));

        List<AdminService.UserSummary> result = adminService.getActiveSeekers();
        assertEquals(1, result.size());
        assertEquals("activeSeek", result.get(0).username);
    }
}
