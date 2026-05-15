package com.ecommerce.user.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserProfileDTO {
    private Long id;
    private Long authUserId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String avatarUrl;
    private VendorProfileDTO vendorProfile;
}
