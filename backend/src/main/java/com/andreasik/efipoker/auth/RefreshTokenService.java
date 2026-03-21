package com.andreasik.efipoker.auth;

import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;
  private final UserRepository userRepository;
  private final UserEntityMapper userEntityMapper;
  private final JwtProperties jwtProperties;
  private final SecureRandom secureRandom = new SecureRandom();

  public RefreshTokenService(
      RefreshTokenRepository refreshTokenRepository,
      UserRepository userRepository,
      UserEntityMapper userEntityMapper,
      JwtProperties jwtProperties) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.userRepository = userRepository;
    this.userEntityMapper = userEntityMapper;
    this.jwtProperties = jwtProperties;
  }

  @Transactional
  public String createRefreshToken(UUID userId, boolean rememberMe) {
    String rawToken = generateRawToken();
    String hash = hashToken(rawToken);

    long ttlSeconds =
        rememberMe ? jwtProperties.refreshRememberExpiration() : jwtProperties.refreshExpiration();

    RefreshTokenEntity entity =
        RefreshTokenEntity.builder()
            .tokenHash(hash)
            .userId(userId)
            .expiresAt(Instant.now().plusSeconds(ttlSeconds))
            .build();

    refreshTokenRepository.save(entity);
    log.info("Refresh token created for userId={}, rememberMe={}", userId, rememberMe);
    return rawToken;
  }

  public record RotationResult(String newRawToken, User user, long ttlSeconds) {}

  @Transactional
  public RotationResult rotateRefreshToken(String rawToken) {
    String hash = hashToken(rawToken);

    RefreshTokenEntity existing =
        refreshTokenRepository
            .findByTokenHash(hash)
            .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

    if (existing.getExpiresAt().isBefore(Instant.now())) {
      refreshTokenRepository.delete(existing);
      throw new UnauthorizedException("Refresh token expired");
    }

    User user =
        userRepository
            .findById(existing.getUserId())
            .map(userEntityMapper::toDomain)
            .orElseThrow(() -> new UnauthorizedException("User not found"));

    // Preserve original TTL (sliding window - each rotation resets the full duration)
    long originalTtlSeconds =
        existing.getExpiresAt().getEpochSecond() - existing.getCreatedAt().getEpochSecond();
    long ttlSeconds = Math.max(originalTtlSeconds, jwtProperties.refreshExpiration());

    // Delete old token
    refreshTokenRepository.delete(existing);

    // Create new token with same TTL
    String newRawToken = generateRawToken();
    String newHash = hashToken(newRawToken);

    RefreshTokenEntity newEntity =
        RefreshTokenEntity.builder()
            .tokenHash(newHash)
            .userId(existing.getUserId())
            .expiresAt(Instant.now().plusSeconds(ttlSeconds))
            .build();

    refreshTokenRepository.save(newEntity);
    log.info("Refresh token rotated for userId={}", existing.getUserId());
    return new RotationResult(newRawToken, user, ttlSeconds);
  }

  @Transactional
  public void revokeAllForUser(UUID userId) {
    refreshTokenRepository.deleteAllByUserId(userId);
    log.info("All refresh tokens revoked for userId={}", userId);
  }

  @Transactional
  public int cleanupExpired() {
    int deleted = refreshTokenRepository.deleteExpired(Instant.now());
    if (deleted > 0) {
      log.info("Cleaned up {} expired refresh tokens", deleted);
    }
    return deleted;
  }

  private String generateRawToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  static String hashToken(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
