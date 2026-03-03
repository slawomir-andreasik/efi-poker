package com.andreasik.efipoker.observability;

import com.andreasik.efipoker.estimation.room.RoomService;
import com.andreasik.efipoker.participant.ParticipantService;
import com.andreasik.efipoker.shared.observability.BusinessMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActiveMetricsScheduler {

  private final BusinessMetrics metrics;
  private final RoomService roomService;
  private final ParticipantService participantService;

  @Scheduled(fixedRate = 30_000)
  public void updateGauges() {
    try {
      metrics.updateActiveRooms(roomService.countOpenRooms());
      metrics.updateActiveParticipants(participantService.countAll());
    } catch (Exception e) {
      log.warn("Failed to update active metrics gauges: {}", e.getMessage());
    }
  }
}
