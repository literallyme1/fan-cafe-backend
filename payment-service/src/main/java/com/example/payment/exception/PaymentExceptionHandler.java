package com.example.payment.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PaymentExceptionHandler {
    @ExceptionHandler(PaymentException.class)
    ResponseEntity<PaymentErrorResponse> handle(PaymentException exception) {
        PaymentErrorCode code = exception.getErrorCode();
        return ResponseEntity.status(code.getStatus())
                .body(new PaymentErrorResponse(code.getCode(), code.getMessage()));
    }
}
