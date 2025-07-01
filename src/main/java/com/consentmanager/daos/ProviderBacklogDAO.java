package com.consentmanager.daos;

import com.consentmanager.models.ProviderBacklog;
import com.consentmanager.utils.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProviderBacklogDAO {

    private static final Logger logger = LoggerFactory.getLogger(ProviderBacklogDAO.class);
    private Connection connection;

    // Standard SQLite format for CURRENT_TIMESTAMP
    private static final DateTimeFormatter SQLITE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    private static final String INSERT_PROVIDER_BACKLOG_SQL = "INSERT INTO ProviderBacklog (username, password, role, first_name, middle_name, last_name, email, date_of_birth, mobile_no, age, gender, public_key, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_PROVIDER_BACKLOG_BY_ID_SQL = "SELECT backlog_id, username, password, role, first_name, middle_name, last_name, email, date_of_birth, mobile_no, age, gender, public_key, status, created_at FROM ProviderBacklog WHERE backlog_id = ?";
    private static final String SELECT_PROVIDER_BACKLOG_BY_USERNAME_SQL = "SELECT backlog_id, username, password, role, first_name, middle_name, last_name, email, date_of_birth, mobile_no, age, gender, public_key, status, created_at FROM ProviderBacklog WHERE username = ?";
    private static final String SELECT_PROVIDER_BACKLOG_BY_EMAIL_SQL = "SELECT backlog_id, username, password, role, first_name, middle_name, last_name, email, date_of_birth, mobile_no, age, gender, public_key, status, created_at FROM ProviderBacklog WHERE email = ?";
    private static final String SELECT_PROVIDER_BACKLOG_BY_STATUS_SQL = "SELECT backlog_id, username, password, role, first_name, middle_name, last_name, email, date_of_birth, mobile_no, age, gender, public_key, status, created_at FROM ProviderBacklog WHERE status = ? ORDER BY created_at ASC";
    private static final String UPDATE_PROVIDER_BACKLOG_STATUS_SQL = "UPDATE ProviderBacklog SET status = ? WHERE backlog_id = ?";

    // Constructor for injecting connection (especially for testing)
    public ProviderBacklogDAO(Connection connection) {
        this.connection = connection;
    }

    // Default constructor using DatabaseUtil for normal operation
    public ProviderBacklogDAO() {
        this.connection = null;
    }

    private Connection getConnection() throws SQLException {
        if (this.connection != null && !this.connection.isClosed()) {
            return this.connection;
        }
        return DatabaseUtil.getConnection();
    }

    public boolean saveProviderBacklog(ProviderBacklog backlog) {
        logger.debug("Attempting to save provider backlog for username: {}", backlog.getUsername());
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        boolean success = false;

        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(INSERT_PROVIDER_BACKLOG_SQL, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, backlog.getUsername());
            preparedStatement.setString(2, backlog.getPassword()); // Assumes password is ALREADY HASHED by service layer
            preparedStatement.setString(3, backlog.getRole());
            preparedStatement.setString(4, backlog.getFirstName());
            preparedStatement.setString(5, backlog.getMiddleName());
            preparedStatement.setString(6, backlog.getLastName());
            preparedStatement.setString(7, backlog.getEmail());
            preparedStatement.setString(8, backlog.getDateOfBirth().toString());
            preparedStatement.setString(9, backlog.getMobileNo());
            preparedStatement.setInt(10, backlog.getAge());
            preparedStatement.setString(11, backlog.getGender());
            preparedStatement.setString(12, backlog.getPublicKey());
            preparedStatement.setString(13, backlog.getStatus());
            // created_at is handled by DB default

            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows > 0) {
                 try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        backlog.setBacklogId(generatedKeys.getInt(1));
                        // We might need to fetch the created_at if it's not set by default in the object
                        // For now, assume it's handled or not strictly needed immediately after save in the object
                        success = true;
                        logger.info("Provider backlog saved successfully with ID: {} for username: {}", backlog.getBacklogId(), backlog.getUsername());
                    }
                }
            } else {
                logger.warn("Saving provider backlog failed, no rows affected for username: {}", backlog.getUsername());
            }
        } catch (SQLException e) {
            logger.error("Error saving provider backlog for username " + backlog.getUsername(), e);
        } finally {
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return success;
    }

    public Optional<ProviderBacklog> findById(int backlogId) {
        logger.debug("Attempting to find provider backlog by ID: {}", backlogId);
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(SELECT_PROVIDER_BACKLOG_BY_ID_SQL);
            preparedStatement.setInt(1, backlogId);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return Optional.of(mapRowToProviderBacklog(resultSet));
            }
        } catch (SQLException e) {
            logger.error("Error finding provider backlog by ID " + backlogId, e);
        } finally {
            DatabaseUtil.close(resultSet);
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return Optional.empty();
    }

    public Optional<ProviderBacklog> findByUsername(String username) {
        logger.debug("Attempting to find provider backlog by username: {}", username);
         Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(SELECT_PROVIDER_BACKLOG_BY_USERNAME_SQL);
            preparedStatement.setString(1, username);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return Optional.of(mapRowToProviderBacklog(resultSet));
            }
        } catch (SQLException e) {
            logger.error("Error finding provider backlog by username " + username, e);
        } finally {
            DatabaseUtil.close(resultSet);
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return Optional.empty();
    }

    public Optional<ProviderBacklog> findByEmail(String email) {
        logger.debug("Attempting to find provider backlog by email: {}", email);
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(SELECT_PROVIDER_BACKLOG_BY_EMAIL_SQL);
            preparedStatement.setString(1, email);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return Optional.of(mapRowToProviderBacklog(resultSet));
            }
        } catch (SQLException e) {
            logger.error("Error finding provider backlog by email " + email, e);
        } finally {
            DatabaseUtil.close(resultSet);
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return Optional.empty();
    }

    public List<ProviderBacklog> findByStatus(String status) {
        logger.debug("Attempting to find provider backlogs by status: {}", status);
        List<ProviderBacklog> backlogs = new ArrayList<>();
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(SELECT_PROVIDER_BACKLOG_BY_STATUS_SQL);
            preparedStatement.setString(1, status);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                backlogs.add(mapRowToProviderBacklog(resultSet));
            }
            logger.info("Found {} provider backlogs with status: {}", backlogs.size(), status);
        } catch (SQLException e) {
            logger.error("Error finding provider backlogs by status " + status, e);
        } finally {
            DatabaseUtil.close(resultSet);
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return backlogs;
    }

    public boolean updateStatus(int backlogId, String status) {
        logger.debug("Attempting to update status for provider backlog ID: {} to {}", backlogId, status);
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(UPDATE_PROVIDER_BACKLOG_STATUS_SQL);
            preparedStatement.setString(1, status);
            preparedStatement.setInt(2, backlogId);
            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows > 0) {
                logger.info("Successfully updated status for provider backlog ID: {}", backlogId);
                return true;
            } else {
                 logger.warn("Failed to update status, provider backlog not found or status unchanged for ID: {}", backlogId);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error updating status for provider backlog ID " + backlogId, e);
            return false;
        } finally {
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
    }

    private ProviderBacklog mapRowToProviderBacklog(ResultSet resultSet) throws SQLException {
        String createdAtString = resultSet.getString("created_at");
        LocalDateTime createdAt = null;
        if (createdAtString != null) {
            try {
                // Try parsing with or without fractional seconds, as SQLite CURRENT_TIMESTAMP might not include them
                if (createdAtString.contains(".")) {
                     createdAt = LocalDateTime.parse(createdAtString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                } else {
                    createdAt = LocalDateTime.parse(createdAtString, SQLITE_TIMESTAMP_FORMATTER);
                }
            } catch (Exception e) {
                logger.warn("Could not parse created_at timestamp '{}' for backlog_id {}. Defaulting to null. Error: {}",
                    createdAtString, resultSet.getInt("backlog_id"), e.getMessage());
                // Fallback or handle error, e.g., by trying another format or setting to null
                 try {
                    // Attempt to parse assuming it's a direct timestamp value if parsing fails
                    Timestamp ts = resultSet.getTimestamp("created_at");
                    if (ts != null) {
                        createdAt = ts.toLocalDateTime();
                    }
                 } catch (SQLException sqle) {
                     logger.warn("Could not get timestamp directly for created_at for backlog_id {}. Error: {}", resultSet.getInt("backlog_id"), sqle.getMessage());
                 }
            }
        }


        return new ProviderBacklog(
                resultSet.getInt("backlog_id"),
                resultSet.getString("username"),
                resultSet.getString("password"),
                resultSet.getString("role"),
                resultSet.getString("first_name"),
                resultSet.getString("middle_name"),
                resultSet.getString("last_name"),
                resultSet.getString("email"),
                LocalDate.parse(resultSet.getString("date_of_birth")),
                resultSet.getString("mobile_no"),
                resultSet.getInt("age"),
                resultSet.getString("gender"),
                resultSet.getString("public_key"),
                resultSet.getString("status"),
                createdAt
        );
    }
}
