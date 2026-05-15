package com.ecommerce.inventory.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InventoryDTO {
    private Long id;
    private Long productId;
    private String sku;
    private int quantity;
    private int reservedQuantity;
    private int availableQuantity;
    private int reorderLevel;
    private String warehouseLocation;
}
