package com.example.payment.experiment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

@Configuration
@Profile("experiment")
@ConfigurationProperties(prefix = "experiment.payment")
public class PaymentApprovalExperimentProperties {
    private boolean partialSuccessEnabled;
    private int partialSuccessPercent = 20;
    private Duration approvalResponseDelay = Duration.ofSeconds(5);

    public boolean isPartialSuccessEnabled() {
        return partialSuccessEnabled;
    }

    public void setPartialSuccessEnabled(boolean partialSuccessEnabled) {
        this.partialSuccessEnabled = partialSuccessEnabled;
    }

    public int getPartialSuccessPercent() {
        return partialSuccessPercent;
    }

    public void setPartialSuccessPercent(int partialSuccessPercent) {
        if (partialSuccessPercent < 0 || partialSuccessPercent > 100) {
            throw new IllegalArgumentException("partial-success-percent must be between 0 and 100");
        }
        this.partialSuccessPercent = partialSuccessPercent;
    }

    public Duration getApprovalResponseDelay() {
        return approvalResponseDelay;
    }

    public void setApprovalResponseDelay(Duration approvalResponseDelay) {
        if (approvalResponseDelay == null || approvalResponseDelay.isNegative()) {
            throw new IllegalArgumentException("approval-response-delay must not be negative");
        }
        this.approvalResponseDelay = approvalResponseDelay;
    }
}
