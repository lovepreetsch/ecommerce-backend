package com.ecommerce.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter for downstream microservices.
 * This filter reads user info from headers set by the API Gateway
 * (X-User-Id, X-User-Email, X-User-Role) and populates the
 * SecurityContext accordingly.
 *
 * For the Auth Service itself, it validates the JWT token directly.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final boolean isAuthService;

    /**
     * Constructor for downstream services (non-auth).
     * These services trust headers from the API Gateway.
     */
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this(jwtTokenProvider, false);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            if (isAuthService) {
                // Auth Service validates JWT directly
                authenticateFromToken(request);
            } else {
                // Other services trust gateway-forwarded headers
                authenticateFromHeaders(request);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Authenticate by validating the JWT token directly.
     * Used only by the Auth Service.
     */
    private void authenticateFromToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            String email = jwtTokenProvider.getEmailFromToken(token);
            String role = jwtTokenProvider.getRoleFromToken(token);

            UserPrincipal userPrincipal = UserPrincipal.fromHeaders(userId, email, role);
            setAuthentication(userPrincipal, request);
        }
    }

    /**
     * Authenticate from headers forwarded by the API Gateway.
     * The Gateway has already validated the JWT; services trust these headers.
     */
    private void authenticateFromHeaders(HttpServletRequest request) {
        String userIdStr = request.getHeader(SecurityConstants.USER_ID_HEADER);
        String email = request.getHeader(SecurityConstants.USER_EMAIL_HEADER);
        String role = request.getHeader(SecurityConstants.USER_ROLE_HEADER);

        if (StringUtils.hasText(userIdStr) && StringUtils.hasText(role)) {
            Long userId = Long.parseLong(userIdStr);
            UserPrincipal userPrincipal = UserPrincipal.fromHeaders(userId, email, role);
            setAuthentication(userPrincipal, request);
        }
    }

    private void setAuthentication(UserPrincipal userPrincipal, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userPrincipal,
                        null,
                        userPrincipal.getAuthorities()
                );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        return null;
    }
}
