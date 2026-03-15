package com.andreasik.efipoker.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest extends BaseUnitTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Nested
  @DisplayName("traceId in ProblemDetail")
  class TraceIdInProblemDetail {

    @Test
    void should_include_trace_id_when_mdc_has_trace_id() {
      // Arrange
      MDC.put("traceId", "abc123def456");

      try {
        // Act
        ProblemDetail problem =
            handler.handleResourceNotFound(new ResourceNotFoundException("Project", "test-slug"));

        // Assert
        assertThat(problem.getProperties()).containsEntry("traceId", "abc123def456");
      } finally {
        MDC.clear();
      }
    }

    @Test
    void should_not_include_trace_id_when_mdc_is_empty() {
      // Arrange
      MDC.clear();

      // Act
      ProblemDetail problem =
          handler.handleResourceNotFound(new ResourceNotFoundException("Project", "test-slug"));

      // Assert - getProperties() returns null when no properties set
      assertThat(problem.getProperties()).isNull();
    }

    @Test
    void should_not_include_trace_id_when_mdc_value_is_blank() {
      // Arrange
      MDC.put("traceId", "  ");

      try {
        // Act
        ProblemDetail problem =
            handler.handleResourceNotFound(new ResourceNotFoundException("Project", "test-slug"));

        // Assert - blank traceId is skipped, no properties set
        assertThat(problem.getProperties()).isNull();
      } finally {
        MDC.clear();
      }
    }

    @Test
    void should_include_trace_id_on_500_errors() {
      // Arrange
      MDC.put("traceId", "trace-for-500");

      try {
        // Act
        ProblemDetail problem = handler.handleGenericException(new RuntimeException("unexpected"));

        // Assert
        assertThat(problem.getProperties()).containsEntry("traceId", "trace-for-500");
      } finally {
        MDC.clear();
      }
    }
  }

  @Nested
  @DisplayName("title and type in ProblemDetail")
  class TitleAndTypeInProblemDetail {

    @Test
    void should_set_title_and_type_for_resource_not_found() {
      ProblemDetail problem =
          handler.handleResourceNotFound(new ResourceNotFoundException("Project", "test-slug"));

      assertThat(problem.getTitle()).isEqualTo("Resource Not Found");
      assertThat(problem.getType()).isEqualTo(URI.create("https://efipoker.com/errors/not-found"));
    }

    @Test
    void should_set_title_and_type_for_unauthorized() {
      ProblemDetail problem =
          handler.handleUnauthorized(new UnauthorizedException("Access denied"));

      assertThat(problem.getTitle()).isEqualTo("Unauthorized");
      assertThat(problem.getType())
          .isEqualTo(URI.create("https://efipoker.com/errors/unauthorized"));
    }

    @Test
    void should_set_title_and_type_for_generic_exception() {
      ProblemDetail problem = handler.handleGenericException(new RuntimeException("boom"));

      assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
      assertThat(problem.getType()).isEqualTo(URI.create("https://efipoker.com/errors/internal"));
    }
  }
}
