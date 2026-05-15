package com.ecommerce.payment.messaging;

import com.ecommerce.common.event.OrderCreatedEvent;
import com.ecommerce.common.event.RabbitMQConstants;
import com.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j @Component @RequiredArgsConstructor
public class PaymentEventConsumer {
    private final PaymentService paymentService;

    @RabbitListener(queues = RabbitMQConstants.PAYMENT_ORDER_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for payment processing: {}", event.getOrderNumber());
        paymentService.handleOrderCreated(event);
    }
}
