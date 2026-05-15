package com.ecommerce.inventory.messaging;

import com.ecommerce.common.event.OrderCreatedEvent;
import com.ecommerce.common.event.PaymentCompletedEvent;
import com.ecommerce.common.event.RabbitMQConstants;
import com.ecommerce.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j @Component @RequiredArgsConstructor
public class InventoryEventConsumer {

    private final InventoryService inventoryService;

    @RabbitListener(queues = RabbitMQConstants.INVENTORY_ORDER_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for order: {}", event.getOrderNumber());
        inventoryService.handleOrderCreated(event);
    }

    @RabbitListener(queues = RabbitMQConstants.INVENTORY_PAYMENT_QUEUE)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent for order: {}", event.getOrderNumber());
        inventoryService.handlePaymentCompleted(event);
    }
}
