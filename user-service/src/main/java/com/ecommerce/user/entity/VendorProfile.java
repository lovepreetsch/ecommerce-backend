package com.ecommerce.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VendorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false, unique = true)
    private UserProfile userProfile;

    @Column(nullable = false)
    private String storeName;

    @Column(length = 1000)
    private String storeDescription;

    private String logoUrl;

    @Builder.Default
    private boolean verified = false;

    @Builder.Default
    private double rating = 0.0;

    @Builder.Default
    private int totalProducts = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
