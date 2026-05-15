package com.ecommerce.payment.service;

import com.ecommerce.common.event.*;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.payment.dto.PaymentDTO;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j @Service @RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${stripe.secret-key}") private String stripeSecretKey;
    @Value("${stripe.currency:usd}") private String defaultCurrency;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    /**
     * Create Stripe PaymentIntent when an order is created via event.
     */
    @Transactional
    public PaymentDTO createPaymentIntent(Long orderId, String orderNumber, Long userId, BigDecimal amount) {
        // Check for existing payment
        if (paymentRepository.findByOrderId(orderId).isPresent()) {
            throw new BadRequestException("Payment already exists for order: " + orderNumber);
        }

        try {
            // Convert to cents for Stripe
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(defaultCurrency)
                    .putMetadata("orderId", orderId.toString())
                    .putMetadata("orderNumber", orderNumber)
                    .putMetadata("userId", userId.toString())
                    .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            Payment payment = Payment.builder()
                    .orderId(orderId).orderNumber(orderNumber).userId(userId)
                    .amount(amount).currency(defaultCurrency)
                    .status(Payment.PaymentStatus.PENDING)
                    .stripePaymentIntentId(intent.getId())
                    .stripeClientSecret(intent.getClientSecret())
                    .build();
            payment = paymentRepository.save(payment);

            log.info("PaymentIntent created: {} for order: {}", intent.getId(), orderNumber);
            return mapToDTO(payment);

        } catch (StripeException e) {
            log.error("Stripe error creating PaymentIntent: {}", e.getMessage());
            throw new BadRequestException("Payment processing failed: " + e.getMessage());
        }
    }

    /**
     * Handle Stripe webhook: payment_intent.succeeded
     */
    @Transactional
    public void handlePaymentSucceeded(String paymentIntentId) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "stripePaymentIntentId", paymentIntentId));

        payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
        paymentRepository.save(payment);

        publishPaymentCompletedEvent(payment, "SUCCEEDED");
        log.info("Payment succeeded for order: {}", payment.getOrderNumber());
    }

    /**
     * Handle Stripe webhook: payment_intent.payment_failed
     */
    @Transactional
    public void handlePaymentFailed(String paymentIntentId, String failureReason) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "stripePaymentIntentId", paymentIntentId));

        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setFailureReason(failureReason);
        paymentRepository.save(payment);

        publishPaymentCompletedEvent(payment, "FAILED");
        log.info("Payment failed for order: {} - reason: {}", payment.getOrderNumber(), failureReason);
    }

    public PaymentDTO getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));
        return mapToDTO(payment);
    }

    /**
     * Handle OrderCreatedEvent — auto-create PaymentIntent.
     */
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Creating payment for order: {}", event.getOrderNumber());
        createPaymentIntent(event.getOrderId(), event.getOrderNumber(), event.getUserId(), event.getTotalAmount());
    }

    private void publishPaymentCompletedEvent(Payment payment, String status) {
        try {
            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .paymentId(payment.getId()).orderId(payment.getOrderId())
                    .orderNumber(payment.getOrderNumber()).userId(payment.getUserId())
                    .amount(payment.getAmount()).currency(payment.getCurrency())
                    .status(status).stripePaymentIntentId(payment.getStripePaymentIntentId())
                    .completedAt(LocalDateTime.now()).build();

            rabbitTemplate.convertAndSend(RabbitMQConstants.PAYMENT_EXCHANGE, RabbitMQConstants.PAYMENT_COMPLETED_KEY, event);
            log.info("Published PaymentCompletedEvent for order: {}", payment.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to publish PaymentCompletedEvent: {}", e.getMessage());
        }
    }

    private PaymentDTO mapToDTO(Payment p) {
        return PaymentDTO.builder()
                .id(p.getId()).orderId(p.getOrderId()).orderNumber(p.getOrderNumber())
                .amount(p.getAmount()).currency(p.getCurrency()).status(p.getStatus().name())
                .stripePaymentIntentId(p.getStripePaymentIntentId())
                .stripeClientSecret(p.getStripeClientSecret())
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null)
                .build();
    }
}
