package com.ecommerce.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "inventory_movements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryMovement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "inventory_id", nullable = false) private Inventory inventory;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private MovementType type;
    @Column(nullable = false) private int quantity;
    private String referenceId;
    private String reason;
    @CreationTimestamp @Column(updatable = false) private LocalDateTime createdAt;

    public enum MovementType { IN, OUT, RESERVE, RELEASE }
}
