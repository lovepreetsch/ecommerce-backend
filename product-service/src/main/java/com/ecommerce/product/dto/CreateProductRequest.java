package com.ecommerce.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateProductRequest {
    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    private BigDecimal compareAtPrice;
    private String sku;
    private Long categoryId;
    private String status; // ACTIVE, DRAFT

    private List<ProductDTO.ImageDTO> images;
    private List<ProductDTO.AttributeDTO> attributes;
}
