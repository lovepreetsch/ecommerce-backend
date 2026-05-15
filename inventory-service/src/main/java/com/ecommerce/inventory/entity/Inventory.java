package com.ecommerce.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "inventory", indexes = {@Index(name = "idx_inv_product", columnList = "productId", unique = true)})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Inventory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private Long productId;
    private String sku;
    @Column(nullable = false) @Builder.Default private int quantity = 0;
    @Builder.Default private int reservedQuantity = 0;
    @Builder.Default private int reorderLevel = 10;
    private String warehouseLocation;
    @Version private Long version; // Optimistic locking
    @CreationTimestamp @Column(updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;

    public int getAvailableQuantity() { return quantity - reservedQuantity; }
}
