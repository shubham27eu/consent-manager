package com.consentmanager.daos;

import com.consentmanager.models.DataItem;
import com.consentmanager.utils.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DataItemDAO {

    private static final Logger logger = LoggerFactory.getLogger(DataItemDAO.class);
    private Connection connection;

    private static final String INSERT_DATA_ITEM_SQL = "INSERT INTO DataItem (provider_id, name, description, type, data, aes_key_encrypted) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SELECT_DATA_ITEM_BY_ID_SQL = "SELECT data_item_id, provider_id, name, description, type, data, aes_key_encrypted, created_at, updated_at FROM DataItem WHERE data_item_id = ?";
    private static final String SELECT_DATA_ITEMS_BY_PROVIDER_ID_SQL = "SELECT data_item_id, provider_id, name, description, type, data, aes_key_encrypted, created_at, updated_at FROM DataItem WHERE provider_id = ?";
    private static final String UPDATE_DATA_ITEM_SQL = "UPDATE DataItem SET name = ?, description = ?, type = ?, data = ?, aes_key_encrypted = ?, updated_at = CURRENT_TIMESTAMP WHERE data_item_id = ? AND provider_id = ?";
    private static final String DELETE_DATA_ITEM_SQL = "DELETE FROM DataItem WHERE data_item_id = ? AND provider_id = ?";
    private static final String SELECT_ALL_DATA_ITEMS_SQL = "SELECT data_item_id, provider_id, name, description, type, data, aes_key_encrypted, created_at, updated_at FROM DataItem";


    public DataItemDAO(Connection connection) {
        this.connection = connection;
    }

    public DataItemDAO() {
        this.connection = null;
    }

    private Connection getConnection() throws SQLException {
        if (this.connection != null && !this.connection.isClosed()) {
            return this.connection;
        }
        return DatabaseUtil.getConnection();
    }

    public DataItem saveDataItem(DataItem dataItem) {
        logger.debug("Saving data item for provider ID: {}", dataItem.getProviderId());
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(INSERT_DATA_ITEM_SQL, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, dataItem.getProviderId());
            ps.setString(2, dataItem.getName());
            ps.setString(3, dataItem.getDescription());
            ps.setString(4, dataItem.getType());
            ps.setString(5, dataItem.getData());
            ps.setString(6, dataItem.getAesKeyEncrypted());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    dataItem.setDataItemId(rs.getInt(1));
                    // Fetch created_at and updated_at as they are set by DB
                    Optional<DataItem> savedItem = findById(dataItem.getDataItemId());
                    if(savedItem.isPresent()){
                        dataItem.setCreatedAt(savedItem.get().getCreatedAt());
                        dataItem.setUpdatedAt(savedItem.get().getUpdatedAt());
                    }
                    logger.info("Data item saved successfully with ID: {}", dataItem.getDataItemId());
                    return dataItem;
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving data item: {}", e.getMessage(), e);
        } finally {
            DatabaseUtil.close(rs);
            DatabaseUtil.close(ps);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return null; // Or throw exception
    }

    public Optional<DataItem> findById(int dataItemId) {
        logger.debug("Finding data item by ID: {}", dataItemId);
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(SELECT_DATA_ITEM_BY_ID_SQL);
            ps.setInt(1, dataItemId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToDataItem(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding data item by ID {}: {}", dataItemId, e.getMessage(), e);
        } finally {
            DatabaseUtil.close(rs);
            DatabaseUtil.close(ps);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return Optional.empty();
    }

    public List<DataItem> findByProviderId(int providerId) {
        logger.debug("Finding data items for provider ID: {}", providerId);
        List<DataItem> dataItems = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(SELECT_DATA_ITEMS_BY_PROVIDER_ID_SQL);
            ps.setInt(1, providerId);
            rs = ps.executeQuery();
            while (rs.next()) {
                dataItems.add(mapRowToDataItem(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding data items for provider ID {}: {}", providerId, e.getMessage(), e);
        } finally {
            DatabaseUtil.close(rs);
            DatabaseUtil.close(ps);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return dataItems;
    }

    public List<DataItem> findAll() {
        logger.debug("Finding all data items");
        List<DataItem> dataItems = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(SELECT_ALL_DATA_ITEMS_SQL);
            rs = ps.executeQuery();
            while (rs.next()) {
                dataItems.add(mapRowToDataItem(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all data items: {}", e.getMessage(), e);
        } finally {
            DatabaseUtil.close(rs);
            DatabaseUtil.close(ps);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return dataItems;
    }


    public boolean updateDataItem(DataItem dataItem) {
        logger.debug("Updating data item ID: {} for provider ID: {}", dataItem.getDataItemId(), dataItem.getProviderId());
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(UPDATE_DATA_ITEM_SQL);
            ps.setString(1, dataItem.getName());
            ps.setString(2, dataItem.getDescription());
            ps.setString(3, dataItem.getType());
            ps.setString(4, dataItem.getData());
            ps.setString(5, dataItem.getAesKeyEncrypted());
            ps.setInt(6, dataItem.getDataItemId());
            ps.setInt(7, dataItem.getProviderId());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                logger.info("Data item {} updated successfully.", dataItem.getDataItemId());
                return true;
            } else {
                logger.warn("Data item {} not found or not updated (owned by provider {}).", dataItem.getDataItemId(), dataItem.getProviderId());
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error updating data item {}: {}", dataItem.getDataItemId(), e.getMessage(), e);
        } finally {
            DatabaseUtil.close(ps);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return false;
    }

    public boolean deleteDataItem(int dataItemId, int providerId) {
        logger.debug("Deleting data item ID: {} for provider ID: {}", dataItemId, providerId);
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(DELETE_DATA_ITEM_SQL);
            ps.setInt(1, dataItemId);
            ps.setInt(2, providerId);
            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                logger.info("Data item {} deleted successfully by provider {}.", dataItemId, providerId);
                return true;
            } else {
                logger.warn("Data item {} not found or not deleted (owned by provider {}).", dataItemId, providerId);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error deleting data item {}: {}", dataItemId, e.getMessage(), e);
        } finally {
            DatabaseUtil.close(ps);
            if (this.connection == null) {
                DatabaseUtil.close(conn);
            }
        }
        return false;
    }

    private DataItem mapRowToDataItem(ResultSet rs) throws SQLException {
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        Timestamp updatedAtTs = rs.getTimestamp("updated_at");
        return new DataItem(
                rs.getInt("data_item_id"),
                rs.getInt("provider_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("type"),
                rs.getString("data"),
                rs.getString("aes_key_encrypted"),
                createdAtTs != null ? createdAtTs.toLocalDateTime() : null,
                updatedAtTs != null ? updatedAtTs.toLocalDateTime() : null
        );
    }
}
