package com.consentmanager;

import static spark.Spark.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        // Configure Spark port
        port(8080); // Default port, can be configured via ENV var later

        logger.info("Consent Manager Java backend starting...");

        // Simple test route
        get("/hello", (req, res) -> {
            logger.info("Received request for /hello");
            return "Hello from Consent Manager (Java)!";
        });

        // You would initialize your controllers and routes here, for example:
        // new UserController(new UserService(...));
        // new AdminController(new AdminService(...));
        // ...

        // Initialize DAOs
        com.consentmanager.daos.CredentialDAO credentialDAO = new com.consentmanager.daos.CredentialDAO();
        com.consentmanager.daos.AdminDAO adminDAO = new com.consentmanager.daos.AdminDAO();
        com.consentmanager.daos.ProviderDAO providerDAO = new com.consentmanager.daos.ProviderDAO();
        com.consentmanager.daos.SeekerDAO seekerDAO = new com.consentmanager.daos.SeekerDAO();
        com.consentmanager.daos.ProviderBacklogDAO providerBacklogDAO = new com.consentmanager.daos.ProviderBacklogDAO();
        com.consentmanager.daos.SeekerBacklogDAO seekerBacklogDAO = new com.consentmanager.daos.SeekerBacklogDAO();

        // Initialize Services
        com.consentmanager.services.AuthService authService = new com.consentmanager.services.AuthService(
                credentialDAO, adminDAO, providerDAO, seekerDAO, providerBacklogDAO, seekerBacklogDAO
        );

        // Initialize Controllers
        new com.consentmanager.controllers.AuthController(authService);
        // Assuming AdminService will be initialized similarly, for now, a placeholder if not fully ready:
        // com.consentmanager.services.AdminService adminService = new com.consentmanager.services.AdminService(...DAOs...);
        // new com.consentmanager.controllers.AdminController(adminService);
        // For now, let's assume AdminService is ready and DAOs are available
        com.consentmanager.services.AdminService adminService = new com.consentmanager.services.AdminService(
            providerBacklogDAO, seekerBacklogDAO, credentialDAO, providerDAO, seekerDAO
        );
        new com.consentmanager.controllers.AdminController(adminService);


        logger.info("Consent Manager Java backend started on port " + port());

        // Graceful shutdown
        initExceptionHandler((e) -> {
            logger.error("Spark initialization failed:", e);
            System.exit(100);
        });

        awaitInitialization();
    }
}
