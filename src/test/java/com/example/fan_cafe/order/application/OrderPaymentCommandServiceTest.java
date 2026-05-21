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

    @BeforeEach
    void setUp() throws JsonProcessingException {
        User user = User.of("u@test.com", "pw", "u", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);

        paymentPendingOrder = Order.paymentPending(user, BigDecimal.valueOf(20000));
        ReflectionTestUtils.setField(paymentPendingOrder, "id", 10L);
        paymentPendingOrder.addItem(OrderItem.snapshot(100L, "응원봉", BigDecimal.valueOf(10000), 2));

        lenient().when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> {
            OutboxEvent e = invocation.getArgument(0);
            if (e.getId() == null) {
                ReflectionTestUtils.setField(e, "id", 999L);
            }
            return e;
        });
        lenient().when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventType\":\"ORDER_CREATED\"}");
    }

    @Test
    @DisplayName("승인 시 PAID 전이·이력·Outbox 저장")
    void approvePayment_shouldMarkPaidAndSaveOutbox() {
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
}
