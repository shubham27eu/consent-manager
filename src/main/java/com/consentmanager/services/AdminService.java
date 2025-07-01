package com.consentmanager.services;

import com.consentmanager.daos.*;
import com.consentmanager.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    private final ProviderBacklogDAO providerBacklogDAO;
    private final SeekerBacklogDAO seekerBacklogDAO;
    private final CredentialDAO credentialDAO;
    private final ProviderDAO providerDAO;
    private final SeekerDAO seekerDAO;
    // Will need DataItemDAO and ConsentDAO for cascading deactivation/activation
    // For now, let's assume they will be added to constructor when those DAOs are created
    // private final DataItemDAO dataItemDAO;
    // private final ConsentDAO consentDAO;


    public AdminService(ProviderBacklogDAO providerBacklogDAO, SeekerBacklogDAO seekerBacklogDAO,
                        CredentialDAO credentialDAO, ProviderDAO providerDAO, SeekerDAO seekerDAO) {
        this.providerBacklogDAO = providerBacklogDAO;
        this.seekerBacklogDAO = seekerBacklogDAO;
        this.credentialDAO = credentialDAO;
        this.providerDAO = providerDAO;
        this.seekerDAO = seekerDAO;
        // this.dataItemDAO = dataItemDAO; // To be added
        // this.consentDAO = consentDAO;   // To be added
    }

    public List<ProviderBacklog> getProviderBacklog() {
        logger.info("Fetching provider backlog.");
        return providerBacklogDAO.findByStatus("pending");
    }

    public List<SeekerBacklog> getSeekerBacklog() {
        logger.info("Fetching seeker backlog.");
        return seekerBacklogDAO.findByStatus("pending");
    }

    /**
     * Approves or rejects a provider signup request.
     * If approved, moves the provider from backlog to active Provider and Credential tables.
     *
     * @param backlogProviderId The ID of the provider in the backlog.
     * @param approve           True to approve, false to reject.
     * @return True if the action was successful, false otherwise.
     */
    public boolean processProviderApproval(int backlogProviderId, boolean approve) {
        logger.info("Processing provider approval for backlog ID: {}, Action: {}", backlogProviderId, approve ? "approve" : "reject");
        Optional<ProviderBacklog> backlogOpt = providerBacklogDAO.findById(backlogProviderId);

        if (backlogOpt.isEmpty()) {
            logger.warn("Provider backlog not found for ID: {}", backlogProviderId);
            return false;
        }
        ProviderBacklog backlog = backlogOpt.get();

        if (!"pending".equals(backlog.getStatus())) {
            logger.warn("Provider backlog ID: {} is not in 'pending' status. Current status: {}", backlogProviderId, backlog.getStatus());
            return false; // Or throw an exception for invalid state
        }

        if (approve) {
            // Check if username or email already exists in main tables (should ideally not happen if signup checks are robust)
             if (credentialDAO.findByUsername(backlog.getUsername()).isPresent()) {
                logger.warn("Username {} already exists in Credentials. Cannot approve backlog ID: {}", backlog.getUsername(), backlogProviderId);
                // Optionally, reject the backlog item here
                providerBacklogDAO.updateStatus(backlogProviderId, "rejected");
                return false;
            }
            if (providerDAO.findByEmail(backlog.getEmail()).isPresent()) {
                logger.warn("Email {} already exists for an active Provider. Cannot approve backlog ID: {}", backlog.getEmail(), backlogProviderId);
                providerBacklogDAO.updateStatus(backlogProviderId, "rejected");
                return false;
            }


            Credential credential = new Credential(backlog.getUsername(), backlog.getPassword(), "provider");
            // Password in backlog is already hashed, so CredentialDAO's hook should skip re-hashing.
            int credentialId = credentialDAO.saveCredential(credential);

            if (credentialId != -1) {
                Provider provider = new Provider(
                        credentialId,
                        backlog.getFirstName(),
                        backlog.getMiddleName(),
                        backlog.getLastName(),
                        backlog.getEmail(),
                        backlog.getMobileNo(),
                        backlog.getDateOfBirth(),
                        backlog.getAge(),
                        backlog.getPublicKey(),
                        true // isActive
                );
                if (providerDAO.saveProvider(provider)) {
                    boolean statusUpdated = providerBacklogDAO.updateStatus(backlogProviderId, "approved");
                    if (!statusUpdated) {
                         logger.error("Failed to update provider backlog status to 'approved' for ID: {} after successful approval.", backlogProviderId);
                        // This indicates a potential data inconsistency. Consider rollback or further logging/alerting.
                    }
                    logger.info("Provider from backlog ID: {} approved successfully. New Provider ID: {}, Credential ID: {}", backlogProviderId, provider.getProviderId(), credentialId);
                    return true;
                } else {
                    logger.error("Failed to save provider profile for backlog ID: {} after creating credential ID: {}. Attempting to clean up credential.", backlogProviderId, credentialId);
                    // TODO: Implement rollback for credential if provider save fails.
                    // For now, this is a critical error state.
                    return false;
                }
            } else {
                logger.error("Failed to save credential for provider from backlog ID: {}", backlogProviderId);
                return false;
            }
        } else { // Reject
            boolean statusUpdated = providerBacklogDAO.updateStatus(backlogProviderId, "rejected");
             if (statusUpdated) {
                logger.info("Provider backlog ID: {} rejected successfully.", backlogProviderId);
                return true;
            } else {
                logger.error("Failed to update provider backlog status to 'rejected' for ID: {}", backlogProviderId);
                return false;
            }
        }
    }

    public boolean processSeekerApproval(int backlogSeekerId, boolean approve) {
        logger.info("Processing seeker approval for backlog ID: {}, Action: {}", backlogSeekerId, approve ? "approve" : "reject");
        Optional<SeekerBacklog> backlogOpt = seekerBacklogDAO.findById(backlogSeekerId);

        if (backlogOpt.isEmpty()) {
            logger.warn("Seeker backlog not found for ID: {}", backlogSeekerId);
            return false;
        }
        SeekerBacklog backlog = backlogOpt.get();

        if (!"pending".equals(backlog.getStatus())) {
            logger.warn("Seeker backlog ID: {} is not in 'pending' status. Current status: {}", backlogSeekerId, backlog.getStatus());
            return false;
        }

        if (approve) {
             if (credentialDAO.findByUsername(backlog.getUsername()).isPresent()) {
                logger.warn("Username {} already exists in Credentials. Cannot approve backlog ID: {}", backlog.getUsername(), backlogSeekerId);
                seekerBacklogDAO.updateStatus(backlogSeekerId, "rejected");
                return false;
            }
            if (seekerDAO.findByEmail(backlog.getEmail()).isPresent()) { // Check main Seeker table by email
                logger.warn("Email {} already exists for an active Seeker. Cannot approve backlog ID: {}", backlog.getEmail(), backlogSeekerId);
                seekerBacklogDAO.updateStatus(backlogSeekerId, "rejected");
                return false;
            }
            // Also check registration_no for uniqueness in Seeker table
            if (seekerDAO.findByRegistrationNumber(backlog.getRegistrationNo()).isPresent()) {
                 logger.warn("Registration number {} already exists for an active Seeker. Cannot approve backlog ID: {}", backlog.getRegistrationNo(), backlogSeekerId);
                seekerBacklogDAO.updateStatus(backlogSeekerId, "rejected");
                return false;
            }


            Credential credential = new Credential(backlog.getUsername(), backlog.getPassword(), "seeker");
            int credentialId = credentialDAO.saveCredential(credential);

            if (credentialId != -1) {
                Seeker seeker = new Seeker(
                        credentialId,
                        backlog.getName(),
                        backlog.getType(),
                        backlog.getRegistrationNo(),
                        backlog.getEmail(),
                        backlog.getContactNo(),
                        backlog.getAddress(),
                        backlog.getPublicKey(),
                        true // isActive
                );
                if (seekerDAO.saveSeeker(seeker)) {
                    boolean statusUpdated = seekerBacklogDAO.updateStatus(backlogSeekerId, "approved");
                     if (!statusUpdated) {
                         logger.error("Failed to update seeker backlog status to 'approved' for ID: {} after successful approval.", backlogSeekerId);
                    }
                    logger.info("Seeker from backlog ID: {} approved successfully. New Seeker ID: {}, Credential ID: {}", backlogSeekerId, seeker.getSeekerId(), credentialId);
                    return true;
                } else {
                    logger.error("Failed to save seeker profile for backlog ID: {} after creating credential ID: {}. Attempting to clean up credential.", backlogSeekerId, credentialId);
                    // TODO: Implement rollback for credential if seeker save fails.
                    return false;
                }
            } else {
                logger.error("Failed to save credential for seeker from backlog ID: {}", backlogSeekerId);
                return false;
            }
        } else { // Reject
            boolean statusUpdated = seekerBacklogDAO.updateStatus(backlogSeekerId, "rejected");
            if (statusUpdated) {
                logger.info("Seeker backlog ID: {} rejected successfully.", backlogSeekerId);
                return true;
            } else {
                logger.error("Failed to update seeker backlog status to 'rejected' for ID: {}", backlogSeekerId);
                return false;
            }
        }
    }

    public boolean setProviderActiveStatus(int providerId, boolean isActive) {
        logger.info("Setting provider ID: {} active status to: {}", providerId, isActive);
        Optional<Provider> providerOpt = providerDAO.findById(providerId);
        if (providerOpt.isEmpty()) {
            logger.warn("Provider not found with ID: {}", providerId);
            return false;
        }
        // TODO: When DataItemDAO and ConsentDAO are available,
        // also update isActive status of related DataItems and Consents if provider is inactivated.
        // If reactivating, DataItems and Consents related might need selective reactivation
        // or remain as they were (e.g. if individually deactivated).
        // For now, just updating provider.
        // if (!isActive) {
        // dataItemDAO.updateActiveStatusByOwner(providerId, false);
        // consentDAO.updateActiveStatusByProvider(providerId, false);
        // }
        return providerDAO.updateActiveStatus(providerId, isActive);
    }

    public boolean setSeekerActiveStatus(int seekerId, boolean isActive) {
        logger.info("Setting seeker ID: {} active status to: {}", seekerId, isActive);
         Optional<Seeker> seekerOpt = seekerDAO.findById(seekerId);
        if (seekerOpt.isEmpty()) {
            logger.warn("Seeker not found with ID: {}", seekerId);
            return false;
        }
        // If seeker is inactivated, related consents might also need to be inactivated.
        // if (!isActive) {
        // consentDAO.updateActiveStatusBySeeker(seekerId, false);
        // }
        return seekerDAO.updateActiveStatus(seekerId, isActive);
    }

    public List<UserSummary> getInactiveUsers() {
        logger.info("Fetching all inactive users.");
        List<Provider> inactiveProviders = providerDAO.findAllByStatus(false);
        List<Seeker> inactiveSeekers = seekerDAO.findAllByStatus(false);

        Stream<UserSummary> providerSummaries = inactiveProviders.stream()
            .map(p -> new UserSummary(p.getProviderId(), getUsernameForCredential(p.getCredentialId()), "provider", p.getEmail(), p.getFirstName() + " " + p.getLastName(), false));
        Stream<UserSummary> seekerSummaries = inactiveSeekers.stream()
            .map(s -> new UserSummary(s.getSeekerId(), getUsernameForCredential(s.getCredentialId()), "seeker", s.getEmail(), s.getName(), false));

        return Stream.concat(providerSummaries, seekerSummaries).collect(Collectors.toList());
    }

    public List<UserSummary> getActiveProviders() {
        logger.info("Fetching all active providers.");
        return providerDAO.findAllByStatus(true).stream()
            .map(p -> new UserSummary(p.getProviderId(), getUsernameForCredential(p.getCredentialId()), "provider", p.getEmail(), p.getFirstName() + " " + p.getLastName(), true))
            .collect(Collectors.toList());
    }

    public List<UserSummary> getActiveSeekers() {
        logger.info("Fetching all active seekers.");
         return seekerDAO.findAllByStatus(true).stream()
            .map(s -> new UserSummary(s.getSeekerId(), getUsernameForCredential(s.getCredentialId()), "seeker", s.getEmail(), s.getName(), true))
            .collect(Collectors.toList());
    }

    private String getUsernameForCredential(Integer credentialId) {
        if (credentialId == null) return "N/A";
        return credentialDAO.findById(credentialId)
                            .map(Credential::getUsername)
                            .orElse("N/A (Credential not found)");
    }

    // DTO for user summary listings
    public static class UserSummary {
        public int id; // provider_id or seeker_id
        public String username;
        public String role;
        public String email;
        public String name; // Full name for provider, org name for seeker
        public boolean isActive;

        public UserSummary(int id, String username, String role, String email, String name, boolean isActive) {
            this.id = id;
            this.username = username;
            this.role = role;
            this.email = email;
            this.name = name;
            this.isActive = isActive;
        }
    }
}
