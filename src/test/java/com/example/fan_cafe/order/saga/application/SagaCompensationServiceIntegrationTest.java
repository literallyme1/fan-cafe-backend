package com.example.fan_cafe.order.saga.application;

import com.example.fan_cafe.order.domain.Status;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.infrastructure.OrderStatusHistoryRepository;
import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import com.example.fan_cafe.order.saga.domain.PaymentSagaStateMachine;
import com.example.fan_cafe.order.saga.domain.SagaInstance;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.domain.SagaStep;
import com.example.fan_cafe.order.saga.infrastructure.SagaInstanceRepository;
import com.example.fan_cafe.order.saga.messaging.PaymentRefundedResult;
import com.example.fan_cafe.order.support.OrderIntegrationTestSupport;
import com.example.fan_cafe.order.support.OrderIntegrationTestSupport.PaymentPendingFixture;
import com.example.fan_cafe.merchandise.infrastructure.MerchandiseRepository;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

@Tag("integration")
@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@ActiveProfiles("ci")
class SagaCompensationServiceIntegrationTest {
    private static final String REASON = "order completion failed";

    @Autowired private SagaCompensationService compensationService;
    @Autowired private SagaTransactionService sagaTransactionService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderStatusHistoryRepository historyRepository;
    @Autowired private SagaInstanceRepository sagaRepository;
    @Autowired private MerchandiseRepository merchandiseRepository;
    @Autowired private OrderIntegrationTestSupport fixtures;
    @Autowired private EntityManager entityManager;
    @MockitoSpyBean private OutboxEventRepository outboxRepository;
    @MockitoSpyBean private PaymentSagaStateMachine stateMachine;

    private PaymentPendingFixture fixture;
    private SagaSnapshot saga;

    @BeforeEach
    void setUpPaymentCompletedSaga() {
        fixture = fixtures.createPaymentPendingOrder();
        saga = sagaTransactionService.start(fixture.order().getId());
        sagaTransactionService.transition(saga.sagaId(), SagaStatus.PAYMENT_PENDING);
        sagaTransactionService.transition(saga.sagaId(), SagaStatus.PAYMENT_COMPLETED);
    }

    @AfterEach
    void tearDown() {
        fixtures.cleanup(fixture);
    }

    @Test
    void orderCompletionFailureTransitionsSagaAndCreatesRefundCommandAtomically() {
        compensationService.start(saga.sagaId(), fixture.order().getId(), REASON);
        compensationService.start(saga.sagaId(), fixture.order().getId(), REASON);

        entityManager.clear();
        SagaInstance reloaded = sagaRepository.findByOrderId(fixture.order().getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SagaStatus.COMPENSATING);
        assertThat(reloaded.getCurrentStep()).isEqualTo(SagaStep.PAYMENT_REFUND);
        var commands = outboxRepository.findAll().stream()
                .filter(event -> "PAYMENT_SAGA".equals(event.getAggregateType()))
                .filter(event -> fixture.order().getId().equals(event.getAggregateId()))
                .toList();
        assertThat(commands).singleElement().satisfies(command -> {
            assertThat(command.getPayload()).contains("\"eventType\":\"REFUND_PAYMENT\"");
            assertThat(command.getPayload()).contains("\"sagaId\":\"" + saga.sagaId() + "\"");
            assertThat(command.getPayload()).contains("\"idempotencyKey\":\"REFUND:" + saga.sagaId() + "\"");
        });
    }

    @Test
    void outboxFailureRollsBackCompensatingTransitionAndCommand() {
        doThrow(new DataIntegrityViolationException("forced outbox failure"))
                .when(outboxRepository).flush();

        assertThatThrownBy(() -> compensationService.start(
                saga.sagaId(), fixture.order().getId(), REASON))
                .isInstanceOf(DataIntegrityViolationException.class);

        entityManager.clear();
        assertThat(sagaRepository.findByOrderId(fixture.order().getId()).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.PAYMENT_COMPLETED);
        assertThat(outboxRepository.countByAggregateTypeAndAggregateId(
                "PAYMENT_SAGA", fixture.order().getId())).isZero();
    }

    @Test
    void refundResultCompletesOrderAndSagaInOneTransaction() {
        compensationService.start(saga.sagaId(), fixture.order().getId(), REASON);

        compensationService.complete(refundedResult());

        entityManager.clear();
        Long orderId = fixture.order().getId();
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(Status.REFUNDED);
        assertThat(sagaRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.COMPENSATED);
        assertThat(historyRepository.countByOrder_Id(orderId)).isEqualTo(1);
        assertThat(merchandiseRepository.findById(fixture.merchandise().getId()).orElseThrow().getStock())
                .isEqualTo(100);
        assertThat(outboxRepository.countByAggregateTypeAndAggregateId("ORDER", orderId)).isEqualTo(1);
    }

    @Test
    void compensatedTransitionFailureRollsBackOrderInventoryHistoryAndResultOutbox() {
        compensationService.start(saga.sagaId(), fixture.order().getId(), REASON);
        doThrow(new IllegalStateException("forced compensated transition failure"))
                .when(stateMachine).transition(any(SagaInstance.class), eq(SagaStatus.COMPENSATED));

        assertThatThrownBy(() -> compensationService.complete(refundedResult()))
                .isInstanceOf(IllegalStateException.class);

        entityManager.clear();
        Long orderId = fixture.order().getId();
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(Status.PAYMENT_PENDING);
        assertThat(sagaRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.COMPENSATING);
        assertThat(historyRepository.countByOrder_Id(orderId)).isZero();
        assertThat(outboxRepository.countByAggregateTypeAndAggregateId("ORDER", orderId)).isZero();
        assertThat(merchandiseRepository.findById(fixture.merchandise().getId()).orElseThrow().getStock())
                .isEqualTo(98);
    }

    private PaymentRefundedResult refundedResult() {
        return new PaymentRefundedResult(
                PaymentRefundedResult.EVENT_TYPE,
                saga.sagaId(),
                fixture.order().getId(),
                PaymentResultStatus.REFUNDED,
                "REFUND:" + saga.sagaId(),
                REASON
        );
    }
}
