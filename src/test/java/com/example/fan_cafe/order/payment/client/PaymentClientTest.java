package com.example.fan_cafe.order.payment.client;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class PaymentClientTest {
    private MockRestServiceServer server;
    private PaymentClient paymentClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        paymentClient = new PaymentClient(builder.baseUrl("http://payment-service").build(), new ObjectMapper());
    }

    @Test
    void knownRemoteErrorCode_isMappedToExistingOrderError() {
        server.expect(requestTo("http://payment-service/internal/payments/10/approve"))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"P005\",\"message\":\"already approved\"}"));

        assertThatThrownBy(() -> approve())
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(OrderErrorCode.ORDER_ALREADY_PAID);
        server.verify();
    }

    @Test
    void malformedRemoteErrorBody_isMappedToPaymentServiceError() {
        server.expect(requestTo("http://payment-service/internal/payments/10/approve"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("not-json"));

        assertThatThrownBy(() -> approve())
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(OrderErrorCode.PAYMENT_SERVICE_ERROR);
        server.verify();
    }

    @Test
    void missingRemoteErrorCode_isMappedToPaymentServiceError() {
        server.expect(requestTo("http://payment-service/internal/payments/10/approve"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"missing code\"}"));

        assertThatThrownBy(() -> approve())
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(OrderErrorCode.PAYMENT_SERVICE_ERROR);
        server.verify();
    }

    private void approve() {
        paymentClient.approve(10L, BigDecimal.TEN, BigDecimal.TEN, "key-1");
    }
}
