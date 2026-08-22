package com.example.payment.application;

import com.example.payment.domain.PaymentStatus;
import com.example.payment.infrastructure.PaymentRepository;
import com.example.payment.interfaces.dto.PaymentResultResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PaymentConcurrencyIntegrationTest {
    private static final Long ORDER_ID = 900_001L;
    private static final BigDecimal AMOUNT = new BigDecimal("20000.00");
    private static final String PAYMENT_KEY = "concurrent-pay-1";

    @Autowired private PaymentService paymentService;
    @Autowired private PaymentRepository paymentRepository;

    @BeforeEach
    @AfterEach
    void clearPayments() {
        paymentRepository.deleteAll();
    }

    @Test
    void concurrentFirstApprovals_returnSameResultWithoutInsertConflictFailure() throws Exception {
        int requestCount = 6;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<PaymentResultResponse>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < requestCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await(10, TimeUnit.SECONDS);
                    return paymentService.approve(ORDER_ID, AMOUNT, AMOUNT, PAYMENT_KEY);
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<PaymentResultResponse> results = new ArrayList<>();
            for (Future<PaymentResultResponse> future : futures) {
                results.add(future.get(20, TimeUnit.SECONDS));
            }

            assertThat(results)
                    .hasSize(requestCount)
                    .allSatisfy(result -> {
                        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
                        assertThat(result.paymentKey()).isEqualTo(PAYMENT_KEY);
                    });
            assertThat(paymentRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
