package com.ecommerce.payment.controller;

import com.ecommerce.payment.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Stripe Webhook controller — listens for payment events.
 * This endpoint is NOT authenticated (Stripe sends webhooks directly).
 * Signature verification ensures authenticity.
 */
@Slf4j @RestController @RequestMapping("/api/payments/webhook") @RequiredArgsConstructor
public class StripeWebhookController {

    private final PaymentService paymentService;
    @Value("${stripe.webhook-secret}") private String webhookSecret;

    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                 @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
                if (intent != null) {
                    paymentService.handlePaymentSucceeded(intent.getId());
                }
            }
            case "payment_intent.payment_failed" -> {
                PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
                if (intent != null) {
                    String reason = intent.getLastPaymentError() != null ? intent.getLastPaymentError().getMessage() : "Unknown";
                    paymentService.handlePaymentFailed(intent.getId(), reason);
                }
            }
            default -> log.info("Unhandled Stripe event type: {}", event.getType());
        }

        return ResponseEntity.ok("Received");
    }
}
