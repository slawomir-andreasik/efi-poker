package com.andreasik.efipoker.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.registration")
public record RegistrationProperties(boolean enabled) {}
