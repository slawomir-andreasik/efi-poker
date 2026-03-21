package com.andreasik.efipoker.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.auth0.enabled", havingValue = "true")
@RequiredArgsConstructor
public class Auth0SuccessHandler implements AuthenticationSuccessHandler {

  private final UserService userService;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final JwtProperties jwtProperties;
  private final AppProperties appProperties;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {

    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

    String sub = oAuth2User.getAttribute("sub");
    String email = oAuth2User.getAttribute("email");
    String name = oAuth2User.getAttribute("name");

    log.info(
        "OAuth2 login success for sub: {}...",
        sub != null ? sub.substring(0, Math.min(20, sub.length())) : "null");

    User user = userService.findOrCreateOAuth2User(sub, email, name);
    userService.updateLastLogin(user.username());

    String token = jwtService.generateToken(user);

    // Set refresh token as httpOnly cookie (default TTL, no "remember me" for OAuth2)
    String refreshToken = refreshTokenService.createRefreshToken(user.id(), false);
    ResponseCookie cookie =
        CookieHelper.createRefreshCookie(refreshToken, jwtProperties.refreshExpiration());
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    // Pass the display name so the frontend can pre-fill identity without asking the user
    String displayName = (name != null && !name.isBlank()) ? name : user.username();
    String encodedName = URLEncoder.encode(displayName, StandardCharsets.UTF_8);
    String redirectUrl =
        appProperties.url() + "/auth/callback?token=" + token + "&name=" + encodedName;

    response.sendRedirect(redirectUrl);
  }
}
