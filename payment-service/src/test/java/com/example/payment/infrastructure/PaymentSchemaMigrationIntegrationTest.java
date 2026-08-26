package com.example.payment.infrastructure;

import com.example.payment.PaymentServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = PaymentServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:payment-migration;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.baseline-on-migrate=true",
                "spring.flyway.baseline-version=1"
        }
)
@ContextConfiguration(initializers = PaymentSchemaMigrationIntegrationTest.StepOneSchemaInitializer.class)
class PaymentSchemaMigrationIntegrationTest {
    private static final String URL =
            "jdbc:h2:mem:payment-migration;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void existingStepOneSchema_isMigratedThroughApprovedAtBeforeHibernateValidation() {
        Integer refundColumnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_name = 'payments'
                  AND column_name IN ('refund_idempotency_key', 'refund_reason', 'refunded_at')
                """, Integer.class);
        assertThat(refundColumnCount).isEqualTo(3);

        Integer approvedAtColumnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_name = 'payments'
                  AND column_name = 'approved_at'
                """, Integer.class);
        assertThat(approvedAtColumnCount).isEqualTo(1);

        java.sql.Timestamp approvedAt = jdbcTemplate.queryForObject(
                "SELECT approved_at FROM payments WHERE order_id = 9001",
                java.sql.Timestamp.class);
        assertThat(approvedAt).isEqualTo(java.sql.Timestamp.valueOf("2026-08-25 12:34:56"));
    }

    static class StepOneSchemaInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            try (var connection = DriverManager.getConnection(URL, "sa", "");
                 var statement = connection.createStatement()) {
                statement.execute("DROP ALL OBJECTS");
                statement.execute("""
                        CREATE TABLE payments (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            order_id BIGINT NOT NULL UNIQUE,
                            status VARCHAR(30) NOT NULL,
                            expected_amount DECIMAL(19, 2) NOT NULL,
                            approved_amount DECIMAL(19, 2),
                            payment_key VARCHAR(100) UNIQUE,
                            failure_reason VARCHAR(500),
                            version BIGINT NOT NULL DEFAULT 0,
                            created_at DATETIME(6) NOT NULL,
                            updated_at DATETIME(6) NOT NULL
                        )
                        """);
                statement.execute("""
                        INSERT INTO payments (
                            order_id, status, expected_amount, approved_amount,
                            payment_key, version, created_at, updated_at
                        ) VALUES (
                            9001, 'APPROVED', 10000.00, 10000.00,
                            'migration-payment-9001', 0,
                            TIMESTAMP '2026-08-25 12:00:00',
                            TIMESTAMP '2026-08-25 12:34:56'
                        )
                        """);
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to prepare Step 1 payment schema", exception);
            }
        }
    }
}
