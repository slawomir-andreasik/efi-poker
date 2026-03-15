package com.andreasik.efipoker.shared.security;

import jakarta.annotation.Nullable;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class SecurityUtils {

  private SecurityUtils() {}

  @Nullable
  public static UUID getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken) {
      return UUID.fromString(auth.getName());
    }
    return null;
  }
}
