package com.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event published when a new user registers.
 * Consumed by: User Service (create profile), Notification Service (welcome email).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent implements Serializable {

    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private LocalDateTime registeredAt;
}
