package com.example.fan_cafe.global.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Mock PG Webhook / Outbox k6 부하 테스트용 PAYMENT_PENDING 주문 시드.
 * <p>
 * {@code load-test.seed.payment-pending-orders.enabled=true} 일 때만 기동 후
 * {@code scripts/seed-payment-pending-orders.sql} 의 DROP → CREATE → CALL → DROP 흐름을 실행한다.
 * 프로시저 내부에서 기존 테스트 데이터를 먼저 삭제한 뒤 100,000건을 새로 생성한다.
 */
@Slf4j
@Component
@Profile("test")
@ConditionalOnProperty(
        name = "load-test.seed.payment-pending-orders.enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
public class PaymentPendingOrderLoadTestSeedRunner implements CommandLineRunner {

    private static final String SEED_SCRIPT_CLASSPATH = "scripts/seed-payment-pending-orders.sql";
    private static final String SQL_STATEMENT_SEPARATOR = ";;";

    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        log.info("[LOAD-TEST-SEED] PAYMENT_PENDING 주문 시드 시작 (기존 데이터 reset 포함, {})", SEED_SCRIPT_CLASSPATH);

        ClassPathResource script = new ClassPathResource(SEED_SCRIPT_CLASSPATH);
        if (!script.exists()) {
            throw new IllegalStateException("Seed script not found on classpath: " + SEED_SCRIPT_CLASSPATH);
        }

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new EncodedResource(script),
                    false,
                    false,
                    ScriptUtils.DEFAULT_COMMENT_PREFIX,
                    SQL_STATEMENT_SEPARATOR,
                    ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
                    ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER
            );
        } catch (Exception e) {
            throw new IllegalStateException("[LOAD-TEST-SEED] PAYMENT_PENDING 주문 시드 실패.", e);
        }

        log.info("[LOAD-TEST-SEED] PAYMENT_PENDING 주문 시드 완료 (orderId 900001~1000000, approvalAmount 9000)");
    }
}
