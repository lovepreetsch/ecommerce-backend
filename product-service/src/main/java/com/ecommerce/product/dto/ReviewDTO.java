package com.ecommerce.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewDTO {
    private Long id;
    private Long userId;
    @Min(1) @Max(5)
    private int rating;
    private String comment;
    private String status;
    private String productName; // For admin view
    private String createdAt;
}
