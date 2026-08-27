package com.example.fan_cafe.order.saga.recovery;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "saga.recovery")
public class SagaRecoveryProperties {
    private boolean enabled = true;
    private Duration fixedDelay = Duration.ofSeconds(5);
    private int batchSize = 10;
    private int maxRetryCount = 3;
    private Duration paymentUnknownInitialDelay = Duration.ofSeconds(10);
    private Duration orderCompletionInitialDelay = Duration.ofSeconds(10);
    private Duration refundResultTimeout = Duration.ofMinutes(1);
    private Duration baseDelay = Duration.ofSeconds(5);
    private Duration maxDelay = Duration.ofMinutes(5);
    private double jitterRatio = 0.2;
    private Duration claimLease = Duration.ofSeconds(30);

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Duration getFixedDelay() { return fixedDelay; }
    public void setFixedDelay(Duration fixedDelay) { this.fixedDelay = fixedDelay; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public int getMaxRetryCount() { return maxRetryCount; }
    public void setMaxRetryCount(int maxRetryCount) { this.maxRetryCount = maxRetryCount; }
    public Duration getPaymentUnknownInitialDelay() { return paymentUnknownInitialDelay; }
    public void setPaymentUnknownInitialDelay(Duration paymentUnknownInitialDelay) {
        this.paymentUnknownInitialDelay = paymentUnknownInitialDelay;
    }
    public Duration getOrderCompletionInitialDelay() { return orderCompletionInitialDelay; }
    public void setOrderCompletionInitialDelay(Duration orderCompletionInitialDelay) {
        this.orderCompletionInitialDelay = orderCompletionInitialDelay;
    }
    public Duration getRefundResultTimeout() { return refundResultTimeout; }
    public void setRefundResultTimeout(Duration refundResultTimeout) {
        this.refundResultTimeout = refundResultTimeout;
    }
    public Duration getBaseDelay() { return baseDelay; }
    public void setBaseDelay(Duration baseDelay) { this.baseDelay = baseDelay; }
    public Duration getMaxDelay() { return maxDelay; }
    public void setMaxDelay(Duration maxDelay) { this.maxDelay = maxDelay; }
    public double getJitterRatio() { return jitterRatio; }
    public void setJitterRatio(double jitterRatio) { this.jitterRatio = jitterRatio; }
    public Duration getClaimLease() { return claimLease; }
    public void setClaimLease(Duration claimLease) { this.claimLease = claimLease; }
}
