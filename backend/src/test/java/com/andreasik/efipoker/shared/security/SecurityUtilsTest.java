package com.andreasik.efipoker.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.andreasik.efipoker.auth.JwtService;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@DisplayName("SecurityUtils")
class SecurityUtilsTest extends BaseUnitTest {

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  private static void setUserJwt(UUID userId, String role) {
    setJwtAuth(Map.of("sub", userId.toString(), "role", role), "ROLE_" + role);
  }

  private static void setGuestJwt(UUID projectId, boolean admin) {
    setJwtAuth(
        Map.of(
            "sub",
            UUID.randomUUID().toString(),
            JwtService.CLAIM_TYPE,
            JwtService.TOKEN_TYPE_GUEST,
            JwtService.CLAIM_PROJECT_ID,
            projectId.toString(),
            JwtService.CLAIM_ADMIN,
            admin),
        admin ? "ROLE_PROJECT_ADMIN" : "ROLE_GUEST");
  }

  private static void setGuestJwtWithParticipant(
      UUID projectId, UUID participantId, boolean admin) {
    setJwtAuth(
        Map.of(
            "sub",
            participantId.toString(),
            JwtService.CLAIM_TYPE,
            JwtService.TOKEN_TYPE_GUEST,
            JwtService.CLAIM_PROJECT_ID,
            projectId.toString(),
            JwtService.CLAIM_ADMIN,
            admin,
            JwtService.CLAIM_PARTICIPANT_ID,
            participantId.toString()),
        admin ? "ROLE_PROJECT_ADMIN" : "ROLE_GUEST");
  }

  private static void setJwtAuth(Map<String, Object> claims, String... roles) {
    Jwt jwt =
        Jwt.withTokenValue("test-token")
            .header("alg", "HS512")
            .claims(c -> c.putAll(claims))
            .build();
    List<SimpleGrantedAuthority> authorities =
        Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, authorities);
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @Nested
  @DisplayName("getCurrentUserId")
  class GetCurrentUserId {

    @Test
    void should_return_user_id_for_user_jwt() {
      UUID userId = UUID.randomUUID();
      setUserJwt(userId, "USER");
      assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(userId);
    }

    @Test
    void should_return_null_for_guest_jwt() {
      setGuestJwt(UUID.randomUUID(), false);
      assertThat(SecurityUtils.getCurrentUserId()).isNull();
    }

    @Test
    void should_return_null_when_not_authenticated() {
      assertThat(SecurityUtils.getCurrentUserId()).isNull();
    }

    @Test
    void should_return_null_for_malformed_uuid_in_subject() {
      setJwtAuth(Map.of("sub", "not-a-uuid", "role", "USER"), "ROLE_USER");
      assertThat(SecurityUtils.getCurrentUserId()).isNull();
    }
  }

  @Nested
  @DisplayName("getCurrentParticipantId")
  class GetCurrentParticipantId {

    @Test
    void should_return_participant_id_from_guest_jwt() {
      UUID participantId = UUID.randomUUID();
      setGuestJwtWithParticipant(UUID.randomUUID(), participantId, false);
      assertThat(SecurityUtils.getCurrentParticipantId()).isEqualTo(participantId);
    }

    @Test
    void should_return_null_for_guest_jwt_without_participant() {
      setGuestJwt(UUID.randomUUID(), true);
      assertThat(SecurityUtils.getCurrentParticipantId()).isNull();
    }

    @Test
    void should_return_null_for_user_jwt() {
      setUserJwt(UUID.randomUUID(), "USER");
      assertThat(SecurityUtils.getCurrentParticipantId()).isNull();
    }

    @Test
    void should_return_null_for_malformed_participant_id() {
      setJwtAuth(
          Map.of(
              "sub",
              UUID.randomUUID().toString(),
              JwtService.CLAIM_TYPE,
              JwtService.TOKEN_TYPE_GUEST,
              JwtService.CLAIM_PROJECT_ID,
              UUID.randomUUID().toString(),
              JwtService.CLAIM_ADMIN,
              false,
              JwtService.CLAIM_PARTICIPANT_ID,
              "not-a-uuid"),
          "ROLE_GUEST");
      assertThat(SecurityUtils.getCurrentParticipantId()).isNull();
    }
  }

  @Nested
  @DisplayName("getProjectIdFromToken")
  class GetProjectIdFromToken {

    @Test
    void should_return_project_id_from_guest_jwt() {
      UUID projectId = UUID.randomUUID();
      setGuestJwt(projectId, false);
      assertThat(SecurityUtils.getProjectIdFromToken()).isEqualTo(projectId);
    }

    @Test
    void should_return_null_for_user_jwt() {
      setUserJwt(UUID.randomUUID(), "USER");
      assertThat(SecurityUtils.getProjectIdFromToken()).isNull();
    }

    @Test
    void should_return_null_for_malformed_project_id() {
      setJwtAuth(
          Map.of(
              "sub",
              UUID.randomUUID().toString(),
              JwtService.CLAIM_TYPE,
              JwtService.TOKEN_TYPE_GUEST,
              JwtService.CLAIM_PROJECT_ID,
              "not-a-uuid",
              JwtService.CLAIM_ADMIN,
              false),
          "ROLE_GUEST");
      assertThat(SecurityUtils.getProjectIdFromToken()).isNull();
    }
  }

  @Nested
  @DisplayName("isGuestToken")
  class IsGuestToken {

    @Test
    void should_return_true_for_guest_jwt() {
      setGuestJwt(UUID.randomUUID(), false);
      assertThat(SecurityUtils.isGuestToken()).isTrue();
    }

    @Test
    void should_return_false_for_user_jwt() {
      setUserJwt(UUID.randomUUID(), "USER");
      assertThat(SecurityUtils.isGuestToken()).isFalse();
    }

    @Test
    void should_return_false_when_not_authenticated() {
      assertThat(SecurityUtils.isGuestToken()).isFalse();
    }
  }

  @Nested
  @DisplayName("isGuestAdmin")
  class IsGuestAdmin {

    @Test
    void should_return_true_for_guest_admin_jwt() {
      setGuestJwt(UUID.randomUUID(), true);
      assertThat(SecurityUtils.isGuestAdmin()).isTrue();
    }

    @Test
    void should_return_false_for_guest_non_admin_jwt() {
      setGuestJwt(UUID.randomUUID(), false);
      assertThat(SecurityUtils.isGuestAdmin()).isFalse();
    }

    @Test
    void should_return_false_when_not_authenticated() {
      assertThat(SecurityUtils.isGuestAdmin()).isFalse();
    }
  }

  @Nested
  @DisplayName("isSiteAdmin")
  class IsSiteAdmin {

    @Test
    void should_return_true_for_site_admin() {
      setUserJwt(UUID.randomUUID(), "ADMIN");
      assertThat(SecurityUtils.isSiteAdmin()).isTrue();
    }

    @Test
    void should_return_false_for_regular_user() {
      setUserJwt(UUID.randomUUID(), "USER");
      assertThat(SecurityUtils.isSiteAdmin()).isFalse();
    }

    @Test
    void should_return_false_when_not_authenticated() {
      assertThat(SecurityUtils.isSiteAdmin()).isFalse();
    }
  }
}
