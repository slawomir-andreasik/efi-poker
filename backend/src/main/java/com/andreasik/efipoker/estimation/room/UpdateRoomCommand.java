package com.andreasik.efipoker.estimation.room;

import java.time.Instant;
import java.util.UUID;

public record UpdateRoomCommand(
    UUID id,
    String title,
    String description,
    Instant deadline,
    String topic,
    String commentTemplate,
    Boolean commentRequired,
    Boolean autoRevealOnDeadline) {}
