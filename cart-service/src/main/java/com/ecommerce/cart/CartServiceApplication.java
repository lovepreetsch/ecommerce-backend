package com.ecommerce.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.ecommerce.cart","com.ecommerce.common.exception","com.ecommerce.common.security"})
public class CartServiceApplication {
    public static void main(String[] args) { SpringApplication.run(CartServiceApplication.class, args); }
}
