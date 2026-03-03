package com.andreasik.efipoker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.andreasik.efipoker.shared.test.BaseUnitTest;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@DisplayName("JwtService")
class JwtServiceTest extends BaseUnitTest {

  private static final String SECRET =
      "test-secret-for-jwt-signing-must-be-at-least-64-characters-long-for-hs512-algorithm";

  private JwtService jwtService;
  private JwtDecoder jwtDecoder;

  @BeforeEach
  void setUp() {
    JwtProperties jwtProperties = new JwtProperties(SECRET, 3600);

    SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512");

    JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    jwtDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS512).build();

    jwtService = new JwtService(jwtEncoder, jwtProperties);
  }

  @Nested
  @DisplayName("generateToken")
  class GenerateToken {

    @Test
    void should_generate_decodable_token_with_correct_claims() {
      // Arrange
      UUID userId = UUID.randomUUID();
      User user = User.builder().id(userId).username("testuser").role("USER").build();

      // Act
      String token = jwtService.generateToken(user);

      // Assert
      Jwt decoded = jwtDecoder.decode(token);
      assertAll(
          () -> assertThat(decoded.getSubject()).isEqualTo(userId.toString()),
          () -> assertThat(decoded.getClaimAsString("username")).isEqualTo("testuser"),
          () -> assertThat(decoded.getClaimAsString("role")).isEqualTo("USER"),
          () -> assertThat(decoded.getIssuedAt()).isNotNull(),
          () -> assertThat(decoded.getExpiresAt()).isAfter(Instant.now()));
    }

    @Test
    void should_generate_token_with_admin_role() {
      // Arrange
      User user = User.builder().id(UUID.randomUUID()).username("admin").role("ADMIN").build();

      // Act
      String token = jwtService.generateToken(user);

      // Assert
      Jwt decoded = jwtDecoder.decode(token);
      assertThat(decoded.getClaimAsString("role")).isEqualTo("ADMIN");
    }
  }

  @Nested
  @DisplayName("getTokenExpiresAt")
  class GetTokenExpiresAt {

    @Test
    void should_return_future_expiration() {
      // Act
      Instant expiresAt = jwtService.getTokenExpiresAt();

      // Assert
      assertThat(expiresAt).isAfter(Instant.now());
    }
  }
}
