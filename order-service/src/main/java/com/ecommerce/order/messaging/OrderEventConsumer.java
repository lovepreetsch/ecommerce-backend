package com.ecommerce.order.messaging;

import com.ecommerce.common.event.PaymentCompletedEvent;
import com.ecommerce.common.event.RabbitMQConstants;
import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j @Component @RequiredArgsConstructor
public class OrderEventConsumer {
    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConstants.ORDER_PAYMENT_QUEUE)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent for order: {}", event.getOrderNumber());
        orderService.handlePaymentCompleted(event);
    }
}
