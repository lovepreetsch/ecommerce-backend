package com.ecommerce.auth.config;

import com.ecommerce.common.event.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for the Auth Service.
 * Defines the user exchange and bindings for UserRegisteredEvent.
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange userExchange() {
        return ExchangeBuilder.topicExchange(RabbitMQConstants.USER_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange notificationExchange() {
        return ExchangeBuilder.topicExchange(RabbitMQConstants.NOTIFICATION_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
