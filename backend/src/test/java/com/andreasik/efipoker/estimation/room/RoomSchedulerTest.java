package com.andreasik.efipoker.estimation.room;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("RoomScheduler")
class RoomSchedulerTest extends BaseUnitTest {

  @Mock private RoomService roomService;
  @InjectMocks private RoomScheduler roomScheduler;

  @Test
  void should_call_close_expired_rooms() {
    // Arrange
    given(roomService.closeExpiredRooms()).willReturn(List.of());

    // Act
    roomScheduler.closeExpiredRooms();

    // Assert
    then(roomService).should().closeExpiredRooms();
  }
}
