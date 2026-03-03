package com.andreasik.efipoker.observability;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import com.andreasik.efipoker.estimation.room.RoomService;
import com.andreasik.efipoker.participant.ParticipantService;
import com.andreasik.efipoker.shared.observability.BusinessMetrics;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("ActiveMetricsScheduler")
class ActiveMetricsSchedulerTest extends BaseUnitTest {

  @Mock private BusinessMetrics metrics;
  @Mock private RoomService roomService;
  @Mock private ParticipantService participantService;
  @InjectMocks private ActiveMetricsScheduler scheduler;

  @Test
  void should_update_gauges_on_schedule() {
    // Arrange
    given(roomService.countOpenRooms()).willReturn(5L);
    given(participantService.countAll()).willReturn(42L);

    // Act
    scheduler.updateGauges();

    // Assert
    then(metrics).should().updateActiveRooms(5L);
    then(metrics).should().updateActiveParticipants(42L);
  }

  @Test
  void should_log_warning_when_service_throws() {
    // Arrange
    doThrow(new RuntimeException("DB connection refused")).when(roomService).countOpenRooms();

    // Act - should not throw
    scheduler.updateGauges();

    // Assert - metrics never updated
    then(metrics).shouldHaveNoInteractions();
  }
}
