package com.auction.testutil;

import com.auction.server.database.DatabaseConnection;
import org.junit.jupiter.api.Assumptions;

import java.sql.Connection;

/**
 * Guards smoke tests that need the configured MySQL/TiDB database.
 *
 * <p>Unit tests should not depend on external services. The older repository
 * and service smoke tests are still useful locally, but GitHub Actions runners
 * may not have database network access, IP allow-listing or stable fixture
 * data. These tests therefore skip in CI and also skip when the database is not
 * reachable.</p>
 */
public final class DbTestSupport {
    private DbTestSupport() {
    }

    public static void assumeDatabaseAvailable() {
        if (isCi()) {
            Assumptions.assumeTrue(false, "Skipping DB smoke test on CI.");
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            Assumptions.assumeTrue(connection.isValid(2), "Database connection is not valid.");
        } catch (Throwable e) {
            Assumptions.assumeTrue(false, "Database is not available for smoke test: " + e.getMessage());
        }
    }

    private static boolean isCi() {
        return "true".equalsIgnoreCase(System.getenv("CI"));
    }
}
