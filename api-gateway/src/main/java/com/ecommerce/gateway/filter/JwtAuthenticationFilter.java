package com.ecommerce.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Global JWT authentication filter for the API Gateway.
 * Validates JWT tokens on incoming requests and forwards user info
 * as headers (X-User-Id, X-User-Email, X-User-Role) to downstream services.
 *
 * Public endpoints (auth, swagger, product browsing) bypass this filter.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String TOKEN_BLACKLIST_PREFIX = "blacklist:token:";

    private final SecretKey secretKey;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Paths that do NOT require authentication.
     */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/**",
            "/api/products",
            "/api/products/search",
            "/api/products/slug/**",
            "/api/products/category/**",
            "/api/products/vendor/**",
            "/api/products/{id}",
            "/api/categories/**",
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            // Service-specific swagger docs
            "/auth-service/v3/api-docs/**",
            "/user-service/v3/api-docs/**",
            "/product-service/v3/api-docs/**",
            "/cart-service/v3/api-docs/**",
            "/order-service/v3/api-docs/**",
            "/payment-service/v3/api-docs/**",
            "/inventory-service/v3/api-docs/**",
            "/notification-service/v3/api-docs/**"
    );

    public JwtAuthenticationFilter(
            @Value("${jwt.secret}") String jwtSecret,
            ReactiveRedisTemplate<String, String> redisTemplate) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip authentication for public endpoints
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Extract Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        try {
            // Parse and validate the JWT
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String jti = claims.getId();
            String userId = claims.getSubject();
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);

            // Check Redis blacklist
            return redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + jti)
                    .flatMap(isBlacklisted -> {
                        if (Boolean.TRUE.equals(isBlacklisted)) {
                            log.warn("Blacklisted token used: jti={}", jti);
                            return onError(exchange, "Token has been revoked", HttpStatus.UNAUTHORIZED);
                        }

                        // Forward user info to downstream services via headers
                        ServerHttpRequest modifiedRequest = request.mutate()
                                .header("X-User-Id", userId)
                                .header("X-User-Email", email)
                                .header("X-User-Role", role)
                                .build();

                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    });

        } catch (Exception ex) {
            log.error("JWT validation failed: {}", ex.getMessage());
            return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public int getOrder() {
        return -1; // Run before other filters
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        String body = String.format(
                "{\"success\":false,\"message\":\"%s\",\"status\":%d,\"timestamp\":\"%s\"}",
                message, status.value(), java.time.LocalDateTime.now()
        );
        org.springframework.core.io.buffer.DataBuffer buffer =
                response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }
}
