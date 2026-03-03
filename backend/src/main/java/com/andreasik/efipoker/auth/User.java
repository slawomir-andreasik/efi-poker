package com.andreasik.efipoker.auth;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record User(
    UUID id,
    String username,
    String email,
    String passwordHash,
    String authProvider,
    String authProviderId,
    String role,
    Instant createdAt,
    Instant lastLoginAt) {}
