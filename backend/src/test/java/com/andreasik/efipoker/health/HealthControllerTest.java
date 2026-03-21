package com.andreasik.efipoker.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.given;

import com.andreasik.efipoker.api.model.HealthResponse;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("HealthController")
class HealthControllerTest extends BaseUnitTest {

  @Mock private BuildProperties buildProperties;
  @Mock private DataSource dataSource;
  @Mock private Connection connection;

  @Nested
  @DisplayName("Database healthy")
  class DatabaseHealthy {

    @Test
    void should_return_up_status_and_version() throws SQLException {
      given(buildProperties.getVersion()).willReturn("1.2.3");
      given(dataSource.getConnection()).willReturn(connection);
      given(connection.isValid(2)).willReturn(true);
      HealthController controller = new HealthController(buildProperties, dataSource);

      ResponseEntity<HealthResponse> response = controller.healthCheck();

      assertAll(
          () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(response.getBody()).isNotNull(),
          () -> assertThat(response.getBody().getStatus()).isEqualTo("UP"),
          () -> assertThat(response.getBody().getVersion()).isEqualTo("1.2.3"));
    }
  }

  @Nested
  @DisplayName("Database unhealthy")
  class DatabaseUnhealthy {

    @Test
    void should_return_down_when_connection_invalid() throws SQLException {
      given(buildProperties.getVersion()).willReturn("1.2.3");
      given(dataSource.getConnection()).willReturn(connection);
      given(connection.isValid(2)).willReturn(false);
      HealthController controller = new HealthController(buildProperties, dataSource);

      ResponseEntity<HealthResponse> response = controller.healthCheck();

      assertAll(
          () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE),
          () -> assertThat(response.getBody()).isNotNull(),
          () -> assertThat(response.getBody().getStatus()).isEqualTo("DOWN"));
    }

    @Test
    void should_return_down_when_connection_throws() throws SQLException {
      given(buildProperties.getVersion()).willReturn("1.2.3");
      given(dataSource.getConnection()).willThrow(new SQLException("Connection refused"));
      HealthController controller = new HealthController(buildProperties, dataSource);

      ResponseEntity<HealthResponse> response = controller.healthCheck();

      assertAll(
          () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE),
          () -> assertThat(response.getBody()).isNotNull(),
          () -> assertThat(response.getBody().getStatus()).isEqualTo("DOWN"));
    }
  }
}
