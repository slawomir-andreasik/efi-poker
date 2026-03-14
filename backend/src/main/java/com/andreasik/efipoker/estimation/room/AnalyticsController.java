package com.andreasik.efipoker.estimation.room;

import com.andreasik.efipoker.api.AnalyticsApi;
import com.andreasik.efipoker.api.model.ProjectAnalyticsResponse;
import com.andreasik.efipoker.api.model.RoomAnalyticsResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AnalyticsController implements AnalyticsApi {

  private final AnalyticsService analyticsService;

  @Override
  public ResponseEntity<RoomAnalyticsResponse> getRoomAnalytics(UUID roomId) {
    log.debug("Get room analytics: roomId={}", roomId);
    return ResponseEntity.ok(analyticsService.computeRoomAnalytics(roomId));
  }

  @Override
  public ResponseEntity<ProjectAnalyticsResponse> getProjectAnalytics(String slug) {
    log.debug("Get project analytics: slug={}", slug);
    return ResponseEntity.ok(analyticsService.computeProjectAnalytics(slug));
  }
}
