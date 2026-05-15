package com.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Generic notification event consumed by the Notification Service.
 * Supports email, SMS, and push notification channels.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {

    private Long userId;
    private String recipientEmail;
    private String recipientPhone;
    private String type; // ORDER_CONFIRMATION, PAYMENT_SUCCESS, WELCOME, LOW_STOCK, etc.
    private String channel; // EMAIL, SMS, PUSH
    private String subject;
    private String templateName;
    private Map<String, String> templateVariables;
    private LocalDateTime createdAt;
}
