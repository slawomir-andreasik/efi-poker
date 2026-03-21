package com.andreasik.efipoker.auth;

import com.andreasik.efipoker.api.AuthApi;
import com.andreasik.efipoker.api.model.AuthConfigResponse;
import com.andreasik.efipoker.api.model.AuthResponse;
import com.andreasik.efipoker.api.model.ChangePasswordRequest;
import com.andreasik.efipoker.api.model.GuestTokenResponse;
import com.andreasik.efipoker.api.model.LoginRequest;
import com.andreasik.efipoker.api.model.RegisterRequest;
import com.andreasik.efipoker.api.model.UserResponse;
import com.andreasik.efipoker.shared.exception.AuthenticationFailedException;
import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import com.andreasik.efipoker.shared.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

  private final UserService userService;
  private final UserMapper userMapper;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final JwtProperties jwtProperties;
  private final PasswordEncoder passwordEncoder;
  private final Auth0Properties auth0Properties;
  private final LdapProperties ldapProperties;
  private final AppProperties appProperties;
  private final RegistrationProperties registrationProperties;
  private final Optional<LdapAuthService> ldapAuthService;

  @Override
  public ResponseEntity<AuthConfigResponse> getAuthConfig() {
    log.debug("GET /auth/config");
    return ResponseEntity.ok(
        new AuthConfigResponse()
            .auth0Enabled(auth0Properties.enabled())
            .registrationEnabled(registrationProperties.enabled())
            .ldapEnabled(ldapProperties.enabled()));
  }

  @Override
  public ResponseEntity<AuthResponse> register(RegisterRequest registerRequest) {
    log.debug("POST /auth/register username={}", registerRequest.getUsername());

    if (!registrationProperties.enabled()) {
      log.warn("Registration attempt while registration is disabled");
      throw new UnauthorizedException("Registration is disabled on this server");
    }

    User user =
        userService.registerLocalUser(
            registerRequest.getUsername(),
            registerRequest.getPassword(),
            registerRequest.getEmail());

    String token = jwtService.generateToken(user);
    Instant expiresAt = jwtService.getTokenExpiresAt();

    String refreshToken = refreshTokenService.createRefreshToken(user.id(), false);
    ResponseCookie cookie =
        CookieHelper.createRefreshCookie(refreshToken, jwtProperties.refreshExpiration());

    return ResponseEntity.status(HttpStatus.CREATED)
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(new AuthResponse().token(token).expiresAt(expiresAt));
  }

  @Override
  public ResponseEntity<AuthResponse> login(LoginRequest loginRequest) {
    log.debug("POST /auth/login username={}", loginRequest.getUsername());
    String username = loginRequest.getUsername();
    String password = loginRequest.getPassword();

    Optional<User> existingUser = userService.findByUsername(username);

    User user;
    if (existingUser.isPresent()) {
      User found = existingUser.get();
      if (AuthProvider.LDAP.name().equals(found.authProvider())) {
        // LDAP user - authenticate via LDAP only
        user = authenticateViaLdap(username, password);
      } else {
        // LOCAL/AUTH0 user - bcrypt check
        if (found.passwordHash() == null
            || !passwordEncoder.matches(password, found.passwordHash())) {
          throw new AuthenticationFailedException("Invalid credentials");
        }
        user = found;
      }
    } else if (ldapProperties.enabled() && ldapAuthService.isPresent()) {
      // User not found, LDAP enabled - try LDAP bind + provision
      user = authenticateViaLdap(username, password);
    } else {
      throw new AuthenticationFailedException("Invalid credentials");
    }

    userService.updateLastLogin(username);

    String token = jwtService.generateToken(user);
    Instant expiresAt = jwtService.getTokenExpiresAt();

    boolean rememberMe = Boolean.TRUE.equals(loginRequest.getRememberMe());
    String refreshToken = refreshTokenService.createRefreshToken(user.id(), rememberMe);
    long cookieTtl =
        rememberMe ? jwtProperties.refreshRememberExpiration() : jwtProperties.refreshExpiration();
    ResponseCookie cookie = CookieHelper.createRefreshCookie(refreshToken, cookieTtl);

    log.info(
        "User logged in: username={}, authProvider={}, rememberMe={}",
        username,
        user.authProvider(),
        rememberMe);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(new AuthResponse().token(token).expiresAt(expiresAt));
  }

  private User authenticateViaLdap(String username, String password) {
    LdapAuthService ldap =
        ldapAuthService.orElseThrow(() -> new AuthenticationFailedException("Invalid credentials"));

    LdapAuthService.LdapUserInfo ldapUser =
        ldap.authenticate(username, password)
            .orElseThrow(() -> new AuthenticationFailedException("Invalid credentials"));

    return userService.findOrCreateLdapUser(ldapUser.uid(), ldapUser.mail(), ldapUser.isAdmin());
  }

  @Override
  public ResponseEntity<Void> changePassword(ChangePasswordRequest changePasswordRequest) {
    log.debug("PUT /auth/me/password");
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    UUID userId = UUID.fromString(authentication.getName());

    userService.changePassword(
        userId, changePasswordRequest.getCurrentPassword(), changePasswordRequest.getNewPassword());

    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<AuthResponse> refreshToken() {
    log.debug("POST /auth/refresh");
    HttpServletRequest request =
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

    String rawToken = CookieHelper.extractCookie(request, CookieHelper.REFRESH_COOKIE_NAME);
    if (rawToken == null || rawToken.isBlank()) {
      throw new UnauthorizedException("No refresh token");
    }

    RefreshTokenService.RotationResult result = refreshTokenService.rotateRefreshToken(rawToken);
    String accessToken = jwtService.generateToken(result.user());
    Instant expiresAt = jwtService.getTokenExpiresAt();
    ResponseCookie cookie =
        CookieHelper.createRefreshCookie(result.newRawToken(), result.ttlSeconds());

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(new AuthResponse().token(accessToken).expiresAt(expiresAt));
  }

  @Override
  public ResponseEntity<GuestTokenResponse> refreshGuestToken() {
    log.debug("POST /auth/guest/refresh");
    if (!SecurityUtils.isGuestToken()) {
      throw new UnauthorizedException("Guest token required");
    }

    Jwt currentToken = SecurityUtils.getCurrentJwt();
    if (currentToken == null) {
      throw new UnauthorizedException("Invalid token");
    }

    String token = jwtService.refreshGuestToken(currentToken);
    Instant expiresAt = jwtService.getGuestTokenExpiresAt();

    return ResponseEntity.ok(new GuestTokenResponse().token(token).expiresAt(expiresAt));
  }

  // OAuth2 endpoints are handled by Spring Security's oauth2Login() filter.
  // These stub implementations satisfy the generated AuthApi interface contract.
  // The actual redirect logic is performed by Auth0SuccessHandler.

  @Override
  public ResponseEntity<Void> oauth2Authorize() {
    // Intercepted by Spring Security before reaching this controller
    return ResponseEntity.status(302).build();
  }

  @Override
  public ResponseEntity<Void> oauth2Callback(
      @Nullable String code, @Nullable String state, @Nullable String error) {
    // Intercepted by Spring Security before reaching this controller
    return ResponseEntity.status(302).build();
  }

  @Override
  public ResponseEntity<UserResponse> getCurrentUser() {
    log.debug("GET /auth/me");
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    UUID userId = UUID.fromString(authentication.getName());

    User user =
        userService.findById(userId).orElseThrow(() -> new UnauthorizedException("User not found"));

    return ResponseEntity.ok(userMapper.toResponse(user));
  }

  @Override
  public ResponseEntity<Void> logout() {
    log.debug("GET /auth/logout");
    HttpServletRequest request =
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }

    // Revoke refresh tokens for the authenticated user
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getName() != null && !SecurityUtils.isGuestToken()) {
      try {
        refreshTokenService.revokeAllForUser(UUID.fromString(auth.getName()));
      } catch (Exception e) {
        log.debug("Could not revoke refresh tokens on logout: {}", e.getMessage());
      }
    }

    String redirectUrl;
    if (auth0Properties.enabled()) {
      String returnTo = URLEncoder.encode(appProperties.url(), StandardCharsets.UTF_8);
      redirectUrl =
          "https://"
              + auth0Properties.domain()
              + "/v2/logout?client_id="
              + auth0Properties.clientId()
              + "&returnTo="
              + returnTo;
      log.info("Federated logout: redirecting to Auth0");
    } else {
      redirectUrl = appProperties.url();
      log.info("Local logout: redirecting to frontend");
    }

    ResponseCookie clearCookie = CookieHelper.clearRefreshCookie();
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
        .header(HttpHeaders.LOCATION, redirectUrl)
        .build();
  }
}
