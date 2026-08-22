package com.example.fan_cafe.order.saga.application;

import com.example.fan_cafe.order.domain.Status;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.infrastructure.OrderStatusHistoryRepository;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.infrastructure.SagaInstanceRepository;
import com.example.fan_cafe.order.support.OrderIntegrationTestSupport;
import com.example.fan_cafe.order.support.OrderIntegrationTestSupport.PaymentPendingFixture;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

@Tag("integration")
@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@ActiveProfiles("ci")
class SagaOrderCompletionServiceIntegrationTest {

    @Autowired
    private SagaOrderCompletionService completionService;

    @SpyBean
    private SagaTransactionService sagaTransactionService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private SagaInstanceRepository sagaInstanceRepository;

    @Autowired
    private OrderIntegrationTestSupport fixtures;

    @Autowired
    private EntityManager entityManager;

    private PaymentPendingFixture fixture;

    @AfterEach
    void tearDown() {
        fixtures.cleanup(fixture);
    }

    @Test
    void completionTransitionFailureRollsBackOrderHistoryOutboxAndSaga() {
        fixture = fixtures.createPaymentPendingOrder();
        Long orderId = fixture.order().getId();
        SagaSnapshot saga = sagaTransactionService.start(orderId);
        sagaTransactionService.transition(saga.sagaId(), SagaStatus.PAYMENT_PENDING);
        sagaTransactionService.transition(saga.sagaId(), SagaStatus.PAYMENT_COMPLETED);

        doThrow(new IllegalStateException("forced completion transition failure"))
                .when(sagaTransactionService)
                .transition(saga.sagaId(), SagaStatus.COMPLETED);

        assertThatThrownBy(() -> completionService.complete(
                saga.sagaId(), orderId, "mock payment approved"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("forced completion transition failure");

        entityManager.clear();
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(Status.PAYMENT_PENDING);
        assertThat(orderStatusHistoryRepository.countByOrder_Id(orderId)).isZero();
        assertThat(outboxEventRepository.countByAggregateTypeAndAggregateId("ORDER", orderId)).isZero();
        assertThat(sagaInstanceRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.PAYMENT_COMPLETED);
    }
}
