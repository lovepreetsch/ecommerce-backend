package com.ecommerce.user.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VendorProfileDTO {
    private Long id;
    private String storeName;
    private String storeDescription;
    private String logoUrl;
    private boolean verified;
    private double rating;
    private int totalProducts;
}
