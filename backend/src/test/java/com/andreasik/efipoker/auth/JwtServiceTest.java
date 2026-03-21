package com.andreasik.efipoker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.andreasik.efipoker.shared.test.BaseUnitTest;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
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
    JwtProperties jwtProperties = new JwtProperties(SECRET, 3600, 7776000);

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
  @DisplayName("generateGuestToken")
  class GenerateGuestToken {

    @Test
    void should_generate_admin_guest_token_without_participant() {
      UUID projectId = UUID.randomUUID();

      String token = jwtService.generateGuestToken(projectId, null, true, null);

      Jwt decoded = jwtDecoder.decode(token);
      assertAll(
          () -> assertThat(decoded.getClaimAsString("type")).isEqualTo("guest"),
          () -> assertThat(decoded.getClaimAsString("projectId")).isEqualTo(projectId.toString()),
          () -> assertThat(decoded.<Boolean>getClaim("admin")).isTrue(),
          () -> assertThat(decoded.getClaimAsString("participantId")).isNull(),
          () -> assertThat(decoded.getSubject()).isNotBlank(),
          () -> assertThat(decoded.getExpiresAt()).isAfter(Instant.now()));
    }

    @Test
    void should_generate_participant_guest_token() {
      UUID projectId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();

      String token = jwtService.generateGuestToken(projectId, participantId, false, "Alice");

      Jwt decoded = jwtDecoder.decode(token);
      assertAll(
          () -> assertThat(decoded.getClaimAsString("type")).isEqualTo("guest"),
          () -> assertThat(decoded.getClaimAsString("projectId")).isEqualTo(projectId.toString()),
          () -> assertThat(decoded.<Boolean>getClaim("admin")).isFalse(),
          () ->
              assertThat(decoded.getClaimAsString("participantId"))
                  .isEqualTo(participantId.toString()),
          () -> assertThat(decoded.getSubject()).isEqualTo(participantId.toString()),
          () -> assertThat(decoded.getClaimAsString("nickname")).isEqualTo("Alice"));
    }
  }

  @Nested
  @DisplayName("refreshGuestToken")
  class RefreshGuestToken {

    @Test
    void should_preserve_claims_with_fresh_expiry() {
      UUID projectId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();
      String original = jwtService.generateGuestToken(projectId, participantId, true, "Bob");
      Jwt originalDecoded = jwtDecoder.decode(original);

      String refreshed = jwtService.refreshGuestToken(originalDecoded);

      Jwt decoded = jwtDecoder.decode(refreshed);
      assertAll(
          () -> assertThat(decoded.getClaimAsString("type")).isEqualTo("guest"),
          () -> assertThat(decoded.getClaimAsString("projectId")).isEqualTo(projectId.toString()),
          () -> assertThat(decoded.<Boolean>getClaim("admin")).isTrue(),
          () ->
              assertThat(decoded.getClaimAsString("participantId"))
                  .isEqualTo(participantId.toString()),
          () -> assertThat(decoded.getClaimAsString("nickname")).isEqualTo("Bob"),
          () ->
              assertThat(decoded.getExpiresAt()).isAfterOrEqualTo(originalDecoded.getExpiresAt()));
    }
  }

  @Nested
  @DisplayName("Token security")
  class TokenSecurity {

    @Test
    void should_reject_tampered_token() {
      UUID projectId = UUID.randomUUID();
      String token = jwtService.generateGuestToken(projectId, null, false, null);

      // Tamper with the payload: flip admin claim
      String[] parts = token.split("\\.");
      String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
      String tampered = payload.replace("\"admin\":false", "\"admin\":true");
      String tamperedB64 =
          Base64.getUrlEncoder()
              .withoutPadding()
              .encodeToString(tampered.getBytes(StandardCharsets.UTF_8));
      String tamperedToken = parts[0] + "." + tamperedB64 + "." + parts[2];

      assertThatThrownBy(() -> jwtDecoder.decode(tamperedToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void should_reject_expired_token() {
      // Build a token manually with expiresAt in the past
      Instant pastExpiry = Instant.now().minusSeconds(60);
      JwtClaimsSet claims =
          JwtClaimsSet.builder()
              .subject(UUID.randomUUID().toString())
              .claim(JwtService.CLAIM_TYPE, JwtService.TOKEN_TYPE_GUEST)
              .claim(JwtService.CLAIM_PROJECT_ID, UUID.randomUUID().toString())
              .claim(JwtService.CLAIM_ADMIN, true)
              .issuedAt(pastExpiry.minusSeconds(3600))
              .expiresAt(pastExpiry)
              .build();

      SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
      JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
      JwsHeader header = JwsHeader.with(MacAlgorithm.HS512).build();
      String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

      assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }
  }

  @Nested
  @DisplayName("getTokenExpiresAt")
  class GetTokenExpiresAt {

    @Test
    void should_return_future_expiration() {
      Instant expiresAt = jwtService.getTokenExpiresAt();
      assertThat(expiresAt).isAfter(Instant.now());
    }

    @Test
    void should_return_guest_future_expiration() {
      Instant expiresAt = jwtService.getGuestTokenExpiresAt();
      assertThat(expiresAt).isAfter(Instant.now());
    }
  }
}
