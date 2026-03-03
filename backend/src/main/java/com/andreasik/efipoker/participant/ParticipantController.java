package com.andreasik.efipoker.participant;

import com.andreasik.efipoker.api.ParticipantsApi;
import com.andreasik.efipoker.api.model.JoinProjectRequest;
import com.andreasik.efipoker.api.model.ParticipantResponse;
import com.andreasik.efipoker.api.model.UpdateParticipantRequest;
import com.andreasik.efipoker.project.Project;
import com.andreasik.efipoker.project.ProjectService;
import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ParticipantController implements ParticipantsApi {

  private final ParticipantService participantService;
  private final ProjectService projectService;
  private final ParticipantMapper participantMapper;

  @Override
  public ResponseEntity<ParticipantResponse> joinProject(
      String slug, JoinProjectRequest joinProjectRequest) {
    log.debug("POST /projects/{}/participants nickname={}", slug, joinProjectRequest.getNickname());
    Project project = projectService.getProjectBySlug(slug);
    Participant participant =
        participantService.joinProject(
            project.id(),
            joinProjectRequest.getNickname(),
            getCurrentUserId(),
            joinProjectRequest.getRoomId());
    return ResponseEntity.ok(participantMapper.toResponse(participant));
  }

  @Override
  public ResponseEntity<List<ParticipantResponse>> listProjectParticipants(
      String slug, String xAdminCode) {
    log.debug("GET /projects/{}/participants", slug);
    Project project = projectService.validateAdminCode(slug, xAdminCode);
    List<Participant> participants = participantService.listParticipants(project.id());
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
      String slug,
      UUID participantId,
      UpdateParticipantRequest updateParticipantRequest,
      UUID xParticipantId) {
    log.debug("PATCH /projects/{}/participants/{}", slug, participantId);
    if (xParticipantId == null || !xParticipantId.equals(participantId)) {
      throw new UnauthorizedException("You can only update your own participant profile");
    }
    Project project = projectService.getProjectBySlug(slug);
    Participant updated =
        participantService.updateNickname(
            project.id(), participantId, updateParticipantRequest.getNickname());
    return ResponseEntity.ok(participantMapper.toResponse(updated));
  }

  @Override
  public ResponseEntity<Void> deleteParticipant(
      String slug, UUID participantId, String xAdminCode) {
    log.debug("DELETE /projects/{}/participants/{}", slug, participantId);
    Project project = projectService.validateAdminCode(slug, xAdminCode);
    participantService.deleteParticipant(project.id(), participantId);
    return ResponseEntity.noContent().build();
  }

  private UUID getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken) {
      return UUID.fromString(auth.getName());
    }
    return null;
  }
}
