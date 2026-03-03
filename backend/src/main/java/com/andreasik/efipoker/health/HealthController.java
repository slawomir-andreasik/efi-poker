package com.andreasik.efipoker.health;

import com.andreasik.efipoker.api.HealthApi;
import com.andreasik.efipoker.api.model.HealthResponse;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthController implements HealthApi {

  private final BuildProperties buildProperties;
  private final DataSource dataSource;

  @Override
  public ResponseEntity<HealthResponse> healthCheck() {
    log.debug("GET /health");
    HealthResponse response = new HealthResponse();
    response.setVersion(buildProperties.getVersion());

    if (isDatabaseHealthy()) {
      response.setStatus("UP");
      return ResponseEntity.ok(response);
    }

    response.setStatus("DOWN");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
  }

  private boolean isDatabaseHealthy() {
    try (var connection = dataSource.getConnection()) {
      return connection.isValid(2);
    } catch (Exception e) {
      log.error("Database health check failed: {}", e.getMessage());
      return false;
    }
  }
}
