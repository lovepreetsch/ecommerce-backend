package com.ecommerce.order.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private int quantity;
    private Long vendorId;
}
