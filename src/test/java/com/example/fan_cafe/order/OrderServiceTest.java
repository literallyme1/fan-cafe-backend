package com.example.fan_cafe.order;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.merchandise.domain.Category;
import com.example.fan_cafe.merchandise.domain.Merchandise;
import com.example.fan_cafe.merchandise.domain.Status;
import com.example.fan_cafe.merchandise.infrastructure.MerchandiseRepository;
import com.example.fan_cafe.order.application.OrderPaymentCommandService;
import com.example.fan_cafe.order.application.OrderPaymentResultService;
import com.example.fan_cafe.order.application.OrderRefundResultService;
import com.example.fan_cafe.order.application.OrderService;
import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.domain.OrderItem;
import com.example.fan_cafe.order.domain.OrderStatusHistory;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.infrastructure.OrderStatusHistoryRepository;
import com.example.fan_cafe.order.interfaces.dto.MockPaymentApproveRequest;
import com.example.fan_cafe.order.interfaces.dto.MockPaymentCancelRequest;
import com.example.fan_cafe.order.interfaces.dto.MockPaymentFailRequest;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.order.payment.client.PaymentClient;
import com.example.fan_cafe.order.payment.client.PaymentResultResponse;
import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import com.example.fan_cafe.order.payment.client.PaymentStatusResponse;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MerchandiseRepository merchandiseRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private OrderPaymentCommandService orderPaymentCommandService;

    @Mock
    private OrderPaymentResultService orderPaymentResultService;

    @Mock
    private OrderRefundResultService orderRefundResultService;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Order paidOrder;
    private Order paymentPendingOrder;

    @BeforeEach
    void setUp() {
        user = User.of("orderer@test.com", "encoded_pw", "orderer", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);

        paymentPendingOrder = Order.paymentPending(user, BigDecimal.valueOf(20000));
        ReflectionTestUtils.setField(paymentPendingOrder, "id", 10L);
        paymentPendingOrder.addItem(OrderItem.snapshot(100L, "응원봉", BigDecimal.valueOf(10000), 2));

        paidOrder = Order.paymentPending(user, BigDecimal.valueOf(20000));
        ReflectionTestUtils.setField(paidOrder, "id", 10L);
        paidOrder.addItem(OrderItem.snapshot(100L, "응원봉", BigDecimal.valueOf(10000), 2));
        paidOrder.markPaid();

        when(userRepository.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));
        lenient().when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> {
            OutboxEvent e = invocation.getArgument(0);
            if (e.getId() == null) {
                ReflectionTestUtils.setField(e, "id", 999L);
            }
            return e;
        });
        lenient().when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("내 주문 단건 조회에 성공한다.")
    void get_shouldReturnOrder_whenMyOrderExists() {
        when(orderRepository.findByIdAndUserIdWithItems(10L, 1L)).thenReturn(Optional.of(paidOrder));

        OrderQueryResponse response = orderService.get(user, 10L);

        assertThat(response.getOrderId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(com.example.fan_cafe.order.domain.Status.PAID);
    }

    @Test
    @DisplayName("Mock 결제 승인 API는 Payment 호출 후 결과 반영을 위임한다.")
    void approveMockPayment_shouldDelegateToCommandService() {
        when(orderRepository.findByIdAndUserIdWithItems(10L, 1L)).thenReturn(Optional.of(paymentPendingOrder));
        PaymentResultResponse result = new PaymentResultResponse(
                10L, PaymentResultStatus.APPROVED, "idem-001", null, null);
        when(paymentClient.approve(10L, BigDecimal.valueOf(20000),
                BigDecimal.valueOf(20000), "idem-001")).thenReturn(result);
        when(orderPaymentResultService.apply(
                10L, result, "mock payment approved", "mock payment failed"))
                .thenReturn(paidOrder);

        MockPaymentApproveRequest request = new MockPaymentApproveRequest();
        ReflectionTestUtils.setField(request, "approvalAmount", BigDecimal.valueOf(20000));
        ReflectionTestUtils.setField(request, "idempotencyKey", "idem-001");

        OrderQueryResponse response = orderService.approveMockPayment(user, 10L, request);

        assertThat(response.getStatus()).isEqualTo(com.example.fan_cafe.order.domain.Status.PAID);
        verify(paymentClient).approve(10L, BigDecimal.valueOf(20000),
                BigDecimal.valueOf(20000), "idem-001");
    }

    @Test
    @DisplayName("Mock 결제 실패 API는 Payment 호출 후 결과 반영을 위임한다.")
    void failMockPayment_shouldDelegateToCommandService() {
        when(orderRepository.findByIdAndUserIdWithItems(10L, 1L)).thenReturn(Optional.of(paymentPendingOrder));
        PaymentResultResponse result = new PaymentResultResponse(
                10L, PaymentResultStatus.FAILED, null, "mock payment failed", null);
        when(paymentClient.fail(10L, BigDecimal.valueOf(20000), "mock payment failed"))
                .thenReturn(result);
        when(orderPaymentResultService.apply(
                10L, result, "mock payment approved", "mock payment failed"))
                .thenReturn(paymentPendingOrder);
        paymentPendingOrder.markPaymentFailed();

        OrderQueryResponse response = orderService.failMockPayment(user, 10L, new MockPaymentFailRequest());

        assertThat(response.getStatus()).isEqualTo(com.example.fan_cafe.order.domain.Status.PAYMENT_FAILED);
        verify(paymentClient).fail(10L, BigDecimal.valueOf(20000), "mock payment failed");
    }

    @Test
    @DisplayName("Mock 취소/환불 API는 Payment 환불 후 Order 결과 반영을 위임한다.")
    void cancelMockPayment_shouldDelegateToCommandService() {
        UUID sagaId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        when(orderRepository.findByIdAndUserIdWithItems(10L, 1L)).thenReturn(Optional.of(paidOrder));
        PaymentStatusResponse payment = new PaymentStatusResponse(
                10L, PaymentResultStatus.REFUNDED, BigDecimal.valueOf(20000), BigDecimal.valueOf(20000),
                "pay-1", null, "REFUND:" + sagaId, "고객 변심", null);
        when(paymentClient.refund(10L, sagaId, "고객 변심")).thenReturn(payment);
        paidOrder.markRefunded();
        when(orderRefundResultService.apply(10L, payment, "고객 변심")).thenReturn(paidOrder);

        MockPaymentCancelRequest request = new MockPaymentCancelRequest();
        ReflectionTestUtils.setField(request, "cancelReason", "고객 변심");
        ReflectionTestUtils.setField(request, "sagaId", sagaId);

        OrderQueryResponse response = orderService.cancelMockPayment(user, 10L, request);

        assertThat(response.getStatus()).isEqualTo(com.example.fan_cafe.order.domain.Status.REFUNDED);
        verify(paymentClient).refund(10L, sagaId, "고객 변심");
        verify(orderRefundResultService).apply(10L, payment, "고객 변심");
    }

    @Test
    @DisplayName("PAYMENT_PENDING 주문 취소 시 재고 복구, 상태 변경, outbox 저장이 수행된다.")
    void cancel_shouldRestoreStockAndCancelOrderAndSaveOutbox() throws JsonProcessingException {
        Merchandise merchandise = Merchandise.builder()
                .id(100L)
                .name("응원봉")
                .description("응원봉")
                .price(10000L)
                .salePrice(9000L)
                .stock(0)
                .status(Status.SOLD_OUT)
                .category(Category.CLOTHES)
                .build();

        when(orderRepository.findByIdAndUserIdWithItems(10L, 1L)).thenReturn(Optional.of(paymentPendingOrder));
        when(merchandiseRepository.findMerchandiseWithPessimisticLock(100L)).thenReturn(Optional.of(merchandise));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventType\":\"ORDER_CANCELLED\"}");

        OrderQueryResponse response = orderService.cancel(user, 10L);

        assertThat(response.getStatus()).isEqualTo(com.example.fan_cafe.order.domain.Status.CANCELLED);
        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(2)).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getAllValues().get(1).getPayload()).contains("ORDER_CANCELLED");
    }

    @Test
    @DisplayName("PAID 주문은 cancel API로 취소할 수 없다.")
    void cancel_shouldThrowException_whenOrderAlreadyPaid() {
        when(orderRepository.findByIdAndUserIdWithItems(10L, 1L)).thenReturn(Optional.of(paidOrder));

        assertThatThrownBy(() -> orderService.cancel(user, 10L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(OrderErrorCode.ORDER_NOT_CANCELLABLE.getMessage());
    }

    @Test
    @DisplayName("이미 취소된 주문은 취소할 수 없다.")
    void cancel_shouldThrowException_whenOrderNotCancellable() {
        ReflectionTestUtils.setField(paymentPendingOrder, "status", com.example.fan_cafe.order.domain.Status.CANCELLED);
        when(orderRepository.findByIdAndUserIdWithItems(10L, 1L)).thenReturn(Optional.of(paymentPendingOrder));

        assertThatThrownBy(() -> orderService.cancel(user, 10L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(OrderErrorCode.ORDER_NOT_CANCELLABLE.getMessage());
    }
}
