package com.example.payment.experiment;

import com.example.payment.domain.PaymentStatus;
import com.example.payment.interfaces.dto.PaymentResultResponse;
import org.junit.jupiter.api.Test;

import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentApprovalResponseDelayAdviceTest {

    @Test
    void deterministicHashSelectsExactlyTwentyPercentOfOneHundredSequentialOrderIds() {
        PaymentApprovalExperimentProperties properties = properties(true, 20);
        PaymentApprovalResponseDelayAdvice advice = new PaymentApprovalResponseDelayAdvice(properties);

        var selected = LongStream.rangeClosed(1, 100)
                .filter(orderId -> advice.shouldDelay(approved(orderId), approvalPath(orderId)))
                .boxed()
                .toList();
        var repeatedSelection = LongStream.rangeClosed(1, 100)
                .filter(orderId -> advice.shouldDelay(approved(orderId), approvalPath(orderId)))
                .boxed()
                .toList();

        assertThat(selected).hasSize(20);
        assertThat(repeatedSelection).isEqualTo(selected);
    }

    @Test
    void delayIsDisabledOutsideExplicitPartialSuccessRunAndNeverAppliesToStatusLookup() {
        PaymentApprovalResponseDelayAdvice disabled = new PaymentApprovalResponseDelayAdvice(
                properties(false, 20));
        PaymentApprovalResponseDelayAdvice enabled = new PaymentApprovalResponseDelayAdvice(
                properties(true, 20));

        assertThat(disabled.shouldDelay(approved(5L), approvalPath(5L))).isFalse();
        assertThat(enabled.shouldDelay(approved(5L), "/internal/payments/5")).isFalse();
        assertThat(enabled.shouldDelay(
                new PaymentResultResponse(5L, PaymentStatus.FAILED, null, "failed", null),
                approvalPath(5L))).isFalse();
    }

    private PaymentApprovalExperimentProperties properties(boolean enabled, int percent) {
        PaymentApprovalExperimentProperties properties = new PaymentApprovalExperimentProperties();
        properties.setPartialSuccessEnabled(enabled);
        properties.setPartialSuccessPercent(percent);
        return properties;
    }

    private PaymentResultResponse approved(Long orderId) {
        return new PaymentResultResponse(orderId, PaymentStatus.APPROVED, "key-" + orderId, null, null);
    }

    private String approvalPath(Long orderId) {
        return "/internal/payments/" + orderId + "/approve";
    }
}
