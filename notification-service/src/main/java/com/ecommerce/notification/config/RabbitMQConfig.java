package com.ecommerce.notification.config;

import com.ecommerce.common.event.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    @Bean public TopicExchange notificationExchange() { return ExchangeBuilder.topicExchange(RabbitMQConstants.NOTIFICATION_EXCHANGE).durable(true).build(); }
    @Bean public TopicExchange orderExchange() { return ExchangeBuilder.topicExchange(RabbitMQConstants.ORDER_EXCHANGE).durable(true).build(); }
    @Bean public TopicExchange paymentExchange() { return ExchangeBuilder.topicExchange(RabbitMQConstants.PAYMENT_EXCHANGE).durable(true).build(); }

    @Bean public Queue notificationQueue() { return QueueBuilder.durable(RabbitMQConstants.NOTIFICATION_QUEUE).build(); }
    @Bean public Queue notificationOrderQueue() { return QueueBuilder.durable(RabbitMQConstants.NOTIFICATION_ORDER_QUEUE).build(); }
    @Bean public Queue notificationPaymentQueue() { return QueueBuilder.durable(RabbitMQConstants.NOTIFICATION_PAYMENT_QUEUE).build(); }

    @Bean public Binding notifBinding() { return BindingBuilder.bind(notificationQueue()).to(notificationExchange()).with(RabbitMQConstants.NOTIFICATION_SEND_KEY); }
    @Bean public Binding notifOrderBinding() { return BindingBuilder.bind(notificationOrderQueue()).to(orderExchange()).with(RabbitMQConstants.ORDER_CREATED_KEY); }
    @Bean public Binding notifPaymentBinding() { return BindingBuilder.bind(notificationPaymentQueue()).to(paymentExchange()).with(RabbitMQConstants.PAYMENT_COMPLETED_KEY); }

    @Bean public Jackson2JsonMessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }
    @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory cf) { RabbitTemplate t = new RabbitTemplate(cf); t.setMessageConverter(messageConverter()); return t; }
}
