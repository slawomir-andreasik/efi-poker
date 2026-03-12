package com.andreasik.efipoker.estimation.room;

import com.andreasik.efipoker.project.Project;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record Room(
    UUID id,
    Project project,
    String slug,
    String title,
    String description,
    String roomType,
    Instant deadline,
    String status,
    String topic,
    int roundNumber,
    boolean autoRevealOnDeadline,
    Instant createdAt) {}
