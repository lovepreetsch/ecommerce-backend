package com.ecommerce.inventory.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.inventory.dto.InventoryDTO;
import com.ecommerce.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController @RequestMapping("/api/inventory") @RequiredArgsConstructor
@Tag(name = "Inventory", description = "Stock management")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    @Operation(summary = "Get stock level for a product")
    public ResponseEntity<ApiResponse<InventoryDTO>> getInventory(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getByProductId(productId)));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @Operation(summary = "Update stock quantity")
    public ResponseEntity<ApiResponse<InventoryDTO>> updateStock(
            @PathVariable Long productId,
            @RequestParam int quantity,
            @RequestParam(required = false) String warehouseLocation) {
        return ResponseEntity.ok(ApiResponse.success("Stock updated", inventoryService.updateStock(productId, quantity, warehouseLocation)));
    }

    @PostMapping("/{productId}/reserve")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @Operation(summary = "Reserve stock for an order")
    public ResponseEntity<ApiResponse<InventoryDTO>> reserveStock(
            @PathVariable Long productId, @RequestParam int quantity, @RequestParam String orderId) {
        return ResponseEntity.ok(ApiResponse.success("Stock reserved", inventoryService.reserveStock(productId, quantity, orderId)));
    }

    @PostMapping("/{productId}/release")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @Operation(summary = "Release reserved stock")
    public ResponseEntity<ApiResponse<InventoryDTO>> releaseStock(
            @PathVariable Long productId, @RequestParam int quantity, @RequestParam String orderId) {
        return ResponseEntity.ok(ApiResponse.success("Stock released", inventoryService.releaseStock(productId, quantity, orderId)));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @Operation(summary = "Get low stock report (async)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<InventoryDTO>>>> getLowStockReport() {
        return inventoryService.getLowStockReport()
                .thenApply(list -> ResponseEntity.ok(ApiResponse.success(list)));
    }
}
