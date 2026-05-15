package com.ecommerce.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Lightweight user summary shared between services.
 * Used when a service needs basic user info without calling User Service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDTO implements Serializable {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
}
