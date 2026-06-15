package com.example.visceralmassageapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

class DevSeedMigrationIT {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("visceral_dev_seed")
                    .withUsername("visceral")
                    .withPassword("visceral");

    static {
        POSTGRES.start();
    }

    @Test
    void stagingFlywayLocationsApplyDevSeedsOnCleanDatabase() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration", "classpath:db/dev")
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.createStatement()) {
            assertThat(count(statement,
                            """
                            SELECT COUNT(*)
                            FROM bookings
                            WHERE status = 'CONFIRMED'
                              AND ends_at < NOW()
                            """))
                    .isGreaterThan(0);
            assertThat(count(statement,
                            """
                            SELECT COUNT(*)
                            FROM bookings
                            WHERE status = 'CANCELLED'
                              AND ends_at < NOW()
                            """))
                    .isGreaterThan(0);
            assertThat(count(statement,
                            """
                            SELECT COUNT(*)
                            FROM fixed_events
                            WHERE active = TRUE
                              AND ends_at < NOW()
                            """))
                    .isGreaterThan(0);
            assertThat(count(statement,
                            """
                            SELECT COUNT(*)
                            FROM specialist_availability_blocks
                            WHERE notes LIKE '%[DEV_STRESS:%'
                              AND starts_at < NOW()
                            """))
                    .isGreaterThan(0);
        }
    }

    private static long count(java.sql.Statement statement, String sql) throws Exception {
        try (var resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
