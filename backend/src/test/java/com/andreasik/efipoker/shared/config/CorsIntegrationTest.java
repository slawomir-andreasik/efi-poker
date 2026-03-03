package com.andreasik.efipoker.shared.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andreasik.efipoker.shared.test.BaseComponentTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CORS Integration")
class CorsIntegrationTest extends BaseComponentTest {

  @Nested
  @DisplayName("Preflight requests")
  class Preflight {

    @Test
    void should_allow_configured_origin() throws Exception {
      mockMvc
          .perform(
              options("/api/v1/boards")
                  .header("Origin", "http://localhost:5173")
                  .header("Access-Control-Request-Method", "POST"))
          .andExpect(status().isOk())
          .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void should_reject_disallowed_origin() throws Exception {
      mockMvc
          .perform(
              options("/api/v1/boards")
                  .header("Origin", "https://evil.com")
                  .header("Access-Control-Request-Method", "POST"))
          .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE"})
    void should_accept_method(String method) throws Exception {
      mockMvc
          .perform(
              options("/api/v1/boards")
                  .header("Origin", "http://localhost:5173")
                  .header("Access-Control-Request-Method", method))
          .andExpect(status().isOk())
          .andExpect(header().exists("Access-Control-Allow-Methods"));
    }
  }
}
