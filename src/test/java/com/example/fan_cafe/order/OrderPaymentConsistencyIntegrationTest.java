package com.example.fan_cafe.order;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.application.MockPgWebhookService;
import com.example.fan_cafe.order.application.OrderService;
import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.domain.Status;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.infrastructure.OrderStatusHistoryRepository;
import com.example.fan_cafe.order.interfaces.dto.MockPaymentCancelRequest;
import com.example.fan_cafe.order.support.OrderIntegrationTestSupport;
import com.example.fan_cafe.order.support.OrderIntegrationTestSupport.PaymentPendingFixture;
import com.example.fan_cafe.order.support.OrderIntegrationTestSupport.SignedWebhook;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Order 결제/환불 정합성 통합 테스트.
 * DB에 남는 {@code Order}, {@code OrderStatusHistory}, {@code OutboxEvent} 건수를 검증한다.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("ci")
class OrderPaymentConsistencyIntegrationTest {

    private static final String PAYMENT_KEY = "pay-idem-integration-001";
    private static final String REFUND_KEY = "refund-idem-integration-001";

    @Autowired
    private MockPgWebhookService mockPgWebhookService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OrderIntegrationTestSupport fixtures;

    @Autowired
    private EntityManager entityManager;

    private final List<PaymentPendingFixture> fixturesToCleanup = new ArrayList<>();

    @AfterEach
    void tearDown() {
        fixturesToCleanup.forEach(fixtures::cleanup);
        fixturesToCleanup.clear();
    }

    // --- 1차: 정상 결제·금액 불일치·웹훅 검증 실패 ---

    @Test
    @DisplayName("[1차] paymentApproved_transitionsFromPaymentPendingToPaid")
    void paymentApproved_transitionsFromPaymentPendingToPaid() {
        PaymentPendingFixture fixture = trackFixture(fixtures.createPaymentPendingOrder());
        SignedWebhook webhook = fixtures.signedPaymentApprovedWebhook(
                fixture.order().getId(), fixture.totalPrice(), PAYMENT_KEY);

        mockPgWebhookService.receive(webhook.rawBody(), webhook.timestamp(), webhook.signature());
        flushAndClear();

        Order reloaded = orderRepository.findById(fixture.order().getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(Status.PAID);
        assertThat(reloaded.getApprovedPaymentKey()).isEqualTo(PAYMENT_KEY);
    }

    @Test
    @DisplayName("[1차] paymentApproved_createsExactlyOneOrderStatusHistory")
    void paymentApproved_createsExactlyOneOrderStatusHistory() {
        PaymentPendingFixture fixture = trackFixture(fixtures.createPaymentPendingOrder());
        SignedWebhook webhook = fixtures.signedPaymentApprovedWebhook(
                fixture.order().getId(), fixture.totalPrice(), PAYMENT_KEY);

        mockPgWebhookService.receive(webhook.rawBody(), webhook.timestamp(), webhook.signature());
        flushAndClear();

        assertThat(orderStatusHistoryRepository.countByOrder_Id(fixture.order().getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("[1차] paymentApproved_createsExactlyOneOutboxEvent")
    void paymentApproved_createsExactlyOneOutboxEvent() {
        PaymentPendingFixture fixture = trackFixture(fixtures.createPaymentPendingOrder());
        SignedWebhook webhook = fixtures.signedPaymentApprovedWebhook(
                fixture.order().getId(), fixture.totalPrice(), PAYMENT_KEY);

        mockPgWebhookService.receive(webhook.rawBody(), webhook.timestamp(), webhook.signature());
        flushAndClear();

        Long orderId = fixture.order().getId();
        assertThat(outboxEventRepository.countByAggregateTypeAndAggregateId("ORDER", orderId)).isEqualTo(1);

        OutboxEvent outbox = outboxEventRepository.findAll().stream()
                .filter(e -> "ORDER".equals(e.getAggregateType()) && orderId.equals(e.getAggregateId()))
                .findFirst()
                .orElseThrow();
        assertThat(outbox.getPayload()).contains("ORDER_CREATED");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("[1차] paymentAmountMismatch_marksPaymentFailed_withoutPaidOrOutbox")
    void paymentAmountMismatch_marksPaymentFailed_withoutPaidOrOutbox() {
        PaymentPendingFixture fixture = trackFixture(fixtures.createPaymentPendingOrder());
        SignedWebhook webhook = fixtures.signedPaymentApprovedWebhookWithAmount(
                fixture.order().getId(), 1L, PAYMENT_KEY);

        assertThatThrownBy(() -> mockPgWebhookService.receive(
                webhook.rawBody(), webhook.timestamp(), webhook.signature()))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(OrderErrorCode.PAYMENT_AMOUNT_MISMATCH);

        entityManager.clear();

        Order reloaded = orderRepository.findById(fixture.order().getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(Status.PAYMENT_FAILED);
        assertThat(orderStatusHistoryRepository.countByOrder_Id(fixture.order().getId())).isEqualTo(1);
        assertThat(outboxEventRepository.countByAggregateTypeAndAggregateId("ORDER", fixture.order().getId()))
                .isZero();
    }

    @Test
    @DisplayName("[1차] webhookInvalidSignature_doesNotChangeOrderOrOutbox")
    void webhookInvalidSignature_doesNotChangeOrderOrOutbox() {
        PaymentPendingFixture fixture = trackFixture(fixtures.createPaymentPendingOrder());
        SignedWebhook webhook = fixtures.signedPaymentApprovedWebhook(
                fixture.order().getId(), fixture.totalPrice(), PAYMENT_KEY);

        assertThatThrownBy(() -> mockPgWebhookService.receive(
                webhook.rawBody(), webhook.timestamp(), "invalid-signature"))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(OrderErrorCode.WEBHOOK_SIGNATURE_INVALID);

        flushAndClear();
        assertOrderUnchanged(fixture);
    }

    @Test
    @DisplayName("[1차] webhookExpiredTimestamp_doesNotChangeOrderOrOutbox")
    void webhookExpiredTimestamp_doesNotChangeOrderOrOutbox() {
        PaymentPendingFixture fixture = trackFixture(fixtures.createPaymentPendingOrder());
        long expiredEpoch = Instant.now().getEpochSecond() - 600;
        SignedWebhook webhook = fixtures.signedWebhookWithTimestamp(
                fixtures.signedPaymentApprovedWebhook(
                        fixture.order().getId(), fixture.totalPrice(), PAYMENT_KEY).rawBody(),
                expiredEpoch
        );

        assertThatThrownBy(() -> mockPgWebhookService.receive(
                webhook.rawBody(), webhook.timestamp(), webhook.signature()))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(OrderErrorCode.WEBHOOK_TIMESTAMP_EXPIRED);

        flushAndClear();
        assertOrderUnchanged(fixture);
    }

    // --- 2차: 멱등성 ---

    @Test
    @DisplayName("[2차] duplicatePaymentWebhook_appliesStateTransitionOnlyOnce")
    void duplicatePaymentWebhook_appliesStateTransitionOnlyOnce() {
        PaymentPendingFixture fixture = trackFixture(fixtures.createPaymentPendingOrder());
        SignedWebhook webhook = fixtures.signedPaymentApprovedWebhook(
                fixture.order().getId(), fixture.totalPrice(), PAYMENT_KEY);

        mockPgWebhookService.receive(webhook.rawBody(), webhook.timestamp(), webhook.signature());
        mockPgWebhookService.receive(webhook.rawBody(), webhook.timestamp(), webhook.signature());
        flushAndClear();

        Long orderId = fixture.order().getId();
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(Status.PAID);
        assertThat(orderStatusHistoryRepository.countByOrder_Id(orderId)).isEqualTo(1);
        assertThat(outboxEventRepository.countByAggregateTypeAndAggregateId("ORDER", orderId)).isEqualTo(1);
    }

    @Test
    @DisplayName("[2차] duplicateRefundRequest_appliesRefundedTransitionOnlyOnce")
    void duplicateRefundRequest_appliesRefundedTransitionOnlyOnce() {
        PaymentPendingFixture fixture = trackFixture(fixtures.createPaymentPendingOrder());
        approveOrder(fixture);

        MockPaymentCancelRequest cancelRequest = new MockPaymentCancelRequest();
        ReflectionTestUtils.setField(cancelRequest, "cancelReason", "고객 변심");
        ReflectionTestUtils.setField(cancelRequest, "idempotencyKey", REFUND_KEY);

        orderService.cancelMockPayment(fixture.user(), fixture.order().getId(), cancelRequest);
        orderService.cancelMockPayment(fixture.user(), fixture.order().getId(), cancelRequest);
        flushAndClear();

        Long orderId = fixture.order().getId();
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(Status.REFUNDED);
        assertThat(orderStatusHistoryRepository.countByOrder_Id(orderId)).isEqualTo(2);
        assertThat(outboxEventRepository.countByAggregateTypeAndAggregateId("ORDER", orderId)).isEqualTo(2);

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll().stream()
                .filter(e -> "ORDER".equals(e.getAggregateType()) && orderId.equals(e.getAggregateId()))
                .toList();
        assertThat(outboxEvents).hasSize(2);
        assertThat(outboxEvents.stream().filter(e -> e.getPayload().contains("ORDER_CREATED")).count()).isEqualTo(1);
        assertThat(outboxEvents.stream().filter(e -> e.getPayload().contains("PAYMENT_REFUNDED")).count()).isEqualTo(1);
    }

    // --- 3차: 동시성 ---

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("[3차] concurrentPaymentWebhook_resultsInPaidWithSingleHistoryAndOutbox")
    void concurrentPaymentWebhook_resultsInPaidWithSingleHistoryAndOutbox() throws InterruptedException {
        PaymentPendingFixture fixture = trackFixture(fixtures.createPaymentPendingOrder());
        SignedWebhook webhook = fixtures.signedPaymentApprovedWebhook(
                fixture.order().getId(), fixture.totalPrice(), PAYMENT_KEY);

        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();

        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await(10, TimeUnit.SECONDS);
                        mockPgWebhookService.receive(webhook.rawBody(), webhook.timestamp(), webhook.signature());
                        successCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception ignored) {
                        // 동시 경합 시 일부 스레드는 DB 락/상태 예외로 실패할 수 있음
                    } finally {
                        done.countDown();
                    }
                });
            }

            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            done.await(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
        }

        entityManager.clear();

        Long orderId = fixture.order().getId();
        Order reloaded = orderRepository.findById(orderId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(Status.PAID);
        assertThat(orderStatusHistoryRepository.countByOrder_Id(orderId)).isEqualTo(1);
        assertThat(outboxEventRepository.countByAggregateTypeAndAggregateId("ORDER", orderId)).isEqualTo(1);
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
    }

    private PaymentPendingFixture trackFixture(PaymentPendingFixture fixture) {
        fixturesToCleanup.add(fixture);
        return fixture;
    }

    private void approveOrder(PaymentPendingFixture fixture) {
        SignedWebhook webhook = fixtures.signedPaymentApprovedWebhook(
                fixture.order().getId(), fixture.totalPrice(), PAYMENT_KEY);
        mockPgWebhookService.receive(webhook.rawBody(), webhook.timestamp(), webhook.signature());
    }

    private void assertOrderUnchanged(PaymentPendingFixture fixture) {
        Long orderId = fixture.order().getId();
        Order reloaded = orderRepository.findById(orderId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(Status.PAYMENT_PENDING);
        assertThat(orderStatusHistoryRepository.countByOrder_Id(orderId)).isZero();
        assertThat(outboxEventRepository.countByAggregateTypeAndAggregateId("ORDER", orderId)).isZero();
    }

    private void flushAndClear() {
        entityManager.clear();
    }
}
