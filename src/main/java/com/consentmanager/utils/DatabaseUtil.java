package com.consentmanager.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseUtil {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseUtil.class);
    private static String appDbUrl = "jdbc:sqlite:consent_manager.db"; // URL for the main application
    public static final String DEFAULT_DB_URL = "jdbc:sqlite:consent_manager.db";
    private static final String IN_MEMORY_DB_URL_PREFIX = "jdbc:sqlite::memory:";
    private static final String SCHEMA_FILE = "/schema.sql";

    private static boolean driverLoaded = false;
    private static volatile boolean defaultSchemaInitialized = false;
    private static final Object defaultInitLock = new Object();

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            logger.info("SQLite JDBC driver loaded successfully.");
            driverLoaded = true;
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load SQLite JDBC driver.", e);
            throw new RuntimeException("Failed to load SQLite JDBC driver.", e);
        }
        // Initialize schema for the default file-based DB for application use
        ensureDefaultSchemaInitialized();
    }

    private static void ensureDefaultSchemaInitialized() {
        synchronized (defaultInitLock) {
            if (defaultSchemaInitialized) {
                return;
            }
            try (Connection conn = DriverManager.getConnection(appDbUrl)) {
                 try (ResultSet rs = conn.getMetaData().getTables(null, null, "Credential", null)) {
                    if (!rs.next()) {
                        logger.info("Default DB schema not found for {}. Initializing...", appDbUrl);
                        executeSchemaScriptOnConnection(conn, appDbUrl);
                    } else {
                        logger.info("Schema for {} already exists (Credential table found).", appDbUrl);
                    }
                }
                defaultSchemaInitialized = true;
            } catch (SQLException e) {
                logger.error("Failed to ensure schema initialization for default DB {}.", appDbUrl, e);
                throw new RuntimeException("Failed to ensure schema initialization for " + appDbUrl, e);
            }
        }
    }

    /**
     * Gets a connection for the main application (file-based DB).
     * Ensures the schema is initialized.
     * @return A Connection to the application database.
     * @throws SQLException if a database access error occurs.
     */
    public static Connection getConnection() throws SQLException {
        if (!driverLoaded) {
            throw new SQLException("JDBC Driver not loaded.");
        }
        if (!defaultSchemaInitialized) { // Should be initialized by static block, but as a safeguard
            ensureDefaultSchemaInitialized();
        }
        Connection connection = DriverManager.getConnection(appDbUrl);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        logger.debug("Acquired application connection to {}", appDbUrl);
        return connection;
    }

    /**
     * Gets a new, isolated in-memory SQLite connection with the schema applied.
     * Intended for testing purposes. Each call returns a connection to a brand new database.
     * @return A new Connection to an in-memory database with schema.
     * @throws SQLException if a database access error occurs or schema script fails.
     */
    public static Connection getTestConnection() throws SQLException {
        if (!driverLoaded) {
            throw new SQLException("JDBC Driver not loaded.");
        }
        // Create a unique in-memory DB for each call by not caching the URL or connection
        String uniqueInMemoryUrl = IN_MEMORY_DB_URL_PREFIX; // SQLite creates a new DB for each :memory: connection
        Connection connection = DriverManager.getConnection(uniqueInMemoryUrl);
        logger.debug("Created new in-memory test connection: {}", uniqueInMemoryUrl);
        try {
            executeSchemaScriptOnConnection(connection, uniqueInMemoryUrl); // Apply schema
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;"); // Enable foreign keys for this connection
            }
        } catch (Exception e) { // Catch SQLException from executeSchema or RuntimeException
            try {
                connection.close(); // Attempt to close connection on error
            } catch (SQLException closeEx) {
                logger.warn("Failed to close test connection after schema execution error.", closeEx);
            }
            throw new SQLException("Failed to initialize test connection with schema.", e);
        }
        return connection;
    }


    /**
     * Executes the schema script on the given connection.
     * @param connection The connection to use for schema execution.
     * @param dbUrlForLog The DB URL string, primarily for accurate logging.
     */
    private static void executeSchemaScriptOnConnection(Connection connection, String dbUrlForLog) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            logger.info("Executing schema script: {} for DB: {}", SCHEMA_FILE, dbUrlForLog);
            InputStream schemaStream = DatabaseUtil.class.getResourceAsStream(SCHEMA_FILE);
            if (schemaStream == null) {
                logger.error("Schema file not found: {}", SCHEMA_FILE);
                throw new RuntimeException("Schema file not found: " + SCHEMA_FILE);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(schemaStream, StandardCharsets.UTF_8))) {
                StringBuilder sqlBuilder = new StringBuilder();
                String line;
                int statementCount = 0;
                while ((line = reader.readLine()) != null) {
                    int commentStartIndex = line.indexOf("--");
                    if (commentStartIndex != -1) {
                        line = line.substring(0, commentStartIndex);
                    }
                    line = line.trim();

                    if (line.isEmpty()) {
                        continue;
                    }
                    sqlBuilder.append(line).append(" ");
                    if (line.endsWith(";")) {
                        try {
                            String sqlStatement = sqlBuilder.toString().trim();
                            if (!sqlStatement.isEmpty()) {
                               statement.execute(sqlStatement);
                            }
                            statementCount++;
                        } catch (SQLException e) {
                            logger.error("Error executing statement from schema.sql: '{}'", sqlBuilder.toString().trim(), e);
                            throw new SQLException("Error executing schema statement: " + sqlBuilder.toString().trim(), e);
                        }
                        sqlBuilder.setLength(0);
                    }
                }
                if (sqlBuilder.length() > 0) {
                     try {
                        String sqlStatement = sqlBuilder.toString().trim();
                        if(!sqlStatement.isEmpty()){
                            statement.execute(sqlStatement);
                        }
                        statementCount++;
                    } catch (SQLException e) {
                        logger.error("Error executing final statement from schema.sql: '{}'", sqlBuilder.toString().trim(), e);
                        throw new SQLException("Error executing final schema statement: " + sqlBuilder.toString().trim(), e);
                    }
                }
                logger.info("Database schema executed successfully for {}. {} statements processed.", dbUrlForLog, statementCount);
            }
        } catch (Exception e) { // Catch broader exceptions from stream handling too
            logger.error("Failed to execute schema script for URL: " + dbUrlForLog, e);
            throw new RuntimeException("Failed to execute schema script for URL: " + dbUrlForLog, e);
        }
    }

    public static void close(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.warn("Error closing connection.", e);
            }
        }
    }

    public static void close(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                logger.warn("Error closing statement.", e);
            }
        }
    }

    public static void close(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                logger.warn("Error closing result set.", e);
            }
        }
    }

    public static void close(Statement statement, Connection connection) {
        close(statement);
        close(connection);
    }

    public static void close(ResultSet resultSet, Statement statement, Connection connection) {
        close(resultSet);
        close(statement);
        close(connection);
    }

    // Main method for testing the database initialization (optional)
    public static void main(String[] args) {
        try {
            // Test default file DB
            logger.info("Testing default file DB initialization...");
            try(Connection conn = DatabaseUtil.getConnection()) {
                 logger.info("Successfully connected to default DB: {}", conn.getMetaData().getURL());
            }
            logger.info("Default file DB test complete.");

            // Test in-memory DB
            logger.info("Testing in-memory DB initialization...");
            try(Connection memConn = DatabaseUtil.getTestConnection()) {
                 logger.info("Successfully connected to and initialized in-memory DB: {}", memConn.getMetaData().getURL());
                 // Optionally, verify tables exist
                 try (ResultSet rs = memConn.getMetaData().getTables(null, null, "Credential", null)) {
                     if (rs.next()) {
                         logger.info("Credential table found in test DB.");
                     } else {
                         logger.error("Credential table NOT found in test DB after getTestConnection().");
                     }
                 }
            }
            logger.info("In-memory DB test complete.");
             logger.info("Testing another in-memory DB to ensure isolation...");
            try(Connection memConn2 = DatabaseUtil.getTestConnection()) {
                 logger.info("Successfully connected to and initialized second in-memory DB: {}", memConn2.getMetaData().getURL());
                 try (ResultSet rs = memConn2.getMetaData().getTables(null, null, "Credential", null)) {
                     if (rs.next()) {
                         logger.info("Credential table found in second test DB.");
                     } else {
                         logger.error("Credential table NOT found in second test DB.");
                     }
                 }
            }
            logger.info("Second In-memory DB test complete.");


        } catch (Exception e) {
            logger.error("DatabaseUtil main test failed.", e);
        }
    }
}
