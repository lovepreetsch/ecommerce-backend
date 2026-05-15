package com.ecommerce.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_user", columnList = "userId"),
    @Index(name = "idx_notif_read", columnList = "isRead")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false) private String recipientEmail;
    @Column(nullable = false) private String type;
    @Column(nullable = false) private String channel; // EMAIL, IN_APP, SMS
    @Column(nullable = false) private String subject;
    @Column(length = 5000) private String body;
    @Builder.Default private boolean isRead = false;
    @Builder.Default private boolean sent = false;
    private String failureReason;
    @CreationTimestamp @Column(updatable = false) private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}
