package com.andreasik.efipoker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andreasik.efipoker.shared.test.BaseComponentTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@DisplayName("Refresh Token Integration")
class RefreshTokenIntegrationTest extends BaseComponentTest {

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Nested
  @DisplayName("POST /api/v1/auth/login - refresh cookie")
  class LoginRefreshCookie {

    @Test
    void should_set_refresh_cookie_on_login() throws Exception {
      // language=JSON
      String body =
          """
          {"username":"testadmin","password":"testpassword"}
          """;

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.token").isNotEmpty())
              .andReturn();

      String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
      assertThat(setCookie).isNotNull();
      assertThat(setCookie).contains("efi_refresh=");
      assertThat(setCookie).contains("HttpOnly");
      assertThat(setCookie).contains("Secure");
      assertThat(setCookie).contains("SameSite=Strict");
      assertThat(setCookie).contains("Path=/api/v1/auth");
    }

    @Test
    void should_set_longer_cookie_with_remember_me() throws Exception {
      // language=JSON
      String body =
          """
          {"username":"testadmin","password":"testpassword","rememberMe":true}
          """;

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
              .andExpect(status().isOk())
              .andReturn();

      String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
      assertThat(setCookie).isNotNull();
      assertThat(setCookie).contains("efi_refresh=");
      // Remember me cookie has longer Max-Age (1200 vs 600 in test profile)
      assertThat(setCookie).contains("Max-Age=1200");
    }
  }

  @Nested
  @DisplayName("POST /api/v1/auth/refresh")
  class Refresh {

    @Test
    void should_refresh_with_valid_cookie() throws Exception {
      // Login first to get refresh cookie
      // language=JSON
      String body =
          """
          {"username":"testadmin","password":"testpassword"}
          """;

      MvcResult loginResult =
          mockMvc
              .perform(
                  post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
              .andExpect(status().isOk())
              .andReturn();

      String refreshCookieValue = extractRefreshCookieValue(loginResult);
      assertThat(refreshCookieValue).isNotNull();

      // Use refresh cookie to get new access token
      MvcResult refreshResult =
          mockMvc
              .perform(
                  post("/api/v1/auth/refresh")
                      .cookie(new Cookie(CookieHelper.REFRESH_COOKIE_NAME, refreshCookieValue)))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.token").isNotEmpty())
              .andExpect(jsonPath("$.expiresAt").isNotEmpty())
              .andReturn();

      // Should set a new refresh cookie (rotation)
      String newSetCookie = refreshResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
      assertThat(newSetCookie).isNotNull();
      assertThat(newSetCookie).contains("efi_refresh=");
    }

    @Test
    void should_rotate_token_on_refresh() throws Exception {
      // Login
      // language=JSON
      String body =
          """
          {"username":"testadmin","password":"testpassword"}
          """;

      MvcResult loginResult =
          mockMvc
              .perform(
                  post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
              .andExpect(status().isOk())
              .andReturn();

      String firstCookie = extractRefreshCookieValue(loginResult);

      // Refresh
      MvcResult refreshResult =
          mockMvc
              .perform(
                  post("/api/v1/auth/refresh")
                      .cookie(new Cookie(CookieHelper.REFRESH_COOKIE_NAME, firstCookie)))
              .andExpect(status().isOk())
              .andReturn();

      String secondCookie = extractRefreshCookieValue(refreshResult);

      // Old cookie should no longer work (rotated)
      mockMvc
          .perform(
              post("/api/v1/auth/refresh")
                  .cookie(new Cookie(CookieHelper.REFRESH_COOKIE_NAME, firstCookie)))
          .andExpect(status().isForbidden());

      // New cookie should work
      mockMvc
          .perform(
              post("/api/v1/auth/refresh")
                  .cookie(new Cookie(CookieHelper.REFRESH_COOKIE_NAME, secondCookie)))
          .andExpect(status().isOk());
    }

    @Test
    void should_reject_without_cookie_403() throws Exception {
      mockMvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isForbidden());
    }

    @Test
    void should_reject_invalid_cookie_403() throws Exception {
      mockMvc
          .perform(
              post("/api/v1/auth/refresh")
                  .cookie(new Cookie(CookieHelper.REFRESH_COOKIE_NAME, "invalid-token")))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/auth/logout - refresh token revocation")
  class LogoutRevocation {

    @Test
    void should_clear_refresh_cookie_on_logout() throws Exception {
      MvcResult result =
          mockMvc.perform(get("/api/v1/auth/logout")).andExpect(status().isFound()).andReturn();

      String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
      assertThat(setCookie).isNotNull();
      assertThat(setCookie).contains("efi_refresh=");
      assertThat(setCookie).contains("Max-Age=0");
    }

    @Test
    void should_revoke_refresh_tokens_on_authenticated_logout() throws Exception {
      // Login to get refresh cookie + JWT
      // language=JSON
      String body =
          """
          {"username":"testadmin","password":"testpassword"}
          """;

      MvcResult loginResult =
          mockMvc
              .perform(
                  post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
              .andExpect(status().isOk())
              .andReturn();

      String token =
          objectMapper
              .readTree(loginResult.getResponse().getContentAsString())
              .get("token")
              .asText();
      String refreshCookie = extractRefreshCookieValue(loginResult);

      // Logout with JWT (authenticated)
      mockMvc
          .perform(
              get("/api/v1/auth/logout")
                  .header("Authorization", "Bearer " + token)
                  .cookie(new Cookie(CookieHelper.REFRESH_COOKIE_NAME, refreshCookie)))
          .andExpect(status().isFound());

      // Refresh should fail (revoked)
      mockMvc
          .perform(
              post("/api/v1/auth/refresh")
                  .cookie(new Cookie(CookieHelper.REFRESH_COOKIE_NAME, refreshCookie)))
          .andExpect(status().isForbidden());
    }
  }

  private String extractRefreshCookieValue(MvcResult result) {
    String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
    if (setCookie == null) return null;
    // Parse "efi_refresh=<value>; Path=...; ..."
    for (String part : setCookie.split(";")) {
      String trimmed = part.trim();
      if (trimmed.startsWith("efi_refresh=")) {
        return trimmed.substring("efi_refresh=".length());
      }
    }
    return null;
  }
}
