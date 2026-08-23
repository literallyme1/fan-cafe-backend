package com.example.payment.experiment;

import com.example.payment.domain.PaymentStatus;
import com.example.payment.interfaces.dto.PaymentResultResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@Profile("experiment")
@RestControllerAdvice
public class PaymentApprovalResponseDelayAdvice implements ResponseBodyAdvice<PaymentResultResponse> {
    private static final String APPROVAL_PATH_SUFFIX = "/approve";
    private final PaymentApprovalExperimentProperties properties;

    public PaymentApprovalResponseDelayAdvice(PaymentApprovalExperimentProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean supports(
            MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return PaymentResultResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public PaymentResultResponse beforeBodyWrite(
            PaymentResultResponse body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (shouldDelay(body, request.getURI().getPath())) {
            delayResponse();
        }
        return body;
    }

    boolean shouldDelay(PaymentResultResponse body, String requestPath) {
        return properties.isPartialSuccessEnabled()
                && requestPath.endsWith(APPROVAL_PATH_SUFFIX)
                && body != null
                && body.orderId() != null
                && body.status() == PaymentStatus.APPROVED
                && Math.floorMod(Long.hashCode(body.orderId()), 100)
                < properties.getPartialSuccessPercent();
    }

    private void delayResponse() {
        try {
            Thread.sleep(properties.getApprovalResponseDelay());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Experiment payment response delay interrupted", interrupted);
        }
    }
}
