package com.consentmanager.daos;

import com.consentmanager.models.Provider;
import com.consentmanager.utils.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProviderDAO {

    private static final Logger logger = LoggerFactory.getLogger(ProviderDAO.class);
    private Connection connection;

    private static final String INSERT_PROVIDER_SQL = "INSERT INTO Provider (credential_id, first_name, middle_name, last_name, email, mobile_no, date_of_birth, age, public_key, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_PROVIDER_BY_CREDENTIAL_ID_SQL = "SELECT provider_id, credential_id, first_name, middle_name, last_name, email, mobile_no, date_of_birth, age, public_key, is_active FROM Provider WHERE credential_id = ?";
    private static final String SELECT_PROVIDER_BY_ID_SQL = "SELECT provider_id, credential_id, first_name, middle_name, last_name, email, mobile_no, date_of_birth, age, public_key, is_active FROM Provider WHERE provider_id = ?";
    private static final String SELECT_PROVIDER_BY_EMAIL_SQL = "SELECT provider_id, credential_id, first_name, middle_name, last_name, email, mobile_no, date_of_birth, age, public_key, is_active FROM Provider WHERE email = ?";
    private static final String UPDATE_PROVIDER_ACTIVE_STATUS_SQL = "UPDATE Provider SET is_active = ? WHERE provider_id = ?";
    private static final String SELECT_ALL_PROVIDERS_BY_STATUS_SQL = "SELECT provider_id, credential_id, first_name, middle_name, last_name, email, mobile_no, date_of_birth, age, public_key, is_active FROM Provider WHERE is_active = ?";

    // Constructor for injecting connection (especially for testing)
    public ProviderDAO(Connection connection) {
        this.connection = connection;
    }

    // Default constructor using DatabaseUtil for normal operation
    public ProviderDAO() {
        this.connection = null;
    }

    private Connection getConnection() throws SQLException {
        if (this.connection != null && !this.connection.isClosed()) {
            return this.connection;
        }
        return DatabaseUtil.getConnection();
    }

    public boolean saveProvider(Provider provider) {
        logger.debug("Attempting to save provider with credential ID: {}", provider.getCredentialId());
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        boolean success = false;

        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(INSERT_PROVIDER_SQL, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, provider.getCredentialId());
            preparedStatement.setString(2, provider.getFirstName());
            preparedStatement.setString(3, provider.getMiddleName());
            preparedStatement.setString(4, provider.getLastName());
            preparedStatement.setString(5, provider.getEmail());
            preparedStatement.setString(6, provider.getMobileNo());
            preparedStatement.setString(7, provider.getDateOfBirth().toString());
            preparedStatement.setInt(8, provider.getAge());
            preparedStatement.setString(9, provider.getPublicKey());
            preparedStatement.setInt(10, provider.getIsActive() ? 1 : 0);

            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        provider.setProviderId(generatedKeys.getInt(1));
                        success = true;
                        logger.info("Provider saved successfully with ID: {} for credential ID: {}", provider.getProviderId(), provider.getCredentialId());
                    }
                }
            } else {
                logger.warn("Saving provider failed, no rows affected for credential ID: {}", provider.getCredentialId());
            }
        } catch (SQLException e) {
            logger.error("Error saving provider with credential ID " + provider.getCredentialId(), e);
        } finally {
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) { // Only close if not injected
                DatabaseUtil.close(conn);
            }
        }
        return success;
    }

    public Optional<Provider> findByCredentialId(int credentialId) {
        logger.debug("Attempting to find provider by credential ID: {}", credentialId);
        return findProviderByField(SELECT_PROVIDER_BY_CREDENTIAL_ID_SQL, credentialId, "credential ID");
    }

    public Optional<Provider> findById(int providerId) {
        logger.debug("Attempting to find provider by provider ID: {}", providerId);
        return findProviderByField(SELECT_PROVIDER_BY_ID_SQL, providerId, "provider ID");
    }

    public Optional<Provider> findByEmail(String email) {
        logger.debug("Attempting to find provider by email: {}", email);
         Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(SELECT_PROVIDER_BY_EMAIL_SQL);
            preparedStatement.setString(1, email);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                 return Optional.of(mapRowToProvider(resultSet));
            } else {
                logger.debug("No provider found for email: {}", email);
            }
        } catch (SQLException e) {
            logger.error("Error finding provider by email " + email, e);
        } finally {
            DatabaseUtil.close(resultSet);
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) { // Only close if not injected
                DatabaseUtil.close(conn);
            }
        }
        return Optional.empty();
    }


    public boolean updateActiveStatus(int providerId, boolean isActive) {
        logger.debug("Attempting to update active status for provider ID: {} to {}", providerId, isActive);
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(UPDATE_PROVIDER_ACTIVE_STATUS_SQL);
            preparedStatement.setInt(1, isActive ? 1 : 0);
            preparedStatement.setInt(2, providerId);
            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows > 0) {
                logger.info("Successfully updated active status for provider ID: {}", providerId);
                return true;
            } else {
                logger.warn("Failed to update active status, provider not found or status unchanged for ID: {}", providerId);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error updating active status for provider ID " + providerId, e);
            return false;
        } finally {
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) { // Only close if not injected
                DatabaseUtil.close(conn);
            }
        }
    }

    public List<Provider> findAllByStatus(boolean isActive) {
        logger.debug("Attempting to find all providers with isActive = {}", isActive);
        List<Provider> providers = new ArrayList<>();
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(SELECT_ALL_PROVIDERS_BY_STATUS_SQL);
            preparedStatement.setInt(1, isActive ? 1 : 0);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                providers.add(mapRowToProvider(resultSet));
            }
            logger.info("Found {} providers with isActive = {}", providers.size(), isActive);
        } catch (SQLException e) {
            logger.error("Error finding providers by status " + isActive, e);
        } finally {
            DatabaseUtil.close(resultSet);
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) { // Only close if not injected
                DatabaseUtil.close(conn);
            }
        }
        return providers;
    }


    private Optional<Provider> findProviderByField(String sql, int id, String fieldName) {
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setInt(1, id);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Provider provider = mapRowToProvider(resultSet);
                logger.debug("Provider found by {}: {}", fieldName, id);
                return Optional.of(provider);
            } else {
                logger.debug("No provider found for {}: {}", fieldName, id);
            }
        } catch (SQLException e) {
            logger.error("Error finding provider by " + fieldName + " " + id, e);
        } finally {
            DatabaseUtil.close(resultSet);
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) { // Only close if not injected
                DatabaseUtil.close(conn);
            }
        }
        return Optional.empty();
    }

    private Provider mapRowToProvider(ResultSet resultSet) throws SQLException {
        return new Provider(
                resultSet.getInt("provider_id"),
                resultSet.getInt("credential_id"),
                resultSet.getString("first_name"),
                resultSet.getString("middle_name"),
                resultSet.getString("last_name"),
                resultSet.getString("email"),
                resultSet.getString("mobile_no"),
                LocalDate.parse(resultSet.getString("date_of_birth")),
                resultSet.getInt("age"),
                resultSet.getString("public_key"),
                resultSet.getInt("is_active") == 1
        );
    }
}
