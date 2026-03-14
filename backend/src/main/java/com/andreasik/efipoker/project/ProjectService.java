package com.andreasik.efipoker.project;

import com.andreasik.efipoker.auth.UserEntity;
import com.andreasik.efipoker.shared.event.ProjectCreatedEvent;
import com.andreasik.efipoker.shared.exception.ResourceNotFoundException;
import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Transactional(readOnly = true)
public class ProjectService implements ProjectApi {

  private static final String SLUG_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
  private static final int SLUG_LENGTH = 8;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final ProjectRepository projectRepository;
  private final ProjectEntityMapper projectEntityMapper;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public Project createProject(String name) {
    return createProject(name, null);
  }

  @Transactional
  public Project createProject(String name, UUID ownerId) {
    String slug = generateSlug();
    String adminCode = UUID.randomUUID().toString();

    ProjectEntity.ProjectEntityBuilder builder =
        ProjectEntity.builder().name(name).slug(slug).adminCode(adminCode);

    if (ownerId != null) {
      builder.createdBy(UserEntity.builder().id(ownerId).build());
    }

    ProjectEntity saved = projectRepository.save(builder.build());
    log.info(
        "Project created: slug={}, name={}, ownerId={}", saved.getSlug(), saved.getName(), ownerId);
    eventPublisher.publishEvent(new ProjectCreatedEvent(saved.getId()));
    return projectEntityMapper.toDomain(saved);
  }

  public List<Project> listProjectsByOwner(UUID ownerId) {
    log.debug("listProjectsByOwner: ownerId={}", ownerId);
    UserEntity owner = UserEntity.builder().id(ownerId).build();
    return projectEntityMapper.toDomainList(projectRepository.findByOwner(owner));
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

  public Project validateAdminCode(String slug, String adminCode) {
    return validateAdminAccess(slug, adminCode, null);
  }

  public Project validateAdminAccess(String slug, String adminCode, UUID currentUserId) {
    ProjectEntity entity =
        projectRepository
            .findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Project", slug));
    if (isSiteAdmin()
        || isOwner(entity, currentUserId)
        || adminCodeMatches(entity.getAdminCode(), adminCode)) {
      return projectEntityMapper.toDomain(entity);
    }
    log.warn("Invalid admin access for project: slug={}", slug);
    throw new UnauthorizedException("Invalid admin code for project: " + slug);
  }

  public void validateAdminCodeForProject(UUID projectId, String adminCode) {
    validateAdminCodeForProject(projectId, adminCode, null);
  }

  public void validateAdminCodeForProject(UUID projectId, String adminCode, UUID currentUserId) {
    ProjectEntity entity =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
    if (isSiteAdmin()
        || isOwner(entity, currentUserId)
        || adminCodeMatches(entity.getAdminCode(), adminCode)) {
      return;
    }
    log.warn("Invalid admin code for project: id={}", projectId);
    throw new UnauthorizedException("Invalid admin code");
  }

  private boolean isSiteAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null
        && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
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

  private boolean adminCodeMatches(String stored, String provided) {
    if (provided == null) {
      return false;
    }
    byte[] a = stored.getBytes(StandardCharsets.UTF_8);
    byte[] b = provided.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(a, b);
  }

  private String generateSlug() {
    StringBuilder sb = new StringBuilder(SLUG_LENGTH);
    for (int i = 0; i < SLUG_LENGTH; i++) {
      sb.append(SLUG_CHARS.charAt(RANDOM.nextInt(SLUG_CHARS.length())));
    }
    return sb.toString();
  }
}
