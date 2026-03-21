package com.andreasik.efipoker.shared.test;

import com.andreasik.efipoker.auth.JwtService;
import com.andreasik.efipoker.participant.ParticipantEntity;
import com.andreasik.efipoker.project.ProjectEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/// Test helper for generating guest JWT tokens in integration tests.
/// Inject via @Autowired in test classes extending BaseComponentTest.
@Component
public class TestJwtHelper {

  @Autowired private JwtService jwtService;

  /// Generate a guest admin JWT for a project (no participant).
  public String guestAdminJwt(ProjectEntity project) {
    return jwtService.generateGuestToken(project.getId(), null, true, null);
  }

  /// Generate a guest participant JWT (non-admin).
  public String guestParticipantJwt(ProjectEntity project, ParticipantEntity participant) {
    return jwtService.generateGuestToken(
        project.getId(), participant.getId(), false, participant.getNickname());
  }

  /// Generate a guest admin+participant JWT.
  public String guestAdminParticipantJwt(ProjectEntity project, ParticipantEntity participant) {
    return jwtService.generateGuestToken(
        project.getId(), participant.getId(), true, participant.getNickname());
  }
}
