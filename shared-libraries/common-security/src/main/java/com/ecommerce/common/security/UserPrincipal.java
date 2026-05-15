package com.ecommerce.common.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Custom UserDetails implementation used by Spring Security
 * to represent the currently authenticated user.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    public UserPrincipal(Long id, String email, String password, String role, boolean enabled) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.authorities = List.of(new SimpleGrantedAuthority(role));
        this.enabled = enabled;
    }

    public static UserPrincipal create(Long id, String email, String password, String role, boolean enabled) {
        return new UserPrincipal(id, email, password, role, enabled);
    }

    /**
     * Create a UserPrincipal from gateway-forwarded headers (no password needed).
     */
    public static UserPrincipal fromHeaders(Long userId, String email, String role) {
        return new UserPrincipal(userId, email, null, role, true);
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
