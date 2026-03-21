package com.andreasik.efipoker.project;

import com.andreasik.efipoker.api.ProjectsApi;
import com.andreasik.efipoker.api.model.AdminCodeExchangeRequest;
import com.andreasik.efipoker.api.model.CreateProjectRequest;
import com.andreasik.efipoker.api.model.GuestTokenResponse;
import com.andreasik.efipoker.api.model.ProjectAdminResponse;
import com.andreasik.efipoker.api.model.ProjectResponse;
import com.andreasik.efipoker.api.model.UpdateProjectRequest;
import com.andreasik.efipoker.auth.JwtService;
import com.andreasik.efipoker.shared.security.SecurityUtils;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ProjectController implements ProjectsApi {

  private final ProjectService projectService;
  private final ProjectMapper projectMapper;
  private final JwtService jwtService;

  @Override
  public ResponseEntity<ProjectAdminResponse> createProject(
      CreateProjectRequest createProjectRequest) {
    log.debug("POST /projects name={}", createProjectRequest.getName());
    UUID ownerId = SecurityUtils.getCurrentUserId();
    Project project = projectService.createProject(createProjectRequest.getName(), ownerId);

    ProjectAdminResponse response = projectMapper.toAdminResponse(project);

    // For unauthenticated (guest) callers, generate a guest admin JWT
    if (ownerId == null) {
      String token = jwtService.generateGuestToken(project.id(), null, true, null);
      response.token(token).tokenExpiresAt(jwtService.getGuestTokenExpiresAt());
    }

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  public ResponseEntity<List<ProjectAdminResponse>> getMyProjects() {
    log.debug("GET /auth/me/projects");
    UUID userId = SecurityUtils.getCurrentUserId();
    return ResponseEntity.ok(
        projectMapper.toAdminResponseList(projectService.listProjectsByOwner(userId)));
  }

  @Override
  public ResponseEntity<List<ProjectResponse>> getMyParticipatedProjects() {
    log.debug("GET /auth/me/participated-projects");
    UUID userId = SecurityUtils.getCurrentUserId();
    List<Project> projects = projectService.getParticipatedProjects(userId);
    return ResponseEntity.ok(projectMapper.toResponseList(projects));
  }

  @Override
  public ResponseEntity<ProjectResponse> getProjectBySlug(String slug) {
    log.debug("GET /projects/{}", slug);
    Project project = projectService.getProjectBySlug(slug);
    return ResponseEntity.ok(projectMapper.toResponse(project));
  }

  @Override
  public ResponseEntity<ProjectAdminResponse> getProjectAdmin(String slug) {
    log.debug("GET /projects/{}/admin", slug);
    Project project = projectService.validateAdminAccessBySlug(slug);
    return ResponseEntity.ok(projectMapper.toAdminResponse(project));
  }

  @Override
  public ResponseEntity<Void> deleteProject(String slug) {
    log.debug("DELETE /projects/{}", slug);
    Project validated = projectService.validateAdminAccessBySlug(slug);
    projectService.deleteProject(validated.id());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<ProjectAdminResponse> updateProject(
      String slug, UpdateProjectRequest updateProjectRequest) {
    log.debug("PATCH /projects/{} name={}", slug, updateProjectRequest.getName());
    Project validated = projectService.validateAdminAccessBySlug(slug);
    Project updated = projectService.updateProject(validated.id(), updateProjectRequest.getName());
    return ResponseEntity.ok(projectMapper.toAdminResponse(updated));
  }

  @Override
  public ResponseEntity<GuestTokenResponse> exchangeAdminCode(
      AdminCodeExchangeRequest adminCodeExchangeRequest) {
    log.debug("POST /auth/guest/admin-exchange slug={}", adminCodeExchangeRequest.getSlug());
    String slug = adminCodeExchangeRequest.getSlug();
    String adminCode = adminCodeExchangeRequest.getAdminCode();

    // Validate admin code against the project - throws if invalid
    Project project =
        projectService.validateAdminAccess(slug, adminCode, SecurityUtils.getCurrentUserId());

    // Preserve participantId from current guest JWT if present
    UUID participantId = SecurityUtils.getCurrentParticipantId();
    String nickname = null;
    var currentJwt = SecurityUtils.getCurrentJwt();
    if (currentJwt != null && SecurityUtils.isGuestToken()) {
      nickname = currentJwt.getClaimAsString(JwtService.CLAIM_NICKNAME);
    }

    String token = jwtService.generateGuestToken(project.id(), participantId, true, nickname);
    Instant expiresAt = jwtService.getGuestTokenExpiresAt();

    log.info("Admin code exchanged for guest admin JWT: slug={}", slug);
    return ResponseEntity.ok(new GuestTokenResponse().token(token).expiresAt(expiresAt));
  }
}
