package com.andreasik.efipoker.estimation.room;

import java.time.Instant;
import java.util.UUID;

public record CreateRoomCommand(
    UUID projectId,
    String title,
    String description,
    RoomType roomType,
    Instant deadline,
    boolean autoRevealOnDeadline,
    String commentTemplate,
    boolean commentRequired) {}
