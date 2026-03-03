package com.andreasik.efipoker.observability;

import com.andreasik.efipoker.shared.event.EstimateSubmittedEvent;
import com.andreasik.efipoker.shared.event.ProjectCreatedEvent;
import com.andreasik.efipoker.shared.event.RoomCreatedEvent;
import com.andreasik.efipoker.shared.event.RoundStartedEvent;
import com.andreasik.efipoker.shared.observability.BusinessMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsEventListener {

  private final BusinessMetrics businessMetrics;

  @EventListener
  public void on(EstimateSubmittedEvent event) {
    log.debug("Metrics: estimate submitted for taskId={}", event.taskId());
    businessMetrics.recordEstimateSubmitted();
  }

  @EventListener
  public void on(RoomCreatedEvent event) {
    log.debug("Metrics: room created id={}, type={}", event.roomId(), event.roomType());
    businessMetrics.recordRoomCreated();
  }

  @EventListener
  public void on(ProjectCreatedEvent event) {
    log.debug("Metrics: project created id={}", event.projectId());
    businessMetrics.recordProjectCreated();
  }

  @EventListener
  public void on(RoundStartedEvent event) {
    log.debug("Metrics: round started roomId={}, round={}", event.roomId(), event.roundNumber());
    businessMetrics.recordRoundStarted();
  }
}
