package com.ecommerce.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductDTO {
    private Long id;
    private Long vendorId;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private String sku;
    private Long categoryId;
    private String categoryName;
    private String status;
    private List<ImageDTO> images;
    private List<AttributeDTO> attributes;
    private double averageRating;
    private int reviewCount;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ImageDTO {
        private Long id;
        private String url;
        private String altText;
        private int displayOrder;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AttributeDTO {
        private Long id;
        private String name;
        private String value;
    }
}
