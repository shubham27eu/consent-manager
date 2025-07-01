package com.consentmanager.daos;

import com.consentmanager.models.SeekerBacklog;
import com.consentmanager.utils.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SeekerBacklogDAO {

    private static final Logger logger = LoggerFactory.getLogger(SeekerBacklogDAO.class);
    private Connection connection;
    private static final DateTimeFormatter SQLITE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String INSERT_SEEKER_BACKLOG_SQL = "INSERT INTO SeekerBacklog (username, password, role, name, type, registration_no, email, contact_no, address, public_key, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_SEEKER_BACKLOG_BY_ID_SQL = "SELECT backlog_id, username, password, role, name, type, registration_no, email, contact_no, address, public_key, status, created_at FROM SeekerBacklog WHERE backlog_id = ?";
    private static final String SELECT_SEEKER_BACKLOG_BY_USERNAME_SQL = "SELECT backlog_id, username, password, role, name, type, registration_no, email, contact_no, address, public_key, status, created_at FROM SeekerBacklog WHERE username = ?";
    private static final String SELECT_SEEKER_BACKLOG_BY_EMAIL_SQL = "SELECT backlog_id, username, password, role, name, type, registration_no, email, contact_no, address, public_key, status, created_at FROM SeekerBacklog WHERE email = ?";
    private static final String SELECT_SEEKER_BACKLOG_BY_STATUS_SQL = "SELECT backlog_id, username, password, role, name, type, registration_no, email, contact_no, address, public_key, status, created_at FROM SeekerBacklog WHERE status = ? ORDER BY created_at ASC";
    private static final String UPDATE_SEEKER_BACKLOG_STATUS_SQL = "UPDATE SeekerBacklog SET status = ? WHERE backlog_id = ?";

    // Constructor for injecting connection (especially for testing)
    public SeekerBacklogDAO(Connection connection) {
        this.connection = connection;
    }

    // Default constructor using DatabaseUtil for normal operation
    public SeekerBacklogDAO() {
        this.connection = null;
    }

    private Connection getConnection() throws SQLException {
        if (this.connection != null && !this.connection.isClosed()) {
            return this.connection;
        }
        return DatabaseUtil.getConnection();
    }

    public boolean saveSeekerBacklog(SeekerBacklog backlog) {
        logger.debug("Attempting to save seeker backlog for username: {}", backlog.getUsername());
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        boolean success = false;

        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(INSERT_SEEKER_BACKLOG_SQL, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, backlog.getUsername());
            preparedStatement.setString(2, backlog.getPassword()); // Assumes password is ALREADY HASHED by service layer
            preparedStatement.setString(3, backlog.getRole());
            preparedStatement.setString(4, backlog.getName());
            preparedStatement.setString(5, backlog.getType());
            preparedStatement.setString(6, backlog.getRegistrationNo());
            preparedStatement.setString(7, backlog.getEmail());
            preparedStatement.setString(8, backlog.getContactNo());
            preparedStatement.setString(9, backlog.getAddress());
            preparedStatement.setString(10, backlog.getPublicKey());
            preparedStatement.setString(11, backlog.getStatus());
            // created_at handled by DB default

            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        backlog.setBacklogId(generatedKeys.getInt(1));
                        success = true;
                        logger.info("Seeker backlog saved successfully with ID: {} for username: {}", backlog.getBacklogId(), backlog.getUsername());
                    }
                }
            } else {
                logger.warn("Saving seeker backlog failed, no rows affected for username: {}", backlog.getUsername());
            }
        } catch (SQLException e) {
            logger.error("Error saving seeker backlog for username " + backlog.getUsername(), e);
        } finally {
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return success;
    }

    public Optional<SeekerBacklog> findById(int backlogId) {
        logger.debug("Attempting to find seeker backlog by ID: {}", backlogId);
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(SELECT_SEEKER_BACKLOG_BY_ID_SQL);
            preparedStatement.setInt(1, backlogId);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return Optional.of(mapRowToSeekerBacklog(resultSet));
            }
        } catch (SQLException e) {
            logger.error("Error finding seeker backlog by ID " + backlogId, e);
        } finally {
            DatabaseUtil.close(resultSet);
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return Optional.empty();
    }

    public Optional<SeekerBacklog> findByUsername(String username) {
        logger.debug("Attempting to find seeker backlog by username: {}", username);
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(SELECT_SEEKER_BACKLOG_BY_USERNAME_SQL);
            preparedStatement.setString(1, username);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return Optional.of(mapRowToSeekerBacklog(resultSet));
            }
        } catch (SQLException e) {
            logger.error("Error finding seeker backlog by username " + username, e);
        } finally {
            DatabaseUtil.close(resultSet);
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return Optional.empty();
    }

    public Optional<SeekerBacklog> findByEmail(String email) {
        logger.debug("Attempting to find seeker backlog by email: {}", email);
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(SELECT_SEEKER_BACKLOG_BY_EMAIL_SQL);
            preparedStatement.setString(1, email);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return Optional.of(mapRowToSeekerBacklog(resultSet));
            }
        } catch (SQLException e) {
            logger.error("Error finding seeker backlog by email " + email, e);
        } finally {
            DatabaseUtil.close(resultSet);
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return Optional.empty();
    }

    public List<SeekerBacklog> findByStatus(String status) {
        logger.debug("Attempting to find seeker backlogs by status: {}", status);
        List<SeekerBacklog> backlogs = new ArrayList<>();
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(SELECT_SEEKER_BACKLOG_BY_STATUS_SQL);
            preparedStatement.setString(1, status);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                backlogs.add(mapRowToSeekerBacklog(resultSet));
            }
            logger.info("Found {} seeker backlogs with status: {}", backlogs.size(), status);
        } catch (SQLException e) {
            logger.error("Error finding seeker backlogs by status " + status, e);
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
        logger.debug("Attempting to update status for seeker backlog ID: {} to {}", backlogId, status);
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(UPDATE_SEEKER_BACKLOG_STATUS_SQL);
            preparedStatement.setString(1, status);
            preparedStatement.setInt(2, backlogId);
            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows > 0) {
                logger.info("Successfully updated status for seeker backlog ID: {}", backlogId);
                return true;
            } else {
                logger.warn("Failed to update status, seeker backlog not found or status unchanged for ID: {}", backlogId);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error updating status for seeker backlog ID " + backlogId, e);
            return false;
        } finally {
            DatabaseUtil.close(preparedStatement);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
    }

    private SeekerBacklog mapRowToSeekerBacklog(ResultSet resultSet) throws SQLException {
         String createdAtString = resultSet.getString("created_at");
        LocalDateTime createdAt = null;
        if (createdAtString != null) {
             try {
                if (createdAtString.contains(".")) {
                     createdAt = LocalDateTime.parse(createdAtString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                } else {
                    createdAt = LocalDateTime.parse(createdAtString, SQLITE_TIMESTAMP_FORMATTER);
                }
            } catch (Exception e) {
                logger.warn("Could not parse created_at timestamp '{}' for backlog_id {}. Defaulting to null. Error: {}",
                    createdAtString, resultSet.getInt("backlog_id"), e.getMessage());
                 try {
                    Timestamp ts = resultSet.getTimestamp("created_at");
                    if (ts != null) {
                        createdAt = ts.toLocalDateTime();
                    }
                 } catch (SQLException sqle) {
                     logger.warn("Could not get timestamp directly for created_at for backlog_id {}. Error: {}", resultSet.getInt("backlog_id"), sqle.getMessage());
                 }
            }
        }

        return new SeekerBacklog(
                resultSet.getInt("backlog_id"),
                resultSet.getString("username"),
                resultSet.getString("password"),
                resultSet.getString("role"),
                resultSet.getString("name"),
                resultSet.getString("type"),
                resultSet.getString("registration_no"),
                resultSet.getString("email"),
                resultSet.getString("contact_no"),
                resultSet.getString("address"),
                resultSet.getString("public_key"),
                resultSet.getString("status"),
                createdAt
        );
    }
}
