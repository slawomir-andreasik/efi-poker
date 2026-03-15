package com.andreasik.efipoker.auth;

import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class JwtService {

  private final JwtEncoder jwtEncoder;
  private final long expirationMs;

  public JwtService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
    this.jwtEncoder = jwtEncoder;
    this.expirationMs = jwtProperties.expiration() * 1000;
  }

  public String generateToken(User user) {
    Instant now = Instant.now();
    Instant expiration = now.plusMillis(expirationMs);

    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(user.id().toString())
            .claim("username", user.username())
            .claim("role", user.role())
            .issuedAt(now)
            .expiresAt(expiration)
            .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS512).build();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

    log.info("JWT generated for user: {}", user.username());
    return token;
  }

  public Instant getTokenExpiresAt() {
    return Instant.now().plusMillis(expirationMs);
  }
}
