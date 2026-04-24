package com.example.fan_cafe.order;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.merchandise.domain.Category;
import com.example.fan_cafe.merchandise.domain.Merchandise;
import com.example.fan_cafe.merchandise.domain.Status;
import com.example.fan_cafe.merchandise.infrastructure.MerchandiseRepository;
import com.example.fan_cafe.order.application.OrderService;
import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.domain.OrderItem;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Order paidOrder;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        user = User.of("orderer@test.com", "encoded_pw", "orderer", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);

        paidOrder = Order.paid(user, BigDecimal.valueOf(20000));
        ReflectionTestUtils.setField(paidOrder, "id", 10L);
        paidOrder.addItem(OrderItem.snapshot(100L, "응원봉", BigDecimal.valueOf(10000), 2));

        when(userRepository.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("내 주문 단건 조회에 성공한다.")
    void get_shouldReturnOrder_whenMyOrderExists() {
        when(orderRepository.findByIdAndUserIdWithItems(10L, 1L)).thenReturn(Optional.of(paidOrder));

        OrderQueryResponse response = orderService.get(user, 10L);

        assertThat(response.getOrderId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(com.example.fan_cafe.order.domain.Status.PAID);
        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("내 주문이 아니거나 존재하지 않으면 단건 조회에서 예외가 발생한다.")
    void get_shouldThrowException_whenOrderNotFound() {
        when(orderRepository.findByIdAndUserIdWithItems(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.get(user, 999L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(OrderErrorCode.ORDER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("내 주문 목록 조회에 성공한다.")
    void getMyOrders_shouldReturnList() {
        when(orderRepository.findAllByUserIdWithItems(1L)).thenReturn(List.of(paidOrder));

        List<OrderQueryResponse> responses = orderService.getMyOrders(user);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getOrderId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("주문 취소 시 재고 복구, 상태 변경, outbox 저장이 수행된다.")
    void cancel_shouldRestoreStockAndCancelOrderAndSaveOutbox() {
        Merchandise merchandise = Merchandise.builder()
                .id(100L)
                .name("응원봉")
                .description("응원봉")
                .price(10000L)
                .salePrice(9000L)
                .stock(0)
                .status(Status.SOLD_OUT)
                .category(Category.LIGHT_STICK)
                .build();

        when(orderRepository.findByIdAndUserIdWithItems(10L, 1L)).thenReturn(Optional.of(paidOrder));
        when(merchandiseRepository.findByIdAndDeletedAtIsNullForUpdate(100L)).thenReturn(Optional.of(merchandise));
        try {
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventType\":\"ORDER_CANCELLED\"}");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        OrderQueryResponse response = orderService.cancel(user, 10L);

        assertThat(response.getStatus()).isEqualTo(com.example.fan_cafe.order.domain.Status.CANCELLED);
        assertThat(merchandise.getStock()).isEqualTo(2);
        assertThat(merchandise.getStatus()).isEqualTo(Status.SALE);

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(1)).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getAggregateType()).isEqualTo("ORDER");
        assertThat(outboxCaptor.getValue().getAggregateId()).isEqualTo(10L);
        assertThat(outboxCaptor.getValue().getPayload()).contains("ORDER_CANCELLED");
    }

    @Test
    @DisplayName("이미 취소된 주문은 취소할 수 없다.")
    void cancel_shouldThrowException_whenOrderNotCancellable() {
        ReflectionTestUtils.setField(paidOrder, "status", com.example.fan_cafe.order.domain.Status.CANCELLED);
        when(orderRepository.findByIdAndUserIdWithItems(10L, 1L)).thenReturn(Optional.of(paidOrder));

        assertThatThrownBy(() -> orderService.cancel(user, 10L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(OrderErrorCode.ORDER_NOT_CANCELLABLE.getMessage());

        verify(merchandiseRepository, never()).findByIdAndDeletedAtIsNullForUpdate(anyLong());
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }
}
