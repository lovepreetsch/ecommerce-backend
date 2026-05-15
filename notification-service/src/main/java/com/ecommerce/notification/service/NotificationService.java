package com.ecommerce.notification.service;

import com.ecommerce.common.event.NotificationEvent;
import com.ecommerce.common.event.OrderCreatedEvent;
import com.ecommerce.common.event.PaymentCompletedEvent;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.notification.dto.NotificationDTO;
import com.ecommerce.notification.entity.Notification;
import com.ecommerce.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j @Service @RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Async
    @Transactional
    public void processNotificationEvent(NotificationEvent event) {
        log.info("Processing notification for user: {} type: {}", event.getUserId(), event.getType());

        // Build email body from template variables
        String body = buildEmailBody(event);

        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .recipientEmail(event.getRecipientEmail())
                .type(event.getType())
                .channel(event.getChannel() != null ? event.getChannel() : "EMAIL")
                .subject(event.getSubject())
                .body(body)
                .build();

        notification = notificationRepository.save(notification);

        // Send email if channel is EMAIL
        if ("EMAIL".equals(notification.getChannel())) {
            try {
                emailService.sendEmail(notification.getRecipientEmail(), notification.getSubject(), body);
                notification.setSent(true);
                notification.setSentAt(LocalDateTime.now());
            } catch (Exception e) {
                notification.setFailureReason(e.getMessage());
                log.error("Failed to send email to {}: {}", notification.getRecipientEmail(), e.getMessage());
            }
            notificationRepository.save(notification);
        }
    }

    @Async
    @Transactional
    public void processOrderCreated(OrderCreatedEvent event) {
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .recipientEmail(event.getUserEmail())
                .type("ORDER_CREATED")
                .channel("EMAIL")
                .subject("Order Confirmation - " + event.getOrderNumber())
                .body("Your order " + event.getOrderNumber() + " has been placed successfully! Total: $" + event.getTotalAmount())
                .build();
        notification = notificationRepository.save(notification);

        try {
            emailService.sendEmail(notification.getRecipientEmail(), notification.getSubject(), notification.getBody());
            notification.setSent(true);
            notification.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            notification.setFailureReason(e.getMessage());
            log.error("Failed to send order confirmation email: {}", e.getMessage());
        }
        notificationRepository.save(notification);
    }

    @Async
    @Transactional
    public void processPaymentCompleted(PaymentCompletedEvent event) {
        String status = "SUCCEEDED".equals(event.getStatus()) ? "successful" : "failed";
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .recipientEmail("")
                .type("PAYMENT_" + event.getStatus())
                .channel("IN_APP")
                .subject("Payment " + status + " - " + event.getOrderNumber())
                .body("Payment of $" + event.getAmount() + " for order " + event.getOrderNumber() + " was " + status + ".")
                .build();
        notificationRepository.save(notification);
    }

    public Page<NotificationDTO> getMyNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::mapToDTO);
    }

    public Page<NotificationDTO> getUnreadNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable).map(this::mapToDTO);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        if (n.getUserId().equals(userId)) {
            n.setRead(true);
            n.setReadAt(LocalDateTime.now());
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    private String buildEmailBody(NotificationEvent event) {
        if (event.getTemplateVariables() != null && !event.getTemplateVariables().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            event.getTemplateVariables().forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
            return sb.toString();
        }
        return event.getSubject();
    }

    private NotificationDTO mapToDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId()).userId(n.getUserId()).type(n.getType())
                .channel(n.getChannel()).subject(n.getSubject()).body(n.getBody())
                .isRead(n.isRead()).sent(n.isSent())
                .createdAt(n.getCreatedAt() != null ? n.getCreatedAt().toString() : null)
                .readAt(n.getReadAt() != null ? n.getReadAt().toString() : null)
                .build();
    }
}
