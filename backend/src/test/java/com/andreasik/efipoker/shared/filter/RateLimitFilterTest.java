package com.andreasik.efipoker.shared.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.andreasik.efipoker.shared.config.RateLimitConfig;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("RateLimitFilter")
class RateLimitFilterTest extends BaseUnitTest {

  private final RateLimitConfig config = new RateLimitConfig(10, 30, 10);
  private final RateLimitFilter filter = new RateLimitFilter(config);

  @Nested
  @DisplayName("Write method rate limiting")
  class WriteMethods {

    @Test
    void should_allow_requests_under_limit() throws Exception {
      for (int i = 0; i < 10; i++) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/boards");
        request.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
      }
    }

    @Test
    void should_reject_when_limit_exceeded() throws Exception {
      // Arrange - fill up the limit
      for (int i = 0; i < 10; i++) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/boards");
        request.setRemoteAddr("192.168.1.2");
        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());
      }

      // Act - 11th request
      MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/boards");
      request.setRemoteAddr("192.168.1.2");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, new MockFilterChain());

      // Assert
      assertThat(response.getStatus()).isEqualTo(429);
      assertThat(response.getContentType()).isEqualTo("application/problem+json");
      assertThat(response.getHeader("Retry-After")).isEqualTo("10");
      assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
      assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
    }

    @Test
    void should_limit_all_write_methods() throws Exception {
      String[] methods = {"POST", "PUT", "DELETE", "PATCH"};

      for (String method : methods) {
        RateLimitFilter freshFilter = new RateLimitFilter(new RateLimitConfig(1, 300, 10));

        // First request passes
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/v1/test");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        freshFilter.doFilterInternal(request, response, new MockFilterChain());
        assertThat(response.getStatus()).as("First %s should pass", method).isEqualTo(200);

        // Second request blocked
        MockHttpServletRequest request2 = new MockHttpServletRequest(method, "/api/v1/test");
        request2.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        freshFilter.doFilterInternal(request2, response2, new MockFilterChain());
        assertThat(response2.getStatus()).as("Second %s should be blocked", method).isEqualTo(429);
      }
    }
  }

  @Nested
  @DisplayName("GET request rate limiting")
  class GetRequests {

    @Test
    void should_allow_get_requests_under_read_limit() throws Exception {
      for (int i = 0; i < 30; i++) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/boards");
        request.setRemoteAddr("192.168.1.3");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
      }
    }

    @Test
    void should_reject_get_when_read_limit_exceeded() throws Exception {
      // Arrange - fill up the read limit (30)
      RateLimitFilter tightReadFilter = new RateLimitFilter(new RateLimitConfig(10, 5, 10));
      for (int i = 0; i < 5; i++) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/boards");
        request.setRemoteAddr("192.168.1.4");
        tightReadFilter.doFilterInternal(
            request, new MockHttpServletResponse(), new MockFilterChain());
      }

      // Act - 6th GET request
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/boards");
      request.setRemoteAddr("192.168.1.4");
      MockHttpServletResponse response = new MockHttpServletResponse();

      tightReadFilter.doFilterInternal(request, response, new MockFilterChain());

      // Assert
      assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void should_use_separate_buckets_for_read_and_write() throws Exception {
      // Arrange - exhaust write limit
      for (int i = 0; i < 10; i++) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/boards");
        request.setRemoteAddr("192.168.1.5");
        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());
      }

      // Act - GET from same IP should still work (separate bucket)
      MockHttpServletRequest getRequest = new MockHttpServletRequest("GET", "/api/v1/boards");
      getRequest.setRemoteAddr("192.168.1.5");
      MockHttpServletResponse getResponse = new MockHttpServletResponse();

      filter.doFilterInternal(getRequest, getResponse, new MockFilterChain());

      // Assert
      assertThat(getResponse.getStatus()).isEqualTo(200);
    }
  }

  @Nested
  @DisplayName("Window expiration")
  class WindowExpiration {

    @Test
    void should_reset_after_window_expires() throws Exception {
      // Use a filter with 1-second window for fast test
      RateLimitFilter shortWindowFilter = new RateLimitFilter(new RateLimitConfig(2, 300, 1));

      // Fill the limit
      for (int i = 0; i < 2; i++) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/boards");
        request.setRemoteAddr("192.168.1.4");
        shortWindowFilter.doFilterInternal(
            request, new MockHttpServletResponse(), new MockFilterChain());
      }

      // Verify blocked
      MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/v1/boards");
      blockedReq.setRemoteAddr("192.168.1.4");
      MockHttpServletResponse blockedResp = new MockHttpServletResponse();
      shortWindowFilter.doFilterInternal(blockedReq, blockedResp, new MockFilterChain());
      assertThat(blockedResp.getStatus()).isEqualTo(429);

      // Wait for window to expire
      Thread.sleep(1100);

      // Should be allowed again
      MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/boards");
      request.setRemoteAddr("192.168.1.4");
      MockHttpServletResponse response = new MockHttpServletResponse();
      shortWindowFilter.doFilterInternal(request, response, new MockFilterChain());
      assertThat(response.getStatus()).isEqualTo(200);
    }
  }

  @Nested
  @DisplayName("IP resolution")
  class IpResolution {

    @Test
    void should_use_remote_addr_for_rate_limiting() {
      // resolveClientIp delegates to remoteAddr (Tomcat RemoteIpValve resolves
      // X-Forwarded-For from trusted proxies before servlet filters run)
      MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/boards");
      request.setRemoteAddr("203.0.113.50");

      String ip = filter.resolveClientIp(request);

      assertThat(ip).isEqualTo("203.0.113.50");
    }

    @Test
    void should_rate_limit_by_remote_addr() throws Exception {
      RateLimitFilter tightFilter = new RateLimitFilter(new RateLimitConfig(1, 300, 10));

      MockHttpServletRequest request1 = new MockHttpServletRequest("POST", "/api/v1/boards");
      request1.setRemoteAddr("192.168.1.100");
      MockHttpServletResponse response1 = new MockHttpServletResponse();
      tightFilter.doFilterInternal(request1, response1, new MockFilterChain());
      assertThat(response1.getStatus()).isEqualTo(200);

      MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/api/v1/boards");
      request2.setRemoteAddr("192.168.1.100");
      MockHttpServletResponse response2 = new MockHttpServletResponse();
      tightFilter.doFilterInternal(request2, response2, new MockFilterChain());
      assertThat(response2.getStatus()).isEqualTo(429);
    }

    @Test
    void should_not_share_limits_between_different_ips() throws Exception {
      RateLimitFilter tightFilter = new RateLimitFilter(new RateLimitConfig(1, 300, 10));

      // First IP - use up limit
      MockHttpServletRequest request1 = new MockHttpServletRequest("POST", "/api/v1/boards");
      request1.setRemoteAddr("10.0.0.1");
      tightFilter.doFilterInternal(request1, new MockHttpServletResponse(), new MockFilterChain());

      // Different IP - should still pass
      MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/api/v1/boards");
      request2.setRemoteAddr("10.0.0.2");
      MockHttpServletResponse response2 = new MockHttpServletResponse();
      tightFilter.doFilterInternal(request2, response2, new MockFilterChain());
      assertThat(response2.getStatus()).isEqualTo(200);
    }
  }
}
