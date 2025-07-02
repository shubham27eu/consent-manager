package com.consentmanager.daos;

import com.consentmanager.models.ConsentHistory;
import com.consentmanager.utils.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConsentHistoryDAO {

    private static final Logger logger = LoggerFactory.getLogger(ConsentHistoryDAO.class);
    private Connection connection;

    private static final String INSERT_CONSENT_HISTORY_SQL = "INSERT INTO ConsentHistory (consent_id, action, actor_id, performed_by_role, details) VALUES (?, ?, ?, ?, ?)";
    private static final String SELECT_CONSENT_HISTORY_BY_CONSENT_ID_SQL = "SELECT history_id, consent_id, action, actor_id, performed_by_role, timestamp, details FROM ConsentHistory WHERE consent_id = ? ORDER BY timestamp DESC";

    public ConsentHistoryDAO(Connection connection) {
        this.connection = connection;
    }

    public ConsentHistoryDAO() {
        this.connection = null;
    }

    private Connection getConnection() throws SQLException {
        if (this.connection != null && !this.connection.isClosed()) {
            return this.connection;
        }
        return DatabaseUtil.getConnection();
    }

    public boolean logAction(ConsentHistory historyEntry) {
        logger.debug("Logging action for consent ID {}: {}", historyEntry.getConsentId(), historyEntry.getAction());
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(INSERT_CONSENT_HISTORY_SQL);
            ps.setInt(1, historyEntry.getConsentId());
            ps.setString(2, historyEntry.getAction());
            if (historyEntry.getActorId() != null) {
                ps.setInt(3, historyEntry.getActorId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setString(4, historyEntry.getPerformedByRole());
            ps.setString(5, historyEntry.getDetails());
            // timestamp is handled by DB default (CURRENT_TIMESTAMP)

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                logger.info("Action logged successfully for consent ID {}.", historyEntry.getConsentId());
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error logging action for consent ID {}: {}", historyEntry.getConsentId(), e.getMessage(), e);
        } finally {
            DatabaseUtil.close(ps);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return false;
    }

    public List<ConsentHistory> findByConsentId(int consentId) {
        logger.debug("Fetching history for consent ID: {}", consentId);
        List<ConsentHistory> historyList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(SELECT_CONSENT_HISTORY_BY_CONSENT_ID_SQL);
            ps.setInt(1, consentId);
            rs = ps.executeQuery();
            while (rs.next()) {
                historyList.add(mapRowToConsentHistory(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching history for consent ID {}: {}", consentId, e.getMessage(), e);
        } finally {
            DatabaseUtil.close(rs);
            DatabaseUtil.close(ps);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return historyList;
    }

    private ConsentHistory mapRowToConsentHistory(ResultSet rs) throws SQLException {
        Timestamp timestampTs = rs.getTimestamp("timestamp");
        Integer actorId = rs.getObject("actor_id") != null ? rs.getInt("actor_id") : null;

        return new ConsentHistory(
                rs.getInt("history_id"),
                rs.getInt("consent_id"),
                rs.getString("action"),
                actorId,
                rs.getString("performed_by_role"),
                timestampTs != null ? timestampTs.toLocalDateTime() : null,
                rs.getString("details")
        );
    }
}
