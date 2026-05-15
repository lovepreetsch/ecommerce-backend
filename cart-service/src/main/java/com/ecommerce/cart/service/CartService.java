package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.*;
import com.ecommerce.cart.entity.*;
import com.ecommerce.cart.repository.*;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String CART_CACHE_PREFIX = "cart:user:";

    public CartDTO getCart(Long userId) {
        Cart cart = getOrCreateActiveCart(userId);
        return mapToDTO(cart);
    }

    @Transactional
    public CartDTO addItem(Long userId, CartItemDTO itemDTO) {
        Cart cart = getOrCreateActiveCart(userId);

        // Check if product already in cart
        var existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), itemDTO.getProductId());
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + itemDTO.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .productId(itemDTO.getProductId())
                    .productName(itemDTO.getProductName())
                    .price(itemDTO.getPrice())
                    .quantity(itemDTO.getQuantity())
                    .imageUrl(itemDTO.getImageUrl())
                    .build();
            cart.getItems().add(item);
            cartRepository.save(cart);
        }

        log.info("Item added to cart for user: {}, product: {}", userId, itemDTO.getProductId());
        evictCartCache(userId);
        return mapToDTO(cartRepository.findById(cart.getId()).orElse(cart));
    }

    @Transactional
    public CartDTO updateItemQuantity(Long userId, Long itemId, int quantity) {
        Cart cart = getActiveCart(userId);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", itemId));

        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(quantity);
        }
        cartRepository.save(cart);
        evictCartCache(userId);
        return mapToDTO(cart);
    }

    @Transactional
    public CartDTO removeItem(Long userId, Long itemId) {
        Cart cart = getActiveCart(userId);
        cart.getItems().removeIf(i -> i.getId().equals(itemId));
        cartRepository.save(cart);
        evictCartCache(userId);
        return mapToDTO(cart);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getActiveCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
        evictCartCache(userId);
        log.info("Cart cleared for user: {}", userId);
    }

    @Transactional
    public void markCartAsCheckedOut(Long userId) {
        Cart cart = getActiveCart(userId);
        cart.setStatus(Cart.CartStatus.CHECKED_OUT);
        cartRepository.save(cart);
        evictCartCache(userId);
    }

    // ===== Helpers =====

    private Cart getActiveCart(Long userId) {
        return cartRepository.findByUserIdAndStatus(userId, Cart.CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
    }

    private Cart getOrCreateActiveCart(Long userId) {
        return cartRepository.findByUserIdAndStatus(userId, Cart.CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().userId(userId).status(Cart.CartStatus.ACTIVE).build()));
    }

    private void evictCartCache(Long userId) {
        redisTemplate.delete(CART_CACHE_PREFIX + userId);
    }

    private CartDTO mapToDTO(Cart cart) {
        var items = cart.getItems().stream().map(i ->
                CartItemDTO.builder()
                        .id(i.getId()).productId(i.getProductId())
                        .productName(i.getProductName()).price(i.getPrice())
                        .quantity(i.getQuantity()).imageUrl(i.getImageUrl())
                        .build()
        ).collect(Collectors.toList());

        BigDecimal total = cart.getItems().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartDTO.builder()
                .id(cart.getId()).userId(cart.getUserId())
                .items(items).totalAmount(total)
                .totalItems(cart.getItems().stream().mapToInt(CartItem::getQuantity).sum())
                .build();
    }
}
