package com.consentmanager.daos;

import com.consentmanager.models.Seeker;
import com.consentmanager.utils.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SeekerDAO {

    private static final Logger logger = LoggerFactory.getLogger(SeekerDAO.class);
    private Connection connection;

    private static final String INSERT_SEEKER_SQL = "INSERT INTO Seeker (credential_id, name, type, registration_no, email, contact_no, address, public_key, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_SEEKER_BY_CREDENTIAL_ID_SQL = "SELECT seeker_id, credential_id, name, type, registration_no, email, contact_no, address, public_key, is_active FROM Seeker WHERE credential_id = ?";
    private static final String SELECT_SEEKER_BY_ID_SQL = "SELECT seeker_id, credential_id, name, type, registration_no, email, contact_no, address, public_key, is_active FROM Seeker WHERE seeker_id = ?";
    private static final String SELECT_SEEKER_BY_EMAIL_SQL = "SELECT seeker_id, credential_id, name, type, registration_no, email, contact_no, address, public_key, is_active FROM Seeker WHERE email = ?";
    private static final String SELECT_SEEKER_BY_REG_NO_SQL = "SELECT seeker_id, credential_id, name, type, registration_no, email, contact_no, address, public_key, is_active FROM Seeker WHERE registration_no = ?";
    private static final String UPDATE_SEEKER_ACTIVE_STATUS_SQL = "UPDATE Seeker SET is_active = ? WHERE seeker_id = ?";
    private static final String SELECT_ALL_SEEKERS_BY_STATUS_SQL = "SELECT seeker_id, credential_id, name, type, registration_no, email, contact_no, address, public_key, is_active FROM Seeker WHERE is_active = ?";

    // Constructor for injecting connection (especially for testing)
    public SeekerDAO(Connection connection) {
        this.connection = connection;
    }

    // Default constructor using DatabaseUtil for normal operation
    public SeekerDAO() {
        this.connection = null;
    }

    private Connection getConnection() throws SQLException {
        if (this.connection != null && !this.connection.isClosed()) {
            return this.connection;
        }
        return DatabaseUtil.getConnection();
    }

    public boolean saveSeeker(Seeker seeker) {
        logger.debug("Attempting to save seeker with credential ID: {}", seeker.getCredentialId());
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        boolean success = false;

        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(INSERT_SEEKER_SQL, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, seeker.getCredentialId());
            preparedStatement.setString(2, seeker.getName());
            preparedStatement.setString(3, seeker.getType());
            preparedStatement.setString(4, seeker.getRegistrationNo());
            preparedStatement.setString(5, seeker.getEmail());
            preparedStatement.setString(6, seeker.getContactNo());
            preparedStatement.setString(7, seeker.getAddress());
            preparedStatement.setString(8, seeker.getPublicKey());
            preparedStatement.setInt(9, seeker.getIsActive() ? 1 : 0);

            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        seeker.setSeekerId(generatedKeys.getInt(1));
                        success = true;
                        logger.info("Seeker saved successfully with ID: {} for credential ID: {}", seeker.getSeekerId(), seeker.getCredentialId());
                    }
                }
            } else {
                logger.warn("Saving seeker failed, no rows affected for credential ID: {}", seeker.getCredentialId());
            }
        } catch (SQLException e) {
            logger.error("Error saving seeker with credential ID " + seeker.getCredentialId(), e);
        } finally {
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return success;
    }

    public Optional<Seeker> findByCredentialId(int credentialId) {
        logger.debug("Attempting to find seeker by credential ID: {}", credentialId);
        return findSeekerByField(SELECT_SEEKER_BY_CREDENTIAL_ID_SQL, credentialId, "credential ID");
    }

    public Optional<Seeker> findById(int seekerId) {
        logger.debug("Attempting to find seeker by seeker ID: {}", seekerId);
        return findSeekerByField(SELECT_SEEKER_BY_ID_SQL, seekerId, "seeker ID");
    }

    public Optional<Seeker> findByEmail(String email) {
        logger.debug("Attempting to find seeker by email: {}", email);
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(SELECT_SEEKER_BY_EMAIL_SQL);
            preparedStatement.setString(1, email);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return Optional.of(mapRowToSeeker(resultSet));
            } else {
                logger.debug("No seeker found for email: {}", email);
            }
        } catch (SQLException e) {
            logger.error("Error finding seeker by email " + email, e);
        } finally {
            DatabaseUtil.close(resultSet);
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return Optional.empty();
    }

    public Optional<Seeker> findByRegistrationNumber(String registrationNo) {
        logger.debug("Attempting to find seeker by registration number: {}", registrationNo);
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(SELECT_SEEKER_BY_REG_NO_SQL);
            preparedStatement.setString(1, registrationNo);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return Optional.of(mapRowToSeeker(resultSet));
            } else {
                logger.debug("No seeker found for registration number: {}", registrationNo);
            }
        } catch (SQLException e) {
            logger.error("Error finding seeker by registration number " + registrationNo, e);
        } finally {
            DatabaseUtil.close(resultSet);
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return Optional.empty();
    }


    public boolean updateActiveStatus(int seekerId, boolean isActive) {
        logger.debug("Attempting to update active status for seeker ID: {} to {}", seekerId, isActive);
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(UPDATE_SEEKER_ACTIVE_STATUS_SQL);
            preparedStatement.setInt(1, isActive ? 1 : 0);
            preparedStatement.setInt(2, seekerId);
            int affectedRows = preparedStatement.executeUpdate();
             if (affectedRows > 0) {
                logger.info("Successfully updated active status for seeker ID: {}", seekerId);
                return true;
            } else {
                logger.warn("Failed to update active status, seeker not found or status unchanged for ID: {}", seekerId);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error updating active status for seeker ID " + seekerId, e);
            return false;
        } finally {
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
    }

    public List<Seeker> findAllByStatus(boolean isActive) {
        logger.debug("Attempting to find all seekers with isActive = {}", isActive);
        List<Seeker> seekers = new ArrayList<>();
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(SELECT_ALL_SEEKERS_BY_STATUS_SQL);
            preparedStatement.setInt(1, isActive ? 1 : 0);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                seekers.add(mapRowToSeeker(resultSet));
            }
            logger.info("Found {} seekers with isActive = {}", seekers.size(), isActive);
        } catch (SQLException e) {
            logger.error("Error finding seekers by status " + isActive, e);
        } finally {
            DatabaseUtil.close(resultSet);
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return seekers;
    }

    private Optional<Seeker> findSeekerByField(String sql, int id, String fieldName) {
         Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setInt(1, id);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Seeker seeker = mapRowToSeeker(resultSet);
                logger.debug("Seeker found by {}: {}", fieldName, id);
                return Optional.of(seeker);
            } else {
                logger.debug("No seeker found for {}: {}", fieldName, id);
            }
        } catch (SQLException e) {
            logger.error("Error finding seeker by " + fieldName + " " + id, e);
        } finally {
            DatabaseUtil.close(resultSet);
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return Optional.empty();
    }

    private Seeker mapRowToSeeker(ResultSet resultSet) throws SQLException {
        return new Seeker(
                resultSet.getInt("seeker_id"),
                resultSet.getInt("credential_id"),
                resultSet.getString("name"),
                resultSet.getString("type"),
                resultSet.getString("registration_no"),
                resultSet.getString("email"),
                resultSet.getString("contact_no"),
                resultSet.getString("address"),
                resultSet.getString("public_key"),
                resultSet.getInt("is_active") == 1
        );
    }
}
