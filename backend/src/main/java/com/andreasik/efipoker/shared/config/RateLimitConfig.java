package com.andreasik.efipoker.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitConfig(int maxRequests, int readMaxRequests, int windowSeconds) {}
