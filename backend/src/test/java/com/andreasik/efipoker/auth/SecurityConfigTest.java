package com.andreasik.efipoker.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.andreasik.efipoker.shared.test.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@DisplayName("SecurityConfig CORS")
class SecurityConfigTest extends BaseUnitTest {

  @Mock private AppProperties appProperties;

  private SecurityConfig securityConfig;

  @BeforeEach
  void setUp() {
    AppProperties.CorsProperties corsProperties =
        new AppProperties.CorsProperties("https://example.com,https://other.com");
    org.mockito.Mockito.when(appProperties.cors()).thenReturn(corsProperties);

    securityConfig =
        new SecurityConfig(
            new com.fasterxml.jackson.databind.ObjectMapper(),
            java.util.Optional.empty(),
            appProperties,
            new Auth0Properties(false, null, null, null),
            new org.springframework.security.oauth2.server.resource.authentication
                .JwtAuthenticationConverter());
  }

  @Test
  @DisplayName("should configure allowed origins from AppProperties")
  void should_configure_allowed_origins() {
    CorsConfiguration config = getCorsConfig();

    assertThat(config.getAllowedOrigins())
        .containsExactlyInAnyOrder("https://example.com", "https://other.com");
  }

  @Test
  @DisplayName("should configure allowed HTTP methods")
  void should_configure_allowed_methods() {
    CorsConfiguration config = getCorsConfig();

    assertThat(config.getAllowedMethods())
        .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
  }

  @Test
  @DisplayName("should configure allowed headers")
  void should_configure_allowed_headers() {
    CorsConfiguration config = getCorsConfig();

    assertThat(config.getAllowedHeaders())
        .containsExactlyInAnyOrder("Content-Type", "Authorization", "traceparent", "tracestate");
  }

  @Test
  @DisplayName("should configure exposed headers")
  void should_configure_exposed_headers() {
    CorsConfiguration config = getCorsConfig();

    assertThat(config.getExposedHeaders()).containsExactly("X-Trace-Id");
  }

  @Test
  @DisplayName("should allow credentials")
  void should_allow_credentials() {
    CorsConfiguration config = getCorsConfig();

    assertThat(config.getAllowCredentials()).isTrue();
  }

  @Test
  @DisplayName("should set max age to 3600 seconds")
  void should_set_max_age() {
    CorsConfiguration config = getCorsConfig();

    assertThat(config.getMaxAge()).isEqualTo(3600L);
  }

  @Test
  @DisplayName("should register configuration for all paths")
  void should_register_configuration_for_wildcard_path() {
    CorsConfigurationSource source = securityConfig.corsConfigurationSource();

    assertThat(source).isInstanceOf(UrlBasedCorsConfigurationSource.class);

    org.springframework.mock.web.MockHttpServletRequest request =
        new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/v1/some/path");
    assertThat(source.getCorsConfiguration(request)).isNotNull();
  }

  private CorsConfiguration getCorsConfig() {
    CorsConfigurationSource source = securityConfig.corsConfigurationSource();
    org.springframework.mock.web.MockHttpServletRequest request =
        new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/v1/test");
    return source.getCorsConfiguration(request);
  }
}
