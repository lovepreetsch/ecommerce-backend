package com.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published when a payment is completed successfully.
 * Consumed by: Order Service (confirm order), Inventory Service (finalize stock), Notification Service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent implements Serializable {

    private Long paymentId;
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private BigDecimal amount;
    private String currency;
    private String stripePaymentIntentId;
    private String status; // SUCCEEDED, FAILED
    private LocalDateTime completedAt;
}
