package com.ecommerce.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a payment processing operation fails.
 */
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class PaymentException extends RuntimeException {

    private String stripeErrorCode;

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, String stripeErrorCode) {
        super(message);
        this.stripeErrorCode = stripeErrorCode;
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getStripeErrorCode() {
        return stripeErrorCode;
    }
}
