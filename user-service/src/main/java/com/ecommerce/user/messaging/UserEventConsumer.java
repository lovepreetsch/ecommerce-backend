package com.ecommerce.user.messaging;

import com.ecommerce.common.event.RabbitMQConstants;
import com.ecommerce.common.event.UserRegisteredEvent;
import com.ecommerce.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ consumer for UserRegisteredEvent.
 * Automatically creates a user profile when a new user registers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final UserService userService;

    @RabbitListener(queues = RabbitMQConstants.USER_REGISTERED_QUEUE)
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent for userId: {}, email: {}", event.getUserId(), event.getEmail());
        try {
            userService.createProfileFromEvent(event);
        } catch (Exception e) {
            log.error("Failed to process UserRegisteredEvent: {}", e.getMessage(), e);
            throw e; // Will be retried or sent to DLQ
        }
    }
}
