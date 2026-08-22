package com.example.fan_cafe.order.application;

import com.example.fan_cafe.merchandise.domain.Category;
import com.example.fan_cafe.merchandise.domain.Merchandise;
import com.example.fan_cafe.merchandise.infrastructure.MerchandiseRepository;
import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.domain.OrderItem;
import com.example.fan_cafe.order.domain.OrderStatusHistory;
import com.example.fan_cafe.order.domain.Status;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.infrastructure.OrderStatusHistoryRepository;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import com.example.fan_cafe.order.payment.client.PaymentStatusResponse;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderRefundResultServiceTest {
    @Mock private OrderRepository orderRepository;
    @Mock private MerchandiseRepository merchandiseRepository;
    @Mock private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private OrderRefundResultService orderRefundResultService;

    private Order paidOrder;
    private Merchandise merchandise;
    private PaymentStatusResponse refundedPayment;

    @BeforeEach
    void setUp() throws Exception {
        User user = User.of("u@test.com", "pw", "u", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        paidOrder = Order.paymentPending(user, BigDecimal.valueOf(20000));
        ReflectionTestUtils.setField(paidOrder, "id", 10L);
        paidOrder.addItem(OrderItem.snapshot(100L, "응원봉", BigDecimal.valueOf(10000), 2));
        paidOrder.markPaid();

        merchandise = Merchandise.builder()
                .id(100L).name("응원봉").description("응원봉")
                .price(10000L).stock(0).status(com.example.fan_cafe.merchandise.domain.Status.SOLD_OUT)
                .category(Category.CLOTHES).build();
        refundedPayment = new PaymentStatusResponse(
                10L, PaymentResultStatus.REFUNDED, BigDecimal.valueOf(20000), BigDecimal.valueOf(20000),
                "pay-1", null, "REFUND:550e8400-e29b-41d4-a716-446655440000", "customer request", null);

        lenient().when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> {
            OutboxEvent event = invocation.getArgument(0);
            if (event.getId() == null) ReflectionTestUtils.setField(event, "id", 99L);
            return event;
        });
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventType\":\"PAYMENT_REFUNDED\"}");
    }

    @Test
    void apply_restoresStockAndPersistsHistoryAndOutboxOnce() {
        when(orderRepository.findPaymentOrderWithPessimisticLock(10L)).thenReturn(Optional.of(paidOrder));
        when(merchandiseRepository.findMerchandiseWithPessimisticLock(100L)).thenReturn(Optional.of(merchandise));

        OrderQueryResponse result = orderRefundResultService.apply(10L, refundedPayment, "customer request");

        assertThat(result.getStatus()).isEqualTo(Status.REFUNDED);
        assertThat(merchandise.getStock()).isEqualTo(2);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    void apply_isIdempotentWhenOrderIsAlreadyRefunded() {
        paidOrder.markRefunded();
        when(orderRepository.findPaymentOrderWithPessimisticLock(10L)).thenReturn(Optional.of(paidOrder));

        OrderQueryResponse result = orderRefundResultService.apply(10L, refundedPayment, "retry");

        assertThat(result.getStatus()).isEqualTo(Status.REFUNDED);
        assertThat(merchandise.getStock()).isZero();
        verifyNoInteractions(merchandiseRepository, orderStatusHistoryRepository, outboxEventRepository);
    }
}
