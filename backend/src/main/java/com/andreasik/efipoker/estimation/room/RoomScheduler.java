package com.andreasik.efipoker.estimation.room;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomScheduler {

  private final RoomService roomService;

  @Scheduled(fixedRate = 60_000)
  public void closeExpiredRooms() {
    List<Room> closed = roomService.closeExpiredRooms();
    if (!closed.isEmpty()) {
      log.info("Auto-closed {} expired room(s)", closed.size());
    }
  }
}
