package com.example.payment.application;

import com.example.payment.domain.Payment;
import com.example.payment.infrastructure.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PaymentCreationService {
    private final PaymentRepository paymentRepository;

    public PaymentCreationService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createPending(Long orderId, BigDecimal expectedAmount) {
        paymentRepository.saveAndFlush(Payment.pending(orderId, expectedAmount));
    }
}
