package com.andreasik.efipoker.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andreasik.efipoker.shared.test.BaseComponentTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@DisplayName("AuthController Integration")
class AuthControllerIntegrationTest extends BaseComponentTest {

  @Nested
  @DisplayName("GET /api/v1/auth/config")
  class GetAuthConfig {

    @Test
    void should_return_auth_config_200() throws Exception {
      mockMvc
          .perform(get("/api/v1/auth/config"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.auth0Enabled").isBoolean());
    }
  }

  @Nested
  @DisplayName("POST /api/v1/auth/login")
  class Login {

    @Test
    void should_login_with_valid_credentials_200() throws Exception {
      // language=JSON
      String body =
          """
          {"username":"testadmin","password":"testpassword"}
          """;

      mockMvc
          .perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.token").isNotEmpty())
          .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void should_reject_wrong_password_401() throws Exception {
      // language=JSON
      String body =
          """
          {"username":"testadmin","password":"wrongpassword"}
          """;

      mockMvc
          .perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void should_reject_nonexistent_user_401() throws Exception {
      // language=JSON
      String body =
          """
          {"username":"nobody","password":"somepassword"}
          """;

      mockMvc
          .perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void should_reject_too_long_password_400() throws Exception {
      String longPassword = "a".repeat(129);
      String body = "{\"username\":\"testadmin\",\"password\":\"" + longPassword + "\"}";

      mockMvc
          .perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/auth/me")
  class GetCurrentUser {

    @Test
    void should_return_user_with_valid_token() throws Exception {
      String token = loginAsTestAdmin();

      mockMvc
          .perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.username").value("testadmin"))
          .andExpect(jsonPath("$.role").value("ADMIN"))
          .andExpect(jsonPath("$.hasPassword").value(true))
          .andExpect(jsonPath("$.authProvider").value("LOCAL"));
    }

    @Test
    void should_return_401_without_token() throws Exception {
      mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_401_with_invalid_token() throws Exception {
      mockMvc
          .perform(get("/api/v1/auth/me").header("Authorization", "Bearer invalid-token"))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/auth/logout")
  class Logout {

    @Test
    void should_redirect_to_frontend_on_logout_302() throws Exception {
      mockMvc
          .perform(get("/api/v1/auth/logout"))
          .andExpect(status().isFound())
          .andExpect(header().exists(HttpHeaders.LOCATION));
    }
  }
}
