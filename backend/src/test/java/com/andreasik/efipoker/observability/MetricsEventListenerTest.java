package com.andreasik.efipoker.observability;

import static org.mockito.BDDMockito.then;

import com.andreasik.efipoker.shared.event.EstimateSubmittedEvent;
import com.andreasik.efipoker.shared.event.ProjectCreatedEvent;
import com.andreasik.efipoker.shared.event.RoomCreatedEvent;
import com.andreasik.efipoker.shared.event.RoundStartedEvent;
import com.andreasik.efipoker.shared.observability.BusinessMetrics;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("MetricsEventListener")
class MetricsEventListenerTest extends BaseUnitTest {

  @Mock private BusinessMetrics businessMetrics;
  @InjectMocks private MetricsEventListener listener;

  @Test
  void should_record_estimate_submitted() {
    // Act
    listener.on(new EstimateSubmittedEvent(UUID.randomUUID(), UUID.randomUUID()));

    // Assert
    then(businessMetrics).should().recordEstimateSubmitted();
  }

  @Test
  void should_record_room_created() {
    // Act
    listener.on(new RoomCreatedEvent(UUID.randomUUID(), "LIVE"));

    // Assert
    then(businessMetrics).should().recordRoomCreated();
  }

  @Test
  void should_record_project_created() {
    // Act
    listener.on(new ProjectCreatedEvent(UUID.randomUUID()));

    // Assert
    then(businessMetrics).should().recordProjectCreated();
  }

  @Test
  void should_record_round_started() {
    // Act
    listener.on(new RoundStartedEvent(UUID.randomUUID(), 3));

    // Assert
    then(businessMetrics).should().recordRoundStarted();
  }
}
