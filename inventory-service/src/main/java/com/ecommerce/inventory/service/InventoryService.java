package com.ecommerce.inventory.service;

import com.ecommerce.common.event.*;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.inventory.dto.InventoryDTO;
import com.ecommerce.inventory.entity.*;
import com.ecommerce.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final RabbitTemplate rabbitTemplate;

    public List<InventoryDTO> getAllInventory() {
        return inventoryRepository.findAll().stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    public InventoryDTO getByProductId(Long productId) {
        Inventory inv = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", productId));
        return mapToDTO(inv);
    }

    @Transactional
    public InventoryDTO updateStock(Long productId, int quantity, String warehouseLocation) {
        Inventory inv = inventoryRepository.findByProductId(productId)
                .orElse(Inventory.builder().productId(productId).quantity(0).build());
        inv.setQuantity(quantity);
        if (warehouseLocation != null) inv.setWarehouseLocation(warehouseLocation);
        inv = inventoryRepository.save(inv);

        recordMovement(inv, InventoryMovement.MovementType.IN, quantity, null, "Stock update");
        publishInventoryEvent(inv, "RESTOCKED");
        return mapToDTO(inv);
    }

    @Transactional
    public InventoryDTO reserveStock(Long productId, int quantity, String orderId) {
        Inventory inv = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", productId));

        if (inv.getAvailableQuantity() < quantity) {
            throw new BadRequestException("Insufficient stock for product: " + productId);
        }

        inv.setReservedQuantity(inv.getReservedQuantity() + quantity);
        inv = inventoryRepository.save(inv);

        recordMovement(inv, InventoryMovement.MovementType.RESERVE, quantity, orderId, "Order reservation");
        publishInventoryEvent(inv, "RESERVED");
        return mapToDTO(inv);
    }

    @Transactional
    public InventoryDTO releaseStock(Long productId, int quantity, String orderId) {
        Inventory inv = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", productId));

        inv.setReservedQuantity(Math.max(0, inv.getReservedQuantity() - quantity));
        inv = inventoryRepository.save(inv);

        recordMovement(inv, InventoryMovement.MovementType.RELEASE, quantity, orderId, "Stock released");
        publishInventoryEvent(inv, "RELEASED");
        return mapToDTO(inv);
    }

    @Transactional
    public void deductReserved(Long productId, int quantity, String orderId) {
        Inventory inv = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", productId));

        inv.setQuantity(inv.getQuantity() - quantity);
        inv.setReservedQuantity(Math.max(0, inv.getReservedQuantity() - quantity));
        inv = inventoryRepository.save(inv);

        recordMovement(inv, InventoryMovement.MovementType.OUT, quantity, orderId, "Payment confirmed - stock deducted");
        publishInventoryEvent(inv, "DEDUCTED");
    }

    // Handle OrderCreatedEvent — reserve stock for all items
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Reserving inventory for order: {}", event.getOrderNumber());
        for (OrderCreatedEvent.OrderItemEvent item : event.getItems()) {
            reserveStock(item.getProductId(), item.getQuantity(), event.getOrderNumber());
        }
    }

    // Handle PaymentCompletedEvent — deduct reserved stock
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Deducting inventory for paid order: {}", event.getOrderNumber());
        // We'll need order items - in production, fetch from order service or include in event
    }

    @Async
    public CompletableFuture<List<InventoryDTO>> getLowStockReport() {
        log.info("Generating low stock report asynchronously");
        List<InventoryDTO> lowStock = inventoryRepository.findByQuantityLessThanEqual(10)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
        return CompletableFuture.completedFuture(lowStock);
    }

    private void recordMovement(Inventory inv, InventoryMovement.MovementType type, int qty, String ref, String reason) {
        movementRepository.save(InventoryMovement.builder()
                .inventory(inv).type(type).quantity(qty).referenceId(ref).reason(reason).build());
    }

    private void publishInventoryEvent(Inventory inv, String updateType) {
        try {
            InventoryUpdatedEvent event = InventoryUpdatedEvent.builder()
                    .productId(inv.getProductId()).sku(inv.getSku())
                    .currentQuantity(inv.getQuantity()).reservedQuantity(inv.getReservedQuantity())
                    .updateType(updateType).lowStock(inv.getQuantity() <= inv.getReorderLevel())
                    .updatedAt(LocalDateTime.now()).build();
            rabbitTemplate.convertAndSend(RabbitMQConstants.INVENTORY_EXCHANGE, RabbitMQConstants.INVENTORY_UPDATED_KEY, event);
        } catch (Exception e) {
            log.error("Failed to publish InventoryUpdatedEvent: {}", e.getMessage());
        }
    }

    private InventoryDTO mapToDTO(Inventory inv) {
        return InventoryDTO.builder().id(inv.getId()).productId(inv.getProductId()).sku(inv.getSku())
                .quantity(inv.getQuantity()).reservedQuantity(inv.getReservedQuantity())
                .availableQuantity(inv.getAvailableQuantity()).reorderLevel(inv.getReorderLevel())
                .warehouseLocation(inv.getWarehouseLocation()).build();
    }
}
