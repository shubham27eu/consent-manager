package com.consentmanager.daos;

import com.consentmanager.models.Admin;
import com.consentmanager.utils.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;

public class AdminDAO {

    private static final Logger logger = LoggerFactory.getLogger(AdminDAO.class);
    private Connection connection;

    private static final String INSERT_ADMIN_SQL = "INSERT INTO Admin (credential_id, first_name, middle_name, last_name, email, mobile_no, date_of_birth) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_ADMIN_BY_CREDENTIAL_ID_SQL = "SELECT admin_id, credential_id, first_name, middle_name, last_name, email, mobile_no, date_of_birth FROM Admin WHERE credential_id = ?";
    private static final String SELECT_ADMIN_BY_ID_SQL = "SELECT admin_id, credential_id, first_name, middle_name, last_name, email, mobile_no, date_of_birth FROM Admin WHERE admin_id = ?";
    private static final String SELECT_ADMIN_BY_EMAIL_SQL = "SELECT admin_id, credential_id, first_name, middle_name, last_name, email, mobile_no, date_of_birth FROM Admin WHERE email = ?";

    // Constructor for injecting connection (especially for testing)
    public AdminDAO(Connection connection) {
        this.connection = connection;
    }

    // Default constructor using DatabaseUtil for normal operation
    public AdminDAO() {
        this.connection = null;
    }

    private Connection getConnection() throws SQLException {
        if (this.connection != null && !this.connection.isClosed()) {
            return this.connection;
        }
        return DatabaseUtil.getConnection();
    }

    /**
     * Saves a new admin to the database.
     *
     * @param admin The Admin object to save.
     * @return true if successful, false otherwise.
     */
    public boolean saveAdmin(Admin admin) {
        logger.debug("Attempting to save admin with credential ID: {}", admin.getCredentialId());
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        boolean success = false;

        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(INSERT_ADMIN_SQL, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, admin.getCredentialId());
            preparedStatement.setString(2, admin.getFirstName());
            preparedStatement.setString(3, admin.getMiddleName());
            preparedStatement.setString(4, admin.getLastName());
            preparedStatement.setString(5, admin.getEmail());
            preparedStatement.setString(6, admin.getMobileNo());
            if (admin.getDateOfBirth() != null) {
                preparedStatement.setString(7, admin.getDateOfBirth().toString());
            } else {
                preparedStatement.setNull(7, Types.VARCHAR);
            }

            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        admin.setAdminId(generatedKeys.getInt(1)); // Set the generated admin_id back to the object
                        success = true;
                        logger.info("Admin saved successfully with ID: {} for credential ID: {}", admin.getAdminId(), admin.getCredentialId());
                    }
                }
            } else {
                logger.warn("Saving admin failed, no rows affected for credential ID: {}", admin.getCredentialId());
            }
        } catch (SQLException e) {
            logger.error("Error saving admin with credential ID " + admin.getCredentialId(), e);
        } finally {
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) { // Only close if not injected
                DatabaseUtil.close(conn);
            }
        }
        return success;
    }

    /**
     * Finds an admin by their credential ID.
     *
     * @param credentialId The credential ID to search for.
     * @return An Optional containing the Admin if found, otherwise empty.
     */
    public Optional<Admin> findByCredentialId(int credentialId) {
        logger.debug("Attempting to find admin by credential ID: {}", credentialId);
        return findAdminByField(SELECT_ADMIN_BY_CREDENTIAL_ID_SQL, credentialId, "credential ID");
    }

    /**
     * Finds an admin by their admin ID.
     *
     * @param adminId The admin ID to search for.
     * @return An Optional containing the Admin if found, otherwise empty.
     */
    public Optional<Admin> findById(int adminId) {
        logger.debug("Attempting to find admin by admin ID: {}", adminId);
        return findAdminByField(SELECT_ADMIN_BY_ID_SQL, adminId, "admin ID");
    }

    /**
     * Finds an admin by their email.
     *
     * @param email The email to search for.
     * @return An Optional containing the Admin if found, otherwise empty.
     */
    public Optional<Admin> findByEmail(String email) {
        logger.debug("Attempting to find admin by email: {}", email);
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(SELECT_ADMIN_BY_EMAIL_SQL);
            preparedStatement.setString(1, email);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return Optional.of(mapRowToAdmin(resultSet));
            } else {
                logger.debug("No admin found for email: {}", email);
            }
        } catch (SQLException e) {
            logger.error("Error finding admin by email " + email, e);
        } finally {
            DatabaseUtil.close(resultSet);
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) { // Only close if not injected
                DatabaseUtil.close(conn);
            }
        }
        return Optional.empty();
    }


    private Optional<Admin> findAdminByField(String sql, Object value, String fieldName) {
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(sql);
            if (value instanceof Integer) {
                preparedStatement.setInt(1, (Integer) value);
            } else if (value instanceof String) {
                preparedStatement.setString(1, (String) value);
            } else {
                throw new IllegalArgumentException("Unsupported value type for query.");
            }

            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Admin admin = mapRowToAdmin(resultSet);
                logger.debug("Admin found by {}: {}", fieldName, value);
                return Optional.of(admin);
            } else {
                logger.debug("No admin found for {}: {}", fieldName, value);
            }
        } catch (SQLException e) {
            logger.error("Error finding admin by " + fieldName + " " + value, e);
        } finally {
            DatabaseUtil.close(resultSet);
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) { // Only close if not injected
                DatabaseUtil.close(conn);
            }
        }
        return Optional.empty();
    }

    private Admin mapRowToAdmin(ResultSet resultSet) throws SQLException {
        String dobString = resultSet.getString("date_of_birth");
        LocalDate dateOfBirth = (dobString != null && !dobString.isEmpty()) ? LocalDate.parse(dobString) : null;

        return new Admin(
                resultSet.getInt("admin_id"),
                resultSet.getInt("credential_id"),
                resultSet.getString("first_name"),
                resultSet.getString("middle_name"),
                resultSet.getString("last_name"),
                resultSet.getString("email"),
                resultSet.getString("mobile_no"),
                dateOfBirth
        );
    }
}
