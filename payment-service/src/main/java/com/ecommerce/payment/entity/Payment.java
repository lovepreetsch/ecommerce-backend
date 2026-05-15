package com.ecommerce.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "payments", indexes = {
    @Index(name = "idx_payment_order", columnList = "orderId"),
    @Index(name = "idx_payment_stripe", columnList = "stripePaymentIntentId")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column(nullable = false) private Long orderId;
    @Column(nullable = false) private String orderNumber;
    @Column(nullable = false) private Long userId;

    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) @Builder.Default private String currency = "usd";

    @Enumerated(EnumType.STRING) @Column(nullable = false) @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    private String stripePaymentIntentId;
    private String stripeClientSecret;
    private String failureReason;

    @CreationTimestamp @Column(updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;

    public enum PaymentStatus { PENDING, PROCESSING, SUCCEEDED, FAILED, REFUNDED }
}
