package com.ecommerce.common.security;

/**
 * Security constants shared across all microservices.
 */
public final class SecurityConstants {

    private SecurityConstants() {
        // Prevent instantiation
    }

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_EMAIL_HEADER = "X-User-Email";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    // Roles
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_VENDOR = "ROLE_VENDOR";
    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";

    // Redis key prefixes
    public static final String TOKEN_BLACKLIST_PREFIX = "blacklist:token:";
    public static final String OTP_PREFIX = "otp:";
    public static final String SESSION_PREFIX = "session:";
    public static final String RATE_LIMIT_PREFIX = "rate:limit:";

    // Public endpoints that bypass authentication
    public static final String[] PUBLIC_URLS = {
            "/api/auth/**",
            "/api/products",
            "/api/products/**",
            "/api/categories/**",
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };
}
