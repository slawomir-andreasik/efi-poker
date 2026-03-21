package com.andreasik.efipoker.project;

import com.andreasik.efipoker.auth.UserEntity;
import com.andreasik.efipoker.shared.event.ProjectCreatedEvent;
import com.andreasik.efipoker.shared.exception.ResourceNotFoundException;
import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import com.andreasik.efipoker.shared.security.SecurityUtils;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ProjectService implements ProjectApi {

  private static final String SLUG_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
  private static final int SLUG_LENGTH = 8;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final ProjectRepository projectRepository;
  private final ProjectEntityMapper projectEntityMapper;
  private final ApplicationEventPublisher eventPublisher;
  private final PasswordEncoder adminCodeEncoder;

  ProjectService(
      ProjectRepository projectRepository,
      ProjectEntityMapper projectEntityMapper,
      ApplicationEventPublisher eventPublisher,
      @Qualifier("adminCodeEncoder") PasswordEncoder adminCodeEncoder) {
    this.projectRepository = projectRepository;
    this.projectEntityMapper = projectEntityMapper;
    this.eventPublisher = eventPublisher;
    this.adminCodeEncoder = adminCodeEncoder;
  }

  @Transactional
  public Project createProject(String name) {
    return createProject(name, null);
  }

  @Transactional
  public Project createProject(String name, UUID ownerId) {
    String slug = generateSlug();
    String rawAdminCode = UUID.randomUUID().toString();
    String adminCode = adminCodeEncoder.encode(rawAdminCode);

    ProjectEntity.ProjectEntityBuilder builder =
        ProjectEntity.builder().name(name).slug(slug).adminCode(adminCode);

    if (ownerId != null) {
      builder.createdBy(UserEntity.builder().id(ownerId).build());
    }

    ProjectEntity saved = projectRepository.save(builder.build());
    log.info(
        "Project created: slug={}, name={}, ownerId={}", saved.getSlug(), saved.getName(), ownerId);
    eventPublisher.publishEvent(new ProjectCreatedEvent(saved.getId()));
    Project project = projectEntityMapper.toDomain(saved);
    return project.toBuilder().adminCode(rawAdminCode).build(); // return raw code to creator
  }

  public List<Project> listProjectsByOwner(UUID ownerId) {
    log.debug("listProjectsByOwner: ownerId={}", ownerId);
    return projectEntityMapper.toDomainList(projectRepository.findByOwnerId(ownerId));
  }

  public Project getProjectBySlug(String slug) {
    log.debug("getProjectBySlug: slug={}", slug);
    ProjectEntity entity =
        projectRepository
            .findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Project", slug));
    return projectEntityMapper.toDomain(entity);
  }

  public Project getProjectById(UUID id) {
    ProjectEntity entity =
        projectRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    return projectEntityMapper.toDomain(entity);
  }

  @Override
  public void validateAdminCodeBySlug(String slug, String adminCode) {
    validateAdminAccess(slug, adminCode, null);
  }

  public Project validateAdminCode(String slug, String adminCode) {
    return validateAdminAccess(slug, adminCode, null);
  }

  public Project validateAdminAccess(String slug, String adminCode, UUID currentUserId) {
    ProjectEntity entity =
        projectRepository
            .findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Project", slug));
    if (SecurityUtils.isSiteAdmin()
        || isOwner(entity, currentUserId)
        || adminCodeMatches(entity.getAdminCode(), adminCode)) {
      return projectEntityMapper.toDomain(entity);
    }
    log.warn("Invalid admin access for project: slug={}", slug);
    throw new UnauthorizedException("Invalid admin code for project: " + slug);
  }

  @Override
  public UUID validateAdminCodeAndGetProjectId(String slug, String adminCode, UUID currentUserId) {
    return validateAdminAccess(slug, adminCode, currentUserId).id();
  }

  public void validateAdminCodeForProject(UUID projectId, String adminCode) {
    validateAdminCodeForProject(projectId, adminCode, null);
  }

  public void validateAdminCodeForProject(UUID projectId, String adminCode, UUID currentUserId) {
    ProjectEntity entity =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
    if (SecurityUtils.isSiteAdmin()
        || isOwner(entity, currentUserId)
        || adminCodeMatches(entity.getAdminCode(), adminCode)) {
      return;
    }
    log.warn("Invalid admin code for project: id={}", projectId);
    throw new UnauthorizedException("Invalid admin code");
  }

  private boolean isOwner(ProjectEntity entity, UUID userId) {
    return userId != null
        && entity.getCreatedBy() != null
        && userId.equals(entity.getCreatedBy().getId());
  }

  @Transactional
  public Project updateProject(UUID projectId, String name) {
    ProjectEntity entity =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
    if (name != null) {
      entity.setName(name);
    }
    ProjectEntity saved = projectRepository.save(entity);
    log.info("Project updated: id={}, slug={}", projectId, saved.getSlug());
    return projectEntityMapper.toDomain(saved);
  }

  @Transactional
  public void deleteProject(UUID projectId) {
    ProjectEntity entity =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
    projectRepository.delete(entity);
    log.info("Project deleted: id={}, slug={}", projectId, entity.getSlug());
  }

  public List<Project> getProjectsByIds(List<UUID> ids) {
    return projectEntityMapper.toDomainList(projectRepository.findByIdIn(ids));
  }

  public List<Project> getParticipatedProjects(UUID userId) {
    List<UUID> projectIds = projectRepository.findParticipatedProjectIds(userId);
    if (projectIds.isEmpty()) {
      return List.of();
    }
    return projectEntityMapper.toDomainList(projectRepository.findByIdIn(projectIds));
  }

  @Override
  public void validateProjectExists(UUID projectId) {
    if (!projectRepository.existsById(projectId)) {
      throw new ResourceNotFoundException("Project", projectId);
    }
  }

  /// JWT-aware admin validation by slug. Checks: site admin, project owner, or guest admin JWT.
  public Project validateAdminAccessBySlug(String slug) {
    ProjectEntity entity =
        projectRepository
            .findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Project", slug));
    validateAdminAccessInternal(entity);
    return projectEntityMapper.toDomain(entity);
  }

  @Override
  public void validateAdminAccessForProject(UUID projectId) {
    ProjectEntity entity =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
    validateAdminAccessInternal(entity);
  }

  private void validateAdminAccessInternal(ProjectEntity entity) {
    if (SecurityUtils.isSiteAdmin()) {
      return;
    }
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    if (isOwner(entity, currentUserId)) {
      return;
    }
    if (SecurityUtils.isGuestAdmin()) {
      UUID tokenProjectId = SecurityUtils.getProjectIdFromToken();
      if (tokenProjectId != null && tokenProjectId.equals(entity.getId())) {
        return;
      }
    }
    log.warn("Admin access denied for project: id={}", entity.getId());
    throw new UnauthorizedException("Admin access required");
  }

  private boolean adminCodeMatches(String stored, String provided) {
    if (provided == null) {
      return false;
    }
    return adminCodeEncoder.matches(provided, stored);
  }

  private String generateSlug() {
    StringBuilder sb = new StringBuilder(SLUG_LENGTH);
    for (int i = 0; i < SLUG_LENGTH; i++) {
      sb.append(SLUG_CHARS.charAt(RANDOM.nextInt(SLUG_CHARS.length())));
    }
    return sb.toString();
  }
}
