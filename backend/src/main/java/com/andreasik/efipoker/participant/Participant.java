package com.andreasik.efipoker.participant;

import com.andreasik.efipoker.project.Project;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record Participant(
    UUID id,
    Project project,
    String nickname,
    UUID userId,
    Set<UUID> invitedRoomIds,
    boolean archived,
    Instant archivedAt,
    Instant createdAt) {}
