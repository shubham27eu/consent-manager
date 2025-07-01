package com.consentmanager.services;

import com.consentmanager.daos.*;
import com.consentmanager.models.*;
import com.consentmanager.utils.JwtUtil;
import com.consentmanager.utils.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private CredentialDAO credentialDAO;
    @Mock
    private AdminDAO adminDAO;
    @Mock
    private ProviderDAO providerDAO;
    @Mock
    private SeekerDAO seekerDAO;
    @Mock
    private ProviderBacklogDAO providerBacklogDAO;
    @Mock
    private SeekerBacklogDAO seekerBacklogDAO;

    @InjectMocks
    private AuthService authService;

    private ProviderBacklog sampleProviderBacklog;
    private SeekerBacklog sampleSeekerBacklog;
    private Admin sampleAdmin;
    private Credential sampleCredential;

    @BeforeEach
    void setUp() {
        sampleProviderBacklog = new ProviderBacklog("provUser", "rawPassword", "provider", "Test", "Prov", "User",
                "prov@example.com", LocalDate.now().minusYears(30), "123", 30, "Male", "pubKey", "pending");
        sampleProviderBacklog.setBacklogId(1); // Simulate saved entity

        sampleSeekerBacklog = new SeekerBacklog("seekUser", "rawPassword", "seeker", "SeekerOrg", "Bank",
                "REG123", "seek@example.com", "456", "Addr", "pubKeyS", "pending");
        sampleSeekerBacklog.setBacklogId(1);

        sampleAdmin = new Admin(1, "Admin", "Super", "User", "admin@example.com", "789", LocalDate.now().minusYears(40));
        sampleAdmin.setAdminId(1);

        sampleCredential = new Credential(1, "testUser", "hashedPassword", "provider");

    }

    // --- Signup Tests ---

    @Test
    void signupProvider_success() {
        try (MockedStatic<PasswordUtil> mockedPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.hashPassword("rawPassword")).thenReturn("hashedPassword");
            when(providerBacklogDAO.saveProviderBacklog(any(ProviderBacklog.class))).thenReturn(true);
            when(credentialDAO.findByUsername(anyString())).thenReturn(Optional.empty());
            when(providerBacklogDAO.findByUsername(anyString())).thenReturn(Optional.empty());
            when(providerDAO.findByEmail(anyString())).thenReturn(Optional.empty());
            when(providerBacklogDAO.findByEmail(anyString())).thenReturn(Optional.empty());


            AuthService.SignupResponse<ProviderBacklog> response = authService.signupProvider(sampleProviderBacklog);

            assertTrue(response.success);
            assertEquals("Provider signup successful. Pending admin approval.", response.message);
            assertNotNull(response.data);
            assertEquals("hashedPassword", response.data.getPassword());
        }
    }

    @Test
    void signupProvider_usernameExists_fails() {
        when(credentialDAO.findByUsername("provUser")).thenReturn(Optional.of(new Credential())); // Username exists

        AuthService.SignupResponse<ProviderBacklog> response = authService.signupProvider(sampleProviderBacklog);

        assertFalse(response.success);
        assertEquals("Username already exists.", response.message);
        assertNull(response.data);
    }

    @Test
    void signupProvider_emailExists_fails() {
        when(credentialDAO.findByUsername(anyString())).thenReturn(Optional.empty());
        when(providerBacklogDAO.findByUsername(anyString())).thenReturn(Optional.empty());
        when(providerDAO.findByEmail("prov@example.com")).thenReturn(Optional.of(new Provider())); // Email exists

        AuthService.SignupResponse<ProviderBacklog> response = authService.signupProvider(sampleProviderBacklog);

        assertFalse(response.success);
        assertEquals("Email already exists.", response.message);
        assertNull(response.data);
    }


    @Test
    void signupSeeker_success() {
         try (MockedStatic<PasswordUtil> mockedPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.hashPassword("rawPassword")).thenReturn("hashedPassword");
            when(seekerBacklogDAO.saveSeekerBacklog(any(SeekerBacklog.class))).thenReturn(true);
            // Assume username/email don't exist
            when(credentialDAO.findByUsername(anyString())).thenReturn(Optional.empty());
            when(seekerBacklogDAO.findByUsername(anyString())).thenReturn(Optional.empty());
            when(seekerDAO.findByEmail(anyString())).thenReturn(Optional.empty());
            when(seekerBacklogDAO.findByEmail(anyString())).thenReturn(Optional.empty());


            AuthService.SignupResponse<SeekerBacklog> response = authService.signupSeeker(sampleSeekerBacklog);
            assertTrue(response.success);
            assertEquals("Seeker signup successful. Pending admin approval.", response.message);
         }
    }

    @Test
    void signupAdmin_success() {
        try (MockedStatic<PasswordUtil> mockedPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.hashPassword("rawPassword")).thenReturn("hashedPassword");
            when(credentialDAO.saveCredential(any(Credential.class))).thenReturn(1); // Returns generated credId
            when(adminDAO.saveAdmin(any(Admin.class))).thenReturn(true);
            // Assume username/email don't exist
            when(credentialDAO.findByUsername(anyString())).thenReturn(Optional.empty());
            when(adminDAO.findByEmail(anyString())).thenReturn(Optional.empty());


            AuthService.SignupResponse<Admin> response = authService.signupAdmin(sampleAdmin, "adminUser", "rawPassword");
            assertTrue(response.success);
            assertEquals("Admin signup successful.", response.message);
        }
    }

    @Test
    void signupAdmin_credentialSaveFails_fails() {
         try (MockedStatic<PasswordUtil> mockedPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.hashPassword("rawPassword")).thenReturn("hashedPassword");
            when(credentialDAO.saveCredential(any(Credential.class))).thenReturn(-1); // Credential save fails
            when(credentialDAO.findByUsername(anyString())).thenReturn(Optional.empty());
            when(adminDAO.findByEmail(anyString())).thenReturn(Optional.empty());

            AuthService.SignupResponse<Admin> response = authService.signupAdmin(sampleAdmin, "adminUser", "rawPassword");
            assertFalse(response.success);
            assertEquals("Failed to create admin credential.", response.message);
        }
    }

    @Test
    void signupAdmin_adminProfileSaveFails_fails() {
         try (MockedStatic<PasswordUtil> mockedPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.hashPassword("rawPassword")).thenReturn("hashedPassword");
            when(credentialDAO.saveCredential(any(Credential.class))).thenReturn(1);
            when(adminDAO.saveAdmin(any(Admin.class))).thenReturn(false); // Admin profile save fails
            when(credentialDAO.findByUsername(anyString())).thenReturn(Optional.empty());
            when(adminDAO.findByEmail(anyString())).thenReturn(Optional.empty());


            AuthService.SignupResponse<Admin> response = authService.signupAdmin(sampleAdmin, "adminUser", "rawPassword");
            assertFalse(response.success);
            assertEquals("Failed to create admin profile.", response.message);
        }
    }


    // --- Login Tests ---
    @Test
    void login_providerSuccess() {
        Credential cred = new Credential(1, "provUser", "hashedPassword", "provider");
        Provider prov = new Provider(1, 1, "Test", null, "User", "prov@example.com", "123", LocalDate.now(), 30, "key", true);

        try (MockedStatic<PasswordUtil> mockedPasswordUtil = Mockito.mockStatic(PasswordUtil.class);
             MockedStatic<JwtUtil> mockedJwtUtil = Mockito.mockStatic(JwtUtil.class)) {

            when(credentialDAO.findByUsername("provUser")).thenReturn(Optional.of(cred));
            mockedPasswordUtil.when(() -> PasswordUtil.verifyPassword("rawPassword", "hashedPassword")).thenReturn(true);
            when(providerDAO.findByCredentialId(1)).thenReturn(Optional.of(prov));
            mockedJwtUtil.when(() -> JwtUtil.generateToken(1, "provider")).thenReturn("fake.jwt.token");

            AuthService.LoginResponse response = authService.login("provUser", "rawPassword", "provider");

            assertTrue(response.success);
            assertEquals("Provider login successful.", response.message);
            assertEquals("fake.jwt.token", response.token);
            assertEquals(1, response.credentialId);
            assertEquals("provider", response.role);
        }
    }

    @Test
    void login_providerInactive_fails() {
        Credential cred = new Credential(1, "provUserInactive", "hashedPassword", "provider");
        Provider prov = new Provider(1, 1, "Test", null, "User", "provInactive@example.com", "123", LocalDate.now(), 30, "key", false); // Inactive

        try (MockedStatic<PasswordUtil> mockedPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {
            when(credentialDAO.findByUsername("provUserInactive")).thenReturn(Optional.of(cred));
            mockedPasswordUtil.when(() -> PasswordUtil.verifyPassword("rawPassword", "hashedPassword")).thenReturn(true);
            when(providerDAO.findByCredentialId(1)).thenReturn(Optional.of(prov));

            AuthService.LoginResponse response = authService.login("provUserInactive", "rawPassword", "provider");

            assertFalse(response.success);
            assertEquals("Account is inactive.", response.message);
            assertEquals("inactive", response.status);
            assertNull(response.token);
        }
    }

    @Test
    void login_providerInBacklogPending_failsWithStatus() {
        ProviderBacklog backlog = new ProviderBacklog("provBacklogUser", "hashedPassword", "provider", "T", "B", "U",
                "provbacklog@example.com", LocalDate.now(), "123", 30, "F", "key", "pending");

        try (MockedStatic<PasswordUtil> mockedPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {
            when(credentialDAO.findByUsername("provBacklogUser")).thenReturn(Optional.empty()); // Not in credentials
            when(providerBacklogDAO.findByUsername("provBacklogUser")).thenReturn(Optional.of(backlog));
            mockedPasswordUtil.when(() -> PasswordUtil.verifyPassword("rawPassword", "hashedPassword")).thenReturn(true);

            AuthService.LoginResponse response = authService.login("provBacklogUser", "rawPassword", "provider");

            assertFalse(response.success);
            assertEquals("Login attempt for backlogged provider.", response.message);
            assertEquals("pending", response.status);
            assertNull(response.token);
        }
    }

    @Test
    void login_providerInBacklogRejected_failsWithStatus() {
        ProviderBacklog backlog = new ProviderBacklog("provRejectedUser", "hashedPassword", "provider", "T", "B", "U",
                "provRejected@example.com", LocalDate.now(), "123", 30, "F", "key", "rejected");

        try (MockedStatic<PasswordUtil> mockedPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {
            when(credentialDAO.findByUsername("provRejectedUser")).thenReturn(Optional.empty());
            when(providerBacklogDAO.findByUsername("provRejectedUser")).thenReturn(Optional.of(backlog));
            mockedPasswordUtil.when(() -> PasswordUtil.verifyPassword("rawPassword", "hashedPassword")).thenReturn(true);

            AuthService.LoginResponse response = authService.login("provRejectedUser", "rawPassword", "provider");

            assertFalse(response.success);
            assertEquals("Login attempt for backlogged provider.", response.message);
            assertEquals("rejected", response.status);
            assertNull(response.token);
        }
    }


    @Test
    void login_adminSuccess() {
        Credential cred = new Credential(2, "adminUser", "hashedPassword", "admin");
        // Admin model does not have age, removed it from constructor call
        Admin admin = new Admin(2, 2, "Admin", null, "User", "admin@example.com", "456", LocalDate.now().minusYears(40));


         try (MockedStatic<PasswordUtil> mockedPasswordUtil = Mockito.mockStatic(PasswordUtil.class);
             MockedStatic<JwtUtil> mockedJwtUtil = Mockito.mockStatic(JwtUtil.class)) {

            when(credentialDAO.findByUsername("adminUser")).thenReturn(Optional.of(cred));
            mockedPasswordUtil.when(() -> PasswordUtil.verifyPassword("rawPassword", "hashedPassword")).thenReturn(true);
            when(adminDAO.findByCredentialId(2)).thenReturn(Optional.of(admin));
            mockedJwtUtil.when(() -> JwtUtil.generateToken(2, "admin")).thenReturn("fake.admin.jwt.token");

            AuthService.LoginResponse response = authService.login("adminUser", "rawPassword", "admin");

            assertTrue(response.success);
            assertEquals("Admin login successful.", response.message);
            assertEquals("fake.admin.jwt.token", response.token);
        }
    }

    @Test
    void login_invalidPassword_fails() {
         try (MockedStatic<PasswordUtil> mockedPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {
            when(credentialDAO.findByUsername("testUser")).thenReturn(Optional.of(sampleCredential));
            mockedPasswordUtil.when(() -> PasswordUtil.verifyPassword("wrongPassword", "hashedPassword")).thenReturn(false);

            AuthService.LoginResponse response = authService.login("testUser", "wrongPassword", "provider");

            assertFalse(response.success);
            assertEquals("Invalid username, password, or role.", response.message);
            assertNull(response.token);
        }
    }

    @Test
    void login_userNotFound_fails() {
        when(credentialDAO.findByUsername("unknownUser")).thenReturn(Optional.empty());
        when(providerBacklogDAO.findByUsername("unknownUser")).thenReturn(Optional.empty()); // Also not in backlog

        AuthService.LoginResponse response = authService.login("unknownUser", "password", "provider");

        assertFalse(response.success);
        assertEquals("Invalid username, password, or role.", response.message);
    }
}
