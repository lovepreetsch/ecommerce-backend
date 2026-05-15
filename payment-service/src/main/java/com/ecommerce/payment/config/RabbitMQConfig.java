package com.ecommerce.payment.config;

import com.ecommerce.common.event.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    @Bean public TopicExchange orderExchange() { return ExchangeBuilder.topicExchange(RabbitMQConstants.ORDER_EXCHANGE).durable(true).build(); }
    @Bean public TopicExchange paymentExchange() { return ExchangeBuilder.topicExchange(RabbitMQConstants.PAYMENT_EXCHANGE).durable(true).build(); }
    @Bean public Queue paymentOrderQueue() { return QueueBuilder.durable(RabbitMQConstants.PAYMENT_ORDER_QUEUE).build(); }
    @Bean public Binding paymentOrderBinding() { return BindingBuilder.bind(paymentOrderQueue()).to(orderExchange()).with(RabbitMQConstants.ORDER_CREATED_KEY); }
    @Bean public Jackson2JsonMessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }
    @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory cf) { RabbitTemplate t = new RabbitTemplate(cf); t.setMessageConverter(messageConverter()); return t; }
}
