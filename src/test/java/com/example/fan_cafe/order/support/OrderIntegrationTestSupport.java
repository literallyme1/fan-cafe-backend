package com.example.fan_cafe.order.support;

import com.example.fan_cafe.merchandise.domain.Category;
import com.example.fan_cafe.merchandise.domain.Merchandise;
import com.example.fan_cafe.merchandise.domain.Status;
import com.example.fan_cafe.merchandise.infrastructure.MerchandiseRepository;
import com.example.fan_cafe.order.application.OrderService;
import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.infrastructure.OrderStatusHistoryRepository;
import com.example.fan_cafe.order.interfaces.dto.OrderCreateRequest;
import com.example.fan_cafe.order.interfaces.dto.OrderCreateResponse;
import com.example.fan_cafe.order.saga.infrastructure.SagaInstanceRepository;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Order 결제/환불 통합 테스트용 픽스처·웹훅 서명 헬퍼.
 */
@Component
public class OrderIntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MerchandiseRepository merchandiseRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private SagaInstanceRepository sagaInstanceRepository;

    @Value("${mock.pg.webhook-secret}")
    private String webhookSecret;

    public record PaymentPendingFixture(User user, Merchandise merchandise, Order order, BigDecimal totalPrice) {
    }

    public SignedWebhook signedPaymentApprovedWebhook(Long orderId, BigDecimal approvalAmount, String idempotencyKey) {
        String rawBody = """
                {"eventType":"PAYMENT_APPROVED","orderId":%d,"approvalAmount":%s,"idempotencyKey":"%s"}
                """.formatted(orderId, approvalAmount.toPlainString(), idempotencyKey).trim();
        return sign(rawBody);
    }

    public SignedWebhook signedPaymentApprovedWebhookWithAmount(Long orderId, long wrongAmount, String idempotencyKey) {
        return signedPaymentApprovedWebhook(orderId, BigDecimal.valueOf(wrongAmount), idempotencyKey);
    }

    public SignedWebhook sign(String rawBody) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = signForTest(webhookSecret, timestamp, rawBody);
        return new SignedWebhook(rawBody, timestamp, signature);
    }

    public SignedWebhook signedWebhookWithTimestamp(String rawBody, long epochSeconds) {
        String timestamp = String.valueOf(epochSeconds);
        String signature = signForTest(webhookSecret, timestamp, rawBody);
        return new SignedWebhook(rawBody, timestamp, signature);
    }

    public PaymentPendingFixture createPaymentPendingOrder() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.of(
                "order-it-" + suffix + "@test.com",
                "encoded-password",
                "order-it-" + suffix,
                Role.USER
        ));

        Merchandise merchandise = merchandiseRepository.save(Merchandise.builder()
                .name("통합테스트 상품-" + suffix)
                .description("order integration test")
                .price(10_000L)
                .salePrice(9_000L)
                .stock(100)
                .status(Status.SALE)
                .category(Category.CLOTHES)
                .build());

        OrderCreateRequest request = new OrderCreateRequest();
        OrderCreateRequest.Item item = new OrderCreateRequest.Item();
        ReflectionTestUtils.setField(item, "productId", merchandise.getId());
        ReflectionTestUtils.setField(item, "quantity", 2);
        ReflectionTestUtils.setField(request, "items", List.of(item));

        OrderCreateResponse created = orderService.create(user, request);
        Order order = orderRepository.findByIdWithItems(created.getOrderId()).orElseThrow();
        return new PaymentPendingFixture(user, merchandise, order, order.getTotalPrice());
    }

    @Transactional
    public void cleanup(PaymentPendingFixture fixture) {
        if (fixture == null || fixture.order().getId() == null) {
            return;
        }
        Long orderId = fixture.order().getId();
        sagaInstanceRepository.deleteByOrderId(orderId);
        orderStatusHistoryRepository.deleteByOrder_Id(orderId);
        outboxEventRepository.deleteByAggregateTypeAndAggregateId("ORDER", orderId);
        orderRepository.deleteById(orderId);
        merchandiseRepository.deleteById(fixture.merchandise().getId());
        userRepository.deleteById(fixture.user().getId());
    }

    public record SignedWebhook(String rawBody, String timestamp, String signature) {
    }

    private String signForTest(String secret, String timestamp, String rawBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(
                    (timestamp + "." + rawBody).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
