package com.andreasik.efipoker.project;

import com.andreasik.efipoker.api.ProjectsApi;
import com.andreasik.efipoker.api.model.CreateProjectRequest;
import com.andreasik.efipoker.api.model.ProjectAdminResponse;
import com.andreasik.efipoker.api.model.ProjectResponse;
import com.andreasik.efipoker.api.model.UpdateProjectRequest;
import com.andreasik.efipoker.shared.security.SecurityUtils;
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

  @Override
  public ResponseEntity<ProjectAdminResponse> createProject(
      CreateProjectRequest createProjectRequest) {
    log.debug("POST /projects name={}", createProjectRequest.getName());
    UUID ownerId = SecurityUtils.getCurrentUserId();
    Project project = projectService.createProject(createProjectRequest.getName(), ownerId);
    return ResponseEntity.status(HttpStatus.CREATED).body(projectMapper.toAdminResponse(project));
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
  public ResponseEntity<ProjectAdminResponse> getProjectAdmin(String slug, String xAdminCode) {
    log.debug("GET /projects/{}/admin", slug);
    Project project =
        projectService.validateAdminAccess(slug, xAdminCode, SecurityUtils.getCurrentUserId());
    return ResponseEntity.ok(projectMapper.toAdminResponse(project));
  }

  @Override
  public ResponseEntity<Void> deleteProject(String slug, String xAdminCode) {
    log.debug("DELETE /projects/{}", slug);
    Project validated =
        projectService.validateAdminAccess(slug, xAdminCode, SecurityUtils.getCurrentUserId());
    projectService.deleteProject(validated.id());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<ProjectAdminResponse> updateProject(
      String slug, UpdateProjectRequest updateProjectRequest, String xAdminCode) {
    log.debug("PATCH /projects/{} name={}", slug, updateProjectRequest.getName());
    Project validated =
        projectService.validateAdminAccess(slug, xAdminCode, SecurityUtils.getCurrentUserId());
    Project updated = projectService.updateProject(validated.id(), updateProjectRequest.getName());
    return ResponseEntity.ok(projectMapper.toAdminResponse(updated));
  }
}
