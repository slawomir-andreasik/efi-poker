package com.andreasik.efipoker.shared.event;

import java.util.UUID;

public record EstimateSubmittedEvent(UUID taskId, UUID participantId) {}
