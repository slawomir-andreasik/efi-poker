package com.andreasik.efipoker.estimation.estimate;

import com.andreasik.efipoker.estimation.task.Task;
import com.andreasik.efipoker.participant.Participant;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record Estimate(
    UUID id,
    Task task,
    Participant participant,
    String storyPoints,
    String comment,
    Instant createdAt,
    Instant updatedAt) {}
