package com.example.fan_cafe.order.saga.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.application.OrderService;
import com.example.fan_cafe.order.domain.Status;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.infrastructure.OrderStatusHistoryRepository;
import com.example.fan_cafe.order.interfaces.dto.MockPaymentApproveRequest;
import com.example.fan_cafe.order.payment.client.PaymentClient;
import com.example.fan_cafe.order.payment.client.PaymentOutcomeUnknownException;
import com.example.fan_cafe.order.payment.client.PaymentResultResponse;
import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import com.example.fan_cafe.order.payment.client.PaymentStatusResponse;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.exception.SagaErrorCode;
import com.example.fan_cafe.order.saga.infrastructure.SagaInstanceRepository;
import com.example.fan_cafe.order.support.OrderIntegrationTestSupport;
import com.example.fan_cafe.order.support.OrderIntegrationTestSupport.PaymentPendingFixture;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("integration")
@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@ActiveProfiles("ci")
class PaymentUnknownIntegrationTest {
    private static final String PAYMENT_KEY = "pay-step5-timeout";

    @Autowired private OrderService orderService;
    @SpyBean private SagaTransactionService sagaTransactionService;
    @Autowired private SagaInstanceRepository sagaRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderStatusHistoryRepository historyRepository;
    @Autowired private OutboxEventRepository outboxRepository;
    @Autowired private OrderIntegrationTestSupport fixtures;

    @MockBean private PaymentClient paymentClient;

    private PaymentPendingFixture fixture;

    @AfterEach
    void tearDown() {
        if (fixture != null) {
            fixtures.cleanup(fixture);
        }
    }

    @Test
    void approvalTimeoutPersistsUnknownBeforeNotFoundLookup_withoutFailureOrCompensation() {
        fixture = fixtures.createPaymentPendingOrder();
        Long orderId = fixture.order().getId();
        MockPaymentApproveRequest request = request(fixture.totalPrice());

        when(paymentClient.approve(orderId, fixture.totalPrice(), fixture.totalPrice(), PAYMENT_KEY))
                .thenAnswer(invocation -> {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                    assertThat(sagaRepository.findByOrderId(orderId).orElseThrow().getStatus())
                            .isEqualTo(SagaStatus.PAYMENT_PENDING);
                    throw new PaymentOutcomeUnknownException(OrderErrorCode.PAYMENT_SERVICE_UNAVAILABLE);
                });
        when(paymentClient.getStatus(orderId)).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(sagaRepository.findByOrderId(orderId).orElseThrow().getStatus())
                    .isEqualTo(SagaStatus.PAYMENT_UNKNOWN);
            throw new CustomException(OrderErrorCode.PAYMENT_NOT_FOUND);
        });

        assertThatThrownBy(() -> orderService.approveMockPayment(fixture.user(), orderId, request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(OrderErrorCode.PAYMENT_NOT_FOUND);

        assertThat(sagaRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.PAYMENT_UNKNOWN);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(Status.PAYMENT_PENDING);
        assertThat(historyRepository.countByOrder_Id(orderId)).isZero();
        assertThat(outboxRepository.countByAggregateTypeAndAggregateId("PAYMENT_SAGA", orderId)).isZero();
        verify(paymentClient, never()).refund(anyLong(), any(), anyString());
    }

    @Test
    void approvedLookupResumesExistingHappyPath() {
        fixture = fixtures.createPaymentPendingOrder();
        Long orderId = fixture.order().getId();
        makeUnknown(orderId);
        when(paymentClient.getStatus(orderId)).thenReturn(status(PaymentResultStatus.APPROVED, null));

        var response = orderService.approveMockPayment(
                fixture.user(), orderId, request(fixture.totalPrice()));

        assertThat(response.getStatus()).isEqualTo(Status.PAID);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(Status.PAID);
        assertThat(sagaRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.COMPLETED);
        assertThat(historyRepository.countByOrder_Id(orderId)).isEqualTo(1);
        assertThat(outboxRepository.countByAggregateTypeAndAggregateId("ORDER", orderId)).isEqualTo(1);
        verify(paymentClient, never()).approve(anyLong(), any(), any(), anyString());
    }

    @Test
    void failedLookupAtomicallyMarksOrderFailedAndSagaCancelled() {
        fixture = fixtures.createPaymentPendingOrder();
        Long orderId = fixture.order().getId();
        makeUnknown(orderId);
        when(paymentClient.getStatus(orderId)).thenReturn(status(PaymentResultStatus.FAILED, "declined"));

        var response = orderService.approveMockPayment(
                fixture.user(), orderId, request(fixture.totalPrice()));

        assertThat(response.getStatus()).isEqualTo(Status.PAYMENT_FAILED);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(Status.PAYMENT_FAILED);
        assertThat(sagaRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.CANCELLED);
        assertThat(historyRepository.countByOrder_Id(orderId)).isEqualTo(1);
        assertThat(outboxRepository.countByAggregateTypeAndAggregateId("PAYMENT_SAGA", orderId)).isZero();
    }

    @Test
    void cancelledTransitionFailureRollsBackOrderFailureHistoryAndSaga() {
        fixture = fixtures.createPaymentPendingOrder();
        Long orderId = fixture.order().getId();
        SagaSnapshot unknown = makeUnknown(orderId);
        when(paymentClient.getStatus(orderId)).thenReturn(status(PaymentResultStatus.FAILED, "declined"));
        CustomException transitionFailure = new CustomException(SagaErrorCode.INVALID_SAGA_TRANSITION);
        doThrow(transitionFailure).when(sagaTransactionService)
                .transition(unknown.sagaId(), SagaStatus.CANCELLED);

        assertThatThrownBy(() -> orderService.approveMockPayment(
                fixture.user(), orderId, request(fixture.totalPrice())))
                .isSameAs(transitionFailure);

        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(Status.PAYMENT_PENDING);
        assertThat(historyRepository.countByOrder_Id(orderId)).isZero();
        assertThat(sagaRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.PAYMENT_UNKNOWN);
    }

    @Test
    void statusLookupFailureKeepsUnknownAndDoesNotCompensate() {
        fixture = fixtures.createPaymentPendingOrder();
        Long orderId = fixture.order().getId();
        makeUnknown(orderId);
        CustomException unavailable = new CustomException(OrderErrorCode.PAYMENT_SERVICE_UNAVAILABLE);
        when(paymentClient.getStatus(orderId)).thenThrow(unavailable);

        assertThatThrownBy(() -> orderService.approveMockPayment(
                fixture.user(), orderId, request(fixture.totalPrice())))
                .isSameAs(unavailable);

        assertThat(sagaRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.PAYMENT_UNKNOWN);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(Status.PAYMENT_PENDING);
        assertThat(historyRepository.countByOrder_Id(orderId)).isZero();
        assertThat(outboxRepository.countByAggregateTypeAndAggregateId("PAYMENT_SAGA", orderId)).isZero();
        verify(paymentClient, never()).refund(anyLong(), any(), anyString());
    }

    @Test
    void explicitApprovalFailureUsesDefinitiveCancelledPath() {
        fixture = fixtures.createPaymentPendingOrder();
        Long orderId = fixture.order().getId();
        when(paymentClient.approve(orderId, fixture.totalPrice(), fixture.totalPrice(), PAYMENT_KEY))
                .thenReturn(new PaymentResultResponse(
                        orderId, PaymentResultStatus.FAILED, null, "declined", null));

        var response = orderService.approveMockPayment(
                fixture.user(), orderId, request(fixture.totalPrice()));

        assertThat(response.getStatus()).isEqualTo(Status.PAYMENT_FAILED);
        assertThat(sagaRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.CANCELLED);
    }

    private SagaSnapshot makeUnknown(Long orderId) {
        SagaSnapshot saga = sagaTransactionService.start(orderId);
        sagaTransactionService.transition(saga.sagaId(), SagaStatus.PAYMENT_PENDING);
        return sagaTransactionService.transition(saga.sagaId(), SagaStatus.PAYMENT_UNKNOWN);
    }

    private MockPaymentApproveRequest request(BigDecimal amount) {
        MockPaymentApproveRequest request = new MockPaymentApproveRequest();
        ReflectionTestUtils.setField(request, "approvalAmount", amount);
        ReflectionTestUtils.setField(request, "idempotencyKey", PAYMENT_KEY);
        return request;
    }

    private PaymentStatusResponse status(PaymentResultStatus status, String failureReason) {
        return new PaymentStatusResponse(
                fixture.order().getId(), status, fixture.totalPrice(), fixture.totalPrice(),
                PAYMENT_KEY, failureReason, null, null, null);
    }
}
