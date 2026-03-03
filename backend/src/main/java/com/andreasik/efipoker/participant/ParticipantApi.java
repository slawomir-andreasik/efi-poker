package com.andreasik.efipoker.participant;

import java.util.List;
import java.util.UUID;

/**
 * Module API for participant operations. Used by other modules to avoid direct repository access.
 */
public interface ParticipantApi {

  void validateParticipantExists(UUID participantId);

  List<UUID> listParticipatedProjectIds(UUID userId);
}
