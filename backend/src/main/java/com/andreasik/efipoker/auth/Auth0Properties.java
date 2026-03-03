package com.andreasik.efipoker.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.auth0")
public record Auth0Properties(
    boolean enabled,
    @DefaultValue("disabled") String domain,
    @DefaultValue("disabled") String clientId,
    @DefaultValue("disabled") String clientSecret) {}
