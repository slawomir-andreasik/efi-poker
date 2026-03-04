package com.andreasik.efipoker.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andreasik.efipoker.shared.test.BaseComponentTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("Password Integration")
class PasswordIntegrationTest extends BaseComponentTest {

  @Nested
  @DisplayName("PUT /api/v1/auth/me/password")
  class ChangePassword {

    @Test
    void should_change_password_204() throws Exception {
      String token = loginAsTestAdmin();
      String body = "{\"currentPassword\":\"testpassword\",\"newPassword\":\"newpassword123\"}";

      mockMvc
          .perform(
              put("/api/v1/auth/me/password")
                  .header("Authorization", "Bearer " + token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isNoContent());
    }

    @Test
    void should_reject_wrong_current_password_403() throws Exception {
      String token = loginAsTestAdmin();
      String body = "{\"currentPassword\":\"wrongpassword\",\"newPassword\":\"newpassword123\"}";

      mockMvc
          .perform(
              put("/api/v1/auth/me/password")
                  .header("Authorization", "Bearer " + token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_short_new_password_400() throws Exception {
      String token = loginAsTestAdmin();
      String body = "{\"currentPassword\":\"testpassword\",\"newPassword\":\"short\"}";

      mockMvc
          .perform(
              put("/api/v1/auth/me/password")
                  .header("Authorization", "Bearer " + token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isBadRequest());
    }

    @Test
    void should_reject_without_token_401() throws Exception {
      String body = "{\"currentPassword\":\"testpassword\",\"newPassword\":\"newpassword123\"}";

      mockMvc
          .perform(
              put("/api/v1/auth/me/password").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void should_reject_missing_new_password_400() throws Exception {
      String token = loginAsTestAdmin();
      String body = "{\"currentPassword\":\"testpassword\"}";

      mockMvc
          .perform(
              put("/api/v1/auth/me/password")
                  .header("Authorization", "Bearer " + token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("PUT /api/v1/admin/users/{id}/password")
  class AdminResetPassword {

    @Test
    void should_reset_password_204() throws Exception {
      String token = loginAsTestAdmin();

      // Get test admin's user ID via /auth/me
      String meJson =
          mockMvc
              .perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
              .andReturn()
              .getResponse()
              .getContentAsString();
      String userId = objectMapper.readTree(meJson).get("id").asText();

      String body = "{\"newPassword\":\"adminreset1\"}";

      mockMvc
          .perform(
              put("/api/v1/admin/users/" + userId + "/password")
                  .header("Authorization", "Bearer " + token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isNoContent());
    }

    @Test
    void should_return_404_for_unknown_user() throws Exception {
      String token = loginAsTestAdmin();
      String body = "{\"newPassword\":\"adminreset1\"}";

      mockMvc
          .perform(
              put("/api/v1/admin/users/00000000-0000-0000-0000-000000000000/password")
                  .header("Authorization", "Bearer " + token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isNotFound());
    }

    @Test
    void should_reject_without_token_401() throws Exception {
      String body = "{\"newPassword\":\"adminreset1\"}";

      mockMvc
          .perform(
              put("/api/v1/admin/users/00000000-0000-0000-0000-000000000000/password")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/auth/me - hasPassword and authProvider")
  class GetCurrentUserPasswordFields {

    @Test
    void should_include_has_password_and_auth_provider() throws Exception {
      String token = loginAsTestAdmin();

      mockMvc
          .perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.hasPassword").value(true))
          .andExpect(jsonPath("$.authProvider").value("LOCAL"));
    }
  }
}
