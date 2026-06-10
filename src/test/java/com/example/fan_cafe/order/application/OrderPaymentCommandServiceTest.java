package com.example.fan_cafe.order.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.domain.OrderItem;
import com.example.fan_cafe.order.domain.OrderStatusHistory;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.infrastructure.OrderStatusHistoryRepository;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPaymentCommandServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderPaymentCommandService orderPaymentCommandService;

    private Order paymentPendingOrder;
    private Order paidOrder;

    private void stubLockedOrder(Order order) {
        when(orderRepository.findByIdWithItemsForUpdate(order.getId())).thenReturn(Optional.of(order));
    }

    @BeforeEach
    void setUp() throws JsonProcessingException {
        User user = User.of("u@test.com", "pw", "u", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);

        paymentPendingOrder = Order.paymentPending(user, BigDecimal.valueOf(20000));
        ReflectionTestUtils.setField(paymentPendingOrder, "id", 10L);
        paymentPendingOrder.addItem(OrderItem.snapshot(100L, "응원봉", BigDecimal.valueOf(10000), 2));

        paidOrder = Order.paymentPending(user, BigDecimal.valueOf(20000));
        ReflectionTestUtils.setField(paidOrder, "id", 11L);
        paidOrder.addItem(OrderItem.snapshot(100L, "응원봉", BigDecimal.valueOf(10000), 2));
        paidOrder.markPaid("pay-key-1");

        lenient().when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> {
            OutboxEvent e = invocation.getArgument(0);
            if (e.getId() == null) {
                ReflectionTestUtils.setField(e, "id", 999L);
            }
            return e;
        });
        lenient().when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventType\":\"ORDER_PAID\"}");
    }

    @Test
    @DisplayName("승인 시 PAID 전이·이력·Outbox 저장")
    void approvePayment_shouldMarkPaidAndSaveOutbox() {
        stubLockedOrder(paymentPendingOrder);
        Order result = orderPaymentCommandService.approvePayment(
                paymentPendingOrder,
                BigDecimal.valueOf(20000),
                "idem-001",
                "mock payment approved"
        );

        assertThat(result.getStatus()).isEqualTo(com.example.fan_cafe.order.domain.Status.PAID);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("동일 paymentKey 재승인은 멱등 (Outbox 없음)")
    void approvePayment_shouldBeIdempotent_whenSameKey() {
        paymentPendingOrder.markPaid("pay-key-1");
        stubLockedOrder(paymentPendingOrder);

        Order result = orderPaymentCommandService.approvePayment(
                paymentPendingOrder,
                BigDecimal.valueOf(20000),
                "pay-key-1",
                "mock payment approved"
        );

        assertThat(result.getStatus()).isEqualTo(com.example.fan_cafe.order.domain.Status.PAID);
        verify(outboxEventRepository, never()).save(any());
        verify(orderStatusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("금액 불일치 시 PAYMENT_FAILED, Outbox 없음")
    void approvePayment_shouldFail_whenAmountMismatch() {
        stubLockedOrder(paymentPendingOrder);
        assertThatThrownBy(() -> orderPaymentCommandService.approvePayment(
                paymentPendingOrder,
                BigDecimal.ONE,
                "bad",
                "mock"
        )).isInstanceOf(CustomException.class)
                .hasMessageContaining(OrderErrorCode.PAYMENT_AMOUNT_MISMATCH.getMessage());

        assertThat(paymentPendingOrder.getStatus()).isEqualTo(com.example.fan_cafe.order.domain.Status.PAYMENT_FAILED);
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("실패 처리 시 PAYMENT_FAILED, Outbox 없음")
    void failPayment_shouldMarkPaymentFailed_withoutOutbox() {
        Order result = orderPaymentCommandService.failPayment(paymentPendingOrder, "webhook fail");

        assertThat(result.getStatus()).isEqualTo(com.example.fan_cafe.order.domain.Status.PAYMENT_FAILED);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("PAID 주문 취소/환불 시 REFUNDED 전이·이력·Outbox PAYMENT_REFUNDED 저장")
    void cancelPayment_shouldRefundPaidOrderAndSaveOutbox() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventType\":\"PAYMENT_REFUNDED\"}");

        Order result = orderPaymentCommandService.cancelPayment(
                paidOrder, "customer request", "cancel-idem-001");

        assertThat(result.getStatus()).isEqualTo(com.example.fan_cafe.order.domain.Status.REFUNDED);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("동일 idempotencyKey 재취소는 멱등 (Outbox·이력 없음)")
    void cancelPayment_shouldBeIdempotent_whenSameKey() {
        paidOrder.markRefunded("cancel-idem-001");

        Order result = orderPaymentCommandService.cancelPayment(
                paidOrder, "customer request", "cancel-idem-001");

        assertThat(result.getStatus()).isEqualTo(com.example.fan_cafe.order.domain.Status.REFUNDED);
        verify(outboxEventRepository, never()).save(any());
        verify(orderStatusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("다른 idempotencyKey로 재취소 시 ORDER_ALREADY_REFUNDED 예외")
    void cancelPayment_shouldThrow_whenDifferentKeyOnRefunded() {
        paidOrder.markRefunded("cancel-idem-001");

        assertThatThrownBy(() -> orderPaymentCommandService.cancelPayment(
                paidOrder, "retry", "cancel-idem-002"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(OrderErrorCode.ORDER_ALREADY_REFUNDED.getMessage());

        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("PAYMENT_PENDING 상태에서 취소 요청 시 ORDER_NOT_REFUNDABLE 예외")
    void cancelPayment_shouldThrow_whenPaymentPending() {
        assertThatThrownBy(() -> orderPaymentCommandService.cancelPayment(
                paymentPendingOrder, "too early", "cancel-x"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(OrderErrorCode.ORDER_NOT_REFUNDABLE.getMessage());

        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("PAYMENT_FAILED 상태에서 취소 요청 시 ORDER_NOT_REFUNDABLE 예외")
    void cancelPayment_shouldThrow_whenPaymentFailed() {
        paymentPendingOrder.markPaymentFailed();

        assertThatThrownBy(() -> orderPaymentCommandService.cancelPayment(
                paymentPendingOrder, "failed order", "cancel-x"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(OrderErrorCode.ORDER_NOT_REFUNDABLE.getMessage());

        verify(outboxEventRepository, never()).save(any());
    }
}
