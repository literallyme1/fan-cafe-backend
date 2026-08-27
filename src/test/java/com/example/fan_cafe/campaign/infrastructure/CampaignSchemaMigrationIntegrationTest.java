package com.example.fan_cafe.campaign.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("integration")
class CampaignSchemaMigrationIntegrationTest {
    private static final String URL =
            "jdbc:h2:mem:campaign-migration;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";

    @Test
    void existingOrderSchema_isMigratedToCampaignSchema() throws SQLException {
        prepareExistingOrderSchema();

        Flyway flyway = Flyway.configure()
                .dataSource(URL, "sa", "")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("8")
                .load();

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);

        try (var connection = DriverManager.getConnection(URL, "sa", "");
             var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SELECT order_type FROM orders WHERE id = 1")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("order_type")).isEqualTo("MERCHANDISE");
            }

            statement.executeUpdate("""
                    INSERT INTO orders (id, user_id, status) VALUES (2, 1, 'PAYMENT_PENDING')
                    """);
            try (var result = statement.executeQuery("SELECT order_type FROM orders WHERE id = 2")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("order_type")).isEqualTo("MERCHANDISE");
            }

            statement.executeUpdate(validCampaignInsert(10, "1000.00"));
            statement.executeUpdate(validCampaignInsert(11, "2000.00"));
            try (var result = statement.executeQuery("""
                    SELECT payment_approved_at FROM saga_instance WHERE saga_id = 'saga-1'
                    """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getTimestamp("payment_approved_at")).isNull();
            }
        }

        assertThatThrownBy(() -> insertCampaign(12, "999.00"))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> insertCampaign(13, "1500.00"))
                .isInstanceOf(SQLException.class);
    }

    private void prepareExistingOrderSchema() throws SQLException {
        try (var connection = DriverManager.getConnection(URL, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
            statement.execute("CREATE TABLE users (id BIGINT PRIMARY KEY)");
            statement.execute("""
                    CREATE TABLE orders (
                        id BIGINT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        status VARCHAR(30) NOT NULL,
                        CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id)
                    )
                    """);
            statement.executeUpdate("INSERT INTO users (id) VALUES (1)");
            statement.executeUpdate("INSERT INTO orders (id, user_id, status) VALUES (1, 1, 'PAID')");
            statement.execute("""
                    CREATE TABLE saga_instance (
                        saga_id VARCHAR(36) PRIMARY KEY,
                        order_id BIGINT NOT NULL,
                        status VARCHAR(40) NOT NULL,
                        current_step VARCHAR(40) NOT NULL,
                        retry_count INT NOT NULL,
                        next_retry_at TIMESTAMP NULL,
                        last_error VARCHAR(1000) NULL,
                        payment_unknown_at TIMESTAMP NULL,
                        resolved_at TIMESTAMP NULL,
                        created_at TIMESTAMP NULL,
                        updated_at TIMESTAMP NULL
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO saga_instance (
                        saga_id, order_id, status, current_step, retry_count
                    ) VALUES ('saga-1', 1, 'COMPLETED', 'DONE', 0)
                    """);
        }
    }

    private void insertCampaign(long id, String targetAmount) throws SQLException {
        try (var connection = DriverManager.getConnection(URL, "sa", "");
             var statement = connection.createStatement()) {
            statement.executeUpdate(validCampaignInsert(id, targetAmount));
        }
    }

    private String validCampaignInsert(long id, String targetAmount) {
        return """
                INSERT INTO campaigns (
                    id, title, content, target_amount, funded_amount, reserved_amount,
                    starts_at, deadline_at, status
                ) VALUES (
                    %d, 'campaign', 'content', %s, 0, 0,
                    TIMESTAMP '2026-08-25 00:00:00',
                    TIMESTAMP '2026-08-26 00:00:00', 'OPEN'
                )
                """.formatted(id, targetAmount);
    }
}
