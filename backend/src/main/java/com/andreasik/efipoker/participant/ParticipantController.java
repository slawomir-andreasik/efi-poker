package com.andreasik.efipoker.participant;

import com.andreasik.efipoker.api.ParticipantsApi;
import com.andreasik.efipoker.api.model.JoinProjectRequest;
import com.andreasik.efipoker.api.model.ParticipantResponse;
import com.andreasik.efipoker.api.model.UpdateParticipantRequest;
import com.andreasik.efipoker.auth.JwtService;
import com.andreasik.efipoker.project.Project;
import com.andreasik.efipoker.project.ProjectService;
import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import com.andreasik.efipoker.shared.security.SecurityUtils;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ParticipantController implements ParticipantsApi {

  private final ParticipantService participantService;
  private final ProjectService projectService;
  private final ParticipantMapper participantMapper;
  private final JwtService jwtService;

  @Override
  public ResponseEntity<ParticipantResponse> joinProject(
      String slug, JoinProjectRequest joinProjectRequest) {
    log.debug("POST /projects/{}/participants nickname={}", slug, joinProjectRequest.getNickname());
    Project project = projectService.getProjectBySlug(slug);
    UUID userId = SecurityUtils.getCurrentUserId();
    Participant participant =
        participantService.joinProject(
            project.id(), joinProjectRequest.getNickname(), userId, joinProjectRequest.getRoomId());

    ParticipantResponse response = participantMapper.toResponse(participant);

    // For unauthenticated (guest) callers, generate a guest JWT
    if (userId == null) {
      boolean isAdmin = SecurityUtils.isGuestToken() && SecurityUtils.isGuestAdmin();
      String token =
          jwtService.generateGuestToken(
              project.id(), participant.id(), isAdmin, participant.nickname());
      response.token(token).tokenExpiresAt(jwtService.getGuestTokenExpiresAt());
    }

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<List<ParticipantResponse>> listProjectParticipants(String slug) {
    log.debug("GET /projects/{}/participants", slug);
    Project project = projectService.validateAdminAccessBySlug(slug);
    List<Participant> participants = participantService.listAllParticipants(project.id());
    return ResponseEntity.ok(participantMapper.toResponseList(participants));
  }

  @Override
  public ResponseEntity<ParticipantResponse> getParticipant(String slug, UUID participantId) {
    log.debug("GET /projects/{}/participants/{}", slug, participantId);
    Project project = projectService.getProjectBySlug(slug);
    Participant participant = participantService.getParticipant(project.id(), participantId);
    return ResponseEntity.ok(participantMapper.toResponse(participant));
  }

  @Override
  public ResponseEntity<ParticipantResponse> updateParticipant(
      String slug, UUID participantId, UpdateParticipantRequest updateParticipantRequest) {
    log.debug("PATCH /projects/{}/participants/{}", slug, participantId);
    Project project = projectService.getProjectBySlug(slug);
    UUID tokenParticipantId = SecurityUtils.getCurrentParticipantId();
    if (tokenParticipantId == null) {
      UUID userId = SecurityUtils.getCurrentUserId();
      if (userId != null) {
        tokenParticipantId =
            participantService.findParticipantIdByProjectAndUser(project.id(), userId);
      }
    }
    if (tokenParticipantId == null || !tokenParticipantId.equals(participantId)) {
      throw new UnauthorizedException("You can only update your own participant profile");
    }
    Participant updated =
        participantService.updateNickname(
            project.id(), participantId, updateParticipantRequest.getNickname());
    return ResponseEntity.ok(participantMapper.toResponse(updated));
  }

  @Override
  public ResponseEntity<ParticipantResponse> getMyParticipant(String slug) {
    log.debug("GET /projects/{}/participants/me", slug);
    UUID userId = SecurityUtils.getCurrentUserId();
    if (userId == null) {
      throw new UnauthorizedException("Authentication required");
    }
    Project project = projectService.getProjectBySlug(slug);
    Participant participant =
        participantService.getParticipantByProjectAndUser(project.id(), userId);
    return ResponseEntity.ok(participantMapper.toResponse(participant));
  }

  @Override
  public ResponseEntity<Void> deleteParticipant(String slug, UUID participantId) {
    log.debug("DELETE /projects/{}/participants/{}", slug, participantId);
    Project project = projectService.validateAdminAccessBySlug(slug);
    participantService.deleteParticipant(project.id(), participantId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<ParticipantResponse> archiveParticipant(String slug, UUID participantId) {
    log.debug("POST /projects/{}/participants/{}/archive", slug, participantId);
    Project project = projectService.validateAdminAccessBySlug(slug);
    Participant archived = participantService.archiveParticipant(project.id(), participantId);
    return ResponseEntity.ok(participantMapper.toResponse(archived));
  }

  @Override
  public ResponseEntity<ParticipantResponse> unarchiveParticipant(String slug, UUID participantId) {
    log.debug("POST /projects/{}/participants/{}/unarchive", slug, participantId);
    Project project = projectService.validateAdminAccessBySlug(slug);
    Participant unarchived = participantService.unarchiveParticipant(project.id(), participantId);
    return ResponseEntity.ok(participantMapper.toResponse(unarchived));
  }
}
