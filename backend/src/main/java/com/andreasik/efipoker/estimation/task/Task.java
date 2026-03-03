package com.andreasik.efipoker.estimation.task;

import com.andreasik.efipoker.estimation.room.Room;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record Task(
    UUID id,
    Room room,
    String title,
    String description,
    int sortOrder,
    String finalEstimate,
    boolean revealed,
    boolean active,
    Instant createdAt) {}
