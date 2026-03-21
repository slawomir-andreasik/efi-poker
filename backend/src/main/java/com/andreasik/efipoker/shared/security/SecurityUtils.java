package com.andreasik.efipoker.shared.security;

import com.andreasik.efipoker.auth.JwtService;
import jakarta.annotation.Nullable;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class SecurityUtils {

  private SecurityUtils() {}

  /// Returns the current user ID from a user JWT, or null if not a user JWT.
  @Nullable
  public static UUID getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwtAuth && !isGuestToken(jwtAuth)) {
      return parseUuid(auth.getName());
    }
    return null;
  }

  /// Returns the participant ID from a guest JWT, or null if not a guest JWT.
  @Nullable
  public static UUID getCurrentParticipantId() {
    Jwt jwt = getCurrentJwt();
    if (jwt == null || !isGuestToken()) {
      return null;
    }
    String participantId = jwt.getClaimAsString(JwtService.CLAIM_PARTICIPANT_ID);
    return participantId != null ? parseUuid(participantId) : null;
  }

  /// Returns the project ID from a guest JWT, or null if not a guest JWT.
  @Nullable
  public static UUID getProjectIdFromToken() {
    Jwt jwt = getCurrentJwt();
    if (jwt == null || !isGuestToken()) {
      return null;
    }
    String projectId = jwt.getClaimAsString(JwtService.CLAIM_PROJECT_ID);
    return projectId != null ? parseUuid(projectId) : null;
  }

  @Nullable
  private static UUID parseUuid(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /// Returns true if the current authentication is a guest JWT.
  public static boolean isGuestToken() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      return isGuestToken(jwtAuth);
    }
    return false;
  }

  /// Returns true if the current guest JWT has admin=true.
  public static boolean isGuestAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null
        && auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_PROJECT_ADMIN"));
  }

  /// Returns true if the current user is a site admin (ROLE_ADMIN).
  public static boolean isSiteAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null
        && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
  }

  /// Returns the current JWT token, or null if not authenticated via JWT.
  @Nullable
  public static Jwt getCurrentJwt() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      return jwtAuth.getToken();
    }
    return null;
  }

  private static boolean isGuestToken(JwtAuthenticationToken jwtAuth) {
    return JwtService.TOKEN_TYPE_GUEST.equals(
        jwtAuth.getToken().getClaimAsString(JwtService.CLAIM_TYPE));
  }
}
