package com.ecommerce.order.service;

import com.ecommerce.common.event.*;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.dto.*;
import com.ecommerce.order.entity.*;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public OrderDTO createOrder(Long userId, String userEmail, CreateOrderRequest request) {
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Order order = Order.builder()
                .orderNumber(orderNumber).userId(userId).userEmail(userEmail)
                .status(Order.OrderStatus.PENDING)
                .shippingAddressJson(request.getShippingAddressJson())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemDTO itemDTO : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .order(order).productId(itemDTO.getProductId())
                    .productName(itemDTO.getProductName()).price(itemDTO.getPrice())
                    .quantity(itemDTO.getQuantity()).vendorId(itemDTO.getVendorId())
                    .build();
            order.getItems().add(item);
            total = total.add(itemDTO.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
        }
        order.setTotalAmount(total);

        // Add initial status history
        order.getStatusHistory().add(OrderStatusHistory.builder()
                .order(order).status(Order.OrderStatus.PENDING).note("Order created").changedBy("SYSTEM").build());

        order = orderRepository.save(order);
        log.info("Order created: {} for user: {}", orderNumber, userId);

        // Publish OrderCreatedEvent asynchronously
        publishOrderCreatedEvent(order);

        return mapToDTO(order);
    }

    public Page<OrderDTO> getMyOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(this::mapToDTO);
    }

    public OrderDTO getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        if (!order.getUserId().equals(userId)) {
            throw new BadRequestException("You can only view your own orders");
        }
        return mapToDTO(order);
    }

    public OrderDTO getOrderByIdAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return mapToDTO(order);
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, String status, String note, String changedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status);
        order.setStatus(newStatus);
        order.getStatusHistory().add(OrderStatusHistory.builder()
                .order(order).status(newStatus).note(note).changedBy(changedBy).build());

        order = orderRepository.save(order);
        log.info("Order {} status updated to {}", order.getOrderNumber(), newStatus);
        return mapToDTO(order);
    }

    @Transactional
    public OrderDTO cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        if (!order.getUserId().equals(userId)) {
            throw new BadRequestException("You can only cancel your own orders");
        }
        if (order.getStatus() != Order.OrderStatus.PENDING && order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw new BadRequestException("Order cannot be cancelled in current status: " + order.getStatus());
        }
        return updateOrderStatus(orderId, "CANCELLED", "Cancelled by customer", "USER:" + userId);
    }

    public Page<OrderDTO> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::mapToDTO);
    }

    // Handle PaymentCompletedEvent
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", event.getOrderId()));

        if ("SUCCEEDED".equals(event.getStatus())) {
            updateOrderStatus(order.getId(), "CONFIRMED", "Payment confirmed: " + event.getStripePaymentIntentId(), "PAYMENT_SERVICE");
        } else {
            updateOrderStatus(order.getId(), "CANCELLED", "Payment failed", "PAYMENT_SERVICE");
        }
    }

    @Async
    public void publishOrderCreatedEvent(Order order) {
        try {
            var items = order.getItems().stream().map(i ->
                    OrderCreatedEvent.OrderItemEvent.builder()
                            .productId(i.getProductId()).productName(i.getProductName())
                            .quantity(i.getQuantity()).price(i.getPrice()).vendorId(i.getVendorId())
                            .build()
            ).collect(Collectors.toList());

            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(order.getId()).orderNumber(order.getOrderNumber())
                    .userId(order.getUserId()).userEmail(order.getUserEmail())
                    .totalAmount(order.getTotalAmount()).currency("USD")
                    .items(items).createdAt(LocalDateTime.now()).build();

            rabbitTemplate.convertAndSend(RabbitMQConstants.ORDER_EXCHANGE, RabbitMQConstants.ORDER_CREATED_KEY, event);
            log.info("Published OrderCreatedEvent for order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to publish OrderCreatedEvent: {}", e.getMessage());
        }
    }

    private OrderDTO mapToDTO(Order o) {
        return OrderDTO.builder()
                .id(o.getId()).orderNumber(o.getOrderNumber()).userId(o.getUserId())
                .status(o.getStatus().name()).totalAmount(o.getTotalAmount())
                .shippingAddressJson(o.getShippingAddressJson())
                .items(o.getItems().stream().map(i -> OrderItemDTO.builder()
                        .id(i.getId()).productId(i.getProductId()).productName(i.getProductName())
                        .price(i.getPrice()).quantity(i.getQuantity()).vendorId(i.getVendorId()).build()
                ).collect(Collectors.toList()))
                .createdAt(o.getCreatedAt() != null ? o.getCreatedAt().toString() : null)
                .updatedAt(o.getUpdatedAt() != null ? o.getUpdatedAt().toString() : null)
                .build();
    }
}
