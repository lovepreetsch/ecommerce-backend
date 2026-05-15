package com.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event published when inventory levels change.
 * Consumed by: Notification Service (low stock alerts).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryUpdatedEvent implements Serializable {

    private Long productId;
    private String productName;
    private String sku;
    private int previousQuantity;
    private int currentQuantity;
    private int reservedQuantity;
    private String updateType; // RESERVED, RELEASED, DEDUCTED, RESTOCKED
    private String referenceId; // orderId or other reference
    private boolean lowStock;
    private LocalDateTime updatedAt;
}
