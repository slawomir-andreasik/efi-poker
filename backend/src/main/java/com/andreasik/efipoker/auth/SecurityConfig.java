package com.andreasik.efipoker.auth;

import com.andreasik.efipoker.shared.exception.ErrorType;
import com.andreasik.efipoker.shared.observability.TraceResponseFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final ObjectMapper objectMapper;
  private final Auth0SuccessHandler auth0SuccessHandler;
  private final AppProperties appProperties;
  private final Auth0Properties auth0Properties;
  private final JwtAuthenticationConverter jwtAuthenticationConverter;

  /// When `true`, disables Spring Security default headers (proxy provides them in prod).
  @Value("${app.security.proxy-managed-headers:false}")
  private boolean proxyManagedHeaders;

  public SecurityConfig(
      ObjectMapper objectMapper,
      Optional<Auth0SuccessHandler> auth0SuccessHandler,
      AppProperties appProperties,
      Auth0Properties auth0Properties,
      JwtAuthenticationConverter jwtAuthenticationConverter) {
    this.objectMapper = objectMapper;
    this.auth0SuccessHandler = auth0SuccessHandler.orElse(null);
    this.appProperties = appProperties;
    this.auth0Properties = auth0Properties;
    this.jwtAuthenticationConverter = jwtAuthenticationConverter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable);

    if (proxyManagedHeaders) {
      // Proxy provides security headers in prod - disable Spring defaults to avoid duplicates
      http.headers(
          headers ->
              headers
                  .frameOptions(frame -> frame.disable())
                  .httpStrictTransportSecurity(hsts -> hsts.disable())
                  .contentTypeOptions(ct -> ct.disable()));
    }

    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session ->
                session.sessionCreationPolicy(
                    // OAuth2 login requires a session to store the state/nonce during the redirect
                    auth0Properties.enabled()
                        ? SessionCreationPolicy.IF_REQUIRED
                        : SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/v1/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/v1/auth/me", "/api/v1/auth/me/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                    .authenticationEntryPoint(problemDetailEntryPoint()))
        .exceptionHandling(ex -> ex.authenticationEntryPoint(problemDetailEntryPoint()));

    if (auth0Properties.enabled()) {
      http.oauth2Login(
          oauth2 ->
              oauth2
                  .authorizationEndpoint(auth -> auth.baseUri("/api/v1/auth/oauth2/authorize"))
                  .redirectionEndpoint(redir -> redir.baseUri("/api/v1/auth/oauth2/callback"))
                  .successHandler(auth0SuccessHandler));
    }

    return http.build();
  }

  private AuthenticationEntryPoint problemDetailEntryPoint() {
    return (request, response, authException) -> {
      ProblemDetail problem =
          ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication required");
      problem.setTitle("Unauthorized");
      problem.setType(ErrorType.UNAUTHORIZED.uri());

      String traceId = MDC.get(TraceResponseFilter.MDC_TRACE_ID);
      if (traceId != null && !traceId.isBlank()) {
        problem.setProperty(TraceResponseFilter.MDC_TRACE_ID, traceId);
      }

      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
      response.getWriter().write(objectMapper.writeValueAsString(problem));
    };
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList(appProperties.cors().origins().split(",")));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        List.of("Content-Type", "Authorization", "traceparent", "tracestate"));
    configuration.setExposedHeaders(List.of("X-Trace-Id"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
