package com.andreasik.efipoker.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;

import com.andreasik.efipoker.shared.test.BaseUnitTest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("TraceResponseFilter")
class TraceResponseFilterTest extends BaseUnitTest {

  private final TraceResponseFilter filter = new TraceResponseFilter();

  @Mock private FilterChain filterChain;

  @Nested
  @DisplayName("X-Trace-Id header")
  class TraceIdHeader {

    @Test
    void should_add_trace_id_header_when_mdc_has_trace_id() throws Exception {
      // Arrange
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();
      MDC.put(TraceResponseFilter.MDC_TRACE_ID, "abc123def456");

      try {
        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("abc123def456");
      } finally {
        MDC.clear();
      }
    }

    @Test
    void should_not_add_header_when_mdc_is_empty() throws Exception {
      // Arrange
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();
      MDC.clear();

      // Act
      filter.doFilterInternal(request, response, filterChain);

      // Assert
      assertThat(response.getHeader("X-Trace-Id")).isNull();
    }

    @Test
    void should_set_header_even_when_filter_chain_throws() throws Exception {
      // Arrange
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();
      MDC.put(TraceResponseFilter.MDC_TRACE_ID, "trace-before-throw");
      willThrow(new ServletException("downstream error")).given(filterChain).doFilter(any(), any());

      try {
        // Act + Assert
        assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
            .isInstanceOf(ServletException.class);
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("trace-before-throw");
      } finally {
        MDC.clear();
      }
    }

    @Test
    void should_not_add_header_when_trace_id_is_blank() throws Exception {
      // Arrange
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();
      MDC.put(TraceResponseFilter.MDC_TRACE_ID, "  ");

      try {
        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(response.getHeader("X-Trace-Id")).isNull();
      } finally {
        MDC.clear();
      }
    }
  }
}
