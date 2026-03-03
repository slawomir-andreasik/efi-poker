package com.andreasik.efipoker.shared.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {

  private final Counter estimatesSubmitted;
  private final Counter roomsCreated;
  private final Counter projectsCreated;
  private final Counter roundsStarted;
  private final AtomicLong activeRooms = new AtomicLong(0);
  private final AtomicLong activeParticipants = new AtomicLong(0);
  private final Timer roundDuration;

  public BusinessMetrics(MeterRegistry registry) {
    this.estimatesSubmitted =
        Counter.builder("efi.estimates.submitted")
            .description("Total estimates submitted")
            .register(registry);
    this.roomsCreated =
        Counter.builder("efi.rooms.created").description("Total rooms created").register(registry);
    this.projectsCreated =
        Counter.builder("efi.projects.created")
            .description("Total projects created")
            .register(registry);
    this.roundsStarted =
        Counter.builder("efi.rounds.started")
            .description("Total live rounds started")
            .register(registry);
    this.roundDuration =
        Timer.builder("efi.estimation.round.duration")
            .description("Duration of estimation rounds")
            .register(registry);

    registry.gauge("efi.rooms.active", activeRooms);
    registry.gauge("efi.participants.active", activeParticipants);
  }

  public void recordEstimateSubmitted() {
    estimatesSubmitted.increment();
  }

  public void recordRoomCreated() {
    roomsCreated.increment();
  }

  public void recordProjectCreated() {
    projectsCreated.increment();
  }

  public void recordRoundStarted() {
    roundsStarted.increment();
  }

  public void updateActiveRooms(long count) {
    activeRooms.set(count);
  }

  public void updateActiveParticipants(long count) {
    activeParticipants.set(count);
  }

  public Timer getRoundDuration() {
    return roundDuration;
  }
}
