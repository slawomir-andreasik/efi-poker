package com.andreasik.efipoker.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.springframework.http.ResponseCookie;

public final class CookieHelper {

  public static final String REFRESH_COOKIE_NAME = "efi_refresh";
  public static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

  private CookieHelper() {}

  public static String extractCookie(HttpServletRequest request, String name) {
    if (request.getCookies() == null) return null;
    return Arrays.stream(request.getCookies())
        .filter(c -> name.equals(c.getName()))
        .map(Cookie::getValue)
        .findFirst()
        .orElse(null);
  }

  public static ResponseCookie createRefreshCookie(String token, long maxAgeSeconds) {
    return ResponseCookie.from(REFRESH_COOKIE_NAME, token)
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .path(REFRESH_COOKIE_PATH)
        .maxAge(maxAgeSeconds)
        .build();
  }

  public static ResponseCookie clearRefreshCookie() {
    return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .path(REFRESH_COOKIE_PATH)
        .maxAge(0)
        .build();
  }
}
