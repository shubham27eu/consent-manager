package com.consentmanager.daos;

import com.consentmanager.models.Credential;
import com.consentmanager.utils.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;

public class CredentialDAO {

    private static final Logger logger = LoggerFactory.getLogger(CredentialDAO.class);
    private Connection connection;

    // SQL queries
    private static final String INSERT_CREDENTIAL_SQL = "INSERT INTO Credential (username, password, role) VALUES (?, ?, ?)";
    private static final String SELECT_CREDENTIAL_BY_USERNAME_SQL = "SELECT credential_id, username, password, role FROM Credential WHERE username = ?";
    private static final String SELECT_CREDENTIAL_BY_ID_SQL = "SELECT credential_id, username, password, role FROM Credential WHERE credential_id = ?";

    // Constructor for injecting connection (especially for testing)
    public CredentialDAO(Connection connection) {
        this.connection = connection;
    }

    // Default constructor using DatabaseUtil for normal operation
    public CredentialDAO() {
        // This will be null here, and connection will be fetched in each method
        // Or, we can decide to fetch it once if DAO instance is short-lived
        this.connection = null;
    }


    private Connection getConnection() throws SQLException {
        if (this.connection != null && !this.connection.isClosed()) {
            return this.connection;
        }
        return DatabaseUtil.getConnection();
    }


    /**
     * Saves a new credential to the database.
     *
     * @param credential The Credential object to save.
     * @return The generated credential_id if successful, otherwise -1.
     */
    public int saveCredential(Credential credential) {
        logger.debug("Attempting to save credential for username: {}", credential.getUsername());
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet generatedKeys = null;
        int generatedId = -1;

        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(INSERT_CREDENTIAL_SQL, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, credential.getUsername());
            preparedStatement.setString(2, credential.getPassword()); // Assumes password is already hashed
            preparedStatement.setString(3, credential.getRole());

            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows > 0) {
                generatedKeys = preparedStatement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    generatedId = generatedKeys.getInt(1);
                    logger.info("Credential saved successfully for username: {} with ID: {}", credential.getUsername(), generatedId);
                }
            } else {
                logger.warn("Saving credential failed, no rows affected for username: {}", credential.getUsername());
            }
        } catch (SQLException e) {
            logger.error("Error saving credential for username " + credential.getUsername(), e);
        } finally {
            DatabaseUtil.close(generatedKeys);
            DatabaseUtil.close(preparedStatement);
            // Only close connection if it was not injected
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return generatedId;
    }

    /**
     * Finds a credential by username.
     *
     * @param username The username to search for.
     * @return An Optional containing the Credential if found, otherwise empty.
     */
    public Optional<Credential> findByUsername(String username) {
        logger.debug("Attempting to find credential by username: {}", username);
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(SELECT_CREDENTIAL_BY_USERNAME_SQL);
            preparedStatement.setString(1, username);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Credential credential = new Credential(
                        resultSet.getInt("credential_id"),
                        resultSet.getString("username"),
                        resultSet.getString("password"),
                        resultSet.getString("role")
                );
                logger.debug("Credential found for username: {}", username);
                return Optional.of(credential);
            } else {
                logger.debug("No credential found for username: {}", username);
            }
        } catch (SQLException e) {
            logger.error("Error finding credential by username " + username, e);
        } finally {
            DatabaseUtil.close(resultSet);
            DatabaseUtil.close(preparedStatement);
            // Only close connection if it was not injected
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return Optional.empty();
    }

    /**
     * Finds a credential by its ID.
     *
     * @param credentialId The ID of the credential.
     * @return An Optional containing the Credential if found, otherwise empty.
     */
    public Optional<Credential> findById(int credentialId) {
        logger.debug("Attempting to find credential by ID: {}", credentialId);
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(SELECT_CREDENTIAL_BY_ID_SQL);
            preparedStatement.setInt(1, credentialId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Credential credential = new Credential(
                        resultSet.getInt("credential_id"),
                        resultSet.getString("username"),
                        resultSet.getString("password"),
                        resultSet.getString("role")
                );
                logger.debug("Credential found for ID: {}", credentialId);
                return Optional.of(credential);
            } else {
                logger.debug("No credential found for ID: {}", credentialId);
            }
        } catch (SQLException e) {
            logger.error("Error finding credential by ID " + credentialId, e);
        } finally {
            DatabaseUtil.close(resultSet);
            DatabaseUtil.close(preparedStatement);
            // Only close connection if it was not injected
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return Optional.empty();
    }
}
