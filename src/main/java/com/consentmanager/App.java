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
        com.consentmanager.daos.DataItemDAO dataItemDAO = new com.consentmanager.daos.DataItemDAO();
        com.consentmanager.daos.ConsentDAO consentDAO = new com.consentmanager.daos.ConsentDAO();
        com.consentmanager.daos.ConsentHistoryDAO consentHistoryDAO = new com.consentmanager.daos.ConsentHistoryDAO();


        // Initialize Services
        com.consentmanager.services.AuthService authService = new com.consentmanager.services.AuthService(
                credentialDAO, adminDAO, providerDAO, seekerDAO, providerBacklogDAO, seekerBacklogDAO
        );
         com.consentmanager.services.AdminService adminService = new com.consentmanager.services.AdminService(
            providerBacklogDAO, seekerBacklogDAO, credentialDAO, providerDAO, seekerDAO, consentDAO, dataItemDAO // Added consentDAO, dataItemDAO
        );
        com.consentmanager.services.DataService dataService = new com.consentmanager.services.DataService(dataItemDAO, consentDAO);
        com.consentmanager.services.ConsentService consentService = new com.consentmanager.services.ConsentService(consentDAO, dataItemDAO, consentHistoryDAO, seekerDAO);


        // Initialize Controllers & Routes
        // Public routes / Auth routes
        path("/api/auth", () -> {
            new com.consentmanager.controllers.AuthController(authService);
        });

        // Admin routes
        path("/api/admin", () -> {
            before("/*", com.consentmanager.web.middleware.AuthMiddleware.requireAuth);
            before("/*", com.consentmanager.web.middleware.AuthMiddleware.requireAdmin);
            new com.consentmanager.controllers.AdminController(adminService);
        });

        // Provider routes
        path("/api/provider", () -> {
            before("/*", com.consentmanager.web.middleware.AuthMiddleware.requireAuth);
            before("/*", com.consentmanager.web.middleware.AuthMiddleware.requireProvider);
            new com.consentmanager.web.ProviderDataController(dataService, consentService).registerRoutes();
        });

        // Seeker routes
        path("/api/seeker", () -> {
            before("/*", com.consentmanager.web.middleware.AuthMiddleware.requireAuth);
            before("/*", com.consentmanager.web.middleware.AuthMiddleware.requireSeeker);
            new com.consentmanager.web.SeekerDataController(dataService, consentService).registerRoutes();
        });


        logger.info("Consent Manager Java backend started on port " + port());

        // Graceful shutdown
        initExceptionHandler((e) -> {
            logger.error("Spark initialization failed:", e);
            System.exit(100);
        });

        awaitInitialization();
    }
}
