package com.ecommerce.payment.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentDTO {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String stripePaymentIntentId;
    private String stripeClientSecret;
    private String createdAt;
}
