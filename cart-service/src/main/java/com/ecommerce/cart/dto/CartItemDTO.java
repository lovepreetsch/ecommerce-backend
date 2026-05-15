package com.ecommerce.cart.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CartItemDTO {
    private Long id;
    @NotNull(message = "Product ID is required")
    private Long productId;
    private String productName;
    private BigDecimal price;
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
    private String imageUrl;
}
