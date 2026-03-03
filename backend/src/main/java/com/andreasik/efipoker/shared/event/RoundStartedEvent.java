package com.andreasik.efipoker.shared.event;

import java.util.UUID;

public record RoundStartedEvent(UUID roomId, int roundNumber) {}
