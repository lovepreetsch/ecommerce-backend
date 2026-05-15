package com.ecommerce.payment.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.security.UserPrincipal;
import com.ecommerce.payment.dto.PaymentDTO;
import com.ecommerce.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController @RequestMapping("/api/payments") @RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing with Stripe")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Create a payment intent for an order")
    public ResponseEntity<ApiResponse<PaymentDTO>> createPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long orderId,
            @RequestParam String orderNumber,
            @RequestParam BigDecimal amount) {
        PaymentDTO payment = paymentService.createPaymentIntent(orderId, orderNumber, principal.getId(), amount);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Payment intent created", payment));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment details by order ID")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPaymentByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentByOrderId(orderId)));
    }
}
