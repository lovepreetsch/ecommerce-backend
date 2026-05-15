package com.ecommerce.notification.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private Long userId;
    private String type;
    private String channel;
    private String subject;
    private String body;
    private boolean isRead;
    private boolean sent;
    private String createdAt;
    private String readAt;
}
