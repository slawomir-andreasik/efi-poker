package com.andreasik.efipoker.shared.event;

import java.util.UUID;

public record RoomCreatedEvent(UUID roomId, String roomType) {}
