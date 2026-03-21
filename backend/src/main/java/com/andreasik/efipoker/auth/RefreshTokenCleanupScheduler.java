package com.andreasik.efipoker.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

  private final RefreshTokenService refreshTokenService;

  @Scheduled(fixedRate = 3600000)
  public void cleanupExpiredTokens() {
    refreshTokenService.cleanupExpired();
  }
}
