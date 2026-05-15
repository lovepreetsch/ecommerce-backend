package com.ecommerce.order.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderDTO {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String status;
    private BigDecimal totalAmount;
    private String shippingAddressJson;
    private List<OrderItemDTO> items;
    private String createdAt;
    private String updatedAt;
}
