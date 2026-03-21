package com.andreasik.efipoker.project;

import java.util.UUID;

/// Module API for project operations. Used by other modules to avoid direct repository access.
public interface ProjectApi {

  void validateProjectExists(UUID projectId);

  void validateAdminCodeForProject(UUID projectId, String adminCode);

  void validateAdminCodeBySlug(String slug, String adminCode);

  /// Validate admin access for a project using JWT-based auth.
  /// Checks: site admin, project owner, or guest admin JWT with matching projectId.
  void validateAdminAccessForProject(UUID projectId);

  /// Validate admin code and return the project ID. Used by auth module for admin code exchange.
  UUID validateAdminCodeAndGetProjectId(String slug, String adminCode, UUID currentUserId);
}
