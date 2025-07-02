package com.consentmanager.daos;

import com.consentmanager.models.Consent;
import com.consentmanager.utils.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConsentDAO {

    private static final Logger logger = LoggerFactory.getLogger(ConsentDAO.class);
    private Connection connection;

    private static final String INSERT_CONSENT_SQL = "INSERT INTO Consent (data_item_id, seeker_id, provider_id, status, re_encrypted_aes_key, expires_at, max_access_count) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_CONSENT_BY_ID_SQL = "SELECT consent_id, data_item_id, seeker_id, provider_id, status, re_encrypted_aes_key, requested_at, approved_at, expires_at, access_count, max_access_count FROM Consent WHERE consent_id = ?";
    private static final String UPDATE_CONSENT_STATUS_SQL = "UPDATE Consent SET status = ?, approved_at = ?, expires_at = ? WHERE consent_id = ?";
    private static final String UPDATE_CONSENT_ON_ACCESS_SQL = "UPDATE Consent SET access_count = access_count + 1 WHERE consent_id = ?";
    private static final String SELECT_CONSENTS_BY_PROVIDER_ID_AND_STATUS_SQL = "SELECT consent_id, data_item_id, seeker_id, provider_id, status, re_encrypted_aes_key, requested_at, approved_at, expires_at, access_count, max_access_count FROM Consent WHERE provider_id = ? AND status = ? ORDER BY requested_at DESC";
    private static final String SELECT_CONSENTS_BY_SEEKER_ID_SQL = "SELECT consent_id, data_item_id, seeker_id, provider_id, status, re_encrypted_aes_key, requested_at, approved_at, expires_at, access_count, max_access_count FROM Consent WHERE seeker_id = ? ORDER BY requested_at DESC";
     private static final String SELECT_CONSENT_BY_DATA_ITEM_AND_SEEKER_SQL = "SELECT consent_id, data_item_id, seeker_id, provider_id, status, re_encrypted_aes_key, requested_at, approved_at, expires_at, access_count, max_access_count FROM Consent WHERE data_item_id = ? AND seeker_id = ?";


    public ConsentDAO(Connection connection) {
        this.connection = connection;
    }

    public ConsentDAO() {
        this.connection = null;
    }

    private Connection getConnection() throws SQLException {
        if (this.connection != null && !this.connection.isClosed()) {
            return this.connection;
        }
        return DatabaseUtil.getConnection();
    }

    public Consent createConsent(Consent consent) {
        logger.debug("Creating consent for data item ID: {} by seeker ID: {}", consent.getDataItemId(), consent.getSeekerId());
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(INSERT_CONSENT_SQL, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, consent.getDataItemId());
            ps.setInt(2, consent.getSeekerId());
            ps.setInt(3, consent.getProviderId());
            ps.setString(4, consent.getStatus());
            ps.setString(5, consent.getReEncryptedAesKey()); // This might be null initially if status is 'pending'

            if (consent.getExpiresAt() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(consent.getExpiresAt()));
            } else {
                ps.setNull(6, Types.TIMESTAMP);
            }
            if (consent.getMaxAccessCount() != null) {
                ps.setInt(7, consent.getMaxAccessCount());
            } else {
                ps.setNull(7, Types.INTEGER);
            }

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    consent.setConsentId(rs.getInt(1));
                     // Fetch requested_at as it's set by DB default
                    Optional<Consent> savedConsent = findById(consent.getConsentId());
                    if(savedConsent.isPresent()){
                        consent.setRequestedAt(savedConsent.get().getRequestedAt());
                        // access_count defaults to 0 in schema
                        consent.setAccessCount(savedConsent.get().getAccessCount() != null ? savedConsent.get().getAccessCount() : 0);
                    }
                    logger.info("Consent created successfully with ID: {}", consent.getConsentId());
                    return consent;
                }
            }
        } catch (SQLException e) {
            logger.error("Error creating consent: {}", e.getMessage(), e);
        } finally {
            DatabaseUtil.close(rs);
            DatabaseUtil.close(ps);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return null;
    }

    public Optional<Consent> findById(int consentId) {
        logger.debug("Finding consent by ID: {}", consentId);
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(SELECT_CONSENT_BY_ID_SQL);
            ps.setInt(1, consentId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToConsent(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding consent by ID {}: {}", consentId, e.getMessage(), e);
        } finally {
            DatabaseUtil.close(rs);
            DatabaseUtil.close(ps);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return Optional.empty();
    }

    public Optional<Consent> findByDataItemAndSeeker(int dataItemId, int seekerId) {
        logger.debug("Finding consent by data item ID {} and seeker ID {}", dataItemId, seekerId);
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(SELECT_CONSENT_BY_DATA_ITEM_AND_SEEKER_SQL);
            ps.setInt(1, dataItemId);
            ps.setInt(2, seekerId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToConsent(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding consent by data item ID {} and seeker ID {}: {}", dataItemId, seekerId, e.getMessage(), e);
        } finally {
            DatabaseUtil.close(rs);
            DatabaseUtil.close(ps);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return Optional.empty();
    }


    public boolean updateConsentStatus(int consentId, String status, LocalDateTime approvedAt, LocalDateTime expiresAt, String reEncryptedKey) {
        logger.debug("Updating consent ID: {} to status: {}", consentId, status);
        Connection conn = null;
        PreparedStatement ps = null;
        // Build SQL dynamically for optional reEncryptedKey update
        StringBuilder sqlBuilder = new StringBuilder("UPDATE Consent SET status = ?, approved_at = ?, expires_at = ?");
        if (reEncryptedKey != null) {
            sqlBuilder.append(", re_encrypted_aes_key = ?");
        }
        sqlBuilder.append(" WHERE consent_id = ?");

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sqlBuilder.toString());
            ps.setString(1, status);
            ps.setTimestamp(2, approvedAt != null ? Timestamp.valueOf(approvedAt) : null);
            ps.setTimestamp(3, expiresAt != null ? Timestamp.valueOf(expiresAt) : null);

            int paramIndex = 4;
            if (reEncryptedKey != null) {
                ps.setString(paramIndex++, reEncryptedKey);
            }
            ps.setInt(paramIndex, consentId);

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                logger.info("Consent {} status updated to {}.", consentId, status);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error updating consent status for ID {}: {}", consentId, e.getMessage(), e);
        } finally {
            DatabaseUtil.close(ps);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return false;
    }

    public boolean incrementAccessCount(int consentId) {
        logger.debug("Incrementing access count for consent ID: {}", consentId);
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(UPDATE_CONSENT_ON_ACCESS_SQL);
            ps.setInt(1, consentId);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Error incrementing access count for consent ID {}: {}", consentId, e.getMessage(), e);
        } finally {
            DatabaseUtil.close(ps);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return false;
    }

    public List<Consent> findByProviderIdAndStatus(int providerId, String status) {
        logger.debug("Finding consents for provider ID: {} with status: {}", providerId, status);
        List<Consent> consents = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(SELECT_CONSENTS_BY_PROVIDER_ID_AND_STATUS_SQL);
            ps.setInt(1, providerId);
            ps.setString(2, status);
            rs = ps.executeQuery();
            while (rs.next()) {
                consents.add(mapRowToConsent(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding consents for provider ID {} and status {}: {}", providerId, status, e.getMessage(), e);
        } finally {
            DatabaseUtil.close(rs);
            DatabaseUtil.close(ps);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return consents;
    }

    public List<Consent> findBySeekerId(int seekerId) {
        logger.debug("Finding consents for seeker ID: {}", seekerId);
        List<Consent> consents = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(SELECT_CONSENTS_BY_SEEKER_ID_SQL);
            ps.setInt(1, seekerId);
            rs = ps.executeQuery();
            while (rs.next()) {
                consents.add(mapRowToConsent(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding consents for seeker ID {}: {}", seekerId, e.getMessage(), e);
        } finally {
            DatabaseUtil.close(rs);
            DatabaseUtil.close(ps);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return consents;
    }


    private Consent mapRowToConsent(ResultSet rs) throws SQLException {
        Timestamp requestedAtTs = rs.getTimestamp("requested_at");
        Timestamp approvedAtTs = rs.getTimestamp("approved_at");
        Timestamp expiresAtTs = rs.getTimestamp("expires_at");

        Integer accessCount = rs.getObject("access_count") != null ? rs.getInt("access_count") : null;
        Integer maxAccessCount = rs.getObject("max_access_count") != null ? rs.getInt("max_access_count") : null;


        return new Consent(
                rs.getInt("consent_id"),
                rs.getInt("data_item_id"),
                rs.getInt("seeker_id"),
                rs.getInt("provider_id"),
                rs.getString("status"),
                rs.getString("re_encrypted_aes_key"),
                requestedAtTs != null ? requestedAtTs.toLocalDateTime() : null,
                approvedAtTs != null ? approvedAtTs.toLocalDateTime() : null,
                expiresAtTs != null ? expiresAtTs.toLocalDateTime() : null,
                accessCount,
                maxAccessCount
        );
    }
}
