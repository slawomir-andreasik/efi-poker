package com.andreasik.efipoker.auth;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class JwtService {

  public static final String CLAIM_TYPE = "type";
  public static final String CLAIM_ROLE = "role";
  public static final String CLAIM_USERNAME = "username";
  public static final String CLAIM_PROJECT_ID = "projectId";
  public static final String CLAIM_ADMIN = "admin";
  public static final String CLAIM_NICKNAME = "nickname";
  public static final String CLAIM_PARTICIPANT_ID = "participantId";
  public static final String TOKEN_TYPE_GUEST = "guest";
  public static final String ISSUER = "https://efi-poker";

  private final JwtEncoder jwtEncoder;
  private final long expirationMs;
  private final long guestExpirationMs;

  public JwtService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
    this.jwtEncoder = jwtEncoder;
    this.expirationMs = jwtProperties.expiration() * 1000;
    this.guestExpirationMs = jwtProperties.guestExpiration() * 1000;
  }

  public String generateToken(User user) {
    Instant now = Instant.now();
    Instant expiration = now.plusMillis(expirationMs);

    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(ISSUER)
            .audience(List.of(ISSUER))
            .subject(user.id().toString())
            .claim(CLAIM_USERNAME, user.username())
            .claim(CLAIM_ROLE, user.role())
            .issuedAt(now)
            .expiresAt(expiration)
            .build();

    return encode(claims);
  }

  /// Generate a guest JWT for project access.
  /// participantId is null when guest creates a project (admin-only, not yet a participant).
  public String generateGuestToken(
      UUID projectId, @Nullable UUID participantId, boolean isAdmin, @Nullable String nickname) {
    Instant now = Instant.now();
    Instant expiration = now.plusMillis(guestExpirationMs);

    String subject =
        participantId != null ? participantId.toString() : UUID.randomUUID().toString();

    // Security: 90-day guest TTL is by design - matches "Remember me" refresh token TTL.
    // Guest tokens are stateless (no revocation). Mitigated by 3-layer XSS prevention.
    JwtClaimsSet.Builder builder =
        JwtClaimsSet.builder()
            .issuer(ISSUER)
            .audience(List.of(ISSUER))
            .subject(subject)
            .claim(CLAIM_TYPE, TOKEN_TYPE_GUEST)
            .claim(CLAIM_PROJECT_ID, projectId.toString())
            .claim(CLAIM_ADMIN, isAdmin)
            .issuedAt(now)
            .expiresAt(expiration);

    if (participantId != null) {
      builder.claim(CLAIM_PARTICIPANT_ID, participantId.toString());
    }
    if (nickname != null) {
      builder.claim(CLAIM_NICKNAME, nickname);
    }

    String token = encode(builder.build());
    log.info(
        "Guest JWT generated: projectId={}, participantId={}, admin={}",
        projectId,
        participantId,
        isAdmin);
    return token;
  }

  /// Refresh a guest JWT with the same claims but a fresh expiry.
  /// Security: copying claims from current token is safe - JWT is HS512-signed, tampering fails
  /// signature validation in Spring Security before reaching this method.
  public String refreshGuestToken(Jwt currentToken) {
    Instant now = Instant.now();
    Instant expiration = now.plusMillis(guestExpirationMs);

    JwtClaimsSet.Builder builder =
        JwtClaimsSet.builder()
            .issuer(ISSUER)
            .audience(List.of(ISSUER))
            .subject(currentToken.getSubject())
            .claim(CLAIM_TYPE, TOKEN_TYPE_GUEST)
            .claim(CLAIM_PROJECT_ID, currentToken.getClaimAsString(CLAIM_PROJECT_ID))
            .claim(CLAIM_ADMIN, currentToken.getClaim(CLAIM_ADMIN))
            .issuedAt(now)
            .expiresAt(expiration);

    String participantId = currentToken.getClaimAsString(CLAIM_PARTICIPANT_ID);
    if (participantId != null) {
      builder.claim(CLAIM_PARTICIPANT_ID, participantId);
    }
    String nickname = currentToken.getClaimAsString(CLAIM_NICKNAME);
    if (nickname != null) {
      builder.claim(CLAIM_NICKNAME, nickname);
    }

    log.info("Guest JWT refreshed: sub={}", currentToken.getSubject());
    return encode(builder.build());
  }

  public Instant getTokenExpiresAt() {
    return Instant.now().plusMillis(expirationMs);
  }

  public Instant getGuestTokenExpiresAt() {
    return Instant.now().plusMillis(guestExpirationMs);
  }

  private String encode(JwtClaimsSet claims) {
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS512).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
