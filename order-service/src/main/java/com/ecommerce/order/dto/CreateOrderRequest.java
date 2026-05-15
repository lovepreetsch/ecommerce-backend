package com.ecommerce.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateOrderRequest {
    @NotEmpty(message = "Order must have at least one item")
    private List<OrderItemDTO> items;
    @NotBlank(message = "Shipping address is required")
    private String shippingAddressJson;
}
