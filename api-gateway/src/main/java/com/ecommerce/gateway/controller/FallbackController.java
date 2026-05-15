package com.ecommerce.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Fallback controller for circuit breaker.
 * Returns graceful error messages when downstream services are unavailable.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authFallback() {
        return buildFallbackResponse("Auth Service is currently unavailable. Please try again later.");
    }

    @GetMapping("/user")
    public Mono<ResponseEntity<Map<String, Object>>> userFallback() {
        return buildFallbackResponse("User Service is currently unavailable. Please try again later.");
    }

    @GetMapping("/product")
    public Mono<ResponseEntity<Map<String, Object>>> productFallback() {
        return buildFallbackResponse("Product Service is currently unavailable. Please try again later.");
    }

    @GetMapping("/cart")
    public Mono<ResponseEntity<Map<String, Object>>> cartFallback() {
        return buildFallbackResponse("Cart Service is currently unavailable. Please try again later.");
    }

    @GetMapping("/order")
    public Mono<ResponseEntity<Map<String, Object>>> orderFallback() {
        return buildFallbackResponse("Order Service is currently unavailable. Please try again later.");
    }

    @GetMapping("/payment")
    public Mono<ResponseEntity<Map<String, Object>>> paymentFallback() {
        return buildFallbackResponse("Payment Service is currently unavailable. Please try again later.");
    }

    @GetMapping("/inventory")
    public Mono<ResponseEntity<Map<String, Object>>> inventoryFallback() {
        return buildFallbackResponse("Inventory Service is currently unavailable. Please try again later.");
    }

    @GetMapping("/notification")
    public Mono<ResponseEntity<Map<String, Object>>> notificationFallback() {
        return buildFallbackResponse("Notification Service is currently unavailable. Please try again later.");
    }

    private Mono<ResponseEntity<Map<String, Object>>> buildFallbackResponse(String message) {
        Map<String, Object> response = Map.of(
                "success", false,
                "message", message,
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "timestamp", LocalDateTime.now().toString()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}
