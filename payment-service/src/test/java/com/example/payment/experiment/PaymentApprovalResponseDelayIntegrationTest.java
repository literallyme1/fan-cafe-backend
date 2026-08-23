package com.example.payment.experiment;

import com.example.payment.domain.PaymentStatus;
import com.example.payment.infrastructure.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("experiment")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:payment-delay;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "experiment.payment.partial-success-enabled=true",
                "experiment.payment.partial-success-percent=100",
                "experiment.payment.approval-response-delay=1s"
        }
)
class PaymentApprovalResponseDelayIntegrationTest {
    private static final Long ORDER_ID = 8_000_001L;

    @LocalServerPort private int port;
    @Autowired private PaymentRepository paymentRepository;

    @AfterEach
    void clearPayments() {
        paymentRepository.deleteAll();
    }

    @Test
    void approvedPaymentIsCommittedAndVisibleBeforeDelayedHttpResponseCompletes() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port
                        + "/internal/payments/" + ORDER_ID + "/approve"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"expectedAmount":9000,"approvalAmount":9000,"paymentKey":"STEP8-DELAY-8000001"}
                        """))
                .build();

        var response = HttpClient.newHttpClient().sendAsync(
                request, HttpResponse.BodyHandlers.ofString());

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (paymentRepository.findByOrderId(ORDER_ID)
                .map(payment -> payment.getStatus() != PaymentStatus.APPROVED)
                .orElse(true)) {
            assertThat(System.nanoTime()).isLessThan(deadline);
            Thread.sleep(Duration.ofMillis(10));
        }

        assertThat(response).isNotDone();
        assertThat(response.get(5, TimeUnit.SECONDS).statusCode()).isEqualTo(200);
        assertThat(paymentRepository.findByOrderId(ORDER_ID).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.APPROVED);
    }
}
