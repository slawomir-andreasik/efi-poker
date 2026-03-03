package com.andreasik.efipoker.project;

import com.andreasik.efipoker.auth.User;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record Project(
    UUID id, String name, String slug, String adminCode, User createdBy, Instant createdAt) {}
