package com.auction.server.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static final Properties properties = new Properties();
    private static final HikariDataSource dataSource;

    static {
        try (InputStream input = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new RuntimeException("[FATAL ERROR] Khong tim thay file db.properties trong resources.");
            }

            properties.load(input);
            Class.forName("com.mysql.cj.jdbc.Driver");

            HikariConfig config = new HikariConfig();
            config.setPoolName(getStringProperty("db.pool.name", "AuctionDbPool"));
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setJdbcUrl(requiredProperty("db.url"));
            config.setUsername(requiredProperty("db.user"));
            config.setPassword(requiredProperty("db.password"));

            config.setMaximumPoolSize(getIntProperty("db.pool.maximumPoolSize", 10));
            config.setMinimumIdle(getIntProperty("db.pool.minimumIdle", 2));
            config.setConnectionTimeout(getLongProperty("db.pool.connectionTimeoutMs", 10_000L));
            config.setIdleTimeout(getLongProperty("db.pool.idleTimeoutMs", 600_000L));
            config.setMaxLifetime(getLongProperty("db.pool.maxLifetimeMs", 1_800_000L));

            config.addDataSourceProperty("cachePrepStmts", getStringProperty("db.pool.cachePrepStmts", "true"));
            config.addDataSourceProperty("prepStmtCacheSize", getStringProperty("db.pool.prepStmtCacheSize", "250"));
            config.addDataSourceProperty("prepStmtCacheSqlLimit", getStringProperty("db.pool.prepStmtCacheSqlLimit", "2048"));

            dataSource = new HikariDataSource(config);
            Runtime.getRuntime().addShutdownHook(new Thread(DatabaseConnection::shutdown, "database-pool-shutdown"));

            System.out.println("[DatabaseConnection] HikariCP started. maxPoolSize="
                    + config.getMaximumPoolSize() + ", minIdle=" + config.getMinimumIdle());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("[FATAL ERROR] Khong tim thay MySQL JDBC Driver.", e);
        } catch (Exception e) {
            throw new RuntimeException("[FATAL ERROR] Loi khoi tao DatabaseConnection: " + e.getMessage(), e);
        }
    }

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("[DatabaseConnection] HikariCP stopped.");
        }
    }

    private static String requiredProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required database property: " + key);
        }
        return value.trim();
    }

    private static String getStringProperty(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid integer database property: " + key + "=" + value, e);
        }
    }

    private static long getLongProperty(String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid long database property: " + key + "=" + value, e);
        }
    }
}
