package com.andreasik.efipoker.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.andreasik.efipoker.shared.test.BaseUnitTest;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BusinessMetrics")
class BusinessMetricsTest extends BaseUnitTest {

  private MeterRegistry registry;
  private BusinessMetrics metrics;

  @BeforeEach
  void setUp() {
    registry = new SimpleMeterRegistry();
    metrics = new BusinessMetrics(registry);
  }

  @Nested
  @DisplayName("Counters")
  class Counters {

    @Test
    void should_register_and_increment_estimates_counter() {
      // Act
      metrics.recordEstimateSubmitted();
      metrics.recordEstimateSubmitted();

      // Assert
      assertThat(registry.counter("efi.estimates.submitted").count()).isEqualTo(2.0);
    }

    @Test
    void should_register_and_increment_rooms_counter() {
      // Act
      metrics.recordRoomCreated();

      // Assert
      assertThat(registry.counter("efi.rooms.created").count()).isEqualTo(1.0);
    }

    @Test
    void should_register_and_increment_projects_counter() {
      // Act
      metrics.recordProjectCreated();

      // Assert
      assertThat(registry.counter("efi.projects.created").count()).isEqualTo(1.0);
    }

    @Test
    void should_register_and_increment_rounds_counter() {
      // Act
      metrics.recordRoundStarted();

      // Assert
      assertThat(registry.counter("efi.rounds.started").count()).isEqualTo(1.0);
    }
  }

  @Nested
  @DisplayName("Gauges")
  class Gauges {

    @Test
    void should_update_active_rooms_gauge() {
      // Act
      metrics.updateActiveRooms(5);

      // Assert
      assertThat(registry.get("efi.rooms.active").gauge().value()).isEqualTo(5.0);
    }

    @Test
    void should_update_active_participants_gauge() {
      // Act
      metrics.updateActiveParticipants(12);

      // Assert
      assertThat(registry.get("efi.participants.active").gauge().value()).isEqualTo(12.0);
    }
  }

  @Nested
  @DisplayName("Timer")
  class TimerTests {

    @Test
    void should_register_round_duration_timer() {
      // Assert
      assertThat(metrics.getRoundDuration()).isNotNull();
      assertThat(registry.get("efi.estimation.round.duration").timer()).isNotNull();
    }
  }
}
