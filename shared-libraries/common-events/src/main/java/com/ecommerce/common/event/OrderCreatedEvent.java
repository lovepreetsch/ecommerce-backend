package com.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Event published when a new order is created.
 * Consumed by: Inventory Service (reserve stock), Payment Service, Notification Service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {

    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private BigDecimal totalAmount;
    private String currency;
    private List<OrderItemEvent> items;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemEvent implements Serializable {
        private Long productId;
        private String productName;
        private int quantity;
        private BigDecimal price;
        private Long vendorId;
    }
}
