package com.ecommerce.notification.messaging;

import com.ecommerce.common.event.NotificationEvent;
import com.ecommerce.common.event.OrderCreatedEvent;
import com.ecommerce.common.event.PaymentCompletedEvent;
import com.ecommerce.common.event.RabbitMQConstants;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j @Component @RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConstants.NOTIFICATION_QUEUE)
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("Received NotificationEvent type: {} for user: {}", event.getType(), event.getUserId());
        notificationService.processNotificationEvent(event);
    }

    @RabbitListener(queues = RabbitMQConstants.NOTIFICATION_ORDER_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for notification: {}", event.getOrderNumber());
        notificationService.processOrderCreated(event);
    }

    @RabbitListener(queues = RabbitMQConstants.NOTIFICATION_PAYMENT_QUEUE)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent for notification: {}", event.getOrderNumber());
        notificationService.processPaymentCompleted(event);
    }
}
